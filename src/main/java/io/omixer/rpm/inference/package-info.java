/**
 * What every inference method shares: the {@link io.omixer.rpm.inference.ModuleInference} contract, its
 * {@link io.omixer.rpm.inference.Results} (abundance and coverage of every module in every profile) and the coverage
 * cutoff that decides which results are reported ({@link io.omixer.rpm.inference.CoverageCutoff} gives a
 * {@link io.omixer.rpm.inference.Cutoff}; an automatic one is the knee of a
 * {@link io.omixer.rpm.inference.CoverageDistribution}).
 */
package io.omixer.rpm.inference;
