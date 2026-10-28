package io.omixer.rpm.model

import spock.lang.Specification

class BasicFeatureSpec extends Specification {

	def "a function id that can be read as a number is flagged, which hints at a wrong -a annotation format"() {

		expect:
		new BasicFeature(null, function, 1d).isFunctionNumeric() == numeric

		where:
		function    | numeric
		"58.842"    | true
		"1"         | true
		"1e3"       | true
		"K00001"    | false
		"K99999"    | false
		"1.1.1.1"   | false
		"MF0001"    | false
	}
}
