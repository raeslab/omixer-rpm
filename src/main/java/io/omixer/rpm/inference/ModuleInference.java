package io.omixer.rpm.inference;

import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.engine.Profiles;
import io.omixer.rpm.engine.Results;

/**
 * A method that infers the modules of every profile: their abundance, coverage and which are reported. Scoring
 * ({@link ScoringInference}) is the built-in one; methods that need extra dependencies (a MIP solver, a sampler) can
 * implement this interface in their own modules, which is why it is not sealed (REFACTORING.md, "Extension points").
 */
public interface ModuleInference {

	/**
	 * @param profiles every profile of the input: a method may pool samples
	 */
	Results infer(CompiledDatabase database, Profiles profiles);
}
