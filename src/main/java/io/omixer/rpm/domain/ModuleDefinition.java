package io.omixer.rpm.domain;

import java.util.List;
import java.util.Objects;

/**
 * A module of the reference database: its steps, in pathway order. Immutable, so one definition serves every sample.
 *
 * @param id    the module id, e.g. MF0001
 * @param name  the module name
 * @param steps in pathway order; at least one
 */
public record ModuleDefinition(String id, String name, List<Step> steps) {

	public ModuleDefinition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
		steps = List.copyOf(steps);
		if (steps.isEmpty()) {
			throw new IllegalArgumentException("Module " + id + " needs at least one step");
		}
	}
}
