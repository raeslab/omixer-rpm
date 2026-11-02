package io.omixer.rpm.inference.scoring;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import io.omixer.rpm.database.CompiledModule;

/**
 * Scores a module in one profile: the path (one alternative per step) with the highest coverage, then the highest
 * abundance — the first one found on ties. Stateless and thread safe.
 *
 * Two ways to find that path, with the same result (REFACTORING.md, "Scoring"):
 * <ul>
 * <li>per step, when every ortholog value of the module is finite and ≥ 0: each step takes its best observed
 * alternative. Coverage is decided step by step and every estimator is monotone in each step value, so no
 * combination can do better;</li>
 * <li>otherwise (negative, NaN or infinite values) every combination in turn, as the estimators' zero rule and NaN
 * comparisons can then favour a path that is not the best in every step.</li>
 * </ul>
 */
public final class ModuleScorer {

	private final ScoringOptions options;

	public ModuleScorer(ScoringOptions options) {
		this.options = Objects.requireNonNull(options, "options");
	}

	/**
	 * @param values the values of one profile, indexed by the ortholog indexes of the module's database (0 when not
	 *               observed)
	 * @return the coverage and abundance of the module's best path
	 */
	public ModuleScore score(CompiledModule module, double[] values) {
		return allFiniteAndNonNegative(module, values) ? scorePerStep(module, values) : scoreAllPaths(module, values);
	}

	/**
	 * Each step takes its observed alternative with the highest value (min: the highest smallest present ortholog), the
	 * first one on ties. A step with no observed alternative keeps alternative 0, which is unobserved too.
	 */
	ModuleScore scorePerStep(CompiledModule module, double[] values) {
		// path[step] = the chosen alternative
		int[] path = new int[module.stepCount()];
		boolean minimum = options.estimator() instanceof Estimator.Min;
		for (int s = 0; s < module.stepCount(); s++) {
			double best = Double.NEGATIVE_INFINITY;
			for (int a = 0; a < module.alternativeCount(s); a++) {
				if (isObserved(module, s, a, values)) {
					double value = minimum ? smallestPresent(module, s, a, values) : mean(module, s, a, values);
					if (value > best) {
						best = value;
						path[s] = a;
					}
				}
			}
		}
		return evaluate(module, values, path);
	}

	/** Every path, in the order of 1.x (the first step's alternative changes fastest); the first best one wins */
	ModuleScore scoreAllPaths(CompiledModule module, double[] values) {
		int[] path = new int[module.stepCount()];
		ModuleScore best = evaluate(module, values, path);
		while (nextPath(module, path)) {
			ModuleScore candidate = evaluate(module, values, path);
			if (candidate.coverage() > best.coverage()
					|| candidate.coverage() == best.coverage() && candidate.abundance() > best.abundance()) {
				best = candidate;
			}
		}
		return best;
	}

	private static boolean allFiniteAndNonNegative(CompiledModule module, double[] values) {
		for (int s = 0; s < module.stepCount(); s++) {
			for (int a = 0; a < module.alternativeCount(s); a++) {
				for (int o = 0; o < module.orthologCount(s, a); o++) {
					double value = values[module.ortholog(s, a, o)];
					// false for NaN too
					if (!(value >= 0 && value < Double.POSITIVE_INFINITY)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static boolean nextPath(CompiledModule module, int[] path) {
		for (int s = 0; s < path.length; s++) {
			if (++path[s] < module.alternativeCount(s)) {
				return true;
			}
			path[s] = 0;
		}
		return false;
	}

	/** The coverage and abundance of one path */
	private ModuleScore evaluate(CompiledModule module, double[] values, int[] path) {
		return switch (options.estimator()) {
			case Estimator.Median() -> median(module, values, path);
			case Estimator.Average() -> sumOfSteps(module, values, path, true);
			case Estimator.Sum() -> sumOfSteps(module, values, path, false);
			case Estimator.Min() -> minimum(module, values, path);
		};
	}

	private ModuleScore median(CompiledModule module, double[] values, int[] path) {
		int length = module.stepCount();
		double[] stepValues = new double[length];
		int count = 0;
		double observedSteps = 0;
		for (int s = 0; s < length; s++) {
			if (isObserved(module, s, path[s], values)) {
				observedSteps++;
				stepValues[count++] = mean(module, s, path[s], values);
			} else if (options.normalizeByLength()) {
				// -n: an unobserved step lowers the median
				stepValues[count++] = 0d;
			}
		}
		double[] observed = Arrays.copyOf(stepValues, count);
		double abundance = count == 0 ? 0d : new Median().evaluate(observed);
		// a 0 median becomes the smallest positive value
		if (Double.compare(abundance, 0d) == 0) {
			Arrays.sort(observed);
			for (double value : observed) {
				if (Double.compare(value, 0d) > 0) {
					abundance = value;
					break;
				}
			}
		}
		return new ModuleScore(abundance, observedSteps / length);
	}

	/** Average (÷ observed steps) or sum (÷ 1); ÷ the module length with -n */
	private ModuleScore sumOfSteps(CompiledModule module, double[] values, int[] path, boolean average) {
		int length = module.stepCount();
		double observedSteps = 0;
		double sum = 0;
		for (int s = 0; s < length; s++) {
			if (isObserved(module, s, path[s], values)) {
				sum += mean(module, s, path[s], values);
				observedSteps++;
			}
		}
		double divisor = options.normalizeByLength() ? length : average ? observedSteps : 1;
		// no observed step: abundance 0 instead of 0 / 0
		double abundance = divisor == 0 ? 0d : sum / divisor;
		return new ModuleScore(abundance, observedSteps / length);
	}

	/** The smallest present KO of the observed steps, 0 when there is none */
	private ModuleScore minimum(CompiledModule module, double[] values, int[] path) {
		int length = module.stepCount();
		double observedSteps = 0;
		double abundance = Double.POSITIVE_INFINITY;
		boolean present = false;
		for (int s = 0; s < length; s++) {
			if (isObserved(module, s, path[s], values)) {
				observedSteps++;
				for (int o = 0; o < module.orthologCount(s, path[s]); o++) {
					double value = values[module.ortholog(s, path[s], o)];
					if (value > 0) {
						abundance = present ? Math.min(abundance, value) : value;
						present = true;
					}
				}
			}
		}
		return new ModuleScore(present ? abundance : 0d, observedSteps / length);
	}

	/** Whether an alternative is observed: its orthologs with a value &gt; 0, judged by the complex rule */
	private boolean isObserved(CompiledModule module, int step, int alternative, double[] values) {
		int orthologs = module.orthologCount(step, alternative);
		int present = 0;
		for (int o = 0; o < orthologs; o++) {
			if (values[module.ortholog(step, alternative, o)] > 0) {
				present++;
			}
		}
		return options.complexRule().isObserved(present, orthologs);
	}

	/** The value of an alternative: the mean of its orthologs, zeros included, summed in database order */
	private static double mean(CompiledModule module, int step, int alternative, double[] values) {
		int orthologs = module.orthologCount(step, alternative);
		double sum = 0;
		for (int o = 0; o < orthologs; o++) {
			sum += values[module.ortholog(step, alternative, o)];
		}
		return sum / orthologs;
	}

	/** The smallest ortholog of an alternative with a value > 0 (an observed alternative has one) */
	private static double smallestPresent(CompiledModule module, int step, int alternative, double[] values) {
		double smallest = Double.POSITIVE_INFINITY;
		for (int o = 0; o < module.orthologCount(step, alternative); o++) {
			double value = values[module.ortholog(step, alternative, o)];
			if (value > 0) {
				smallest = Math.min(smallest, value);
			}
		}
		return smallest;
	}
}
