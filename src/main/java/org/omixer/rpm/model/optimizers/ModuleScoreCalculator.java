package org.omixer.rpm.model.optimizers;

import java.util.List;

import org.omixer.rpm.model.ModuleInferenceOptions;
import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.model.PathwaySummary;

public interface ModuleScoreCalculator {

	PathwaySummary computeBestScore(List<List<List<Ortholog>>> allPaths, ModuleInferenceOptions options);
	
}
