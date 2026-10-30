package io.omixer.rpm.inference

import io.omixer.rpm.Settings
import io.omixer.rpm.engine.ModuleResult
import java.nio.file.Path

import io.omixer.rpm.scoring.Estimator
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.io.ModuleDatabaseReader

import spock.lang.Specification

/**
 * Modules of which no step is observed in a sample
 */
class UnobservedModuleSpec extends Specification {

	def "a module with no observed step has abundance 0 and coverage 0 with every estimator"() {

		setup: "only K00001 is observed; MF0004 (K00012) has no observed step"
		def options = new Settings()
		options.setEstimator(estimator)
		options.setNormalizeByLength(normalize)
		options.setCoverage(0d)
		def database = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list")))

		when:
		ModuleResult module = options.infer([Settings.row(null, "K00001", 2d)], database)
				.all().find { it.module() == "MF0004" }

		then: "before, median and average (without -n) gave NaN"
		module.abundance() == 0d
		module.coverage() == 0d

		where:
		[estimator, normalize] << [[new Estimator.Median(), new Estimator.Average(), new Estimator.Sum(),
				new Estimator.Min()], [false, true]].combinations()
	}
}
