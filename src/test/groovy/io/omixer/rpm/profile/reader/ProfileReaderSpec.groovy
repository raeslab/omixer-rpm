package io.omixer.rpm.profile.reader

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification
import spock.lang.TempDir

class ProfileReaderSpec extends Specification {

	@TempDir
	Path tmp

	def "a directory is read as one file per sample, anything else as a matrix"() {

		given:
		Path directory = Files.createDirectory(tmp.resolve("samples"))
		Path matrix = Files.writeString(tmp.resolve("matrix.tsv"), "entry\tS1\n")

		expect:
		ProfileReader.forInput(directory, InputFormat.KO) instanceof DirectoryReader
		ProfileReader.forInput(matrix, InputFormat.KO) instanceof MatrixReader
		ProfileReader.forInput(tmp.resolve("missing.tsv"), InputFormat.KO) instanceof MatrixReader
	}
}
