package io.omixer.rpm.golden

import java.nio.file.Files

import io.omixer.rpm.LogCapture
import io.omixer.rpm.cli.OmixerRpmCommand
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Tag

/**
 * Golden tests: the results of the command line on every input, compared with the recorded reference in
 * src/test/resources/golden/expected/<input>.tsv. The reference is the 1.1 behaviour plus the bug fixes approved in
 * BUGS.md.
 *
 * <pre>
 * gradle goldenTest     # compare (also part of gradle check)
 * gradle goldenRecord   # rewrite expected/ with the current code
 * </pre>
 *
 * Only what can't be derived is recorded: for each input, one row per sample, (taxon) and module with the coverage and
 * the abundance of every scoring variant at -c 0, and the automatic cutoff. Everything else is checked against that
 * while running: fixed and automatic cutoffs, per-sample output (-e 1), threads, directory input, and that coverage
 * doesn't depend on the estimator. Command line errors and warnings are tested in CommandLineSpec.
 */
@Tag("golden")
class GoldenSpec extends Specification {

	private static final File EXPECTED = new File("src/test/resources/golden/expected")
	private static final boolean RECORD = Boolean.getBoolean("golden.record")

	private static final String RES = "src/test/resources"
	private static final String GOLDEN_IN = RES + "/golden/inputs"
	private static final String GMM_DB = RES + "/module.list"
	private static final String EDGE_DB = GOLDEN_IN + "/edge/edge-modules.list"

	/** name: arguments; a taxon input also gets a community version (--ignore-taxonomic-info) */
	private static final Map<String, List<String>> INPUTS = [:]
	static {
		def taxonInput = { String name, List<String> args ->
			INPUTS[name] = args
			INPUTS[name + "-community"] = args + ["--ignore-taxonomic-info"]
		}
		INPUTS["legacy-matrix"] = ["-i", RES + "/matrix.tsv", "-d", GMM_DB, "-a", "1"]
		taxonInput("legacy-taxon", ["-i", RES + "/taxon_matrix.tsv", "-d", GMM_DB, "-a", "2"])
		INPUTS["edge-matrix"] = ["-i", GOLDEN_IN + "/edge/edge-matrix.tsv", "-d", EDGE_DB, "-a", "1"]
		taxonInput("edge-taxon", ["-i", GOLDEN_IN + "/edge/edge-taxon-matrix.tsv", "-d", EDGE_DB, "-a", "2"])
		INPUTS["synth-matrix"] = ["-i", GOLDEN_IN + "/synthetic/synth-matrix.tsv", "-d", GMM_DB, "-a", "1"]
		taxonInput("synth-taxon", ["-i", GOLDEN_IN + "/synthetic/synth-taxon-matrix.tsv", "-d", GMM_DB, "-a", "2"])
		// -a h2 is added once the HUMAnN 3/4 input is settled: synthetic/synth-humann2.tsv
	}

	/** Directory inputs, each written from one of the matrices: name: [matrix input, arguments] */
	private static final Map<String, List> DIRECTORIES = [
		"legacy-dir": ["legacy-matrix", ["-i", RES + "/input_directory", "-d", GMM_DB, "-a", "1"]],
		"synth-dir1": ["synth-matrix", ["-i", GOLDEN_IN + "/synthetic/synth-dir-1", "-d", GMM_DB, "-a", "1"]],
		"synth-dir2": ["synth-taxon", ["-i", GOLDEN_IN + "/synthetic/synth-dir-2", "-d", GMM_DB, "-a", "2"]],
	]

	/** Scoring variants, one abundance column each: column: arguments */
	private static final Map<String, List<String>> VARIANTS = [:]
	static {
		def estimators = ["median", "average", "sum", "min"]
		estimators.each { VARIANTS[it] = ["-s", it]; VARIANTS[it + "_n"] = ["-s", it, "-n"] }
		estimators.each { VARIANTS["xd_" + it] = ["-s", it, "--Xdistribute"] }
		estimators.each { VARIANTS["all_" + it] = ["-s", it, "--complex-rule", "all"] }
	}

	/** Coverage of the default complex rule (any) and of --complex-rule all, each from one of the variants */
	private static final Map<String, String> COVERAGE = ["coverage": "median", "coverage_all": "all_median"]

	@Shared
	LogCapture logCapture = new LogCapture()

	/** Runs by arguments, so that features share them */
	@Shared
	Map<List<String>, Map> runs = [:]

	def setupSpec() {
		logCapture.install()
		if (RECORD) {
			EXPECTED.deleteDir()
			EXPECTED.mkdirs()
		}
	}

	def cleanupSpec() {
		logCapture.uninstall()
	}

	def "#input: results match the recorded reference"() {

		given:
		def actual = reference(input)
		def file = new File(EXPECTED, input + ".tsv")

		when:
		if (RECORD) {
			file.text = actual.collect { it + "\n" }.join()
		}
		def report = differences(file.exists() ? file.readLines("UTF-8") : [], actual)

		then:
		report == ""

		where:
		input << INPUTS.keySet()
	}

	def "#input: coverage does not depend on the estimator, -n or --Xdistribute"() {

		expect:
		variants(input).findAll { coverage(input, it) != coverage(input, it.startsWith("all_") ? "all_median" : "median") } == []

		where:
		input << INPUTS.keySet()
	}

	def "#input: cutoff #cutoffArg keeps the modules covered at or above it in any sample, and writes 0 below it"() {

		given:
		def automatic = run(INPUTS[input] + ["-e", "2"])
		Double cutoff = cutoffArg == "auto" ? automaticCutoff(automatic) : cutoffArg as double
		def output = cutoffArg == "auto" ? automatic : run(INPUTS[input] + ["-c", cutoffArg, "-e", "2"])

		expect:
		cutoff != null
		[abundance(output), coverage(output)] == applyCutoff(abundance(input, "median"), coverage(input, "median"), cutoff)
		output.files.containsKey("coverage-plot.svg") == (cutoffArg == "auto")

		where:
		[input, cutoffArg] << [INPUTS.keySet(), ["0.5", "1", "auto"]].combinations()
	}

	def "#input: the per-sample files hold the matrix values that have coverage"() {

		given:
		def matrix = run(INPUTS[input] + ["-e", "2"])
		def files = run(INPUTS[input] + ["-e", "1"])
		def abundance = abundance(matrix)
		def coverage = coverage(matrix)
		def samples = abundance.values().collectMany { it.keySet() }.toSet()

		expect:
		files.files.keySet().findAll { it.endsWith(".modules") } == samples.collect { it + ".modules" }.toSet()
		perSampleCells(files) == abundance.collectMany { key, values ->
			values.findAll { sample, value -> !isZero(coverage[key][sample]) }
				.collect { sample, value -> [sample, key, value, coverage[key][sample]] }
		}.toSet()

		where:
		input << INPUTS.keySet()
	}

	def "#input: threads do not change the results"() {

		given:
		def threaded = run(INPUTS[input] + ["-c", "0", "-e", "2", "-t", "4"])

		expect:
		abundance(threaded) == abundance(input, "median")
		coverage(threaded) == coverage(input, "median")

		where:
		input << INPUTS.keySet()
	}

	def "#directory: a directory gives the same results as the matrix it was written from (#variant)"() {

		given:
		def (matrixInput, args) = DIRECTORIES[directory]
		def output = run(args + VARIANTS[variant] + ["-c", "0", "-e", "2"])

		expect:
		abundance(output) == abundance(matrixInput, variant)
		coverage(output) == coverage(matrixInput, variant)

		where:
		[directory, variant] << [DIRECTORIES.keySet(), ["median", "min_n", "xd_sum"]].combinations()
	}

	def "every recorded reference belongs to an input"() {

		expect:
		EXPECTED.list().toList().toSet() - INPUTS.keySet().collect { it + ".tsv" }.toSet() == [] as Set
	}

	/**
	 * The reference of one input: the automatic cutoff, then one row per sample, (taxon) and module with the coverages
	 * and the abundance of every variant at -c 0, sorted; rows where every value is 0 are left out.
	 */
	private List<String> reference(String input) {
		Map<String, Map> columns = COVERAGE.collectEntries { column, variant -> [column, coverage(input, variant)] }
		columns += variants(input).collectEntries { [it, abundance(input, it)] }
		Map<String, Map<String, String>> modules = abundance(input, "median")
		boolean perTaxon = modules.keySet().any { it.contains("\t") }
		def rows = modules.collectMany { key, values ->
			values.keySet().collect { sample ->
				def cells = columns.values().collect { it[key][sample] }
				cells.every { isZero(it) } ? null : ([sample, key] + cells).join("\t")
			}.findAll()
		}
		def cutoff = automaticCutoff(run(INPUTS[input] + ["-e", "2"]))
		["# automatic coverage cutoff: " + (cutoff == null ? "none" : cutoff),
			(["sample"] + (perTaxon ? ["taxon"] : []) + ["module"] + columns.keySet()).join("\t")] + rows.sort()
	}

	/** The variants of an input: all of them, or for a community version the default, --Xdistribute and complex rule all */
	private static List<String> variants(String input) {
		input.endsWith("-community") ? ["median", "xd_median", "all_median"] : VARIANTS.keySet().toList()
	}

	/** Runs the command line in this JVM (once per arguments), returns its output files and log */
	private Map run(List<String> args) {
		runs.computeIfAbsent(args*.toString()) { execute(it) }
	}

	private Map execute(List<String> args) {
		def outputDir = new File(Files.createTempDirectory("golden").toFile(), "out")
		logCapture.lines.clear()
		int status = OmixerRpmCommand.run((args + ["-o", outputDir.path]) as String[])
		def log = new ArrayList<>(logCapture.lines)
		assert status == 0: "${args.join(' ')} exited with ${status}: ${log}"
		def files = outputDir.listFiles().collectEntries { [it.name, it.name.endsWith(".svg") ? "" : it.getText("UTF-8")] }
		outputDir.parentFile.deleteDir()
		[files: files, log: log]
	}

	private Map<String, Map<String, String>> abundance(String input, String variant) {
		abundance(run(INPUTS[input] + VARIANTS[variant] + ["-c", "0", "-e", "2"]))
	}

	private Map<String, Map<String, String>> coverage(String input, String variant) {
		coverage(run(INPUTS[input] + VARIANTS[variant] + ["-c", "0", "-e", "2"]))
	}

	private static Map<String, Map<String, String>> abundance(Map output) {
		matrix(output.files["modules.tsv"])
	}

	private static Map<String, Map<String, String>> coverage(Map output) {
		matrix(output.files["modules-coverage.tsv"])
	}

	/** A matrix output as {module (or taxon TAB module): {sample: value}} */
	private static Map<String, Map<String, String>> matrix(String text) {
		def lines = text.readLines()
		def header = lines[0].split("\t", -1) as List
		int keyColumns = header.take(2) == ["Taxon", "Module"] ? 2 : 1
		def samples = header.drop(keyColumns)
		lines.drop(1).collectEntries { line ->
			def fields = line.split("\t", -1) as List
			[fields.take(keyColumns).join("\t"), [samples, fields.drop(keyColumns)].transpose().collectEntries()]
		}
	}

	/** The cells of the per-sample files (-e 1) as [sample, module (or taxon TAB module), value, coverage] */
	private static Set<List<String>> perSampleCells(Map output) {
		output.files.findAll { name, text -> name.endsWith(".modules") }.collectMany { name, text ->
			def lines = text.readLines()
			def header = lines[0].split("\t", -1) as List
			lines.drop(1).collect { line ->
				def fields = [header, line.split("\t", -1) as List].transpose().collectEntries()
				def key = fields.containsKey("Taxon") ? fields.Taxon + "\t" + fields.Module : fields.Module
				[name - ".modules", key, fields.Value, fields.Coverage]
			}
		}.toSet()
	}

	/** The -c 0 matrices with a cutoff applied: modules covered at or above it in any sample, 0 below it */
	private static List<Map> applyCutoff(Map<String, Map<String, String>> abundance, Map<String, Map<String, String>> coverage,
			double cutoff) {
		def kept = coverage.findAll { key, values -> values.values().any { (it as double) >= cutoff } }.keySet()
		[abundance, coverage].collect { matrix ->
			matrix.subMap(kept).collectEntries { key, values ->
				[key, values.collectEntries { sample, value -> [sample, (coverage[key][sample] as double) < cutoff ? "0.0" : value] }]
			}
		}
	}

	/** The cutoff learned by an automatic run, or null when no module was observed */
	private static Double automaticCutoff(Map output) {
		def value = output.log.find { it.startsWith("INFO Coverage cutoff: ") } - "INFO Coverage cutoff: "
		value.isDouble() ? value as double : null
	}

	private static boolean isZero(String value) {
		(value as double) == 0d
	}

	/** The rows only in the reference and only in the results (at most 10 each); empty when the same */
	private static String differences(List<String> expected, List<String> actual) {
		if (expected == actual) {
			return ""
		}
		def expectedRows = expected as LinkedHashSet
		def actualRows = actual as LinkedHashSet
		def missing = expectedRows.findAll { !actualRows.contains(it) }
		def extra = actualRows.findAll { !expectedRows.contains(it) }
		(["${missing.size()} rows only in the reference, ${extra.size()} only in the results; header: ${actual.take(2).join(' / ')}"]
			+ missing.take(10).collect { "  - " + it } + extra.take(10).collect { "  + " + it }).join("\n")
	}
}
