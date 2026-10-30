package io.omixer.rpm.engine

import io.omixer.rpm.domain.Alternative
import io.omixer.rpm.domain.ModuleDatabase
import io.omixer.rpm.domain.ModuleDefinition
import io.omixer.rpm.domain.Step
import spock.lang.Specification

class ProfilesBuilderSpec extends Specification {

	/** One module using K1 and K2 */
	static final CompiledDatabase DATABASE = CompiledDatabase.compile(new ModuleDatabase([
		new ModuleDefinition("M1", "m", [new Step([new Alternative(["K1"])]), new Step([new Alternative(["K2"])])])]))

	private static double value(Profiles profiles, int profile, String ko) {
		profiles.values(profile)[DATABASE.indexOf(ko)]
	}

	def "values of the same KO in a profile are summed in input order"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, false)
		builder.sample("S1")
		[0.1d, 0.2d, 0.3d].each { builder.add("S1", null, "K1", it) }

		expect: "(0.1 + 0.2) + 0.3, which differs from 0.1 + (0.2 + 0.3) in floating point"
		value(builder.build(), 0, "K1") == (0.1d + 0.2d) + 0.3d
		(0.1d + 0.2d) + 0.3d != 0.1d + (0.2d + 0.3d)
	}

	def "values of a KO interleaved with other KOs are summed in their own input order"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, false)
		builder.sample("S1")
		builder.add("S1", null, "K1", 0.1d)
		builder.add("S1", null, "K2", 1d)
		builder.add("S1", null, "K1", 0.2d)
		builder.add("S1", null, "K2", 2d)
		builder.add("S1", null, "K1", 0.3d)

		when:
		def profiles = builder.build()

		then:
		value(profiles, 0, "K1") == (0.1d + 0.2d) + 0.3d
		value(profiles, 0, "K2") == 3d
	}

	def "a sample with no observed value is still a profile, with every value 0"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, false)
		builder.sample("S1")
		builder.add("S1", null, "K1", 0d)
		builder.sample("S2")

		when:
		def profiles = builder.build()

		then:
		profiles.samples() == ["S1", "S2"]
		(0..<profiles.size()).collect { profiles.key(it) } == [new ProfileKey("S1", null), new ProfileKey("S2", null)]
		profiles.values(0) == [0d, 0d] as double[]
		profiles.values(1) == [0d, 0d] as double[]
	}

	def "per taxon, every taxon of a sample is a profile, also one with only zeros"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, true)
		builder.sample("S1")
		builder.add("S1", "Sp1", "K1", 2d)
		builder.add("S1", "Sp2", "K1", 0d)

		when:
		def profiles = builder.build()

		then:
		(0..<profiles.size()).collect { profiles.key(it) } as Set == [new ProfileKey("S1", "Sp1"), new ProfileKey("S1", "Sp2")] as Set
		(0..<profiles.size()).collect { value(profiles, it, "K1") } as Set == [2d, 0d] as Set
	}

	def "without taxa, the values of all taxa of a sample are summed into one profile"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, false)
		builder.sample("S1")
		builder.add("S1", "Sp1", "K1", 2d)
		builder.add("S1", "Sp2", "K1", 3d)

		when:
		def profiles = builder.build()

		then:
		profiles.size() == 1
		profiles.key(0) == new ProfileKey("S1", null)
		value(profiles, 0, "K1") == 5d
	}

	def "a function no module uses is not stored"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, false)
		builder.sample("S1")
		builder.add("S1", null, "K99", 7d)
		builder.add("S1", null, "K2", 1d)

		when:
		def profiles = builder.build()

		then: "only K2's value is kept"
		value(profiles, 0, "K2") == 1d
		(profiles.values(0) as List).sum() == 1d
	}

	def "the first three distinct function ids that read as numbers are kept, in input order"() {

		given:
		def builder = new ProfilesBuilder(DATABASE, false)
		builder.sample("S1")
		["K1", "1", "2", "1", "0", "5"].each { builder.add("S1", null, it, 1d) }

		expect:
		builder.numericFunctions() == ["1", "2", "0"]
	}

	def "function ids repeated in every sample give the numeric ids of their first occurrences"() {

		given: "a matrix row passes its function id once per sample"
		def builder = new ProfilesBuilder(DATABASE, false)
		["S1", "S2"].each { builder.sample(it) }
		["K1", "1", "K2", "2"].each { function -> ["S1", "S2"].each { builder.add(it, null, function, 1d) } }
		["0", "5"].each { builder.add("S1", null, it, 1d) }

		expect:
		builder.numericFunctions() == ["1", "2", "0"]
	}
}
