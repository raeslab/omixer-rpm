package io.omixer.rpm.parsers

import io.omixer.rpm.exceptions.InputFormatException
import io.omixer.rpm.model.BasicFeature

import spock.lang.Specification

/**
 * Row mappers for headerless sample files (directory input)
 */
class RowMapperSpec extends Specification {

	def "FunctionRowMapper maps KO TAB abundance"() {

		when:
		BasicFeature feature = new FunctionRowMapper().apply(new InputLine("S1", 1, "K00001\t2.5"))

		then:
		feature.getFunction() == "K00001"
		feature.getCount() == 2.5d
		feature.getTaxon() == null
	}

	def "TaxonFunctionRowMapper maps taxon TAB KO TAB abundance"() {

		when:
		BasicFeature feature = new TaxonFunctionRowMapper().apply(new InputLine("S1", 1, "Sp1\tK00001\t2.5"))

		then:
		feature.getTaxon() == "Sp1"
		feature.getFunction() == "K00001"
		feature.getCount() == 2.5d
	}

	def "FunctionRowMapper rejects a malformed line, reporting the file, line number and expected format"() {

		when:
		new FunctionRowMapper().apply(new InputLine("S1.tsv", 7, line))

		then:
		InputFormatException e = thrown()
		e.message == "S1.tsv, line 7: '${line}' is not KO<TAB>abundance"

		where:
		line << ["K00002", "K00002\tmany", "Sp1\tK00002\t3", ""]
	}

	def "TaxonFunctionRowMapper rejects a malformed line, reporting the file, line number and expected format"() {

		when:
		new TaxonFunctionRowMapper().apply(new InputLine("S1.tsv", 7, line))

		then:
		InputFormatException e = thrown()
		e.message == "S1.tsv, line 7: '${line}' is not taxon<TAB>KO<TAB>abundance"

		where:
		line << ["K00002\t3", "Sp1\tK00002\tmany", ""]
	}

	def "a row mapper keeps no state, so one instance can be shared across files"() {

		setup:
		def mapper = new FunctionRowMapper()
		mapper.apply(new InputLine("S1.tsv", 1, "K00001\t1"))
		mapper.apply(new InputLine("S1.tsv", 2, "K00002\t2"))

		when: "the second file's first line is malformed"
		mapper.apply(new InputLine("S2.tsv", 1, "oops"))

		then: "the error reports S2.tsv, line 1, not a line count carried over from S1.tsv"
		InputFormatException e = thrown()
		e.message.startsWith("S2.tsv, line 1:")
	}
}
