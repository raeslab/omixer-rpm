package io.omixer.rpm.domain;

import java.util.List;

/**
 * One reaction of a module: covered when one of its alternatives (TAB-separated in the module database) is observed.
 *
 * @param alternatives in database order; at least one
 */
public record Step(List<Alternative> alternatives) {

	public Step {
		alternatives = List.copyOf(alternatives);
		if (alternatives.isEmpty()) {
			throw new IllegalArgumentException("A step needs at least one alternative");
		}
	}
}
