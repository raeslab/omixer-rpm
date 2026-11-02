package io.omixer.rpm.profile.reader

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.exceptions.DuplicateSampleNameException
import io.omixer.rpm.exceptions.InputFormatException
import spock.lang.Specification
import spock.lang.TempDir

/** The HUMAnN table parser: every row is passed on; which rows are used is HumannRowSelectionSpec */
class HumannReaderSpec extends Specification {

	@TempDir
	Path tmp

	private RecordingSink read(Path input) {
		def sink = new RecordingSink()
		new HumannReader().read(input, sink)
		sink
	}

	private Path table(String content) {
		Files.writeString(tmp.resolve("ko.tsv"), content)
	}

	def "every row is passed on: special rows, totals (no taxon) and strata (the stratum as taxon), KO names removed"() {

		when:
		def sink = read(table("""\
			# Gene Family\tS1_Abundance-RPKs\tS2_Abundance-RPKs
			UNMAPPED\t100\t200
			UNGROUPED|g__Bacteroides.s__Bacteroides_fragilis\t50\t60
			K00001: alcohol dehydrogenase [EC:1.1.1.1]\t3\t4
			K00001: alcohol dehydrogenase [EC:1.1.1.1]|unclassified\t3\t4
			""".stripIndent()))

		then: "samples first, then one value per row and sample, zeros included, in input order"
		sink.events == [
			"sample S1", "sample S2",
			["S1", null, "UNMAPPED", 100d], ["S2", null, "UNMAPPED", 200d],
			["S1", "g__Bacteroides.s__Bacteroides_fragilis", "UNGROUPED", 50d],
			["S2", "g__Bacteroides.s__Bacteroides_fragilis", "UNGROUPED", 60d],
			["S1", null, "K00001", 3d], ["S2", null, "K00001", 4d],
			["S1", "unclassified", "K00001", 3d], ["S2", "unclassified", "K00001", 4d]
		]
	}

	def "the sample of column #column is #sample"() {

		when:
		def sink = read(table("# Gene Family\t${column}\nK00001\t1\n"))

		then:
		sink.events[0] == "sample " + sample

		where:
		column                   | sample
		"S1_Abundance-RPKs"      | "S1"
		"S1_Abundance-CPM"       | "S1"
		"S1_Abundance"           | "S1"
		"S1"                     | "S1"
		"S1_Abundance_extra"     | "S1_Abundance_extra"
		"_Abundance-RPKs"        | "_Abundance-RPKs"
	}

	def "two columns that give the same sample are rejected"() {

		when:
		read(table("# Gene Family\tS1_Abundance-RPKs\tS1\nK00001\t1\t2\n"))

		then:
		DuplicateSampleNameException e = thrown()
		e.message == "ko.tsv: columns 'S1_Abundance-RPKs' and 'S1' both give sample name 'S1'"
	}

	def "a table with only a header gives its samples and no values"() {

		expect:
		read(table("# Gene Family\tS1_Abundance-RPKs\n")).events == ["sample S1"]
	}

	def "a directory is rejected: HUMAnN tables are joined into one (humann_join_tables)"() {

		when:
		read(Files.createDirectory(tmp.resolve("tables")))

		then:
		InputFormatException e = thrown()
		e.message == "tables is a directory: -a humann reads one HUMAnN table (join per-sample tables with humann_join_tables)"
	}

	def "an empty file is rejected"() {

		when:
		read(table(""))

		then:
		IOException e = thrown()
		e.message == "ko.tsv is empty: a matrix starts with a header line"
	}

	def "a value that is not a number is rejected, reporting the file, line number and expected format"() {

		when:
		read(table("# Gene Family\tS1\nK00001|tax\tabc\n"))

		then:
		InputFormatException e = thrown()
		e.message == "ko.tsv, line 2: 'K00001|tax\tabc' is not function[|taxon]<TAB>one abundance per sample"
	}
}
