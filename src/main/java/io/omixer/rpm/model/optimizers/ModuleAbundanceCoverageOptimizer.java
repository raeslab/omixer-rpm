package io.omixer.rpm.model.optimizers;

import java.util.List;

import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Ortholog;
import io.omixer.rpm.model.PathwaySummary;
import io.omixer.rpm.model.enums.ComplexRule;

/**
 * 
 * TODO report best paths instead of best path
 * 
 * Maximize the abundance and the coverage. 
 * Prefers higher coverage no matter how the abundance is
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public abstract class ModuleAbundanceCoverageOptimizer implements ModuleScoreCalculator {

	protected boolean normalizeByLength;
	/** How a partially observed complex step counts: whether it is observed and what coverage it earns */
	protected final ComplexRule complexRule;
	
	public ModuleAbundanceCoverageOptimizer(boolean normalizeByLength, ComplexRule complexRule) {
		this.normalizeByLength = normalizeByLength;
		this.complexRule = complexRule;
	}
	
	public void setNormalizeByLength(boolean normalizeByLength) {
		this.normalizeByLength = normalizeByLength;
	}
	
	public boolean isNormalizeByLength() {
		return normalizeByLength;
	}
	
	public PathwaySummary computeBestScore(List<List<List<Ortholog>>> allPaths,
			ModuleInferenceOptions options) {
		// worst case module is not present with coverage and abundance equals
		// to zero
		PathwaySummary bestPath = computePathScoreAndCoverage(allPaths.get(0));
		// the number of alternative pathways 
		int pathCount = allPaths.size();
		// keep the best path
		for (int i = 1; i < pathCount; i++) {
			// get the score and coverage for the next path
			PathwaySummary pathwaySummary = computePathScoreAndCoverage(allPaths
					.get(i));
			// The new pathway coverage is above or equal to the current best
			if (pathwaySummary.getCoverage() >= bestPath.getCoverage()) {
				// The new pathway is higher in coverage -> accept as best solution
				if (pathwaySummary.getCoverage() > bestPath.getCoverage()) {
					bestPath = pathwaySummary;
				// the new pathway have equal coverage but higher abundance -> accept new pathway as optimal solution
				// TODO maybe bug in case of minimizer OR not because we are using a global maximum and not per pathway
				} else if(pathwaySummary.getAbundance() > bestPath.getAbundance()) {
					bestPath = pathwaySummary;
				}
			}
		}
		return bestPath;
	}

	/**
	 * 
	 * Computes pathway score and coverage 
	 * 
	 * @param path
	 * @return
	 */
	protected abstract PathwaySummary computePathScoreAndCoverage(List<List<Ortholog>> path);
	
}
