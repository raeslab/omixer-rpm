package io.omixer.rpm.cli

import io.omixer.rpm.LogCapture
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class CommandLineSpec extends Specification {

	private static final String MATRIX = "src/test/resources/matrix.tsv"
	private static final String TAXON_MATRIX = "src/test/resources/taxon_matrix.tsv"
	private static final String HUMANN = "src/test/resources/humann2KoTaxa.tsv"
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
		OmixerRpmCommand.run((args + ["-o", outputDir().path]) as String[])
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
		OmixerRpmCommand.run(args.collect { it == "OUT" ? outputDir().path : it } as String[]) == 0

		where:
		args << [
			["-i", MATRIX, "-d", DATABASE, "-o", "OUT"],
			["-h"],
			[],
			// the automatic cutoff as the omixerRpm R package passes it
			["-i", MATRIX, "-d", DATABASE, "-c", "-1", "-o", "OUT"],
			// as 1.x: a unique prefix of a long option, an = value, a repeated flag, and an extra argument
			["-i", MATRIX, "-d", DATABASE, "--cov", "0.5", "-o", "OUT"],
			["-i", MATRIX, "-d", DATABASE, "--coverage=0.5", "-o", "OUT"],
			["-i", MATRIX, "-d", DATABASE, "-n", "-n", "-o", "OUT"],
			["-i", MATRIX, "-d", DATABASE, "extra", "-o", "OUT"],
			// a HUMAnN KO table, per taxon and per sample
			["-i", HUMANN, "-d", DATABASE, "-a", "humann", "-o", "OUT"],
			["-i", HUMANN, "-d", DATABASE, "-a", "humann", "--ignore-taxonomic-info", "-o", "OUT"]
		]
	}

	def "an option given twice takes its last value"() {

		when:
		int status = run(["-i", MATRIX, "-d", DATABASE, "-c", "0", "-c", "0.5"])

		then: "1.x took the first value"
		status == 0
		log.lines.contains("INFO Coverage cutoff: 0.5")
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
		["-i", MATRIX, "-d", DATABASE, "-s"]                          | "Missing argument for option: s"
		["-i", MATRIX, "-d", DATABASE, "--complex-rule"]              | "Missing argument for option: complex-rule"
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
		// an abundance matrix given as the module database
		["-i", MATRIX, "-d", MATRIX]                                              | "matrix.tsv: no module ends with ///, so it is not a module database (a module is a header ID<TAB>name, one line per step, then ///)"
	}

	def "a module database whose last module does not end with /// is read, with a warning"() {

		given: "the GMMs without the last ///, as GBMs.v1.0 ends"
		File database = new File(tempDir, "modules.list")
		database.text = new File(DATABASE).text.replaceFirst(/\/\/\/\s*$/, "")

		when:
		int status = run(["-i", MATRIX, "-d", database.path])

		then:
		status == 0
		problems() == ["WARN modules.list, line 571: the last module, 'MF0121', does not end with ///: if the file was cut off, modules are missing"]
	}

	/**
	 * A HUMAnN table with only the community totals, as humann_split_stratified_table writes it (*_unstratified.tsv):
	 * the HUMAnN 2 test table without its function|taxon rows. It keeps the header, UNMAPPED, UNGROUPED and one total
	 * row per KO.
	 */
	private File totalsOnlyTable() {
		File table = new File(tempDir, "ko.tsv")
		table.text = new File(HUMANN).readLines().findAll { !it.contains("|") }.join("\n")
		table
	}

	def "a HUMAnN table with only community totals has no taxa: it is read per sample, saying so"() {

		when: "without --ignore-taxonomic-info"
		int status = run(["-i", totalsOnlyTable().path, "-d", DATABASE, "-a", "humann", "-e", "1"])

		then: "per-sample files without a taxon column, and an INFO line on why"
		status == 0
		log.lines.contains("INFO ko.tsv has no stratified rows (function|taxon): one profile per sample")
		new File(outputDir(), "10776.Healthy.modules").readLines()[0] == "Module\tValue\tCoverage"
	}

	def "a HUMAnN table with only community totals is read per sample, as a KO matrix (-a 1) of the same rows (#options)"() {

		given:
		File table = totalsOnlyTable()

		when: "read as HUMAnN: per sample, with or without --ignore-taxonomic-info"
		int status = run(["-i", table.path, "-d", DATABASE, "-a", "humann", "-c", "0", "-e", "2"] + options)
		def humann = outputFiles().collectEntries { [it, new File(outputDir(), it).text] }
		def humannProblems = problems()

		and: "the same file read as a plain KO matrix: its UNMAPPED and UNGROUPED rows are KOs of no module there"
		outputDir().deleteDir()
		log.lines.clear()
		run(["-i", table.path, "-d", DATABASE, "-a", "1", "-c", "0", "-e", "2"])
		def matrix = outputFiles().collectEntries { [it, new File(outputDir(), it).text] }

		then: "the same module matrices, with something observed, and the same warnings"
		status == 0
		humann.keySet() == ["modules-coverage.tsv", "modules.tsv"] as Set
		humann == matrix
		humann["modules.tsv"].readLines().size() > 1
		humannProblems == problems()

		where:
		options << [[], ["--ignore-taxonomic-info"]]
	}

	def "a directory is an input error with -a humann, which reads one joined table (#options)"() {

		given: "a directory with a HUMAnN table"
		File directory = new File(tempDir, "tables")
		directory.mkdirs()
		new File(directory, "a.tsv").text = new File(HUMANN).text

		expect: "per taxon and per sample"
		run(["-i", directory.path, "-d", DATABASE, "-a", "humann"] + options) == 1
		problems() == ["ERROR Exception while reading input data: tables is a directory: -a humann reads one HUMAnN table (join per-sample tables with humann_join_tables)"]
		outputFiles() == []

		where:
		options << [[], ["--ignore-taxonomic-info"]]
	}

	def "HUMAnN per-sample files are named after the samples, with a taxon column only per taxon (#options)"() {

		expect:
		run(["-i", HUMANN, "-d", DATABASE, "-a", "humann", "-c", "0"] + options) == 0
		outputFiles().size() == 9
		new File(outputDir(), "10776.Healthy.modules").readLines()[0] == header

		where:
		options                     | header
		[]                          | "Module\tTaxon\tValue\tCoverage"
		["--ignore-taxonomic-info"] | "Module\tValue\tCoverage"
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
