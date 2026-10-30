package io.omixer.rpm.cutoff;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The minimum coverage of a reported module (-c).
 */
public sealed interface CoverageCutoff {

	/**
	 * @param observedCoverages the coverage of every observed module (coverage &gt; 0) in every profile, in any order
	 * @return the cutoff of the run
	 */
	Cutoff resolve(double[] observedCoverages);

	/** A given cutoff between 0 and 1 */
	record Fixed(double value) implements CoverageCutoff {

		public Fixed {
			// the negated check also rejects NaN
			if (!(value >= 0d && value <= 1d)) {
				throw new IllegalArgumentException("A coverage cutoff is between 0 and 1, not " + value);
			}
		}

		@Override
		public Cutoff resolve(double[] observedCoverages) {
			return new Cutoff(OptionalDouble.of(value), Optional.empty());
		}
	}

	/** Learned from the coverage of every module in every profile (-c -1): the knee of their distribution */
	record Automatic() implements CoverageCutoff {

		/** @return the knee, or no cutoff when no module was observed */
		@Override
		public Cutoff resolve(double[] observedCoverages) {
			CoverageDistribution distribution = new CoverageDistribution(observedCoverages);
			return distribution.knee().isPresent() ? new Cutoff(distribution.knee(), Optional.of(distribution))
					: new Cutoff(OptionalDouble.empty(), Optional.empty());
		}
	}
}
