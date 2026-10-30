package io.omixer.rpm.service

import java.nio.file.Path

import io.omixer.rpm.model.Module
import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.scoring.ComplexRule
import io.omixer.rpm.scoring.Estimator
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.io.ModuleDatabaseReader
import io.omixer.rpm.service.impl.ModuleManagerImpl

import spock.lang.Specification

/**
 * How --complex-rule changes coverage and abundance, with every estimator.
 * Modules from src/test/resources/golden/inputs/edge/edge-modules.list:
 *   E001: K90001 / K90002 / K90003                       (no complex)
 *   E003: K90007,K90008 / K90009                         (2-subunit complex)
 *   E011: K90040,K90041,K90042 / K90043                  (3-subunit complex)
 */
class ComplexRuleScoringSpec extends Specification {

	static final def MEDIAN = new Estimator.Median()
	static final def AVERAGE = new Estimator.Average()
	static final def SUM = new Estimator.Sum()
	static final def MIN = new Estimator.Min()

	/** One subunit of each complex is observed: K90008 = 0; K90041 = K90042 = 0 */
	static final Map<String, Double> PARTIAL = [K90007: 8d, K90009: 4d, K90040: 6d, K90043: 6d]
	static final Map<String, Double> FULL = [K90007: 8d, K90008: 2d, K90009: 4d, K90040: 6d, K90041: 3d, K90042: 9d, K90043: 6d]

	private static Module score(String module, Map<String, Double> abundances, def estimator, ComplexRule rule,
			boolean normalize = false) {
		def options = new ModuleInferenceOptions()
		options.setEstimator(estimator)
		options.setComplexRule(rule)
		options.setNormalizeByLength(normalize)
		options.setCoverage(0d)
		def modules = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/golden/inputs/edge/edge-modules.list")))
		SampleInput.infer(new ModuleManagerImpl(), abundances.collect { ko, value -> SampleInput.row(null, ko, value) },
				options, modules)
				.getModules().find { it.getModuleId() == module }
	}

	def "a partial complex: covered with any, not covered and no abundance with all"() {

		when:
		Module result = score(module, PARTIAL, estimator, rule)

		then:
		result.getCount() == count
		Math.abs(result.getCoverage() - coverage) < 1e-12

		where:
		module | rule                 | estimator | count | coverage
		"E003" | ComplexRule.ANY      | MEDIAN    | 4d    | 1d
		"E003" | ComplexRule.ANY      | AVERAGE   | 4d    | 1d
		"E003" | ComplexRule.ANY      | SUM       | 8d    | 1d
		"E003" | ComplexRule.ANY      | MIN       | 4d    | 1d
		"E003" | ComplexRule.ALL      | MEDIAN    | 4d    | 0.5d
		"E003" | ComplexRule.ALL      | AVERAGE   | 4d    | 0.5d
		"E003" | ComplexRule.ALL      | SUM       | 4d    | 0.5d
		"E003" | ComplexRule.ALL      | MIN       | 4d    | 0.5d
		"E011" | ComplexRule.ANY      | MEDIAN    | 4d    | 1d
		"E011" | ComplexRule.ANY      | AVERAGE   | 4d    | 1d
		"E011" | ComplexRule.ANY      | SUM       | 8d    | 1d
		"E011" | ComplexRule.ANY      | MIN       | 6d    | 1d
		"E011" | ComplexRule.ALL      | MEDIAN    | 6d    | 0.5d
		"E011" | ComplexRule.ALL      | AVERAGE   | 6d    | 0.5d
		"E011" | ComplexRule.ALL      | SUM       | 6d    | 0.5d
		"E011" | ComplexRule.ALL      | MIN       | 6d    | 0.5d
	}

	def "with all and -n, a partial complex counts as an unobserved step"() {

		expect: "median of [0, 6]"
		score("E011", PARTIAL, MEDIAN, ComplexRule.ALL, true).getCount() == 3d
	}

	def "a complete complex gives the same result with every rule"() {

		when:
		def results = ComplexRule.values().collect { score(module, FULL, estimator, it) }

		then:
		results*.getCount().unique().size() == 1
		results*.getCoverage().unique() == [1d]

		where:
		[module, estimator] << [["E003", "E011"], [MEDIAN, AVERAGE, SUM, MIN]].combinations()
	}

	def "a complex with no observed subunit is an unobserved step with every rule"() {

		when:
		def results = ComplexRule.values().collect { score("E003", [K90009: 4d], estimator, it) }

		then:
		results*.getCoverage().unique() == [0.5d]
		results*.getCount().unique() == [4d]

		where:
		estimator << [MEDIAN, AVERAGE, SUM, MIN]
	}

	def "a module without complexes is not affected by the rule"() {

		when: "E001 with K90002 missing"
		def results = ComplexRule.values().collect { score("E001", [K90001: 4d, K90003: 6d], estimator, it) }

		then:
		results*.getCount().unique().size() == 1
		results*.getCoverage().unique().size() == 1

		where:
		estimator << [MEDIAN, AVERAGE, SUM, MIN]
	}
}
