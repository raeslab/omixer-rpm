package org.omixer.rpm.model.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

public class ModuleMatrixWriter extends MatrixWriter {

	public void writeMatrix(Map<String, Modules> moduleInference, File outfile, Function<Module, Double> f)
			throws IOException {

		Set<String> observedModules = new HashSet<>();
		/**
		 * As the module space is very small i.e max 120 modules. For a 1000
		 * samples we have 120000 objects to store which is nothing. So add all
		 * the Modules to a list => put species_ko into has => iterate and save
		 * sample/features
		 */
		// generate the observed taxonModules
		for (Entry<String, Modules> entry : moduleInference.entrySet()) {
			List<Module> modules = entry.getValue().toAboveCutoffList();
			// all modules are above cutoff and there is no need to
			// filter them anymore
			entry.getValue().setModules(modules);
			for (Module module : modules) {
				observedModules.add(module.getModuleId());
			}
		}
		/*
		 * The number of samples is known, so write header For each observed
		 * entry < find observed value
		 */
		try (BufferedWriter out = new BufferedWriter(new FileWriter(outfile))) {
			List<String> samples = moduleInference.keySet().stream().collect(Collectors.toList());
			// output header
			String header = samples.stream().reduce("Module", (a, b) -> (a + Constants.TAB + b));
			out.write(header + Constants.NEW_LINE);
			/**
			 * Think of another way to optimize. - Could also reduce the search
			 * space after each iteration by removing matched object - Or sort
			 * and compare based on sort to ensure next object is the closets to
			 * top object
			 */
			// each module
			for (String observedModule : observedModules) {
				String outputString = observedModule;
				for (String sample : samples) {
					Double count = Constants.ZERO;
					for (Module module : moduleInference.get(sample).getModules()) {
						if (module.getModuleId().equals(observedModule)) {
							count = f.apply(module);
							break;
						}
					}
					outputString += Constants.TAB + count;
				}
				out.write(outputString);
				out.newLine();
			}
		}
	}

	/* 
	 * Simply delegates to writeMatrix for now
	 * 
	 * (non-Javadoc)
	 * @see org.omixer.rpm.model.io.MatrixWriter#exportModules(java.util.Map, java.io.File, java.io.File)
	 */
	@Override
	public void exportModules(Map<String, Modules> moduleInference, File outCounts, File outCouverage)
			throws IOException {
		writeCounts(moduleInference, outCounts);
		writeCoverage(moduleInference, outCouverage);
	}
}