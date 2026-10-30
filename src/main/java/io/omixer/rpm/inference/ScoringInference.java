package io.omixer.rpm.inference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.ForkJoinPool;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import io.omixer.rpm.cutoff.CoverageCutoff;
import io.omixer.rpm.cutoff.CoverageDistribution;
import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.engine.CompiledModule;
import io.omixer.rpm.engine.Profiles;
import io.omixer.rpm.engine.Results;
import io.omixer.rpm.scoring.ModuleScore;
import io.omixer.rpm.scoring.ModuleScorer;
import io.omixer.rpm.scoring.ScoringOptions;
import io.omixer.rpm.scoring.SharedKoResolution;

/**
 * Infers modules by scoring: resolves shared KOs, scores every module of every profile with a {@link ModuleScorer},
 * then applies the coverage cutoff.
 */
public final class ScoringInference implements ModuleInference {

	private final ScoringOptions scoring;
	private final SharedKoResolution resolution;
	private final CoverageCutoff cutoff;
	private final int threads;

	/**
	 * @param threads the number of threads scoring profiles in parallel (-t); 1 scores them in turn
	 */
	public ScoringInference(ScoringOptions scoring, SharedKoResolution resolution, CoverageCutoff cutoff, int threads) {
		this.scoring = Objects.requireNonNull(scoring, "scoring");
		this.resolution = Objects.requireNonNull(resolution, "resolution");
		this.cutoff = Objects.requireNonNull(cutoff, "cutoff");
		if (threads < 1) {
			throw new IllegalArgumentException("threads must be at least 1, not " + threads);
		}
		this.threads = threads;
	}

	@Override
	public Results infer(CompiledDatabase database, Profiles profiles) {
		Profiles resolved = resolution.apply(profiles, database);
		ModuleScorer scorer = new ModuleScorer(scoring);
		int size = profiles.size();
		int[][] observed = new int[size][];
		double[][] abundance = new double[size][];
		double[][] coverage = new double[size][];
		// each profile fills its own slots, so profiles can be scored in parallel without locks
		forEachProfile(size, profile -> {
			double[] values = resolved.values(profile);
			List<CompiledModule> modules = database.modules();
			int[] indexes = new int[modules.size()];
			double[] abundances = new double[modules.size()];
			double[] coverages = new double[modules.size()];
			int count = 0;
			for (int module = 0; module < modules.size(); module++) {
				ModuleScore score = scorer.score(modules.get(module), values);
				// coverage 0 means no observed step, and so abundance 0: not stored
				if (score.coverage() > 0) {
					indexes[count] = module;
					abundances[count] = score.abundance();
					coverages[count] = score.coverage();
					count++;
				}
			}
			observed[profile] = Arrays.copyOf(indexes, count);
			abundance[profile] = Arrays.copyOf(abundances, count);
			coverage[profile] = Arrays.copyOf(coverages, count);
		});
		List<String> moduleIds = database.modules().stream().map(CompiledModule::id).toList();
		return switch (cutoff) {
			case CoverageCutoff.Fixed(double value) ->
				new Results(profiles, moduleIds, observed, abundance, coverage, OptionalDouble.of(value), null);
			case CoverageCutoff.Automatic() -> {
				CoverageDistribution distribution = new CoverageDistribution(observedCoverages(coverage));
				yield distribution.findOptimalCoverage()
						.map(learned -> new Results(profiles, moduleIds, observed, abundance, coverage,
								OptionalDouble.of(learned), distribution))
						.orElseGet(() -> new Results(profiles, moduleIds, observed, abundance, coverage,
								OptionalDouble.empty(), null));
			}
		};
	}

	private void forEachProfile(int size, IntConsumer action) {
		if (threads == 1) {
			IntStream.range(0, size).forEach(action);
			return;
		}
		ForkJoinPool pool = new ForkJoinPool(threads);
		try {
			pool.submit(() -> IntStream.range(0, size).parallel().forEach(action)).join();
		} finally {
			pool.shutdown();
		}
	}

	/** The coverage of every observed module in every profile */
	private static List<Double> observedCoverages(double[][] coverage) {
		List<Double> coverages = new ArrayList<>();
		for (double[] profile : coverage) {
			for (double value : profile) {
				coverages.add(value);
			}
		}
		return coverages;
	}
}
