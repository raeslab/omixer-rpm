package org.omixer.rpm.model.optimizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.model.PathwaySummary;
import org.omixer.utils.Constants;


/**
 * 
 * Computes the score and coverage as a function of reactions present in the pathway
 * The score is equal to the median value of observed KOs. In case Step is a complex then average
 * of the step is added to the array for median computation 
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class ModuleAbundanceCoverageMedianMaximizer extends ModuleAbundanceCoverageOptimizer {

	public ModuleAbundanceCoverageMedianMaximizer(boolean normalizeByLength) {
		super(normalizeByLength);
	}

	/** 
	 * 
	 * Find the median abundance for the Modules 
	 * 
	 * @param path
	 * @return
	 */
	protected PathwaySummary computePathScoreAndCoverage(List<List<Ortholog>> path) {

		double pathLength = path.size();
		Double presentSteps = 0d;
		List<Double> observedValues = new ArrayList<Double>();
		
		// each step
		for (int i = 0; i < pathLength; i++) {
			List<Ortholog> step = path.get(i);
			double stepSize = step.size();
			double stepCount = 0.d;
			// take the average value for the Reaction instead of summing up all KO values
			for (int j = 0; j < stepSize; j++) {
				stepCount += step.get(j).getCount();
			}
			// Step is observed
			if (Double.compare(stepCount, Constants.ZERO) > 0) {
				presentSteps++;
				// divide observed value by step size to have the mean for complex steps
				observedValues.add(stepCount / stepSize);
			} else if (isNormalizeByLength()){
				// add observed value so the median is penalized but the missing counts
				observedValues.add(Constants.ZERO);
			}
		}
		// the array of observations
		int obsevationLength = presentSteps.intValue();
		if (isNormalizeByLength()) {
			obsevationLength = Double.valueOf(pathLength).intValue();
		}
		double[] observedArray = new double[obsevationLength];
		for (int i = 0; i < obsevationLength; i++) {
			observedArray[i] = observedValues.get(i);
		}

		double pathScore = new Median().evaluate(observedArray);

		// If median estimator is zero then replace it by minimum
		if (Double.compare(pathScore, Constants.ZERO) == 0) {
			
			Arrays.sort(observedArray);
			// search for the first observation that is > 0
			for (int i = 0; i < observedArray.length; i++) {
				if (Double.compare(observedArray[i], Constants.ZERO) > 0) {
					pathScore = observedArray[i];
					break;
				}
			}
		}
		
		double coverage = presentSteps / pathLength;
		return new PathwaySummary(path, coverage, pathScore);
	}
}
