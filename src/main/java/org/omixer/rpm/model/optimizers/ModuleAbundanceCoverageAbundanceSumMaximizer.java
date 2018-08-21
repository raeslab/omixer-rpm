package org.omixer.rpm.model.optimizers;

/**
 * 
 * Computes the score and coverage as a function of orthologs present in the pathway.
 * The score is the sum of ortholog abundances that maximize the score 
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class ModuleAbundanceCoverageAbundanceSumMaximizer extends ModuleAbundanceMaximizer {
	
	public ModuleAbundanceCoverageAbundanceSumMaximizer(boolean normalizeByLength) {
		super(normalizeByLength);
	}

	/**
	 *  The normalizer is either pathway length or 1.
	 */
	@Override
	protected double getNormalizer(double pathLength, double observedSteps) {		
		return isNormalizeByLength() ? pathLength : 1;
	}
}
