package io.omixer.rpm.io;

/**
 * The output files (-e).
 */
public enum OutputFormat {

	/** One file per sample, sample.modules, with the reported modules (-e 1) */
	PER_SAMPLE,

	/** An abundance and a coverage matrix, modules.tsv and modules-coverage.tsv (-e 2) */
	MATRIX
}
