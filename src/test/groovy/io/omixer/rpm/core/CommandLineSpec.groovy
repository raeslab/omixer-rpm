package io.omixer.rpm.core

import io.omixer.rpm.LogCapture
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class CommandLineSpec extends Specification {

	private static final String MATRIX = "src/test/resources/matrix.tsv"
	private static final String TAXON_MATRIX = "src/test/resources/taxon_matrix.tsv"
	private static final String DATABASE = "src/test/resources/module.list"
	private static final String EDGE = "src/test/resources/golden/inputs/edge"

	@Shared
	LogCapture log = new LogCapture()

	@TempDir
	File tempDir

	def setupSpec() {
		log.install()
	}

	def cleanupSpec() {
		log.uninstall()
	}

	def setup() {
		log.lines.clear()
	}

	/** Runs the command line with an output directory of its own */
	private int run(List<String> args) {
		InferenceApp.run((args + ["-o", outputDir().path]) as String[])
	}

	private File outputDir() {
		new File(tempDir, "out")
	}

	private List<String> outputFiles() {
		outputDir().exists() ? outputDir().list().toList().sort() : []
	}

	/** The warnings and errors logged by the run */
	private List<String> problems() {
		log.lines.findAll { !it.startsWith("INFO ") }
	}

	def "a successful run, the help and no arguments exit with 0"() {

		expect:
		InferenceApp.run(args.collect { it == "OUT" ? outputDir().path : it } as String[]) == 0

		where:
		args << [
			["-i", MATRIX, "-d", DATABASE, "-o", "OUT"],
			["-h"],
			[]
		]
	}

	def "a command line error exits with 2, says what is wrong and writes nothing"() {

		expect:
		run(args) == 2
		problems() == ["ERROR " + message]
		outputFiles() == []

		where:
		args                                                          | message
		["-i", MATRIX, "-d", DATABASE, "-s", "foo"]                   | "foo is not a valid score estimator. Please choose one of [median|average|sum|min]"
		["-i", MATRIX, "-d", DATABASE, "--complex-rule", "explained"] | "explained is not a valid complex rule. Please choose one of [any|all]"
		["-i", MATRIX, "-d", DATABASE, "-c", "2"]                     | "2 is not a valid coverage value. Please provide a decimal value between 0 and 1, or -1 to enable automatic guessing of the coverage threshold."
		["-i", MATRIX, "-d", DATABASE, "-c", "-0.5"]                  | "-0.5 is not a valid coverage value. Please provide a decimal value between 0 and 1, or -1 to enable automatic guessing of the coverage threshold."
		["-i", MATRIX, "-d", DATABASE, "-c", "NaN"]                   | "NaN is not a valid coverage value. Please provide a decimal value between 0 and 1, or -1 to enable automatic guessing of the coverage threshold."
		// HUMAnN2 (h2) is not offered, neither as annotation nor as export format
		["-i", MATRIX, "-d", DATABASE, "-a", "h2"]                    | "Annotation (h2) is not a valid annotation. Please choose one of the available annotations"
		["-i", MATRIX, "-d", DATABASE, "-e", "h2"]                    | "Export format (h2) is not a valid output format. Please choose one of the available formats"
		["-i", MATRIX, "-d", DATABASE, "--no-such-option"]            | "Unrecognized option: --no-such-option"
		["-i", MATRIX]                                                | "Missing value for module database file"
		["-d", DATABASE]                                              | "Missing value for input matrix/directory"
	}

	def "an input error exits with 1, says where and writes nothing"() {

		expect:
		run(args) == 1
		problems() == ["ERROR Exception while reading input data: " + message]
		outputFiles() == []

		where:
		args                                                                      | message
		["-i", EDGE + "/edge-non-numeric.tsv", "-d", EDGE + "/edge-modules.list"] | "edge-non-numeric.tsv, line 3: 'K90002\t1\tabc' is not KO<TAB>one abundance per sample"
		// a taxon matrix read with -a 1
		["-i", TAXON_MATRIX, "-d", DATABASE, "-a", "1"]                           | "taxon_matrix.tsv, line 2: 'Sp1\tK00001\t1\t0' is not KO<TAB>one abundance per sample"
		["-i", "no-such-input.tsv", "-d", DATABASE]                               | "no-such-input.tsv (No such file or directory)"
		["-i", MATRIX, "-d", "no-such-database.list"]                             | "no-such-database.list (No such file or directory)"
		["-i", "src/test/resources/empty-matrix.tsv", "-d", DATABASE]             | "empty-matrix.tsv is empty: a matrix starts with a header line"
	}

	def "an input where no module is observed reports none, with a warning and no coverage plot"() {

		expect:
		run(["-i", EDGE + "/edge-all-zero.tsv", "-d", EDGE + "/edge-modules.list", "-e", "2"]) == 0
		problems() == ["WARN No module was observed in any sample: no coverage cutoff can be learned and no module is reported"]
		log.lines.contains("INFO Coverage cutoff: none (no module observed)")
		outputFiles() == ["modules-coverage.tsv", "modules.tsv"]
		new File(outputDir(), "modules.tsv").text == "Module\tS1\tS2\n"
		new File(outputDir(), "modules-coverage.tsv").text == "Module\tS1\tS2\n"
	}

	def "function ids that read as numbers give a warning to check -a"() {

		when: "a plain matrix is read with -a 2: its first sample column becomes the function ids"
		int status = run(["-i", MATRIX, "-d", DATABASE, "-a", "2"])

		then:
		status == 0
		problems() == [
			"WARN Some function ids in the input are numbers (e.g. 1, 2, 0): please check that the annotation format (-a) matches the input",
			"WARN No module was observed in any sample: no coverage cutoff can be learned and no module is reported"
		]
		outputFiles() == ["S2.modules"]
		new File(outputDir(), "S2.modules").text == "Module\tTaxon\tValue\tCoverage\n"
	}

	def "per-sample files have a taxon column only when the results are per taxon"() {

		expect:
		run(["-i", TAXON_MATRIX, "-d", DATABASE, "-a", "2", "-e", "1"] + options) == 0
		new File(outputDir(), "S1.modules").readLines()[0] == header

		where:
		options                     | header
		[]                          | "Module\tTaxon\tValue\tCoverage"
		["--ignore-taxonomic-info"] | "Module\tValue\tCoverage"
	}
}
