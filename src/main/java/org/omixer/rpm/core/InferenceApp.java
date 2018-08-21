package org.omixer.rpm.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.ModuleCoverageDistribution;
import org.omixer.rpm.model.ModuleInferenceOptions;
import org.omixer.rpm.model.Modules;
import org.omixer.rpm.model.enums.ScalingMethod;
import org.omixer.rpm.model.io.MatrixWriter;
import org.omixer.rpm.model.io.ModuleMatrixWriter;
import org.omixer.rpm.model.io.ModuleTaxonomyMatrixWriter;
import org.omixer.rpm.parsers.ModuleParser;
import org.omixer.rpm.service.impl.ModuleManagerImpl;
import org.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * A client app to infer modules from command line
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 *
 */
public class InferenceApp extends AbstractInferenceApp {

	public static final String TOOL_NAME="omixer-rpm.jar";
	public static final String EXEC_COMMAND = "java -jar " + TOOL_NAME + " ";
	public static final String HEADER = "\n\nDESCRIPTION\n"
			+ " Omixer-RPM\n A Reference Pathways Mapper for turning metagenomic functional profiles into pathway/module profiles\n\n"
			+ "VERSION: 1.0 (13 June 2018)\n" + "AUTHOR: Youssef Darzi <youssef.darzi@gmail.com>\n\n"
			+ "ARGUMENTS (Options starting with -X are non-standard and subject to change without notice.)\n\n";

	public static final String FOOTER = "\nLicensed under an Academic Non-commercial Software License Agreement, https://github.com/raeslab/omixer-rpm/blob/master/LICENSE";

	public static void main(String[] args) {

		InferenceApp app = new InferenceApp();

		CommandLineParser parser = new DefaultParser();
		Options options = new Options();

		options.addOption(Option.builder("c").longOpt("coverage")
				.desc("The minimum coverage cut-off to accept a module [0.0 to 1.0].\nDefaults to -1, where the coverage is learned from the coverage distribution of all modules")
				.hasArg().argName("COVERAGE").build());

		options.addOption(Option.builder("s").longOpt("score-estimator")
				.desc("The score estimatore.\nAccepted values are [median|average].\nDefaults to median").hasArg()
				.argName("SCORE-ESTIMATOR").build());

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

		ScalingMethod scalingMethod = ScalingMethod.NONE;
		// Use name match and get id
		File moduleFile = null;
		File inputdir = null;
		File outputDir = null;

		Set<String> formatValues = new HashSet<>();
		formatValues.add("1");
		formatValues.add("2");

		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(800);
		if (args.length == 0) {
			formatter.printHelp(EXEC_COMMAND, HEADER, options, FOOTER, true);
			return;
		}

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			// validate that block-size has been set

			if (line.hasOption('h')) {
				formatter.printHelp(EXEC_COMMAND, HEADER, options, FOOTER, true);
				return;
			}

			if (line.hasOption("coverage")) {
				String coverageValue = line.getOptionValue("coverage");
				try {
					coverage = Double.valueOf(coverageValue);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(coverageValue
							+ " is not a valid coverage value. Please provide a decimal value between 0 and 1, or -1 to enable automatic guessing of the coverage threshold.");
				}
			}
			if (line.hasOption("score-estimator")) {
				String estimator = line.getOptionValue("score-estimator");

				if ("average".equals(estimator)) {
					algorithm = "ABUNDANCE_COVERAGE_REACTION_BASED";
				} else if (!"median".equals(estimator)) {
					throw new IllegalArgumentException(estimator
							+ " is not a valid value for score calculation. Please chose between median or average");
				}
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
				if (annotation.equals("2")) {
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
			return;
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
		long start = System.currentTimeMillis();
		try {
			final List<Module> referenceModules = ModuleParser.parseModuleFile(moduleFile);
			ConcurrentHashMap<String, Modules> moduleInference = moduleManager.inferModules(inputdir,
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
				Double optimalCoverage = mcd.findOptimalCoverage();
				coverage = optimalCoverage;
				for (Modules modules : moduleInference.values()) {
					modules.setCoverageCutoff(optimalCoverage);
				}
				// Export as svg 
				Files.write(new File(outputDir, "coverage-plot.svg").toPath(), mcd.toSVG().getBytes());
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
				MatrixWriter matrixWriter = (annotation.equals("2") && isPerTaxon) ? new ModuleTaxonomyMatrixWriter() : new ModuleMatrixWriter();
				matrixWriter.writeCounts(moduleInference, outCounts);
				matrixWriter.writeCoverage(moduleInference, outCoverage);
				
			}
		} catch (IOException | IncorrectNumberOfEntriesException e) {
			app.log.error("Exception while reading input data: " + e.getMessage());
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			app.log.error("Exception while reading input. Please make sure you are using the correct annotation value (cf. --annotation description) for your input.");
			return;
		}
		long time = System.currentTimeMillis() - start;
		app.log.info("Total run time: " + (time / 1000l));
		app.log.info("Coverage cutoff: " + coverage);
		app.log.info("Output files are in " + outputDir.getAbsolutePath());
	}
}