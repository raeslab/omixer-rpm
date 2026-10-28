package io.omixer.rpm.core

import java.nio.file.Files

import spock.lang.Specification

class ExitStatusSpec extends Specification {

	private static final String MATRIX = "src/test/resources/matrix.tsv"
	private static final String DATABASE = "src/test/resources/module.list"

	def "a successful run, the help and no arguments exit with 0"() {

		given:
		def outputDir = Files.createTempDirectory("exit-status").toString()

		expect:
		InferenceApp.run(args.collect { it == "OUT" ? outputDir : it } as String[]) == 0

		where:
		args << [
			["-i", MATRIX, "-d", DATABASE, "-o", "OUT"],
			["-h"],
			[]
		]
	}

	def "a command line error exits with 2"() {

		expect:
		InferenceApp.run(args as String[]) == 2

		where:
		args << [
			["-i", MATRIX, "-d", DATABASE, "-s", "foo"],
			["-i", MATRIX, "-d", DATABASE, "--complex-rule", "foo"],
			["-i", MATRIX, "-d", DATABASE, "-c", "2"],
			["-i", MATRIX, "-d", DATABASE, "--no-such-option"],
			["-i", MATRIX],
			["-d", DATABASE]
		]
	}

	def "an input error exits with 1"() {

		given:
		def outputDir = Files.createTempDirectory("exit-status").toString()

		expect:
		InferenceApp.run(["-i", input, "-d", database, "-a", annotation, "-o", outputDir] as String[]) == 1

		where:
		input                                 | database                | annotation
		"no-such-input.tsv"                   | DATABASE                | "1"
		MATRIX                                | "no-such-database.list" | "1"
		"src/test/resources/taxon_matrix.tsv" | DATABASE                | "1"
	}
}
