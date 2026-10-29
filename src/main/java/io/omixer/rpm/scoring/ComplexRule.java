package io.omixer.rpm.scoring;

import java.util.Optional;

/**
 * How an alternative made of a complex (several subunits, written K1,K2 in the module database) counts when only
 * some of its subunits are observed. An alternative with a single ortholog is observed or not, whatever the rule.
 *
 * Stateless: one place decides, for every score estimator, whether an alternative is observed. An observed step
 * earns coverage and contributes abundance; any other step does neither.
 */
public enum ComplexRule {

	/** Observed when at least one subunit is: a missing subunit is more often unsampled than absent (default) */
	ANY {
		@Override
		public boolean isObserved(int presentSubunits, int subunits) {
			return presentSubunits > 0;
		}
	},

	/** Observed only when every subunit is; otherwise no coverage and no abundance, like an unobserved step */
	ALL {
		@Override
		public boolean isObserved(int presentSubunits, int subunits) {
			return presentSubunits == subunits;
		}
	};

	/**
	 * @param presentSubunits the subunits of the alternative with a value > 0
	 * @param subunits        all subunits of the alternative (1 for a single ortholog)
	 * @return true if the alternative earns coverage and contributes abundance
	 */
	public abstract boolean isObserved(int presentSubunits, int subunits);

	/**
	 * @return the name used on the command line, e.g. any
	 */
	public String cliName() {
		return name().toLowerCase();
	}

	/**
	 * @param name a command line name, e.g. any
	 * @return the matching rule, or empty if there is none
	 */
	public static Optional<ComplexRule> fromName(String name) {
		for (ComplexRule rule : values()) {
			if (rule.cliName().equals(name)) {
				return Optional.of(rule);
			}
		}
		return Optional.empty();
	}
}
