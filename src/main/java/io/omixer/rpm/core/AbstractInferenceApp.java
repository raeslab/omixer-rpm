package io.omixer.rpm.core;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.enums.ScalingMethod;
import io.omixer.utils.Constants;
import io.omixer.utils.utils.FileUtils;

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

	/**
	 * Writes the modules of one sample to &lt;input name&gt;.modules, with a taxon column when the results are per taxon
	 */
	protected void exportModules(File outputDir, File input, Iterable<Module> modules, boolean perTaxon)
			throws IOException {

		String header = perTaxon ? MODULE_TAXON_FILE_HEADER : MODULE_FILE_HEADER;
		Function<Module, String> writer = perTaxon ? TAXON_MODULE_FILE_WRITER : MODULE_FILE_WRITER;

		FileUtils.writeObjects(new File(outputDir, input.getName() + ".modules"), header, modules, writer);
	}
}
