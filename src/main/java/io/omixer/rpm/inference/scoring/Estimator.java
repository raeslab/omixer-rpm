package io.omixer.rpm.inference.scoring;

/**
 * How the values of a module's observed steps combine into its abundance (-s). Every estimator works on step values,
 * never on single KOs: a step's value is the mean of its subunits, zeros included (min: its smallest present
 * subunit).
 */
public sealed interface Estimator {

	/** The median of the observed steps; with -n unobserved steps count as 0, and a 0 median becomes the smallest positive value */
	record Median() implements Estimator {
	}

	/** The sum of the observed steps ÷ the observed steps (÷ the module length with -n) */
	record Average() implements Estimator {
	}

	/** The sum of the observed steps (÷ the module length with -n) */
	record Sum() implements Estimator {
	}

	/** The smallest present KO of the observed steps; -n does not apply */
	record Min() implements Estimator {
	}
}
