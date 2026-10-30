package io.omixer.rpm.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.omixer.rpm.cutoff.CoverageCutoff;
import io.omixer.rpm.engine.Results;
import io.omixer.rpm.exceptions.OmixerRpmException;
import io.omixer.rpm.inference.ScoringInference;
import io.omixer.rpm.io.InputFormat;
import io.omixer.rpm.io.OutputFormat;
import io.omixer.rpm.pipeline.Pipeline;
import io.omixer.rpm.pipeline.Request;
import io.omixer.rpm.scoring.ComplexRule;
import io.omixer.rpm.scoring.Estimator;
import io.omixer.rpm.scoring.ScoringOptions;
import io.omixer.rpm.scoring.SharedKoResolution;
import io.omixer.utils.exceptions.DuplicateColumnException;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * The command line: validates the options (same flags, messages and exit codes as 1.x, which the omixerRpm R package
 * relies on), runs the {@link Pipeline} and reports errors. Errors are only caught here.
 *
 * @author <a href="mailto:youssef.darzi@omixer.io">Youssef Darzi</a>
 */
@Command(name = "java -jar omixer-rpm.jar", sortOptions = true)
public final class OmixerRpmCommand implements Callable<Integer> {

	private static final Log LOG = LogFactory.getLog(OmixerRpmCommand.class);

	/** Exit status of a run that stopped on an input or run error */
	public static final int EXIT_ERROR = 1;
	/** Exit status of a run that stopped on a command line error */
	public static final int EXIT_USAGE = 2;

	/** The build version, from the jar manifest; absent when not running from a jar (e.g. in the IDE) */
	public static final String VERSION = Objects
			.requireNonNullElse(OmixerRpmCommand.class.getPackage().getImplementationVersion(), "unknown");

	/** Accepted values of --score-estimator, used in the help text and in its error message */
	private static final List<String> SCORE_ESTIMATORS = List.of("median", "average", "sum", "min");

	/** Accepted values of --complex-rule, from the rules themselves */
	private static final String COMPLEX_RULES = Arrays.stream(ComplexRule.values()).map(ComplexRule::cliName)
			.collect(Collectors.joining("|"));

	/** Accepted values of --annotation and --export-format; h2 (HUMAnN2) waits for the HUMAnN 3/4 rework (B08, B09, B14) */
	private static final List<String> FORMATS = List.of("1", "2");

	@Spec
	private CommandLine.Model.CommandSpec spec;

	@Option(names = { "-c", "--coverage" }, paramLabel = "COVERAGE", description = {
			"The minimum coverage cut-off to accept a module [0.0 to 1.0].",
			"Defaults to -1, where the coverage is learned from the coverage distribution of all modules" })
	private String coverage;

	@Option(names = { "-s", "--score-estimator" }, paramLabel = "SCORE-ESTIMATOR", description = {
			"The score estimator.", "Accepted values are [median|average|sum|min].", "Defaults to median" })
	private String scoreEstimator;

	@Option(names = "--complex-rule", paramLabel = "RULE", description = {
			"How a complex step (subunits written K1,K2 in the module database) counts when only some subunits are observed.",
			"any: observed when at least one subunit is, since a missing subunit is more often unsampled than absent;",
			"all: observed only when every subunit is (otherwise no coverage and no abundance).",
			"Accepted values are [any|all].", "Defaults to any" })
	private String complexRule;

	@Option(names = { "-n", "--normalize-by-length" }, description = {
			"Divide module score by its length. When combined with a median estimator, missing reactions (score = 0 )",
			"are included when estimating the median. If the estimated score equals zero then it is replaced by",
			"the minimum observed reaction score. If this option is specified, score calculation is based only on",
			"the number of observed reactions" })
	private boolean normalizeByLength;

	@Option(names = "--ignore-taxonomic-info", description = "Ignore taxonomic info from input file and infer modules for the whole metagenome instead")
	private boolean ignoreTaxonomicInfo;

	@Option(names = { "-d", "--database" }, paramLabel = "FILE", description = "The path to the modules database")
	private String database;

	@Option(names = { "-a", "--annotation" }, paramLabel = "ANNOTATION", description = { "Input file annotation.",
			"Use 1 for orthologs only files or 2 for taxonomic annotation followed by orthologs.", "Defaults to 1" })
	private String annotation;

	@Option(names = { "-e", "--export-format" }, paramLabel = "FORMAT", description = { "The output file format.",
			"Use 1 for single tab separated files containing module id, abundance and coverage. Use 2 for an abundance and a coverage matrices.",
			"Defaults to 1." })
	private String exportFormat;

	@Option(names = { "-t", "--threads" }, paramLabel = "THREADS", description = {
			"Number of threads to use when mapping the modules.", "Defaults to 1" })
	private String threads;

	@Option(names = { "-i", "--input" }, paramLabel = "PATH", description = "Path to the input matrix or input directory with one headerless file per sample")
	private String input;

	@Option(names = { "-o", "--output-dir" }, paramLabel = "DIRECTORY", description = "Path to the output directory")
	private String outputDir;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Show this help message and exit")
	private boolean help;

	@Option(names = "--Xdistribute", description = "Experimental feature - When an ortholog is shared by N modules then its abundance is divided by N.")
	private boolean distribute;

	/** As in 1.x, arguments that are not options are ignored */
	@Parameters(hidden = true)
	private List<String> ignored;

	public static void main(String[] args) {
		int status = run(args);
		if (status != 0) {
			System.exit(status);
		}
	}

	/**
	 * Runs Omixer-RPM on the command line arguments.
	 *
	 * @return the exit status: 0 on success, {@link #EXIT_ERROR} or {@link #EXIT_USAGE}
	 */
	public static int run(String[] args) {
		CommandLine commandLine = new CommandLine(new OmixerRpmCommand());
		// as 1.x: a unique prefix of a long option is accepted, and so is a repeated option; its last value counts
		// (1.x: the first)
		commandLine.setAbbreviatedOptionsAllowed(true);
		commandLine.setOverwrittenOptionsAllowed(true);
		commandLine.getCommandSpec().usageMessage().width(160)
				.description(" Omixer-RPM",
						" A Reference Pathways Mapper for turning metagenomic functional profiles into pathway/module profiles",
						"", "VERSION: " + VERSION, "AUTHOR: Youssef Darzi <youssef.darzi@omixer.io>", "")
				.descriptionHeading("%nDESCRIPTION%n")
				.optionListHeading("ARGUMENTS (Options starting with -X are non-standard and subject to change without notice.)%n%n")
				.footer("%nLicensed under an Academic Non-commercial Software License Agreement, https://github.com/raeslab/omixer-rpm/blob/master/LICENSE");
		commandLine.setParameterExceptionHandler((exception, arguments) -> {
			LOG.error(message(exception));
			exception.getCommandLine().usage(System.out);
			return EXIT_USAGE;
		});
		if (args.length == 0) {
			commandLine.usage(System.out);
			return 0;
		}
		return commandLine.execute(args);
	}

	/** The message of a command line error, worded as in 1.x */
	private static String message(CommandLine.ParameterException exception) {
		if (exception instanceof CommandLine.UnmatchedArgumentException unmatched && !unmatched.getUnmatched().isEmpty()) {
			return "Unrecognized option: " + unmatched.getUnmatched().get(0);
		}
		if (exception instanceof CommandLine.MissingParameterException missing && !missing.getMissing().isEmpty()
				&& missing.getMissing().get(0) instanceof OptionSpec option) {
			return "Missing argument for option: " + option.shortestName().replaceFirst("^-+", "");
		}
		return exception.getMessage();
	}

	@Override
	public Integer call() {
		Request request;
		try {
			request = request();
		} catch (IllegalArgumentException e) {
			LOG.error(e.getMessage());
			spec.commandLine().usage(System.out);
			return EXIT_USAGE;
		}
		long start = System.currentTimeMillis();
		Results results;
		try {
			results = new Pipeline().run(request);
		} catch (IOException | IncorrectNumberOfEntriesException | OmixerRpmException | DuplicateColumnException e) {
			LOG.error("Exception while reading input data: " + e.getMessage());
			return EXIT_ERROR;
		}
		LOG.info("Total run time: " + (System.currentTimeMillis() - start) / 1000L);
		LOG.info("Coverage cutoff: "
				+ (results.cutoff().value().isPresent() ? results.cutoff().value().getAsDouble()
						: "none (no module observed)"));
		LOG.info("Output files are in " + request.outputDir().toAbsolutePath());
		return 0;
	}

	/**
	 * The options as a request, checked in the order of 1.x so that the first problem reported is the same
	 *
	 * @throws IllegalArgumentException for an invalid or missing option
	 */
	private Request request() {
		double minimumCoverage = coverage == null ? -1d : coverage();
		Estimator estimator = scoreEstimator == null ? new Estimator.Median() : estimator();
		ComplexRule rule = complexRule == null ? ComplexRule.ANY
				: ComplexRule.fromName(complexRule).orElseThrow(() -> new IllegalArgumentException(
						complexRule + " is not a valid complex rule. Please choose one of [" + COMPLEX_RULES + "]"));
		if (database == null) {
			throw new IllegalArgumentException("Missing value for module database file");
		}
		if (annotation != null && !FORMATS.contains(annotation)) {
			throw new IllegalArgumentException("Annotation (" + annotation
					+ ") is not a valid annotation. Please choose one of the available annotations");
		}
		if (exportFormat != null && !FORMATS.contains(exportFormat)) {
			throw new IllegalArgumentException("Export format (" + exportFormat
					+ ") is not a valid output format. Please choose one of the available formats");
		}
		if (input == null) {
			throw new IllegalArgumentException("Missing value for input matrix/directory");
		}
		Path output = Path.of(outputDir != null ? outputDir : "module-prediction-" + System.currentTimeMillis());
		int threadCount = threads == null ? 1 : threadCount();

		boolean taxonInput = "2".equals(annotation);
		InputFormat format = taxonInput ? InputFormat.TAXON_KO : InputFormat.KO;
		CoverageCutoff cutoff = minimumCoverage == -1d ? new CoverageCutoff.Automatic()
				: new CoverageCutoff.Fixed(minimumCoverage);
		SharedKoResolution resolution = distribute ? new SharedKoResolution.EqualSplit() : new SharedKoResolution.None();
		ScoringInference inference = new ScoringInference(new ScoringOptions(estimator, rule, normalizeByLength),
				resolution, cutoff, threadCount);
		return new Request(Path.of(input), format, taxonInput && !ignoreTaxonomicInfo, Path.of(database), inference,
				"2".equals(exportFormat) ? OutputFormat.MATRIX : OutputFormat.PER_SAMPLE, output);
	}

	/** -1 means automatic; anything else must be a fraction (the negated check also rejects NaN) */
	private double coverage() {
		String invalid = coverage
				+ " is not a valid coverage value. Please provide a decimal value between 0 and 1, or -1 to enable automatic guessing of the coverage threshold.";
		double value;
		try {
			value = Double.parseDouble(coverage);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(invalid);
		}
		if (Double.compare(value, -1d) != 0 && !(value >= 0d && value <= 1d)) {
			throw new IllegalArgumentException(invalid);
		}
		return value;
	}

	private Estimator estimator() {
		return switch (scoreEstimator) {
			case "median" -> new Estimator.Median();
			case "average" -> new Estimator.Average();
			case "sum" -> new Estimator.Sum();
			case "min" -> new Estimator.Min();
			default -> throw new IllegalArgumentException(scoreEstimator + " is not a valid score estimator. Please choose one of ["
					+ String.join("|", SCORE_ESTIMATORS) + "]");
		};
	}

	private int threadCount() {
		int count;
		try {
			count = Integer.parseInt(threads);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Illegal value (" + threads
					+ ") for the number of threads. Please make sure you specify a valid positive Integer");
		}
		if (count < 1) {
			throw new IllegalArgumentException("Number of threads (" + threads
					+ ") is not a valid number. Please specify a values greater than 0.");
		}
		return count;
	}
}
