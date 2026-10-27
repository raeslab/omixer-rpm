package io.omixer.rpm.model.optimizers;

import io.omixer.rpm.model.enums.ComplexRule;

/**
 * Computes the score as the average in a complex/simple enzyme, sums up then divide by pathway size
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class ModuleAbundanceCoverageReactionMaximizer extends ModuleAbundanceMaximizer {

	public ModuleAbundanceCoverageReactionMaximizer(boolean normalizeByLength, ComplexRule complexRule) {
		super(normalizeByLength, complexRule);
	}

	/**
	 * The normalizer is either pathway length or the number of observed steps
	 */
	@Override
	protected double getNormalizer(double pathLength, double observedSteps) {
		return isNormalizeByLength() ? pathLength : observedSteps;
	}
}
