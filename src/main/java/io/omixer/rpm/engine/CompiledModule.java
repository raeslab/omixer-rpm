package io.omixer.rpm.engine;

/**
 * A module of a {@link CompiledDatabase}: its steps, alternatives and subunits as KO indexes of that database.
 * Immutable; the arrays never leave this class.
 */
public final class CompiledModule {

	private final String id;
	/** step → alternative → subunit → KO index */
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

	public int subunitCount(int step, int alternative) {
		return steps[step][alternative].length;
	}

	/** @return the KO index of a subunit */
	public int subunit(int step, int alternative, int subunit) {
		return steps[step][alternative][subunit];
	}
}
