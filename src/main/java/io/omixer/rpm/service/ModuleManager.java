package io.omixer.rpm.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.model.BasicFeature;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Modules;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * 
 * @author <a href="mailto:youssef.darzi@omixer.io">Youssef Darzi</a>
 *
 */
public interface ModuleManager {

	/**
	 *Does module inference on a list of Orthologs
	 * 
	 * The orthologs that were mapped in the module have a value greater than 0 
	 * 
	 * @param orthologs
	 * @param options
	 * @param database the reference modules
	 * @return a {@link Module} result for every module of the database (per taxon in per-taxon runs), with the
	 *         coverage cutoff defined in {@link ModuleInferenceOptions}
	 */
	Modules inferModules(List<BasicFeature> orthologs, ModuleInferenceOptions options, CompiledDatabase database);

	/**
	 * Does module inference on an Orthologs abundance file, optionally annotated with taxonomic information
	 * 
	 * 
	 * @param input
	 * @param options
	 * @param database the reference modules
	 * @return the modules inferred for each sample, iterating in input order: matrix header order, or file name
	 *         order for a directory
	 * @throws IncorrectNumberOfEntriesException
	 * @throws IOException
	 */
	Map<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			CompiledDatabase database) throws IncorrectNumberOfEntriesException, IOException;

}
