package io.omixer.rpm.inference

import io.omixer.rpm.database.CompiledDatabase
import io.omixer.rpm.database.ModuleDatabase
import io.omixer.rpm.profile.Profiles
import io.omixer.rpm.profile.ProfilesBuilder
import spock.lang.Specification

class ResultsSpec extends Specification {

	static final List<String> MODULES = ["M1", "M2", "M3"]

	/** S1 (or its taxon Sp1) and S2; nothing observed in S2, so per taxon it has no profile */
	private static Profiles profiles(boolean perTaxon) {
		def builder = new ProfilesBuilder(CompiledDatabase.compile(new ModuleDatabase([])), perTaxon)
		builder.sample("S1")
		builder.add("S1", perTaxon ? "Sp1" : null, "K1", 1d)
		builder.sample("S2")
		builder.build()
	}

	private static Cutoff fixed(double value) {
		new CoverageCutoff.Fixed(value).resolve(new double[0])
	}

	/** S1 observes M2 (coverage 0.5) and M3 (coverage 1); S2 observes nothing */
	private static Results results(Cutoff cutoff, boolean perTaxon = false) {
		def builder = Results.builder(profiles(perTaxon), MODULES)
		builder.profile(0, [1, 2] as int[], [4d, 6d] as double[], [0.5d, 1d] as double[])
		if (!perTaxon) {
			builder.profile(1, new int[0], new double[0], new double[0])
		}
		builder.build(cutoff)
	}

	private static List<List> rows(List<ModuleResult> results) {
		results.collect { [it.sample(), it.taxon(), it.module(), it.abundance(), it.coverage()] }
	}

	def "every module of a sample is a result in database order; an unobserved one has abundance and coverage 0"() {

		expect:
		rows(results(fixed(0.5d)).results("S1")) ==
				[["S1", null, "M1", 0d, 0d], ["S1", null, "M2", 4d, 0.5d], ["S1", null, "M3", 6d, 1d]]
		rows(results(fixed(0.5d)).results("S2")) ==
				[["S2", null, "M1", 0d, 0d], ["S2", null, "M2", 0d, 0d], ["S2", null, "M3", 0d, 0d]]
	}

	def "a module is reported when its coverage is at or above the cutoff"() {

		expect:
		rows(results(fixed(cutoff)).reported("S1"))*.get(2) == reported

		where:
		cutoff | reported
		0.5d   | ["M2", "M3"]
		0.6d   | ["M3"]
		1d     | ["M3"]
	}

	def "with cutoff 0 the unobserved modules are reported too, as in 1.x"() {

		expect:
		rows(results(fixed(0d)).reported("S1"))*.get(2) == MODULES
		rows(results(fixed(0d)).reported("S2")) ==
				[["S2", null, "M1", 0d, 0d], ["S2", null, "M2", 0d, 0d], ["S2", null, "M3", 0d, 0d]]
	}

	def "without a cutoff (none could be learned) nothing is reported"() {

		when:
		def results = results(new CoverageCutoff.Automatic().resolve(new double[0]))

		then:
		results.cutoff().value().isEmpty()
		results.reported("S1").isEmpty()
	}

	def "per taxon, a sample without any profile is kept, with no results"() {

		when: "S2 has no value, so no taxon"
		def results = results(fixed(0d), true)

		then:
		results.samples() == ["S1", "S2"]
		results.perTaxon()
		rows(results.reported("S1"))*.get(1).unique() == ["Sp1"]
		results.results("S2").isEmpty()
		results.reported("S2").isEmpty()
	}

	def "the builder gives the coverage of every observed module, profile by profile"() {

		given:
		def builder = Results.builder(profiles(false), MODULES)
		builder.profile(1, [0] as int[], [1d] as double[], [0.25d] as double[])
		builder.profile(0, [1, 2] as int[], [4d, 6d] as double[], [0.5d, 1d] as double[])

		expect:
		builder.observedCoverages() as List == [0.5d, 1d, 0.25d]
	}

	def "the builder rejects a profile that breaks the results' invariants"() {

		given:
		def builder = Results.builder(profiles(false), MODULES)

		when:
		builder.profile(0, modules as int[], abundance as double[], coverage as double[])

		then:
		thrown(IllegalArgumentException)

		where: "lengths differ, modules not ascending or unknown, an unobserved module stored"
		modules | abundance  | coverage
		[1, 2]  | [4d]       | [0.5d, 1d]
		[1, 2]  | [4d, 6d]   | [0.5d]
		[2, 1]  | [4d, 6d]   | [0.5d, 1d]
		[1, 1]  | [4d, 6d]   | [0.5d, 1d]
		[3]     | [4d]       | [0.5d]
		[-1]    | [4d]       | [0.5d]
		[1]     | [0d]       | [0d]
	}

	def "the builder rejects a profile given twice, and building before every profile is given"() {

		given:
		def builder = Results.builder(profiles(false), MODULES)
		builder.profile(0, new int[0], new double[0], new double[0])

		when:
		builder.profile(0, new int[0], new double[0], new double[0])

		then:
		thrown(IllegalStateException)

		when: "profile 1 (S2) is missing"
		builder.build(fixed(0d))

		then:
		thrown(IllegalStateException)
	}
}
