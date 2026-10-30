package io.omixer.rpm.output;

import java.io.IOException;
import java.nio.file.Path;

import io.omixer.rpm.inference.Results;

/**
 * Writes results into an output directory.
 */
public interface ResultWriter {

	void write(Results results, Path outputDir) throws IOException;
}
