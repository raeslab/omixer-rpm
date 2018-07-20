package org.omixer.rpm.core;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.ModuleInferenceOptions;
import org.omixer.rpm.model.enums.ScalingMethod;
import org.omixer.utils.Constants;
import org.omixer.utils.utils.FileUtils;

/**
 * A client app to infer modules from command line
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 *
 */
public abstract class AbstractInferenceApp {

	protected final Log log = LogFactory.getLog(getClass());
	final static String MODULE_FILE_HEADER = "Module\tValue\tCoverage";
	final static String MODULE_TAXON_FILE_HEADER = "Module\tTaxon\tValue\tCoverage";

	final static Function<Module, String> MODULE_FILE_WRITER = new Function<Module, String>() {

		public String apply(Module module) {
			return module.getModuleId() + Constants.TAB + module.getCount() + Constants.TAB + module.getCoverage();
		}
	};

	final static Function<Module, String> TAXON_MODULE_FILE_WRITER = new Function<Module, String>() {

		@Override
		public String apply(Module module) {
			return module.getModuleId() + Constants.TAB + module.getTaxon() + Constants.TAB + module.getCount()
					+ Constants.TAB + module.getCoverage();
		}
	};

	protected ModuleInferenceOptions makeModuleSetOptions(String algorithm, double coverage,
			ScalingMethod scalingMethod) {
		final ModuleInferenceOptions options = new ModuleInferenceOptions();
		options.setAlgorithm(algorithm);
		options.setCoverage(coverage);
		options.setScalingMethod(scalingMethod);
		return options;
	}

	protected void exportModules(File outputDir, File input, Iterable<Module> modules, String format)
			throws IOException {

		String header = null;
		Function<Module, String> writer = null;

		if (format.equals("1")) {
			header = MODULE_FILE_HEADER;
			writer = MODULE_FILE_WRITER;
		} else if (format.equals("2")) {
			header = MODULE_TAXON_FILE_HEADER;
			writer = TAXON_MODULE_FILE_WRITER;
		}

		FileUtils.writeObjects(new File(outputDir, input.getName() + ".modules"), header, modules, writer);
	}
}
