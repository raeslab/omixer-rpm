package io.omixer.rpm.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import io.omixer.rpm.engine.ModuleResult;
import io.omixer.rpm.engine.Results;
import io.omixer.utils.Constants;
import io.omixer.utils.utils.FileUtils;

/**
 * Writes one file per sample, sample.modules, with its reported results: Module, (Taxon,) Value, Coverage.
 */
public final class PerSampleWriter implements ResultWriter {

	@Override
	public void write(Results results, Path outputDir) throws IOException {
		boolean perTaxon = results.perTaxon();
		String header = perTaxon ? "Module\tTaxon\tValue\tCoverage" : "Module\tValue\tCoverage";
		Function<ModuleResult, String> line = perTaxon
				? result -> result.module() + Constants.TAB + result.taxon() + Constants.TAB + result.abundance()
						+ Constants.TAB + result.coverage()
				: result -> result.module() + Constants.TAB + result.abundance() + Constants.TAB + result.coverage();
		for (String sample : results.samples()) {
			// as 1.x: the file is named after the last part of the sample name
			File file = new File(outputDir.toFile(), new File(sample).getName() + ".modules");
			FileUtils.writeObjects(file, header, results.reported(sample), line);
		}
	}
}
