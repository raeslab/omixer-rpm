package io.omixer.rpm.database;

import java.util.List;

/**
 * One way to carry out a step: a single ortholog, or the subunits of a complex that are all needed (comma-separated
 * in the module database).
 *
 * @param subunits the KO ids, in database order; at least one
 */
public record Alternative(List<String> subunits) {

	public Alternative {
		subunits = List.copyOf(subunits);
		if (subunits.isEmpty()) {
			throw new IllegalArgumentException("An alternative needs at least one subunit");
		}
	}
}
