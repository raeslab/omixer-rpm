package io.omixer.rpm.service

import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.engine.ProfilesBuilder
import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.model.Modules
import io.omixer.rpm.service.impl.ModuleManagerImpl

/**
 * One sample's input for the module manager specs, as rows of [taxon, KO, value], read through a ProfilesBuilder
 * like a real input
 */
class SampleInput {

	static final String SAMPLE = "S"

	static List row(String taxon, String ko, double value) {
		[taxon, ko, value]
	}

	/** @return the sample's modules */
	static Modules infer(ModuleManagerImpl manager, List<List> rows, ModuleInferenceOptions options,
			CompiledDatabase database) {
		def builder = new ProfilesBuilder(database, options.isPerTaxon())
		builder.sample(SAMPLE)
		rows.each { taxon, ko, value -> builder.add(SAMPLE, taxon, ko, value) }
		manager.inferModules(builder.build(), options, database)[SAMPLE]
	}
}
