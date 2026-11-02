package io.omixer.rpm.inference.scoring

import spock.lang.Specification

class ComplexRuleSpec extends Specification {

	def "a complex rule decides whether an alternative is observed from its present subunits"() {

		expect:
		rule.isObserved(present, subunits) == observed

		where:
		rule            | present | subunits | observed
		ComplexRule.ANY | 1       | 3        | true
		ComplexRule.ALL | 1       | 3        | false
		ComplexRule.ANY | 3       | 3        | true
		ComplexRule.ALL | 3       | 3        | true
		ComplexRule.ANY | 0       | 2        | false
		ComplexRule.ALL | 0       | 2        | false
		ComplexRule.ANY | 1       | 1        | true
		ComplexRule.ALL | 1       | 1        | true
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
