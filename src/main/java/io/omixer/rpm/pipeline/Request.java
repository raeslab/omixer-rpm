package io.omixer.rpm.pipeline;

import java.nio.file.Path;
import java.util.Objects;

import io.omixer.rpm.inference.ModuleInference;
import io.omixer.rpm.output.OutputFormat;
import io.omixer.rpm.profile.reader.InputFormat;

/**
 * Everything a run needs.
 *
 * @param input     a matrix, or a directory with one file per sample
 * @param format    the layout of the input rows (-a)
 * @param perTaxon  true for results per taxon of a sample, false for one result per sample
 * @param database  the module database
 * @param inference how modules are inferred
 * @param output    the output files (-e)
 * @param outputDir created when missing
 */
public record Request(Path input, InputFormat format, boolean perTaxon, Path database, ModuleInference inference,
		OutputFormat output, Path outputDir) {

	public Request {
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(format, "format");
		Objects.requireNonNull(database, "database");
		Objects.requireNonNull(inference, "inference");
		Objects.requireNonNull(output, "output");
		Objects.requireNonNull(outputDir, "outputDir");
	}
}
