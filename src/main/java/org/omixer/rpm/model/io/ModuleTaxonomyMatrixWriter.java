package org.omixer.rpm.model.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.Modules;
import org.omixer.utils.Constants;

public class ModuleTaxonomyMatrixWriter extends MatrixWriter {

	/*
	 * (non-Javadoc)
	 * @see org.omixer.rpm.model.io.MatrixWriter#writeMatrix(java.util.Map, java.io.File, java.util.function.Function)
	 */
	public void writeMatrix(Map<String, Modules> moduleInference, File outfile, Function<Module, Double> f)
			throws IOException {
		// Map of all observed combinations of taxa and modules
		Map<String, Set<String>> taxaModules = new HashMap<>();
		// Map of modules by taxon and moduleId for a quick lookup
		Map<String, Map<String, Module>> sampleTaxonModules = new HashMap<>();
		// generate the Observed taxonModules, as row names for the matrix
		for (Entry<String, Modules> entry : moduleInference.entrySet()) {
			// map modules by taxon and moduleId
			Map<String, Module> taxonMods = new HashMap<>();
			sampleTaxonModules.put(entry.getKey(), taxonMods);
			// retain above cutoff modules
			List<Module> modules = entry.getValue().toAboveCutoffList();
			// set the new modules
			entry.getValue().setModules(modules);
			for (Module module : modules) {
				String taxon = module.getTaxon();
				// make sure it is not null
				if (taxon == null) {
					taxon = Constants.EMPTY_STRING;
				}
				Set<String> taxonModules = taxaModules.get(taxon);
				if (taxonModules == null) {
					taxonModules = new HashSet<>();
					taxaModules.put(taxon, taxonModules);
				}
				taxonModules.add(module.getModuleId());
				taxonMods.put(taxon + module.getModuleId() , module);
			}
		}

		/*
		 * The number of samples is known, so write header For each observed entry <
		 * find observed value
		 */
		try (BufferedWriter out = new BufferedWriter(new FileWriter(outfile))) {
			List<String> samples = moduleInference.keySet().stream().collect(Collectors.toList());
			// output header
			String header = samples.stream().reduce("Taxon\tModule", (a, b) -> (a + Constants.TAB + b));
			out.write(header + Constants.NEW_LINE);
			// each taxon
			// TODO remove each entry after iteration
			for (Entry<String, Set<String>> taxonModules : taxaModules.entrySet()) {
				// each module
				for (String observedModule : taxonModules.getValue()) {
					String countOutputString = taxonModules.getKey() + Constants.TAB + observedModule;
					// each sample
					for (String sample : samples) {
						Module module = sampleTaxonModules.get(sample).remove(taxonModules.getKey() + observedModule);
						Double count = Constants.ZERO;
						
						if (module != null) {
							count = f.apply(module);
						}
						countOutputString += Constants.TAB + count;
					}
					out.write(countOutputString);
					out.newLine();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.omixer.rpm.model.io.MatrixWriter#exportModules(java.util.Map,
	 * java.io.File, java.io.File)
	 */
	@Override
	public void exportModules(Map<String, Modules> moduleInference, File outCounts, File outCoverage)
			throws IOException {
		// Map of all observed combinations of taxa and modules
		Map<String, Set<String>> taxaModules = new HashMap<>();
		// Map of modules by taxon and moduleId for a quick lookup
		Map<String, Map<String, Module>> sampleTaxonModules = new HashMap<>();
		// generate the Observed taxonModules, as row names for the matrix
		for (Entry<String, Modules> entry : moduleInference.entrySet()) {
			// map modules by taxon and moduleId
			Map<String, Module> taxonMods = new HashMap<>();
			sampleTaxonModules.put(entry.getKey(), taxonMods);
			// retain above cutoff modules
			List<Module> modules = entry.getValue().toAboveCutoffList();
			// set the new modules
			entry.getValue().setModules(modules);
			for (Module module : modules) {
				String taxon = module.getTaxon();
				// make sure it is not null
				if (taxon == null) {
					taxon = Constants.EMPTY_STRING;
				}
				Set<String> taxonModules = taxaModules.get(taxon);
				if (taxonModules == null) {
					taxonModules = new HashSet<>();
					taxaModules.put(taxon, taxonModules);
				}
				taxonModules.add(module.getModuleId());
				taxonMods.put(taxon + module.getModuleId() , module);
			}
		}

		/*
		 * The number of samples is known, so write header For each observed entry <
		 * find observed value
		 */
		try (BufferedWriter countOut = new BufferedWriter(new FileWriter(outCounts));
				BufferedWriter coverageOut = new BufferedWriter(new FileWriter(outCoverage))) {
			List<String> samples = moduleInference.keySet().stream().collect(Collectors.toList());
			// output header
			String header = samples.stream().reduce("Taxon\tModule", (a, b) -> (a + Constants.TAB + b));
			countOut.write(header + Constants.NEW_LINE);
			coverageOut.write(header + Constants.NEW_LINE);
			// each taxon
			// TODO remove each entry after iteration
			for (Entry<String, Set<String>> taxonModules : taxaModules.entrySet()) {
				// each module
				for (String observedModule : taxonModules.getValue()) {
					String countOutputString = taxonModules.getKey() + Constants.TAB + observedModule;
					String coverageOutputString = countOutputString;
					// each sample
					for (String sample : samples) {
						Module module = sampleTaxonModules.get(sample).remove(taxonModules.getKey() + observedModule);
						Double count = Constants.ZERO;
						Double coverage = Constants.ZERO;
						
						if (module != null) {
							count = module.getCount();
							coverage = module.getCoverage();
						}

						countOutputString += Constants.TAB + count;
						coverageOutputString += Constants.TAB + coverage;
					}
					countOut.write(countOutputString);
					countOut.newLine();

					coverageOut.write(coverageOutputString);
					coverageOut.newLine();
				}
			}
		}
	}
}