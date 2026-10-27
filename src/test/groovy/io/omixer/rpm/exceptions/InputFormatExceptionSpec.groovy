package io.omixer.rpm.exceptions

import io.omixer.utils.readers.FileLine

import spock.lang.Specification

class InputFormatExceptionSpec extends Specification {

	def "the message shortens a long line, which a matrix with many samples can have"() {

		setup:
		String line = "K00001" + ("\t1.5" * 1000)

		when:
		String message = new InputFormatException(new FileLine("matrix.tsv", 2, line), "KO<TAB>values", null).message

		then:
		message.startsWith("matrix.tsv, line 2: 'K00001\t1.5")
		message.contains("...' is not KO<TAB>values")
		message.length() < 150
	}

	def "the message keeps a short line whole"() {

		expect:
		new InputFormatException(new FileLine("S1", 7, "K00001\tmany"), "KO<TAB>abundance", null).message ==
				"S1, line 7: 'K00001\tmany' is not KO<TAB>abundance"
	}
}
