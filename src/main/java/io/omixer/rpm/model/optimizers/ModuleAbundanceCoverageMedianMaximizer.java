package io.omixer.rpm.model.optimizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import io.omixer.rpm.model.Ortholog;
import io.omixer.rpm.model.PathwaySummary;
import io.omixer.rpm.model.enums.ComplexRule;
import io.omixer.utils.Constants;


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

	public ModuleAbundanceCoverageMedianMaximizer(boolean normalizeByLength, ComplexRule complexRule) {
		super(normalizeByLength, complexRule);
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
		// coverage earned by the steps, according to the complex rule
		double coveredSteps = 0;
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
			coveredSteps += complexRule.coverage(step);
			// Step is observed
			if (complexRule.isObserved(step)) {
				// divide observed value by step size to have the mean for complex steps
				observedValues.add(stepCount / stepSize);
			} else if (isNormalizeByLength()){
				// add observed value so the median is penalized but the missing counts
				observedValues.add(Constants.ZERO);
			}
		}
		// the array of observations: the observed steps, plus a 0 for each unobserved step with -n
		double[] observedArray = observedValues.stream().mapToDouble(Double::doubleValue).toArray();

		// no observed step: abundance 0 (the median of no value is NaN)
		double pathScore = observedArray.length == 0 ? Constants.ZERO : new Median().evaluate(observedArray);

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
		
		double coverage = coveredSteps / pathLength;
		return new PathwaySummary(path, coverage, pathScore);
	}
}
