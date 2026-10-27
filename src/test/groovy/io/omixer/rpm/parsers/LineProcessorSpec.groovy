package io.omixer.rpm.parsers

import io.omixer.rpm.exceptions.InputFormatException
import io.omixer.rpm.model.BasicFeature
import io.omixer.utils.readers.FileLine

import spock.lang.Specification

/**
 * Line processors for matrix input: a row of the matrix becomes one feature per sample
 */
class LineProcessorSpec extends Specification {

	def "a matrix line processor maps one feature per sample"() {

		when:
		List<BasicFeature> features = processor.process(new FileLine("matrix.tsv", 2, line), "\t")

		then:
		features*.getFunction() == ["K00001", "K00001"]
		features*.getTaxon() == taxa
		features*.getCount() == [1d, 2.5d]

		where:
		processor                              | line                           | taxa
		new FunctionLineProcessor()            | "K00001\t1\t2.5"               | [null, null]
		new TaxonFunctionLineProcessor()       | "Sp1\tK00001\t1\t2.5"          | ["Sp1", "Sp1"]
		new TaxonFunctionHumannLineProcessor() | "K00001|g__Sp1\t1\t2.5"       | ["g__Sp1", "g__Sp1"]
	}

	def "a matrix line processor rejects a value that is not a number, reporting the file, line number and expected format"() {

		when:
		processor.process(new FileLine("matrix.tsv", 3, line), "\t")

		then:
		InputFormatException e = thrown()
		e.message == "matrix.tsv, line 3: '${line}' is not ${expected}"
		e.cause instanceof NumberFormatException

		where: "a misplaced KO column (wrong -a) gives the same error"
		processor                              | line                     | expected
		new FunctionLineProcessor()            | "K00001\t1\tabc"         | "KO<TAB>one abundance per sample"
		new FunctionLineProcessor()            | "Sp1\tK00001\t1"         | "KO<TAB>one abundance per sample"
		new TaxonFunctionLineProcessor()       | "Sp1\tK00001\tabc"       | "taxon<TAB>KO<TAB>one abundance per sample"
		new TaxonFunctionHumannLineProcessor() | "K00001\tabc"            | "KO[|taxon]<TAB>one abundance per sample"
	}
}
