package io.omixer.rpm.service

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.scoring.Estimator
import io.omixer.rpm.exceptions.DuplicateSampleNameException
import io.omixer.rpm.exceptions.InputFormatException
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.io.ModuleDatabaseReader
import io.omixer.rpm.service.impl.ModuleManagerImpl

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Input given as a directory with one headerless file per sample (-i dir/).
 * Each test copies the two samples of src/test/resources/input_directory (S1, S2) and adds one extra entry.
 */
class DirectoryInputSpec extends Specification {

	@TempDir
	Path dir

	def manager
	def database
	def options

	def setup() {
		manager = new ModuleManagerImpl()
		manager.setInputFormat("1")
		database = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module.list")))
		options = new ModuleInferenceOptions()
		options.setEstimator(new Estimator.Median())
		options.setCoverage(0d)
		["S1", "S2"].each { Files.copy(Path.of("src/test/resources/input_directory", it), dir.resolve(it)) }
	}

	def "hidden files in the input directory are not read as samples"() {

		setup:
		dir.resolve(name).toFile().bytes = content

		when:
		def inference = manager.inferModules(dir.toFile(), options, database)

		then: "only S1 and S2; a valid hidden file used to become a sample with an empty name"
		inference.keySet() == ["S1", "S2"] as Set

		where:
		name           | content
		".hidden"      | new File("src/test/resources/input_directory/S1").bytes
		".DS_Store"    | [0, 0, 0, 1, 66, 117, 100, 49, 0] as byte[]
	}

	def "sub-directories of the input directory are ignored"() {

		setup:
		Files.createDirectory(dir.resolve("subdir"))

		when:
		def inference = manager.inferModules(dir.toFile(), options, database)

		then:
		inference.keySet() == ["S1", "S2"] as Set
	}

	def "a visible file that is not a sample stops the run with an error naming the file"() {

		setup:
		dir.resolve("notes.txt").toFile().text = "these are not abundances\n"

		when:
		manager.inferModules(dir.toFile(), options, database)

		then: "not skipped silently: a misformatted sample must not disappear from the results"
		InputFormatException e = thrown()
		e.message.contains("notes.txt, line 1")
	}

	def "a sample is named after its file without the last extension only"() {

		setup:
		["run.v2.tsv", "run.v3.tsv"].each { Files.copy(dir.resolve("S1"), dir.resolve(it)) }

		when:
		def inference = manager.inferModules(dir.toFile(), options, database)

		then: "before, everything after the first dot was dropped and both files became sample 'run'"
		inference.keySet() == ["S1", "S2", "run.v2", "run.v3"] as Set
	}

	def "two files that give the same sample name stop the run with an error naming both"() {

		setup:
		["S3.a", "S3.b"].each { Files.copy(dir.resolve("S1"), dir.resolve(it)) }

		when:
		manager.inferModules(dir.toFile(), options, database)

		then: "before, one of the two files was silently lost"
		DuplicateSampleNameException e = thrown()
		e.message.contains("S3.a")
		e.message.contains("S3.b")
		e.message.contains("'S3'")
	}
}
