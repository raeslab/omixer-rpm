package org.omixer.rpm.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omixer.rpm.model.BasicFeature;
import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.ModuleInferenceOptions;
import org.omixer.rpm.model.Modules;
import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.model.PathwaySummary;
import org.omixer.rpm.model.enums.ModuleInferenceOptimizers;
import org.omixer.rpm.model.optimizers.ModuleAbundanceCoverageMedianMaximizer;
import org.omixer.rpm.model.optimizers.ModuleAbundanceCoverageOrthologMaximizer;
import org.omixer.rpm.model.optimizers.ModuleAbundanceCoverageReactionMaximizer;
import org.omixer.rpm.model.optimizers.ModuleScoreCalculator;
import org.omixer.rpm.parsers.FunctionLineProcessor;
import org.omixer.rpm.parsers.FunctionRowMapper;
import org.omixer.rpm.parsers.TaxonFunctionLineProcessor;
import org.omixer.rpm.parsers.TaxonFunctionRowMapper;
import org.omixer.rpm.service.ModuleManager;
import org.omixer.utils.Constants;
import org.omixer.utils.exceptions.IncorrectNumberOfEntriesException;
import org.omixer.utils.readers.MatrixLineProcessor;
import org.omixer.utils.utils.FileUtils;

/**
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class ModuleManagerImpl implements ModuleManager {

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

			allModules.setCoverageCutoff(options.getCoverage());
			for (Entry<String, List<BasicFeature>> entry : orthologMap.entrySet()) {
				
				Modules taxonMondules = null;
				if (options.isDistributeOrhologAbundance()) {
					taxonMondules = inferModulesWithDistributedOrthologAbundance(entry.getValue(), options, modules);
				} else {
					taxonMondules = inferModulesCore(entry.getValue(), options, modules);
				}
				
				for (Module module : taxonMondules.getModules()) {
					module.setTaxon(entry.getKey());
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
	
	public ConcurrentHashMap<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			List<Module> referenceModules) throws IncorrectNumberOfEntriesException, IOException {
		ConcurrentHashMap<String, Modules> moduleInference = new ConcurrentHashMap<>();

		final Function<String, BasicFeature> rowMapper = getInputFormat().equals("1") ? new FunctionRowMapper()
				: new TaxonFunctionRowMapper();

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
			MatrixLineProcessor<BasicFeature> mlp = getInputFormat().equals("1") ? new FunctionLineProcessor() : new TaxonFunctionLineProcessor();
			// reads matrix
			Map<String, List<BasicFeature>> sampleOrthologs = FileUtils.readMatrix(input, Constants.TAB, mlp);
			// create a stream to process the data
			Stream<Entry<String, List<BasicFeature>>> stream = isConcurrent()
					? sampleOrthologs.entrySet().parallelStream() : sampleOrthologs.entrySet().stream();
			// infer the modules
			stream.forEach(entry -> {
				// create a module list for each different process
				List<Module> refMods = referenceModules.stream().map(x -> x.clone()).collect(Collectors.toList());
				// do module inference
				Modules modules = inferModules(entry.getValue(), options, refMods);
				// put sample name => the current module inference, in the result
				moduleInference.put(entry.getKey(), modules);
			});
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
		// A map of KO to Modules
		Map<String, List<Module>> koModules = new HashMap<>();
		// give counts to orthologs in order to quantify
		for (Module module : modules) {
			for (Ortholog ortholog : module.getFlatOrthologs()) {
				Double count = orthologAbundanceMap.get(ortholog.getOrthologId());
				if (count != null) {
					ortholog.setCount(count);
					// Populate the list of observed modules
					List<Module> observedModules = koModules.get(ortholog.getOrthologId());
					if (observedModules == null) {
						observedModules = new ArrayList<>();
						koModules.put(ortholog.getOrthologId(), observedModules);
					}
					observedModules.add(module);
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
		for (Entry<String, List<Module>> entry : koModules.entrySet()) {
			int observedModulesCount = entry.getValue().size();
			// if Ortholog is observed in more than one Module
			if (observedModulesCount > 1) {
				// Proportion the value
				double proportionnedValue = orthologAbundanceMap.get(entry.getKey()) / observedModulesCount;
				for (Module m : entry.getValue()) {
					Map<String, Ortholog> orthologsMap = m.getFlatOrthologsAsMap();
					// update the value of the matched Ortholog
					orthologsMap.get(entry.getKey()).setCount(proportionnedValue);
				}
			}
		}
		
		for (Module module : modules) {
			optimizeModule(module, options);
		}

		return new Modules(modules, options.getCoverage());
	}
	
	protected Module optimizeModule(Module module, ModuleInferenceOptions options) {
		// generate alternative orthologs in a structure
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