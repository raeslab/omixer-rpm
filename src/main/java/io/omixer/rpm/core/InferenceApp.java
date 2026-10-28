package io.omixer.rpm.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import io.omixer.rpm.exceptions.OmixerRpmException;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.ModuleCoverageDistribution;
import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Modules;
import io.omixer.rpm.model.enums.ComplexRule;
import io.omixer.rpm.model.enums.ModuleInferenceOptimizers;
import io.omixer.rpm.model.enums.ScalingMethod;
import io.omixer.rpm.model.io.MatrixWriter;
import io.omixer.rpm.model.io.ModuleMatrixWriter;
import io.omixer.rpm.model.io.ModuleTaxonomyMatrixWriter;
import io.omixer.rpm.parsers.ModuleParser;
import io.omixer.rpm.service.impl.ModuleManagerImpl;
import io.omixer.utils.exceptions.DuplicateColumnException;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * A client app to infer modules from command line
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 *
 */
public class InferenceApp extends AbstractInferenceApp {

	public static final String TOOL_NAME="omixer-rpm.jar";
	public static final String EXEC_COMMAND = "java -jar " + TOOL_NAME + " ";
	/** The build version, from the jar manifest; absent when not running from a jar (e.g. in the IDE) */
	public static final String VERSION = Objects.requireNonNullElse(InferenceApp.class.getPackage().getImplementationVersion(),
			"unknown");
	public static final String HEADER = "\n\nDESCRIPTION\n"
			+ " Omixer-RPM\n A Reference Pathways Mapper for turning metagenomic functional profiles into pathway/module profiles\n\n"
			+ "VERSION: " + VERSION + "\n" + "AUTHOR: Youssef Darzi <youssef.darzi@gmail.com>\n\n"
			+ "ARGUMENTS (Options starting with -X are non-standard and subject to change without notice.)\n\n";

	/** Accepted values of --score-estimator, used in the help text and in its error message */
	private static final List<String> SCORE_ESTIMATORS = List.of("median", "average", "sum", "min");

	/** Accepted values of --complex-rule, from the rules themselves */
	private static final String COMPLEX_RULES = Arrays.stream(ComplexRule.values()).map(ComplexRule::cliName)
			.collect(Collectors.joining("|"));

	public static final String FOOTER = "\nLicensed under an Academic Non-commercial Software License Agreement, https://github.com/raeslab/omixer-rpm/blob/master/LICENSE";

	/** Exit status of a run that stopped on an input or run error */
	public static final int EXIT_ERROR = 1;
	/** Exit status of a run that stopped on a command line error */
	public static final int EXIT_USAGE = 2;

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

		InferenceApp app = new InferenceApp();

		CommandLineParser parser = new DefaultParser();
		Options options = new Options();

		options.addOption(Option.builder("c").longOpt("coverage")
				.desc("The minimum coverage cut-off to accept a module [0.0 to 1.0].\nDefaults to -1, where the coverage is learned from the coverage distribution of all modules")
				.hasArg().argName("COVERAGE").build());

		options.addOption(Option.builder("s").longOpt("score-estimator")
				.desc("The score estimator.\nAccepted values are [" + String.join("|", SCORE_ESTIMATORS) + "].\nDefaults to median").hasArg()
				.argName("SCORE-ESTIMATOR").build());

		options.addOption(Option.builder().longOpt("complex-rule")
				.desc("How a complex step (subunits written K1,K2 in the module database) counts when only some subunits are observed.\n"
						+ "any: observed when at least one subunit is, since a missing subunit is more often unsampled than absent;\n"
						+ "all: observed only when every subunit is (otherwise no coverage and no abundance).\n"
						+ "Accepted values are [" + COMPLEX_RULES + "].\nDefaults to any")
				.hasArg().argName("RULE").build());

		options.addOption(Option.builder("n").longOpt("normalize-by-length")
				.desc("Divide module score by its length. When combined with a median estimator, missing reactions (score = 0 )\n"
						+ "are included when estimating the median. If the estimated score equals zero then it is replaced by\n"
						+ "the minimum observed reaction score. If this option is specified, score calculation is based only on\n"
						+ "the number of observed reactions")
				.build());

		options.addOption(Option.builder().longOpt("ignore-taxonomic-info")
				.desc("Ignore taxonomic info from input file and infer modules for the whole metagenome instead")
				.build());

		options.addOption(Option.builder("d").longOpt("database").desc("The path to the modules database").hasArg()
				.argName("FILE").build());

		options.addOption(Option.builder("a").longOpt("annotation")
				.desc("Input file annotation.\nUse 1 for orthologs only files or 2 for taxonomic annotation followed by orthologs.\nDefaults to 1")
				.hasArg().argName("ANNOTATION").build());

		options.addOption(Option.builder("e").longOpt("export-format")
				.desc("The output file format.\nUse 1 for single tab separated files containing module id, abundance and coverage. Use 2 for an abundance and a coverage matrices.\nDefaults to 1.\n").hasArg()
				.argName("FORMAT").build());

		options.addOption(Option.builder("t").longOpt("threads")
				.desc("Number of threads to use when mapping the modules.\nDefaults to 1").hasArg().argName("THREADS")
				.build());

		options.addOption(Option.builder("i").longOpt("input").desc("Path to the input matrix or input directory with one headerless file per sample").hasArg()
				.argName("PATH").build());

		options.addOption(Option.builder("o").longOpt("output-dir").desc("Path to the output directory").hasArg()
				.argName("DIRECTORY").build());

		options.addOption(Option.builder("h").longOpt("help").desc("Show this help message and exit").build());

		options.addOption(Option.builder().longOpt("Xdistribute")
				.desc("Experimental feature - When an ortholog is shared by N modules then its abundance is divided by N.")
				.build());
		
		double coverage = -1d;
		String algorithm = "ABUNDANCE_COVERAGE_MEDIAN_BASED";
		String annotation = "1";
		// 1 for files 2 for matrix
		String outFormat = "1";
		String threads = "1";
		boolean normalizeByLength = false;
		boolean isPerTaxon = false;
		boolean isDistributeOrhologAbundance = false;
		ComplexRule complexRule = ComplexRule.ANY;

		ScalingMethod scalingMethod = ScalingMethod.NONE;
		// Use name match and get id
		File moduleFile = null;
		File inputdir = null;
		File outputDir = null;

		Set<String> formatValues = new HashSet<>();
		formatValues.add("1");
		formatValues.add("2");
		// h2 (HUMAnN2) is not offered until the HUMAnN 3/4 rework, see BUGS.md B08, B09, B14

		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(800);
		if (args.length == 0) {
			formatter.printHelp(EXEC_COMMAND, HEADER, options, FOOTER, true);
			return 0;
		}

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			// validate that block-size has been set

			if (line.hasOption('h')) {
				formatter.printHelp(EXEC_COMMAND, HEADER, options, FOOTER, true);
				return 0;
			}

			if (line.hasOption("coverage")) {
				String coverageValue = line.getOptionValue("coverage");
				String invalidCoverage = coverageValue
						+ " is not a valid coverage value. Please provide a decimal value between 0 and 1, or -1 to enable automatic guessing of the coverage threshold.";
				try {
					coverage = Double.valueOf(coverageValue);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(invalidCoverage);
				}
				// -1 means automatic; anything else must be a fraction (the negated check also rejects NaN)
				if (Double.compare(coverage, -1d) != 0 && !(coverage >= 0d && coverage <= 1d)) {
					throw new IllegalArgumentException(invalidCoverage);
				}
			}
			if (line.hasOption("score-estimator")) {
				String estimator = line.getOptionValue("score-estimator");

				if ("average".equals(estimator)) {
					algorithm = ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName();
				} else if ("sum".equals(estimator)) {
					algorithm = ModuleInferenceOptimizers.SUM.displayName();
				} else if ("min".equals(estimator)) {
					algorithm = ModuleInferenceOptimizers.MIN.displayName();
				} else if (!"median".equals(estimator)) {
					throw new IllegalArgumentException(estimator
							+ " is not a valid score estimator. Please choose one of [" + String.join("|", SCORE_ESTIMATORS) + "]");
				}
			}

			if (line.hasOption("complex-rule")) {
				String rule = line.getOptionValue("complex-rule");
				complexRule = ComplexRule.fromName(rule).orElseThrow(() -> new IllegalArgumentException(
						rule + " is not a valid complex rule. Please choose one of [" + COMPLEX_RULES + "]"));
			}

			if (line.hasOption('n')) {
				normalizeByLength = true;
			}

			if (line.hasOption("database")) {
				moduleFile = new File(line.getOptionValue('d'));
			} else {
				throw new MissingArgumentException("Missing value for module database file");
			}

			if (line.hasOption('a')) {
				annotation = line.getOptionValue('a');
				if (!formatValues.contains(annotation)) {
					throw new IllegalArgumentException("Annotation (" + annotation
							+ ") is not a valid annotation. Please choose one of the available annotations");
				}
				// TODO let the builder check if per taxon is possible
				if (annotation.equals("2") || annotation.equals("h2")) {
					isPerTaxon = true;
				}
			}

			if (line.hasOption('e')) {
				outFormat = line.getOptionValue('e');
				if (!formatValues.contains(outFormat)) {
					throw new IllegalArgumentException("Export format (" + outFormat
							+ ") is not a valid output format. Please choose one of the available formats");
				}
			}

			if (line.hasOption("ignore-taxonomic-info")) {
				if (annotation.equals("2")) {
					isPerTaxon = false;
				}
			}

			if (line.hasOption('i')) {
				inputdir = new File(line.getOptionValue('i'));
			} else {
				throw new MissingArgumentException("Missing value for input matrix/directory");
			}

			if (line.hasOption('o')) {
				outputDir = new File(line.getOptionValue('o'));
			} else {
				outputDir = new File("module-prediction-" + System.currentTimeMillis());
			}

			if (line.hasOption('t')) {
				threads = line.getOptionValue('t');
				try {
					if (Integer.valueOf(threads) < 1) {
						throw new IllegalArgumentException("Number of threads (" + threads
								+ ") is not a valid number. Please specify a values greater than 0.");
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal value (" + threads
							+ ") for the number of threads. Please make sure you specify a valid positive Integer");
				}

				System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", threads);
			}
			
			if (line.hasOption("Xdistribute")) {
				isDistributeOrhologAbundance = true;
			}

		} catch (ParseException | IllegalArgumentException e) {
			app.log.error(e.getMessage());
			formatter.printHelp(EXEC_COMMAND, HEADER, options, FOOTER, true);
			return EXIT_USAGE;
		}

		// Replace by optionparser
		ModuleManagerImpl moduleManager = new ModuleManagerImpl();
		moduleManager.setInputFormat(annotation);
		if (!threads.equals("1")) {
			moduleManager.setConcurrent(true);
		}
		outputDir.mkdirs();

		final ModuleInferenceOptions moduleInferenceOptions = app.makeModuleSetOptions(algorithm, coverage,
				scalingMethod);
		moduleInferenceOptions.setNormalizeByLength(normalizeByLength);
		moduleInferenceOptions.setPerTaxon(isPerTaxon);
		moduleInferenceOptions.setDistributeOrhologAbundance(isDistributeOrhologAbundance);
		moduleInferenceOptions.setComplexRule(complexRule);
		long start = System.currentTimeMillis();
		// false when no module is observed in any sample, so that no automatic cutoff exists
		boolean modulesObserved = true;
		try {
			final List<Module> referenceModules = ModuleParser.parseModuleFile(moduleFile);
			Map<String, Modules> moduleInference = moduleManager.inferModules(inputdir,
					moduleInferenceOptions, referenceModules);
			// Check if the automatic threshold detection is required
			if (Double.compare(coverage, -1d) == 0) {

				// put in manager
				Modules allModules = new Modules();
				for (Modules modules : moduleInference.values()) {
					allModules.addModules(modules);
				}
				// put in t Modules
				ModuleCoverageDistribution mcd = new ModuleCoverageDistribution(allModules);
				Optional<Double> optimalCoverage = mcd.findOptimalCoverage();
				if (optimalCoverage.isPresent()) {
					coverage = optimalCoverage.get();
					for (Modules modules : moduleInference.values()) {
						modules.setCoverageCutoff(coverage);
					}
					// Export as svg
					Files.write(new File(outputDir, "coverage-plot.svg").toPath(), mcd.toSVG().getBytes());
				} else {
					modulesObserved = false;
					app.log.warn("No module was observed in any sample: no coverage cutoff can be learned and no module is reported");
					// every module has coverage 0, so the smallest positive cutoff reports none
					for (Modules modules : moduleInference.values()) {
						modules.setCoverageCutoff(Double.MIN_VALUE);
					}
				}
			}
			// export sample as simple files
			if (outFormat.equals("1")) {
				for (Entry<String, Modules> entry : moduleInference.entrySet()) {
					app.exportModules(outputDir, new File(entry.getKey()), entry.getValue().toAboveCutoffList(),
							annotation);
				}
				// export as Matrix
			} else if (outFormat.equals("2")) {
				// Output as Matrix
				// --with-coverage
				File outCounts = new File(outputDir, "modules.tsv");
				File outCoverage = new File(outputDir, "modules-coverage.tsv");
				MatrixWriter matrixWriter = ((annotation.equals("2") || annotation.equals("h2")) && isPerTaxon) ? new ModuleTaxonomyMatrixWriter() : new ModuleMatrixWriter();
				// exportModules in one go instead of iterating uselessly twice
				matrixWriter.exportModules(moduleInference, outCounts, outCoverage);
			}
		} catch (IOException | IncorrectNumberOfEntriesException | OmixerRpmException | DuplicateColumnException e) {
			app.log.error("Exception while reading input data: " + e.getMessage());
			return EXIT_ERROR;
		}
		long time = System.currentTimeMillis() - start;
		app.log.info("Total run time: " + (time / 1000l));
		app.log.info("Coverage cutoff: " + (modulesObserved ? coverage : "none (no module observed)"));
		app.log.info("Output files are in " + outputDir.getAbsolutePath());
		return 0;
	}
}