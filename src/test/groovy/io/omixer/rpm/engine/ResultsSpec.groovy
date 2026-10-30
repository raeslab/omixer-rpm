package io.omixer.rpm.engine

import io.omixer.rpm.domain.ModuleDatabase
import spock.lang.Specification

class ResultsSpec extends Specification {

	static final List<String> MODULES = ["M1", "M2", "M3"]

	/** S1 observes M2 (coverage 0.5) and M3 (coverage 1); S2 observes nothing */
	private static Results results(OptionalDouble cutoff, boolean perTaxon = false) {
		def builder = new ProfilesBuilder(CompiledDatabase.compile(new ModuleDatabase([])), perTaxon)
		builder.sample("S1")
		builder.add("S1", perTaxon ? "Sp1" : null, "K1", 1d)
		builder.sample("S2")
		def profiles = builder.build()
		int[][] observed = [[1, 2], []]
		double[][] abundance = [[4d, 6d], []]
		double[][] coverage = [[0.5d, 1d], []]
		if (perTaxon) {
			observed = [[1, 2]]
			abundance = [[4d, 6d]]
			coverage = [[0.5d, 1d]]
		}
		new Results(profiles, MODULES, observed, abundance, coverage, cutoff, null)
	}

	private static List<List> rows(List<ModuleResult> results) {
		results.collect { [it.sample(), it.taxon(), it.module(), it.abundance(), it.coverage()] }
	}

	def "every module of a sample is a result in database order; an unobserved one has abundance and coverage 0"() {

		expect:
		rows(results(OptionalDouble.of(0.5d)).results("S1")) ==
				[["S1", null, "M1", 0d, 0d], ["S1", null, "M2", 4d, 0.5d], ["S1", null, "M3", 6d, 1d]]
		rows(results(OptionalDouble.of(0.5d)).results("S2")) ==
				[["S2", null, "M1", 0d, 0d], ["S2", null, "M2", 0d, 0d], ["S2", null, "M3", 0d, 0d]]
	}

	def "a module is reported when its coverage is at or above the cutoff"() {

		expect:
		rows(results(OptionalDouble.of(cutoff)).reported("S1"))*.get(2) == reported

		where:
		cutoff | reported
		0.5d   | ["M2", "M3"]
		0.6d   | ["M3"]
		1d     | ["M3"]
	}

	def "with cutoff 0 the unobserved modules are reported too, as in 1.x"() {

		expect:
		rows(results(OptionalDouble.of(0d)).reported("S1"))*.get(2) == MODULES
		rows(results(OptionalDouble.of(0d)).reported("S2")) ==
				[["S2", null, "M1", 0d, 0d], ["S2", null, "M2", 0d, 0d], ["S2", null, "M3", 0d, 0d]]
	}

	def "without a cutoff (none could be learned) nothing is reported"() {

		expect:
		results(OptionalDouble.empty()).reported("S1").isEmpty()
	}

	def "per taxon, a sample without any profile is kept, with no results"() {

		when: "S2 has no value, so no taxon"
		def results = results(OptionalDouble.of(0d), true)

		then:
		results.samples() == ["S1", "S2"]
		results.perTaxon()
		rows(results.reported("S1"))*.get(1).unique() == ["Sp1"]
		results.results("S2").isEmpty()
		results.reported("S2").isEmpty()
	}
}
