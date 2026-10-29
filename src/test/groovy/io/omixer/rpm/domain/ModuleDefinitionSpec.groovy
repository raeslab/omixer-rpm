package io.omixer.rpm.domain

import spock.lang.Specification

class ModuleDefinitionSpec extends Specification {

	def "an alternative, a step and a module each need at least one part"() {

		when:
		create()

		then:
		thrown(IllegalArgumentException)

		where:
		create << [
			{ new Alternative([]) },
			{ new Step([]) },
			{ new ModuleDefinition("M1", "first", []) }
		]
	}

	def "a definition keeps its own copy of the lists it is built from"() {

		given:
		def subunits = ["K1"]
		def alternative = new Alternative(subunits)

		when:
		subunits << "K2"

		then:
		alternative.subunits() == ["K1"]
	}
}
