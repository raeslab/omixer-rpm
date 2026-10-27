package io.omixer.rpm.service

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.model.enums.ModuleInferenceOptimizers
import io.omixer.rpm.exceptions.InputFormatException
import io.omixer.rpm.parsers.ModuleParser
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
	def referenceModules
	def options

	def setup() {
		manager = new ModuleManagerImpl()
		manager.setInputFormat("1")
		referenceModules = ModuleParser.parseModuleFile(new File("src/test/resources/module.list"))
		options = new ModuleInferenceOptions()
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName())
		options.setCoverage(0d)
		["S1", "S2"].each { Files.copy(Path.of("src/test/resources/input_directory", it), dir.resolve(it)) }
	}

	def "hidden files in the input directory are not read as samples"() {

		setup:
		dir.resolve(name).toFile().bytes = content

		when:
		def inference = manager.inferModules(dir.toFile(), options, referenceModules)

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
		def inference = manager.inferModules(dir.toFile(), options, referenceModules)

		then:
		inference.keySet() == ["S1", "S2"] as Set
	}

	def "a visible file that is not a sample stops the run with an error naming the file"() {

		setup:
		dir.resolve("notes.txt").toFile().text = "these are not abundances\n"

		when:
		manager.inferModules(dir.toFile(), options, referenceModules)

		then: "not skipped silently: a misformatted sample must not disappear from the results"
		InputFormatException e = thrown()
		e.message.contains("notes.txt, line 1")
	}
}
