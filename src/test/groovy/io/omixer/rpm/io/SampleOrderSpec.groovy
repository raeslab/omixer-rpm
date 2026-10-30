package io.omixer.rpm.io

import io.omixer.rpm.Settings
import java.nio.file.Path

import io.omixer.rpm.scoring.Estimator
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.io.InputFormat
import io.omixer.rpm.io.ModuleDatabaseReader

import spock.lang.Specification
import spock.lang.TempDir

/**
 * The inference keeps a predictable sample order, which the matrix output uses for its columns
 */
class SampleOrderSpec extends Specification {

	@TempDir
	Path tmp

	def database
	def options

	def setup() {
		database = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list")))
		options = new Settings()
		options.setEstimator(new Estimator.Median())
		options.setCoverage(0d)
	}

	def "matrix input keeps the samples in header order"() {

		setup:
		List<String> samples = ["S3", "S1", "Z9", "S10", "A2", "S2"]
		File matrix = tmp.resolve("matrix.tsv").toFile()
		matrix.text = (["entry"] + samples).join("\t") + "\n" +
				(["K00001"] + samples.collect { "1" }).join("\t") + "\n"
		options.setThreads(concurrent ? 4 : 1)

		when:
		def inference = options.inferSamples(matrix, InputFormat.KO, database)

		then:
		inference.keySet() as List == samples

		where:
		concurrent << [false, true]
	}

	def "directory input orders the samples by file name"() {

		setup:
		File dir = tmp.resolve("samples").toFile()
		dir.mkdir()
		["S3", "S1", "Z9", "S10", "A2", "S2"].each { new File(dir, it).text = "K00001\t1\n" }

		when:
		def inference = options.inferSamples(dir, InputFormat.KO, database)

		then:
		inference.keySet() as List == ["A2", "S1", "S10", "S2", "S3", "Z9"]
	}
}
