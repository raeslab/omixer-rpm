package io.omixer.rpm.io

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.exceptions.InputFormatException
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Reading the rows of the files of a directory (sample naming and file selection: DirectoryInputSpec)
 */
class DirectoryReaderSpec extends Specification {

	@TempDir
	Path tmp

	private RecordingSink read(InputFormat format, String content) {
		Files.writeString(tmp.resolve("S1.tsv"), content)
		def sink = new RecordingSink()
		new DirectoryReader(format).read(tmp, sink)
		sink
	}

	def "a row is KO TAB abundance, or taxon TAB KO TAB abundance"() {

		when:
		def sink = read(format, row)

		then:
		sink.events == ["sample S1", ["S1", taxon, "K00001", 2.5d]]

		where:
		format               | row                  | taxon
		InputFormat.KO       | "K00001\t2.5"        | null
		InputFormat.TAXON_KO | "Sp1\tK00001\t2.5"   | "Sp1"
	}

	def "a malformed row is rejected, reporting the file, line number and expected format"() {

		when: "a valid first line, then the malformed one"
		read(format, (format == InputFormat.KO ? "K00001\t1" : "Sp1\tK00001\t1") + "\n" + line + "\n")

		then:
		InputFormatException e = thrown()
		e.message == "S1.tsv, line 2: '${line}' is not ${expected}"

		where:
		format               | line                 | expected
		InputFormat.KO       | "K00002"             | "KO<TAB>abundance"
		InputFormat.KO       | "K00002\tmany"       | "KO<TAB>abundance"
		InputFormat.KO       | ""                   | "KO<TAB>abundance"
		InputFormat.TAXON_KO | "K00002\t3"          | "taxon<TAB>KO<TAB>abundance"
		InputFormat.TAXON_KO | "Sp1\tK00002\tmany"  | "taxon<TAB>KO<TAB>abundance"
	}

	def "line numbers start again in every file"() {

		given:
		Files.writeString(tmp.resolve("S1.tsv"), "K00001\t1\nK00002\t2\n")
		Files.writeString(tmp.resolve("S2.tsv"), "oops\n")

		when:
		new DirectoryReader(InputFormat.KO).read(tmp, new RecordingSink())

		then: "the error reports S2.tsv, line 1, not a line count carried over from S1.tsv"
		InputFormatException e = thrown()
		e.message.startsWith("S2.tsv, line 1:")
	}
}
