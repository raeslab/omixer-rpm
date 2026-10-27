package io.omixer.rpm.model.enums

import io.omixer.rpm.model.Ortholog

import spock.lang.Specification

class ComplexRuleSpec extends Specification {

	private static List<Ortholog> step(List<Double> counts) {
		counts.collect { def o = new Ortholog("K"); o.setCount(it); o }
	}

	def "a complex rule decides whether a step is observed and how much coverage it earns"() {

		expect:
		rule.isObserved(step(counts)) == observed
		Math.abs(rule.coverage(step(counts)) - coverage) < 1e-12

		where:
		rule                  | counts         | observed | coverage
		ComplexRule.ANY       | [6d, 0d, 0d]   | true     | 1d
		ComplexRule.ALL       | [6d, 0d, 0d]   | false    | 0d
		ComplexRule.FRACTION  | [6d, 0d, 0d]   | true     | 1d / 3d
		ComplexRule.ANY       | [6d, 3d, 9d]   | true     | 1d
		ComplexRule.ALL       | [6d, 3d, 9d]   | true     | 1d
		ComplexRule.FRACTION  | [6d, 3d, 9d]   | true     | 1d
		ComplexRule.ANY       | [0d, 0d]       | false    | 0d
		ComplexRule.ALL       | [0d, 0d]       | false    | 0d
		ComplexRule.FRACTION  | [0d, 0d]       | false    | 0d
		ComplexRule.ANY       | [5d]           | true     | 1d
		ComplexRule.ALL       | [5d]           | true     | 1d
		ComplexRule.FRACTION  | [5d]           | true     | 1d
	}

	def "a complex rule is chosen on the command line by its lower-case name"() {

		expect:
		ComplexRule.fromName(name) == rule

		where:
		name        | rule
		"any"       | Optional.of(ComplexRule.ANY)
		"all"       | Optional.of(ComplexRule.ALL)
		"fraction"  | Optional.of(ComplexRule.FRACTION)
		"ANY"       | Optional.empty()
		"explained" | Optional.empty()
	}
}
