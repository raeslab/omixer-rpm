package io.omixer.rpm.output;

import java.io.IOException;
import java.nio.file.Path;

import io.omixer.rpm.inference.Results;

/**
 * Writes results into an output directory.
 */
public interface ResultWriter {

	/**
	 * @param results   the results; writers use the reported ones ({@link Results#reported(String)})
	 * @param outputDir an existing directory; files in it are overwritten
	 */
	void write(Results results, Path outputDir) throws IOException;
}
