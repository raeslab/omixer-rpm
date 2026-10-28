package io.omixer.rpm.model.optimizers;

import java.util.List;

import io.omixer.rpm.model.Ortholog;
import io.omixer.rpm.model.PathwaySummary;
import io.omixer.rpm.model.enums.ComplexRule;


/**
 * 
 * Computes the score and coverage as a function of reactions present in the pathway
 *  
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public abstract class ModuleAbundanceMaximizer extends ModuleAbundanceCoverageOptimizer {

	public ModuleAbundanceMaximizer(boolean normalizeByLength, ComplexRule complexRule) {
		super(normalizeByLength, complexRule);
	}
	
	/**
	 * A scaling factor to account for the difference in module length
	 * 
	 * @param pathLength
	 * @param observedSteps
	 * @return
	 */
	protected abstract double getNormalizer(double pathLength, double observedSteps);
	
	/**
	 * 
	 * Computes the score as the average in a complex/simple enzyme, sums up then divide by pathway size
	 * 
	 * @param path
	 * @return
	 */
	protected PathwaySummary computePathScoreAndCoverage(List<List<Ortholog>> path) {

		double pathLength = path.size();
		// the observed steps, according to the complex rule
		double observedSteps = 0;
		double pathCount = 0;

		// each step
		for (int i = 0; i < pathLength; i++) {
			List<Ortholog> step = path.get(i);
			double stepSize = step.size();
			double stepCount = 0;
			// take the average value for the Reaction instead of summing up all KO values
			for (int j = 0; j < stepSize; j++) {
				stepCount += step.get(j).getCount();
			}
			// only an observed step earns coverage and contributes abundance
			if (complexRule.isObserved(step)) {
				pathCount += (stepCount / stepSize);
				observedSteps++;
			}
		}
		
		double normalizer = getNormalizer(pathLength, observedSteps);
		
		// no observed step with an observed-steps normalizer: abundance 0 instead of 0 / 0 = NaN
		double pathScore = normalizer == 0 ? 0 : pathCount / normalizer;
		double coverage = observedSteps / pathLength;
		return new PathwaySummary(path, coverage, pathScore);
	}
}
