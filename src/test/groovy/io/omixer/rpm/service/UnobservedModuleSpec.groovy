package io.omixer.rpm.service

import java.nio.file.Path

import io.omixer.rpm.model.BasicFeature
import io.omixer.rpm.model.Module
import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.model.enums.ModuleInferenceOptimizers
import io.omixer.rpm.io.ModuleDatabaseReader
import io.omixer.rpm.service.impl.ModuleManagerImpl

import spock.lang.Specification

/**
 * Modules of which no step is observed in a sample
 */
class UnobservedModuleSpec extends Specification {

	def "a module with no observed step has abundance 0 and coverage 0 with every estimator"() {

		setup: "only K00001 is observed; MF0004 (K00012) has no observed step"
		def options = new ModuleInferenceOptions()
		options.setAlgorithm(algorithm.displayName())
		options.setNormalizeByLength(normalize)
		options.setCoverage(0d)
		def referenceModules = Module.fromDatabase(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list")))

		when:
		Module module = new ModuleManagerImpl()
				.inferModules([new BasicFeature(null, "K00001", 2d)], options, referenceModules)
				.getModules().find { it.getModuleId() == "MF0004" }

		then: "before, median and average (without -n) gave NaN"
		module.getCount() == 0d
		module.getCoverage() == 0d

		where:
		[algorithm, normalize] << [[ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED,
				ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED,
				ModuleInferenceOptimizers.SUM, ModuleInferenceOptimizers.MIN], [false, true]].combinations()
	}
}
