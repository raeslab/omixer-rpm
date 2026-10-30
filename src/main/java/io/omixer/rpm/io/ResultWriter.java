package io.omixer.rpm.io;

import java.io.IOException;
import java.nio.file.Path;

import io.omixer.rpm.engine.Results;

/**
 * Writes results into an output directory.
 */
public interface ResultWriter {

	void write(Results results, Path outputDir) throws IOException;
}
