package io.omixer.rpm.profile.reader

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.exceptions.InputFormatException
import spock.lang.Specification
import spock.lang.TempDir

/** Which rows of a HUMAnN table reach the profiles: the selection between the parser and the profile builder */
class HumannRowSelectionSpec extends Specification {

	/** A HUMAnN 3 KO table: header, special rows, community totals followed by their strata */
	private static final String TABLE = """\
		# Gene Family\tS1_Abundance-RPKs\tS2_Abundance-RPKs
		UNMAPPED\t100\t200
		UNGROUPED\t50\t60
		UNGROUPED|g__Bacteroides.s__Bacteroides_fragilis\t50\t60
		K00001: alcohol dehydrogenase [EC:1.1.1.1]\t3\t4
		K00001: alcohol dehydrogenase [EC:1.1.1.1]|g__Bacteroides.s__Bacteroides_fragilis\t2\t4
		K00001: alcohol dehydrogenase [EC:1.1.1.1]|unclassified\t1\t0
		K00002\t5\t0
		K00002|g__Escherichia.s__Escherichia_coli\t5\t0
		""".stripIndent()

	@TempDir
	Path tmp

	/** Parses a table through the selection of a run (per taxon or per sample) into a recording sink */
	private RecordingSink select(Path table, boolean perTaxon) {
		def sink = new RecordingSink()
		new HumannReader().read(table, HumannRowSelection.of(table, perTaxon, sink))
		sink
	}

	private Path table(String content) {
		Files.writeString(tmp.resolve("ko.tsv"), content)
	}

	def "per taxon: the stratified rows, with the stratum as taxon; unclassified is a taxon"() {

		when:
		def sink = select(table(TABLE), true)

		then: "the samples, then the strata only: no total, no special row"
		sink.events.take(2) == ["sample S1", "sample S2"]
		sink.values() == [
			["S1", "g__Bacteroides.s__Bacteroides_fragilis", "K00001", 2d],
			["S2", "g__Bacteroides.s__Bacteroides_fragilis", "K00001", 4d],
			["S1", "unclassified", "K00001", 1d],
			["S2", "unclassified", "K00001", 0d],
			["S1", "g__Escherichia.s__Escherichia_coli", "K00002", 5d],
			["S2", "g__Escherichia.s__Escherichia_coli", "K00002", 0d]
		]
	}

	def "per sample: the community totals only, never added to their strata"() {

		when:
		def sink = select(table(TABLE), false)

		then:
		sink.values() == [
			["S1", null, "K00001", 3d],
			["S2", null, "K00001", 4d],
			["S1", null, "K00002", 5d],
			["S2", null, "K00002", 0d]
		]
	}

	def "per sample, a table with only stratified rows (humann_split_stratified_table): the strata, summed later"() {

		given: "the table without its total rows: header and the lines with a stratum"
		def stratified = TABLE.readLines().findAll { it.startsWith("#") || it.contains("|") }.join("\n")

		when:
		def sink = select(table(stratified), false)

		then: "every stratum (the profile builder drops the taxon per sample and sums a KO's values: K00001 = 3 and 4, the removed totals)"
		sink.values() == [
			["S1", "g__Bacteroides.s__Bacteroides_fragilis", "K00001", 2d],
			["S2", "g__Bacteroides.s__Bacteroides_fragilis", "K00001", 4d],
			["S1", "unclassified", "K00001", 1d],
			["S2", "unclassified", "K00001", 0d],
			["S1", "g__Escherichia.s__Escherichia_coli", "K00002", 5d],
			["S2", "g__Escherichia.s__Escherichia_coli", "K00002", 0d]
		]
	}

	def "special rows are skipped: #row (per taxon: #perTaxon)"() {

		when:
		def sink = select(table("# Gene Family\tS1\n${row}\t7\nK00001\t1\nK00001|tax\t1\n"), perTaxon)

		then:
		sink.values().collect { it[2] } == ["K00001"]

		where:
		[row, perTaxon] << [["UNMAPPED", "READS_UNMAPPED", "UNGROUPED", "UNINTEGRATED", "UNGROUPED|tax",
			"UNGROUPED|unclassified"], [true, false]].combinations()
	}

	def "an unstratified special row does not count as a total: per sample, a table with only strata besides it gives its strata"() {

		when:
		def sink = select(table("# Gene Family\tS1\nUNMAPPED\t9\nUNGROUPED\t4\nK00001|tax\t2\nK00001|unclassified\t1\n"), false)

		then:
		sink.values() == [["S1", "tax", "K00001", 2d], ["S1", "unclassified", "K00001", 1d]]
	}

	def "a table with no function row (only a header and special rows) gives its samples and no values (per taxon: #perTaxon)"() {

		expect:
		select(table("# Gene Family\tS1\nUNMAPPED\t9\n"), perTaxon).events == ["sample S1"]

		where:
		perTaxon << [true, false]
	}

	def "a table is stratified unless it has only community totals: #content"() {

		expect:
		HumannRowSelection.stratified(table("# Gene Family\tS1\n" + content)) == stratified

		where:
		content                                      | stratified
		"K00001\t3\nK00001|tax\t3\n"                 | true
		"K00001|tax\t3\n"                            | true
		"K00001\t3\n"                                | false
		"UNMAPPED\t9\nK00001\t3\n"                   | false
		"UNMAPPED\t9\nUNGROUPED|tax\t3\nK00001\t3\n" | false
		""                                           | true
	}

	def "a directory is rejected before it is scanned (#use)"() {

		given:
		Path directory = Files.createDirectory(tmp.resolve("tables"))

		when:
		call(directory)

		then:
		InputFormatException e = thrown()
		e.message == "tables is a directory: -a humann reads one HUMAnN table (join per-sample tables with humann_join_tables)"

		where:
		use          | call
		"stratified" | { Path input -> HumannRowSelection.stratified(input) }
		"of"         | { Path input -> HumannRowSelection.of(input, true, new RecordingSink()) }
	}

	def "a HUMAnN 2 table per taxon: 9 samples, 2 KOs and their 14 taxa (the taxa of UNGROUPED only are not profiles), no NA taxon"() {

		given:
		Path table = Path.of("src/test/resources/humann2KoTaxa.tsv")

		when:
		def sink = select(table, true)
		def sample = sink.values().findAll { it[0] == "10776.Healthy" }

		then:
		sink.events.count { it instanceof String } == 9
		sample.collect { it[2] }.toSet() == ["K00001", "K19611"] as Set
		sample.collect { it[1] }.toSet().size() == 14
		!sample.any { it[1] == null || it[1] == "NA" }
	}
}
