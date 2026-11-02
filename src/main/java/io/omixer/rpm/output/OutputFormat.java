package io.omixer.rpm.output;

import java.util.function.Supplier;

/**
 * The output files (-e).
 */
public enum OutputFormat {

	/** One file per sample, sample.modules, with the reported modules (-e 1) */
	PER_SAMPLE(PerSampleWriter::new),

	/** An abundance and a coverage matrix, modules.tsv and modules-coverage.tsv (-e 2) */
	MATRIX(MatrixWriter::new);

	private final Supplier<ResultWriter> writer;

	OutputFormat(Supplier<ResultWriter> writer) {
		this.writer = writer;
	}

	/** @return the writer of these files */
	public ResultWriter writer() {
		return writer.get();
	}
}
