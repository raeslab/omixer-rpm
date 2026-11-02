package io.omixer.rpm.profile.reader

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification
import spock.lang.TempDir

class ProfileReaderSpec extends Specification {

	@TempDir
	Path tmp

	def "a directory is read as one file per sample, anything else as a matrix; HUMAnN tables by their own reader"() {

		given:
		Path directory = Files.createDirectory(tmp.resolve("samples"))
		Path matrix = Files.writeString(tmp.resolve("matrix.tsv"), "entry\tS1\n")

		expect:
		ProfileReader.forInput(directory, InputFormat.KO) instanceof DirectoryReader
		ProfileReader.forInput(matrix, InputFormat.KO) instanceof MatrixReader
		ProfileReader.forInput(tmp.resolve("missing.tsv"), InputFormat.KO) instanceof MatrixReader
		ProfileReader.forInput(directory, InputFormat.TAXON_KO) instanceof DirectoryReader
		ProfileReader.forInput(matrix, InputFormat.HUMANN) instanceof HumannReader
	}

	def "a reader feeds the profiles directly, a HUMAnN reader through its row selection"() {

		given:
		Path input = Files.writeString(tmp.resolve("input.tsv"), "# Gene Family\tS1\nK00001\t1\nK00001|tax\t1\n")
		def profiles = new RecordingSink()

		expect:
		ProfileReader.selection(input, InputFormat.KO, false, profiles).is(profiles)
		ProfileReader.selection(input, InputFormat.TAXON_KO, true, profiles).is(profiles)
		ProfileReader.selection(input, InputFormat.HUMANN, true, profiles) instanceof HumannRowSelection
	}

	def "profiles are per taxon when asked for and the input has taxa: a HUMAnN table only when it is stratified (#format, #content)"() {

		given:
		Path input = Files.writeString(tmp.resolve("input.tsv"), content)

		expect:
		ProfileReader.perTaxon(input, format, requested) == perTaxon

		where:
		format               | requested | content                                         | perTaxon
		InputFormat.TAXON_KO | true      | "Species\tKO\tS1\ntax\tK00001\t1\n"            | true
		InputFormat.TAXON_KO | false     | "Species\tKO\tS1\ntax\tK00001\t1\n"            | false
		InputFormat.HUMANN   | true      | "# Gene Family\tS1\nK00001\t1\nK00001|tax\t1\n" | true
		InputFormat.HUMANN   | true      | "# Gene Family\tS1\nK00001\t1\n"                | false
		InputFormat.HUMANN   | false     | "# Gene Family\tS1\nK00001\t1\nK00001|tax\t1\n" | false
	}
}
