package io.omixer.rpm.inference;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The coverage cutoff of a run.
 *
 * @param value       the minimum coverage of a reported module; empty when none could be learned (no module observed),
 *                    in which case nothing is reported
 * @param learnedFrom the distribution an automatic cutoff was learned from; empty for a given cutoff
 */
public record Cutoff(OptionalDouble value, Optional<CoverageDistribution> learnedFrom) {

	public Cutoff {
		Objects.requireNonNull(value, "value");
		Objects.requireNonNull(learnedFrom, "learnedFrom");
	}
}
