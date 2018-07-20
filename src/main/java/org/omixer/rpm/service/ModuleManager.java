package org.omixer.rpm.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.omixer.rpm.model.BasicFeature;
import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.ModuleInferenceOptions;
import org.omixer.rpm.model.Modules;
import org.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 *
 */
public interface ModuleManager {

	/**
	 * 
	 * Does module inference on a list of Orthologs
	 * 
	 * The orthologs that were mapped in the module have a value greater than 0 
	 * 
	 * @param orthologs
	 * @param options
	 * @param modules The reference module set
	 * @return a list of {@link Module}s that have a higher coverage than the cutoff defined in {@link ModuleInferenceOptions}
	 */
	Modules inferModules(List<BasicFeature> orthologs, ModuleInferenceOptions options, List<Module> modules);

	ConcurrentHashMap<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			List<Module> referenceModules) throws IncorrectNumberOfEntriesException, IOException;

}
