package io.omixer.rpm.inference.scoring;

import java.util.Objects;

/**
 * How a module is scored in a profile, the same for every module and profile.
 *
 * @param estimator         how step values combine into the abundance (-s)
 * @param complexRule       when a complex counts as observed (--complex-rule)
 * @param normalizeByLength whether unobserved steps count in the abundance (-n)
 */
public record ScoringOptions(Estimator estimator, ComplexRule complexRule, boolean normalizeByLength) {

	public ScoringOptions {
		Objects.requireNonNull(estimator, "estimator");
		Objects.requireNonNull(complexRule, "complexRule");
	}
}
