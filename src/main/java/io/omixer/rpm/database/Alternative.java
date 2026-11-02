package io.omixer.rpm.database;

import java.util.List;

/**
 * One way to carry out a step (TAB-separated in the module database): a single ortholog, or several orthologs that
 * together form a complex, its subunits (comma-separated). A step is covered when one of its alternatives is observed;
 * a complex is observed according to {@code --complex-rule}.
 *
 * @param orthologs the KO ids, in database order: one, or the subunits of a complex
 */
public record Alternative(List<String> orthologs) {

	public Alternative {
		orthologs = List.copyOf(orthologs);
		if (orthologs.isEmpty()) {
			throw new IllegalArgumentException("An alternative needs at least one ortholog");
		}
	}
}
