package io.omixer.rpm.model.optimizers;

import java.util.List;

import io.omixer.rpm.model.Ortholog;
import io.omixer.rpm.model.PathwaySummary;
import io.omixer.rpm.model.enums.ComplexRule;


/**
 * 
 * Computes the score and coverage as a function of orthologs present in the pathway 
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class ModuleAbundanceCoverageAbundanceMinimizer extends ModuleAbundanceCoverageOptimizer {
	
	public ModuleAbundanceCoverageAbundanceMinimizer(boolean normalizeByLength, ComplexRule complexRule) {
		super(normalizeByLength, complexRule);
	}

	/** 
	 * 
	 * Minimize the abundance by giving the pathway the abundance of the least abundant Ortholog 
	 * 
	 * @param path
	 * @return
	 */
	protected PathwaySummary computePathScoreAndCoverage(List<List<Ortholog>> path) {

		double pathLength = path.size();
		// abundance and coverage are computed independently: a shared counter used to make the minimum skip
		// the first observed step's subunits but the last one
		// the minimum: the smallest abundance of a present ortholog of an observed step, 0 when there is none
		double pathScore = path.stream().filter(complexRule::isObserved).flatMap(List::stream)
				.mapToDouble(Ortholog::getCount).filter(count -> count > 0).min().orElse(0);
		// the observed steps, according to the complex rule
		double coveredSteps = path.stream().filter(complexRule::isObserved).count();
		double coverage = coveredSteps / pathLength;
		return new PathwaySummary(path, coverage, pathScore);
	}
}
