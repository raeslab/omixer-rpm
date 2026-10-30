package io.omixer.rpm.io

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Which reader reads an input (-i) and which writer writes an output format (-e)
 */
class ReaderWriterChoiceSpec extends Specification {

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

	def "-e 1 writes per-sample files, -e 2 the matrices"() {

		expect:
		OutputFormat.PER_SAMPLE.writer() instanceof PerSampleWriter
		OutputFormat.MATRIX.writer() instanceof MatrixWriter
	}
}
