package org.omixer.rpm.model.optimizers;

import java.util.List;

import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.model.PathwaySummary;


/**
 * 
 * Computes the score and coverage as a function of reactions present in the pathway
 *  
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public abstract class ModuleAbundanceMaximizer extends ModuleAbundanceCoverageOptimizer {

	public ModuleAbundanceMaximizer(boolean normalizeByLength) {
		super(normalizeByLength);
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
		double presentSteps = 0;
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
			pathCount += (stepCount / stepSize);

			if (stepCount > 0) {
				presentSteps++;
			}
		}
		
		double normalizer = getNormalizer(pathLength, presentSteps);
		
		double pathScore = pathCount / normalizer;
		double coverage = presentSteps / pathLength;
		return new PathwaySummary(path, coverage, pathScore);
	}
}
