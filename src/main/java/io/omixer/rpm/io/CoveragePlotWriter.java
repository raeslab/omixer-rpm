package io.omixer.rpm.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.omixer.rpm.cutoff.CoverageDistribution;
import io.omixer.rpm.engine.Results;

/**
 * Writes coverage-plot.svg, the coverage distribution the cutoff was learned from; nothing for a given cutoff.
 */
public final class CoveragePlotWriter implements ResultWriter {

	public static final String PLOT_FILE = "coverage-plot.svg";

	@Override
	public void write(Results results, Path outputDir) throws IOException {
		if (results.coverageDistribution().isPresent()) {
			CoverageDistribution distribution = results.coverageDistribution().get();
			Files.write(outputDir.resolve(PLOT_FILE), distribution.toSVG().getBytes());
		}
	}
}
