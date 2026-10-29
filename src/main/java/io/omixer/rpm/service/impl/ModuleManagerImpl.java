package io.omixer.rpm.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.engine.CompiledModule;
import io.omixer.rpm.exceptions.DuplicateSampleNameException;
import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.model.BasicFeature;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Modules;
import io.omixer.rpm.parsers.FunctionLineProcessor;
import io.omixer.rpm.parsers.FunctionRowMapper;
import io.omixer.rpm.parsers.TaxonFunctionHumannLineProcessor;
import io.omixer.rpm.parsers.TaxonFunctionLineProcessor;
import io.omixer.rpm.parsers.TaxonFunctionRowMapper;
import io.omixer.rpm.scoring.ModuleScore;
import io.omixer.rpm.scoring.ModuleScorer;
import io.omixer.rpm.service.ModuleManager;
import io.omixer.utils.Constants;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;
import io.omixer.utils.readers.FileLine;
import io.omixer.utils.readers.MatrixLineProcessor;
import io.omixer.utils.utils.FileUtils;

/**
 * 
 * @author <a href="mailto:youssef.darzi@omixer.io">Youssef Darzi</a>
 * 
 */
public class ModuleManagerImpl implements ModuleManager {

	protected final Log log = LogFactory.getLog(getClass());
	
	// use 1 as default
	private String inputFormat = "1";
	boolean concurrent;

	public boolean isConcurrent() {
		return concurrent;
	}

	public String getInputFormat() {
		return inputFormat;
	}

	public void setConcurrent(boolean concurrent) {
		this.concurrent = concurrent;
	}

	public void setInputFormat(String inputFormat) {
		this.inputFormat = inputFormat;
	}

	private Map<String, Double> makeOrthologAbundanceMap(List<BasicFeature> orthologs) {
		// mapping of ortholog id to it abundance
		Map<String, Double> orthologAbundanceMap = new HashMap<String, Double>();
		// populate the orthologs and the abundance map
		for (BasicFeature bf : orthologs) {
            orthologAbundanceMap.merge(bf.getFunction(), bf.getCount(), Double::sum);
		}
		return orthologAbundanceMap;
	}

	public Modules inferModules(List<BasicFeature> orthologs, ModuleInferenceOptions options, CompiledDatabase database) {
		ModuleScorer scorer = new ModuleScorer(options.getScoringOptions());
		if (!options.isPerTaxon()) {
			return score(orthologs, null, options, database, scorer);
		}
		Modules allModules = new Modules();
		allModules.setCoverageCutoff(options.getCoverage());
		// infer per taxon, removing each taxon's features once inferred
		Map<String, List<BasicFeature>> orthologMap = orthologs.stream()
				.collect(Collectors.groupingBy(BasicFeature::getTaxon));
		for (String taxon : new ArrayList<>(orthologMap.keySet())) {
			allModules.addModules(score(orthologMap.remove(taxon), taxon, options, database, scorer));
		}
		return allModules;
	}

	/** Scores every module of the database in one profile (a sample, or a taxon of a sample) */
	private Modules score(List<BasicFeature> orthologs, String taxon, ModuleInferenceOptions options,
			CompiledDatabase database, ModuleScorer scorer) {
		double[] values = koValues(makeOrthologAbundanceMap(orthologs), database, options.isDistributeOrhologAbundance());
		List<Module> modules = new ArrayList<>(database.modules().size());
		for (CompiledModule module : database.modules()) {
			ModuleScore score = scorer.score(module, values);
			modules.add(new Module(module.id(), taxon, score.coverage(), score.abundance()));
		}
		return new Modules(modules, options.getCoverage());
	}

	/**
	 * The profile's value of every KO of the database (0 when absent from the input). With --Xdistribute, a KO used by
	 * N distinct modules gives each of them its value ÷ N.
	 */
	private static double[] koValues(Map<String, Double> abundances, CompiledDatabase database, boolean distribute) {
		double[] values = new double[database.koCount()];
		for (int ko = 0; ko < values.length; ko++) {
			Double value = abundances.get(database.ko(ko));
			if (value != null) {
				int modules = database.modulesUsing(ko);
				values[ko] = distribute && modules > 1 ? value / modules : value;
			}
		}
		return values;
	}

	/**
	 * Current possible values for iput format are 1 (orthologs), 2(orthologs+taxa), h2(humann2 matrix)
	 */
	public Map<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			CompiledDatabase database) throws IncorrectNumberOfEntriesException, IOException {

		Map<String, List<BasicFeature>> sampleOrthologs;
		long start = System.currentTimeMillis();
		if (input.isDirectory()) {
			sampleOrthologs = readSampleDirectory(input);
		} else {
			// set MatrixLineProcessor according to input format
			MatrixLineProcessor<BasicFeature> mlp = switch (getInputFormat()) {
                case "1" -> new FunctionLineProcessor();
                case "2" -> new TaxonFunctionLineProcessor();
                case "h2" -> new TaxonFunctionHumannLineProcessor();
                default ->
                        throw new RuntimeException("Input format " + getInputFormat() + " is not compatible with this version.");
            };

            // reads matrix
			sampleOrthologs = FileUtils.readMatrix(input, Constants.TAB, mlp);
		}
		log.debug("Loaded samples in " + ((System.currentTimeMillis() - start)/1000));
		warnAboutNumericFunctions(sampleOrthologs);
			
		// create a stream to process the data per samples
		Stream<Entry<String, List<BasicFeature>>> stream = isConcurrent()
				? sampleOrthologs.entrySet().parallelStream() : sampleOrthologs.entrySet().stream();
				
		start = System.currentTimeMillis();
		// infer the modules: sample name => the module inference. Collecting an ordered stream into a
		// LinkedHashMap keeps the input sample order, also in parallel
		// TODO check if removal of orthologs here will save memory
		Map<String, Modules> moduleInference = stream.collect(Collectors.toMap(Entry::getKey,
				entry -> inferModules(entry.getValue(), options, database), (first, second) -> first, LinkedHashMap::new));
		log.debug("Inferred modules in " + ((System.currentTimeMillis() - start)/1000));
		return moduleInference;
	}

	/**
	 * Function ids that read as numbers usually mean an abundance column was read as the function column, i.e. the
	 * wrong input annotation format (-a). Only a warning: the ids are not checked against a fixed list, so that
	 * annotations from newer databases still work.
	 */
	private void warnAboutNumericFunctions(Map<String, List<BasicFeature>> sampleOrthologs) {
		List<String> numericFunctions = sampleOrthologs.values().stream().flatMap(List::stream)
				.filter(BasicFeature::isFunctionNumeric).map(BasicFeature::getFunction).distinct().limit(3).toList();
		if (!numericFunctions.isEmpty()) {
			log.warn("Some function ids in the input are numbers (e.g. " + String.join(", ", numericFunctions)
					+ "): please check that the annotation format (-a) matches the input");
		}
	}

	/**
	 * Reads a directory with one headerless file per sample. Sub-directories and hidden files (e.g. .DS_Store)
	 * are skipped; any other file must be a sample.
	 * 
	 * @throws InputFormatException if a line of a sample file does not follow the input format
	 * @throws IOException if the directory or a sample file cannot be read
	 */
	private Map<String, List<BasicFeature>> readSampleDirectory(File input) throws IOException {
		File[] files = input.listFiles();
		if (files == null) {
			// input is a directory, so a null listing means it could not be read
			throw new IOException("Cannot list the files of input directory " + input);
		}
		Function<FileLine, BasicFeature> rowMapper = getInputFormat().equals("1") ? new FunctionRowMapper()
				: new TaxonFunctionRowMapper();
		// samples in file name order
		Arrays.sort(files, Comparator.comparing(File::getName));
		Map<String, List<BasicFeature>> sampleOrthologs = new LinkedHashMap<>();
		// sample name => the file it comes from, to report two files giving the same name
		Map<String, File> sampleFiles = new HashMap<>();
		for (File file : files) {
			if (!file.isFile() || file.isHidden()) {
				continue;
			}
			String sample = sampleName(file);
			File other = sampleFiles.putIfAbsent(sample, file);
			if (other != null) {
				throw new DuplicateSampleNameException("Files " + other.getName() + " and " + file.getName() + " in "
						+ input + " both give sample name '" + sample + "'");
			}
			sampleOrthologs.put(sample, readSampleFile(file, rowMapper));
		}
		return sampleOrthologs;
	}

	/**
	 * The sample name of a file is its name without the last extension, e.g. run.v2.tsv gives run.v2
	 */
	private static String sampleName(File file) {
		String name = file.getName();
		int extension = name.lastIndexOf('.');
		return extension > 0 ? name.substring(0, extension) : name;
	}

	/**
	 * Maps every line of a headerless sample file, passing the file name and line number along for error messages
	 */
	private List<BasicFeature> readSampleFile(File file, Function<FileLine, BasicFeature> rowMapper)
			throws IOException {
		List<BasicFeature> features = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String text;
			int lineNumber = 0;
			while ((text = reader.readLine()) != null) {
				features.add(rowMapper.apply(new FileLine(file.getName(), ++lineNumber, text)));
			}
		}
		return features;
	}
}
