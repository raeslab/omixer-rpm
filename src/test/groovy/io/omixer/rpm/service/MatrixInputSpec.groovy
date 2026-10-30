package io.omixer.rpm.service

import java.nio.file.Path

import io.omixer.rpm.exceptions.InputFormatException
import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.scoring.Estimator
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.io.InputFormat
import io.omixer.rpm.io.ModuleDatabaseReader
import io.omixer.rpm.service.impl.ModuleManagerImpl
import io.omixer.utils.exceptions.DuplicateColumnException

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Input given as one matrix: a header with the sample names, then one row per KO (-i matrix.tsv)
 */
class MatrixInputSpec extends Specification {

	@TempDir
	Path tmp

	def "a sample name that appears twice in the header stops the run with an error naming it"() {

		setup:
		def manager = new ModuleManagerImpl()
		manager.setInputFormat(InputFormat.KO)
		def options = new ModuleInferenceOptions()
		options.setEstimator(new Estimator.Median())
		File matrix = tmp.resolve("matrix.tsv").toFile()
		matrix.text = "entry\tS1\tS2\tS1\nK00001\t1\t2\t3\n"

		when:
		manager.inferModules(matrix, options,
				CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list"))))

		then: "before, the values of both S1 columns were silently added together"
		DuplicateColumnException e = thrown()
		e.message.contains("'S1'")
		e.message.contains("matrix.tsv")
	}

	def "a value that is not a number stops the run with an error giving the file and line"() {

		setup:
		def manager = new ModuleManagerImpl()
		manager.setInputFormat(annotation)
		def options = new ModuleInferenceOptions()
		options.setEstimator(new Estimator.Median())
		File matrix = tmp.resolve(file).toFile()
		matrix.text = content

		when:
		manager.inferModules(matrix, options,
				CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list"))))

		then: "before, a raw NumberFormatException with no file or line"
		InputFormatException e = thrown()
		e.message.startsWith("${file}, line ${line}:")

		where: "a typo in a value, or a taxon matrix read with -a 1"
		file           | annotation     | content                                              | line
		"typo.tsv"     | InputFormat.KO | "entry\tS1\tS2\nK00001\t1\t2\nK00002\t1\tabc\n"      | 3
		"taxon.tsv"    | InputFormat.KO | "Species\tKO\tS1\nSp1\tK00001\t1\n"                  | 2
	}
}
