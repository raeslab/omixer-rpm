package io.omixer.rpm.model.optimizers;

import java.util.List;

import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Ortholog;
import io.omixer.rpm.model.PathwaySummary;

public interface ModuleScoreCalculator {

	PathwaySummary computeBestScore(List<List<List<Ortholog>>> allPaths, ModuleInferenceOptions options);
	
}
