package io.omixer.rpm.model.enums;

import java.util.List;
import java.util.Optional;

import io.omixer.rpm.model.Ortholog;

/**
 * How a step made of a complex (several subunits, written K1,K2 in the module database) counts when only some of
 * its subunits are observed. A step with a single ortholog is observed or not, whatever the rule.
 *
 * Stateless: one place decides, for every score estimator, whether a step is observed (contributes abundance) and
 * how much coverage it earns.
 */
public enum ComplexRule {

	/** Observed when at least one subunit is: a missing subunit is more often unsampled than absent (default) */
	ANY {
		@Override
		public boolean isObserved(List<Ortholog> step) {
			return presentSubunits(step) > 0;
		}

		@Override
		public double coverage(List<Ortholog> step) {
			return isObserved(step) ? 1 : 0;
		}
	},

	/** Observed only when every subunit is; otherwise no coverage and no abundance, like an unobserved step */
	ALL {
		@Override
		public boolean isObserved(List<Ortholog> step) {
			return presentSubunits(step) == step.size();
		}

		@Override
		public double coverage(List<Ortholog> step) {
			return isObserved(step) ? 1 : 0;
		}
	},

	/** Observed like ANY, but earns coverage in proportion to its observed subunits */
	FRACTION {
		@Override
		public boolean isObserved(List<Ortholog> step) {
			return presentSubunits(step) > 0;
		}

		@Override
		public double coverage(List<Ortholog> step) {
			return (double) presentSubunits(step) / step.size();
		}
	};

	/**
	 * @param step the orthologs of one alternative of a step (several for a complex)
	 * @return true if the step contributes abundance
	 */
	public abstract boolean isObserved(List<Ortholog> step);

	/**
	 * @param step the orthologs of one alternative of a step (several for a complex)
	 * @return the coverage the step earns, between 0 and 1
	 */
	public abstract double coverage(List<Ortholog> step);

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

	private static long presentSubunits(List<Ortholog> step) {
		return step.stream().filter(ortholog -> ortholog.getCount() > 0).count();
	}
}
