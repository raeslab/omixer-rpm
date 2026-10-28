package io.omixer.rpm.model.enums

import io.omixer.rpm.model.Ortholog

import spock.lang.Specification

class ComplexRuleSpec extends Specification {

	private static List<Ortholog> step(List<Double> counts) {
		counts.collect { def o = new Ortholog("K"); o.setCount(it); o }
	}

	def "a complex rule decides whether a step is observed"() {

		expect:
		rule.isObserved(step(counts)) == observed

		where:
		rule             | counts         | observed
		ComplexRule.ANY  | [6d, 0d, 0d]   | true
		ComplexRule.ALL  | [6d, 0d, 0d]   | false
		ComplexRule.ANY  | [6d, 3d, 9d]   | true
		ComplexRule.ALL  | [6d, 3d, 9d]   | true
		ComplexRule.ANY  | [0d, 0d]       | false
		ComplexRule.ALL  | [0d, 0d]       | false
		ComplexRule.ANY  | [5d]           | true
		ComplexRule.ALL  | [5d]           | true
	}

	def "a complex rule is chosen on the command line by its lower-case name"() {

		expect:
		ComplexRule.fromName(name) == rule

		where:
		name        | rule
		"any"       | Optional.of(ComplexRule.ANY)
		"all"       | Optional.of(ComplexRule.ALL)
		"fraction"  | Optional.empty()
		"ANY"       | Optional.empty()
		"explained" | Optional.empty()
	}
}
