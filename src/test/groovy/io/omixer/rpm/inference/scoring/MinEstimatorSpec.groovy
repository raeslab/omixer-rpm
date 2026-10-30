package io.omixer.rpm.inference.scoring

import io.omixer.rpm.Settings
import io.omixer.rpm.database.CompiledDatabase
import io.omixer.rpm.database.ModuleDatabaseReader
import io.omixer.rpm.inference.ModuleResult
import java.nio.file.Path

import spock.lang.Specification

/**
 * The min score estimator (-s min): a module gets the abundance of its least abundant KO
 *
 * MF0007 (src/test/resources/module-inference.list): K02786,K02787,K02788 / K01220 / K01819 / K00917 / K08302 | K01635
 */
class MinEstimatorSpec extends Specification {

	private static ModuleResult mf0007(Map<String, Double> abundances) {
		def options = new Settings()
		options.setEstimator(new Estimator.Min())
		options.setCoverage(0d)
		def database = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list")))
		options.infer(abundances.collect { ko, value -> Settings.row(null, ko, value) }, database)
				.all().find { it.module() == "MF0007" }
	}

	def "the minimum does not depend on the order of the subunits of a complex in the first step"() {

		when:
		ModuleResult module = mf0007([K02786: subunits[0], K02787: subunits[1], K02788: subunits[2],
				K01220: 7d, K01819: 8d, K00917: 5d, K08302: 4d])

		then: "the smallest KO is 3; before, the last subunit of the first step replaced the others and gave 4"
		module.abundance() == 3d
		module.coverage() == 1d

		where:
		subunits << [[6d, 3d, 9d], [3d, 9d, 6d], [9d, 6d, 3d], [3d, 6d, 9d], [6d, 9d, 3d], [9d, 3d, 6d]]
	}

	def "the minimum is taken over every present KO when the first observed step is not the first step"() {

		when: "the first step (the complex) is absent; the first observed step is K01220"
		ModuleResult module = mf0007([K01220: 7d, K01819: 2d, K00917: 5d, K08302: 4d])

		then:
		module.abundance() == 2d
		module.coverage() == 4d / 5d
	}
}
