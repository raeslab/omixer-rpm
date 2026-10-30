package io.omixer.rpm.io

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.exceptions.InputFormatException
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException
import spock.lang.Specification
import spock.lang.TempDir

class MatrixReaderSpec extends Specification {

	@TempDir
	Path tmp

	private RecordingSink read(InputFormat format, String content) {
		Path file = tmp.resolve("matrix.tsv")
		Files.writeString(file, content)
		def sink = new RecordingSink()
		new MatrixReader(format).read(file, sink)
		sink
	}

	def "every sample of the header, then one value per sample and row"() {

		when:
		def sink = read(format, header + "\n" + row + "\n")

		then:
		sink.events.take(2) == ["sample S1", "sample S2"]
		sink.values() == [["S1", taxon, "K00001", 1d], ["S2", taxon, "K00001", 2.5d]]

		where:
		format               | header                | row                      | taxon
		InputFormat.KO       | "entry\tS1\tS2"       | "K00001\t1\t2.5"         | null
		InputFormat.TAXON_KO | "Species\tKO\tS1\tS2" | "Sp1\tK00001\t1\t2.5"    | "Sp1"
		InputFormat.HUMANN   | "entry\tS1\tS2"       | "K00001|g__Sp1\t1\t2.5"  | "g__Sp1"
	}

	def "a value that is not a number is rejected, reporting the file, line number and expected format"() {

		when:
		read(format, header + "\n" + row + "\n")

		then:
		InputFormatException e = thrown()
		e.message == "matrix.tsv, line 2: '${row}' is not ${expected}"
		e.cause instanceof NumberFormatException

		where: "a misplaced KO column (wrong -a) gives the same error"
		format               | header                | row                | expected
		InputFormat.KO       | "entry\tS1\tS2"       | "K00001\t1\tabc"   | "KO<TAB>one abundance per sample"
		InputFormat.KO       | "entry\tS1\tS2"       | "Sp1\tK00001\t1"   | "KO<TAB>one abundance per sample"
		InputFormat.TAXON_KO | "Species\tKO\tS1"     | "Sp1\tK00001\tabc" | "taxon<TAB>KO<TAB>one abundance per sample"
		InputFormat.HUMANN   | "entry\tS1"           | "K00001\tabc"      | "KO[|taxon]<TAB>one abundance per sample"
	}

	def "an empty cell is skipped, so its row has too few values"() {

		when:
		read(InputFormat.KO, "entry\tS1\tS2\tS3\nK00001\t1\t\t3\n")

		then:
		IncorrectNumberOfEntriesException e = thrown()
		e.message == "Line 2 have 2 entries instead of 4"
	}

	def "HUMAnN rows: KO or KO|taxon; an unstratified row has taxon NA"() {

		when:
		def sink = read(InputFormat.HUMANN, "entry\tS1\n${row}\t1\n")

		then:
		sink.values() == [["S1", taxon, function, 1d]]

		where:
		row                                                    | function    | taxon
		"K00001|g__Acidaminococcus.s__Acidaminococcus_fermentans" | "K00001" | "g__Acidaminococcus.s__Acidaminococcus_fermentans"
		"K19611|unclassified"                                  | "K19611"    | "unclassified"
		"K19611"                                               | "K19611"    | "NA"
		"UNGROUPED"                                            | "UNGROUPED" | "NA"
		"UNMAPPED"                                             | "UNMAPPED"  | "NA"
	}

	def "a HUMAnN file: 9 samples, 4 functions (2 KOs, UNMAPPED, UNGROUPED) and 20 taxa (18, NA, unclassified)"() {

		when:
		def sink = new RecordingSink()
		new MatrixReader(InputFormat.HUMANN).read(Path.of("src/test/resources/humann2KoTaxa.tsv"), sink)
		def sample = sink.values().findAll { it[0] == "10776.Healthy" }

		then:
		sink.events.count { it instanceof String } == 9
		sample.collect { it[2] }.toSet().size() == 4
		sample.collect { it[1] }.toSet().size() == 20
	}
}
