package org.omixer.rpm.model.optimizers;

import java.util.List;

import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.model.PathwaySummary;


/**
 * Computes the score and coverage as a function of orthologs present in the pathway 
 * i.e. considers the number of orthologs presents as pathway length instead of the number of enzymatic steps
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class ModuleAbundanceCoverageOrthologMaximizer extends ModuleAbundanceCoverageOptimizer {

	public ModuleAbundanceCoverageOrthologMaximizer(boolean normalizeByLength) {
		super(normalizeByLength);
	}

	/**
	 * 
	 * Computes the score as the sum of present orthologs abundance divided by the number of orthologs that constitute the pathway 
	 * 
	 * @param path
	 * @return
	 */
	protected PathwaySummary computePathScoreAndCoverage(List<List<Ortholog>> path) {

		// the number of present orthologs (not the number of steps). 
		double pathLength = 0;
		// The number of orthologs present 
		double presentOrthologs = 0;
		// the abundance of KOs in this pathway
		double pathCount = 0;
		// on one path at each step
		for (List<Ortholog> step : path) {
			// these are the orthologs of a complex
			for (Ortholog ortholog : step) {
				if (ortholog.getCount() > 0) {
					// update orthologs abundance
					pathCount += ortholog.getCount();
					// increment the number of present orthologs
					presentOrthologs++;
				}
				// increment the pathLength to reflect the number of KOs
				pathLength++;
			}
		}

		double normalizer = presentOrthologs;
		if (isNormalizeByLength()) {
			normalizer = pathLength;
		}
		
		double pathScore = pathCount / normalizer;
		double coverage = presentOrthologs / pathLength;
		return new PathwaySummary(path, coverage, pathScore);
	}
}
