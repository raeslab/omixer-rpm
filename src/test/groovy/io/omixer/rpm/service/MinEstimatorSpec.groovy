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
 * The min score estimator (-s min): a module gets the abundance of its least abundant KO
 *
 * MF0007 (src/test/resources/module-inference.list): K02786,K02787,K02788 / K01220 / K01819 / K00917 / K08302 | K01635
 */
class MinEstimatorSpec extends Specification {

	private static Module mf0007(Map<String, Double> abundances) {
		def options = new ModuleInferenceOptions()
		options.setAlgorithm(ModuleInferenceOptimizers.MIN.displayName())
		options.setCoverage(0d)
		def referenceModules = Module.fromDatabase(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list")))
		new ModuleManagerImpl()
				.inferModules(abundances.collect { ko, value -> new BasicFeature(null, ko, value) }, options, referenceModules)
				.getModules().find { it.getModuleId() == "MF0007" }
	}

	def "the minimum does not depend on the order of the subunits of a complex in the first step"() {

		when:
		Module module = mf0007([K02786: subunits[0], K02787: subunits[1], K02788: subunits[2],
				K01220: 7d, K01819: 8d, K00917: 5d, K08302: 4d])

		then: "the smallest KO is 3; before, the last subunit of the first step replaced the others and gave 4"
		module.getCount() == 3d
		module.getCoverage() == 1d

		where:
		subunits << [[6d, 3d, 9d], [3d, 9d, 6d], [9d, 6d, 3d], [3d, 6d, 9d], [6d, 9d, 3d], [9d, 3d, 6d]]
	}

	def "the minimum is taken over every present KO when the first observed step is not the first step"() {

		when: "the first step (the complex) is absent; the first observed step is K01220"
		Module module = mf0007([K01220: 7d, K01819: 2d, K00917: 5d, K08302: 4d])

		then:
		module.getCount() == 2d
		module.getCoverage() == 4d / 5d
	}
}
