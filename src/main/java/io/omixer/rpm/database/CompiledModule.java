package io.omixer.rpm.database;

/**
 * A module of a {@link CompiledDatabase}: its steps, their alternatives and the orthologs of each alternative (one, or
 * the subunits of a complex), as ortholog indexes of that database. Immutable; the arrays never leave this class.
 */
public final class CompiledModule {

	private final String id;
	/** step → alternative → ortholog → ortholog index */
	private final int[][][] steps;

	CompiledModule(String id, int[][][] steps) {
		this.id = id;
		this.steps = steps;
	}

	public String id() {
		return id;
	}

	public int stepCount() {
		return steps.length;
	}

	public int alternativeCount(int step) {
		return steps[step].length;
	}

	/** @return the number of orthologs of an alternative: 1, or the subunits of a complex */
	public int orthologCount(int step, int alternative) {
		return steps[step][alternative].length;
	}

	/** @return the index of an ortholog of an alternative, in the values of a profile */
	public int ortholog(int step, int alternative, int ortholog) {
		return steps[step][alternative][ortholog];
	}
}
