package io.omixer.rpm.inference.scoring;

/**
 * The score of one module in one profile.
 *
 * @param abundance as computed by the {@link Estimator}
 * @param coverage  observed steps ÷ steps
 */
public record ModuleScore(double abundance, double coverage) {
}
