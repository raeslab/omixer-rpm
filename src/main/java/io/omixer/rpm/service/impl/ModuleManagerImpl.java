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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.model.BasicFeature;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Modules;
import io.omixer.rpm.model.Ortholog;
import io.omixer.rpm.model.PathwaySummary;
import io.omixer.rpm.model.enums.ModuleInferenceOptimizers;
import io.omixer.rpm.model.optimizers.ModuleAbundanceCoverageAbundanceMinimizer;
import io.omixer.rpm.model.optimizers.ModuleAbundanceCoverageAbundanceSumMaximizer;
import io.omixer.rpm.model.optimizers.ModuleAbundanceCoverageMedianMaximizer;
import io.omixer.rpm.model.optimizers.ModuleAbundanceCoverageOrthologMaximizer;
import io.omixer.rpm.model.optimizers.ModuleAbundanceCoverageReactionMaximizer;
import io.omixer.rpm.model.optimizers.ModuleScoreCalculator;
import io.omixer.rpm.parsers.FunctionLineProcessor;
import io.omixer.rpm.parsers.FunctionRowMapper;
import io.omixer.rpm.parsers.InputLine;
import io.omixer.rpm.parsers.TaxonFunctionHumannLineProcessor;
import io.omixer.rpm.parsers.TaxonFunctionLineProcessor;
import io.omixer.rpm.parsers.TaxonFunctionRowMapper;
import io.omixer.rpm.service.ModuleManager;
import io.omixer.utils.Constants;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;
import io.omixer.utils.readers.MatrixLineProcessor;
import io.omixer.utils.utils.FileUtils;

/**
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
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

	public Modules inferModules(List<BasicFeature> orthologs, ModuleInferenceOptions options, List<Module> modules) {

		Modules allModules = new Modules();
		if (options.isPerTaxon()) {
			// Do inference per Taxon after Mapping on Taxon Id
			Map<String, List<BasicFeature>> orthologMap = orthologs.stream()
					.collect(Collectors.groupingBy(BasicFeature::getTaxon));
			// Get an iterator over taxonomic entries
			List<String> taxa = new ArrayList<>(orthologMap.keySet());
			
			allModules.setCoverageCutoff(options.getCoverage());
			// iterate over taxa then remove BasicFeature for each taxa after inference
			for (String taxon : taxa) {
				Modules taxonMondules = null;
				if (options.isDistributeOrhologAbundance()) {
					taxonMondules = inferModulesWithDistributedOrthologAbundance(orthologMap.remove(taxon), options, modules);
				} else {
					taxonMondules = inferModulesCore(orthologMap.remove(taxon), options, modules);
				}
				
				for (Module module : taxonMondules.getModules()) {
					module.setTaxon(taxon);
				}
				allModules.addModules(taxonMondules);
			}
		} else {
			// proceed with inference
			if (options.isDistributeOrhologAbundance()) {
				allModules = inferModulesWithDistributedOrthologAbundance(orthologs, options, modules);
			} else {
				allModules = inferModulesCore(orthologs, options, modules);
			}
		}

		return allModules;
	}
	
	/**
	 * Current possible values for iput format are 1 (orthologs), 2(orthologs+taxa), h2(humann2 matrix)
	 */
	public Map<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			List<Module> referenceModules) throws IncorrectNumberOfEntriesException, IOException {

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
			
		// create a stream to process the data per samples
		Stream<Entry<String, List<BasicFeature>>> stream = isConcurrent()
				? sampleOrthologs.entrySet().parallelStream() : sampleOrthologs.entrySet().stream();
				
		start = System.currentTimeMillis();
		// infer the modules: sample name => the module inference. Collecting an ordered stream into a
		// LinkedHashMap keeps the input sample order, also in parallel
		// TODO check if removal of orthologs here will save memory
		Map<String, Modules> moduleInference = stream.collect(Collectors.toMap(Entry::getKey,
				// each sample gets its own copy of the reference modules
				entry -> inferModules(entry.getValue(), options,
						referenceModules.stream().map(Module::clone).collect(Collectors.toList())),
				(first, second) -> first, LinkedHashMap::new));
		log.debug("Inferred modules in " + ((System.currentTimeMillis() - start)/1000));
		return moduleInference;
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
		Function<InputLine, BasicFeature> rowMapper = getInputFormat().equals("1") ? new FunctionRowMapper()
				: new TaxonFunctionRowMapper();
		// samples in file name order
		Arrays.sort(files, Comparator.comparing(File::getName));
		Map<String, List<BasicFeature>> sampleOrthologs = new LinkedHashMap<>();
		for (File file : files) {
			if (!file.isFile() || file.isHidden()) {
				continue;
			}
			// FIXME careful not to remove more than necessary
			sampleOrthologs.put(file.getName().split("\\.")[0], readSampleFile(file, rowMapper));
		}
		return sampleOrthologs;
	}

	/**
	 * Maps every line of a headerless sample file, passing the file name and line number along for error messages
	 */
	private List<BasicFeature> readSampleFile(File file, Function<InputLine, BasicFeature> rowMapper)
			throws IOException {
		List<BasicFeature> features = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String text;
			int lineNumber = 0;
			while ((text = reader.readLine()) != null) {
				features.add(rowMapper.apply(new InputLine(file.getName(), ++lineNumber, text)));
			}
		}
		return features;
	}
	
	protected Modules inferModulesCore(List<BasicFeature> orthologs, ModuleInferenceOptions options,
			List<Module> modules) {

		// mapping of ortholog id to its abundance
		Map<String, Double> orthologAbundanceMap = makeOrthologAbundanceMap(orthologs);
		// list of predicted modules
		List<Module> predictedModules = new ArrayList<Module>();
        for (Module value : modules) {
            /*
             * Clone to avoid keeping only best paths after first inference.
             * This is important as the methods below will change the module
             * state
             */
            Module module = value.clone();
            predictedModules.add(module);
            List<List<List<Ortholog>>> moduleOrthologs = module.getOrthologs();
            // give counts to orthologs in order to quantify
            for (List<List<Ortholog>> path : moduleOrthologs) {
                for (List<Ortholog> step : path) {
                    for (Ortholog ortholog : step) {
                        Double count = orthologAbundanceMap.get(ortholog.getOrthologId());
                        if (count != null) {
                            ortholog.setCount(count);
                        } else {
                            ortholog.setCount(0);
                        }
                    }
                }
            }
            optimizeModule(module, options);
        }
		return new Modules(predictedModules, options.getCoverage());
	}

	/**
	 * When an ortholog is shared by N modules, divide its abundance by N before the actual mapping and quantification.
	 * 
	 * @param orthologs
	 * @param options
	 * @param modules
	 * @return
	 */
	public Modules inferModulesWithDistributedOrthologAbundance(List<BasicFeature> orthologs, ModuleInferenceOptions options, List<Module> modules) {

		// mapping of ortholog id to its abundance
		Map<String, Double> orthologAbundanceMap = makeOrthologAbundanceMap(orthologs);
		/*
		 * Clone to leave the reference modules untouched, as in inferModulesCore: the methods below
		 * change the module state, and the same reference modules are reused for every taxon (B01).
		 */
		List<Module> predictedModules = modules.stream().map(Module::clone).collect(Collectors.toList());
		// A map of KO to the distinct Modules it occurs in. Module does not override equals, so the set is
		// identity-based: a KO repeated inside one module counts that module once (B02)
		Map<String, Set<Module>> koModules = new HashMap<>();
		// give counts to orthologs in order to quantify
		for (Module module : predictedModules) {
			for (Ortholog ortholog : module.getFlatOrthologs()) {
				Double count = orthologAbundanceMap.get(ortholog.getOrthologId());
				if (count != null) {
					ortholog.setCount(count);
					// Populate the set of observed modules
					koModules.computeIfAbsent(ortholog.getOrthologId(), ko -> new LinkedHashSet<>()).add(module);
				} else {
					ortholog.setCount(0);
				}
			}
		}

		/*
		 * Redistribute the values of Orthologs that appear in several modules
		 * This is an unbiased distribution as the value is simply divided
		 * by the number of modules to which it maps.
		 */
		for (Entry<String, Set<Module>> entry : koModules.entrySet()) {
			int observedModulesCount = entry.getValue().size();
			// if Ortholog is observed in more than one Module
			if (observedModulesCount > 1) {
				// Proportion the value
				double proportionedValue = orthologAbundanceMap.get(entry.getKey()) / observedModulesCount;
				for (Module m : entry.getValue()) {
					// update every occurrence of the Ortholog in this module, not only one of them (B02)
					for (Ortholog ortholog : m.getFlatOrthologs()) {
						if (ortholog.getOrthologId().equals(entry.getKey())) {
							ortholog.setCount(proportionedValue);
						}
					}
				}
			}
		}

		for (Module module : predictedModules) {
			optimizeModule(module, options);
		}

		return new Modules(predictedModules, options.getCoverage());
	}
	
	protected Module optimizeModule(Module module, ModuleInferenceOptions options) {
		// generate alternative orthologs in a structure
		/*
		 * FIXME avoid computing all paths for every instance of a module template
		 * Use some sort of a matrix or a better data structure to hold all the paths
		 */
		List<List<List<Ortholog>>> allPaths = module.makeAllPaths();
		// use a resolver to know which optimizer should be used
		ModuleScoreCalculator optimizer = null;
		
		if (options.getAlgorithm()
				.equals(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())) {
			optimizer = new ModuleAbundanceCoverageReactionMaximizer(options.isNormalizeByLength());
		} else if (options.getAlgorithm()
				.equals(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_ORTHOLOG_BASED.displayName())) {
			optimizer = new ModuleAbundanceCoverageOrthologMaximizer(options.isNormalizeByLength());
		} else if (options.getAlgorithm()
				.equals(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName())) {
			optimizer = new ModuleAbundanceCoverageMedianMaximizer(options.isNormalizeByLength());
		} else if (options.getAlgorithm().equals(ModuleInferenceOptimizers.SUM.displayName())) {
			optimizer = new ModuleAbundanceCoverageAbundanceSumMaximizer(options.isNormalizeByLength());
		} else if (options.getAlgorithm().equals(ModuleInferenceOptimizers.MIN.displayName())) {
			optimizer = new ModuleAbundanceCoverageAbundanceMinimizer(options.isNormalizeByLength());
		}

		// call the optimizer
		PathwaySummary bestPath = optimizer.computeBestScore(allPaths, options);

		// Skip filtering here and filter later
		module.setCount(bestPath.getAbundance());
		module.setCoverage(bestPath.getCoverage());
		// Modify the modules to keep only the best pathway
		List<List<Ortholog>> bestOrthologs = bestPath.getPathway();
		int bestOrthologsSize = bestOrthologs.size();
		List<List<List<Ortholog>>> orths = module.getOrthologs();
		// replace the alternatives at each step by the best enzyme
		for (int i = 0; i < bestOrthologsSize; i++) {
			List<List<Ortholog>> step = new LinkedList<List<Ortholog>>();
			step.add(bestOrthologs.get(i));
			orths.set(i, step);
		}
		return module;
	}
}