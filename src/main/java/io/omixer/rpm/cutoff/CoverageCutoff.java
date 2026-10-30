package io.omixer.rpm.cutoff;

/**
 * The minimum coverage of a reported module (-c).
 */
public sealed interface CoverageCutoff {

	/** A given cutoff between 0 and 1 */
	record Fixed(double value) implements CoverageCutoff {
	}

	/** Learned from the coverage of every module in every profile (-c -1): see {@link CoverageDistribution} */
	record Automatic() implements CoverageCutoff {
	}
}
