package io.omixer.rpm.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	boolean concurent;

	public boolean isConcurrent() {
		return concurent;
	}

	public String getInputFormat() {
		return inputFormat;
	}

	public void setConcurrent(boolean concurrent) {
		this.concurent = concurrent;
	}

	public void setInputFormat(String inputFormat) {
		this.inputFormat = inputFormat;
	}

	private Map<String, Double> makeOrthologAbundanceMap(List<BasicFeature> orthologs) {
		// mapping of ortholog id to it abundance
		Map<String, Double> orthologAbundanceMap = new HashMap<String, Double>();
		// populate the orthologs and the abundance map
		for (BasicFeature bf : orthologs) {

			Double abundance = orthologAbundanceMap.get(bf.getFunction());
			// first time ortholog observed?
			if (abundance == null) {
				orthologAbundanceMap.put(bf.getFunction(), bf.getCount());
			} else {
				orthologAbundanceMap.put(bf.getFunction(), bf.getCount() + abundance);
			}
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
	public ConcurrentHashMap<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			List<Module> referenceModules) throws IncorrectNumberOfEntriesException, IOException {
		ConcurrentHashMap<String, Modules> moduleInference = new ConcurrentHashMap<>();

		if (input.isDirectory()) {
			File[] files = input.listFiles();
			if (files == null) {
				throw new RuntimeException("Input directory " + input
						+ " is empty or does not exist. Please make sure the input directory path is correct!");
			}
			// do module inference for all samples
			List<File> inputFiles = Arrays.asList(files);
			// create a processing stream
			Stream<File> stream = isConcurrent() ? inputFiles.parallelStream() : inputFiles.stream();
			// create the row mapper
			final Function<String, BasicFeature> rowMapper = getInputFormat().equals("1") ? new FunctionRowMapper()
					: new TaxonFunctionRowMapper();

			stream.forEach(file -> {
				List<BasicFeature> orthologs;
				try {
					orthologs = FileUtils.readCSV(file, 0, rowMapper);
					List<Module> refMods = referenceModules.stream().map(x -> x.clone()).collect(Collectors.toList());
					Modules modules = inferModules(orthologs, options, refMods);
					// FIXME careful not to remove more than necessary
					moduleInference.put(file.getName().split("\\.")[0], modules);
				} catch (IOException e) {
					throw new RuntimeException(
							"Exception while reading " + file.getAbsolutePath() + " " + e.getMessage());
				}
			});

		} else {
			// set MatrixLineProcessor according to input format
			MatrixLineProcessor<BasicFeature> mlp = null;

			switch (getInputFormat()) {
			case "1":
				mlp = new FunctionLineProcessor();
				break;
			case "2":
				mlp = new TaxonFunctionLineProcessor();
				break;
			case "h2":
				mlp = new TaxonFunctionHumannLineProcessor();
				break;
			default:
				throw new RuntimeException("Input format " + getInputFormat() + " is not compatible with this version.");
			}		
			// reads matrix
			long start = System.currentTimeMillis();		
			
			Map<String, List<BasicFeature>> sampleOrthologs = FileUtils.readMatrix(input, Constants.TAB, mlp);
			
			log.debug("Loaded samples in " + ((System.currentTimeMillis() - start)/1000));
			
			// create a stream to process the data per samples
			Stream<Entry<String, List<BasicFeature>>> stream = isConcurrent()
					? sampleOrthologs.entrySet().parallelStream() : sampleOrthologs.entrySet().stream();
					
			start = System.currentTimeMillis();
			// infer the modules
			stream.forEach(entry -> {
				// create a module list for each different process
				List<Module> refMods = referenceModules.stream().map(x -> x.clone()).collect(Collectors.toList());
				// do module inference
				
				// TODO check if removal of orthologs here will save memory
				
				Modules modules = inferModules(entry.getValue(), options, refMods);
				// put sample name => the current module inference, in the result
				moduleInference.put(entry.getKey(), modules);
			});
			log.debug("Inferred modules in " + ((System.currentTimeMillis() - start)/1000));
		}
		return moduleInference;
	}
	
	protected Modules inferModulesCore(List<BasicFeature> orthologs, ModuleInferenceOptions options,
			List<Module> modules) {

		// mapping of ortholog id to its abundance
		Map<String, Double> orthologAbundanceMap = makeOrthologAbundanceMap(orthologs);
		// list of predicted modules
		List<Module> predictedModules = new ArrayList<Module>();
		for (Iterator<Module> iterator = modules.iterator(); iterator.hasNext();) {
			/*
			 * Clone to avoid keeping only best paths after first inference.
			 * This is important as the methods below will change the module
			 * state
			 */
			Module module = iterator.next().clone();
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
				double proportionnedValue = orthologAbundanceMap.get(entry.getKey()) / observedModulesCount;
				for (Module m : entry.getValue()) {
					// update every occurrence of the Ortholog in this module, not only one of them (B02)
					for (Ortholog ortholog : m.getFlatOrthologs()) {
						if (ortholog.getOrthologId().equals(entry.getKey())) {
							ortholog.setCount(proportionnedValue);
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