package io.omixer.rpm.inference;

/**
 * The result of one module in one profile.
 *
 * @param sample    the sample
 * @param taxon     the taxon in per-taxon runs, otherwise null
 * @param module    the module id
 * @param abundance as computed by the score estimator
 * @param coverage  observed steps ÷ steps
 */
public record ModuleResult(String sample, String taxon, String module, double abundance, double coverage) {
}
