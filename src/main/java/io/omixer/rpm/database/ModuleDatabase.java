package io.omixer.rpm.database;

import java.util.List;
import java.util.stream.Stream;

/**
 * The reference modules, in database order (the order of the output rows).
 *
 * @param modules in database order
 */
public record ModuleDatabase(List<ModuleDefinition> modules) {

	public ModuleDatabase {
		modules = List.copyOf(modules);
	}

	/** @return every function id of every module (the subunits of every alternative), in database order, with repeats */
	public Stream<String> functions() {
		return modules.stream().flatMap(module -> module.steps().stream()).flatMap(step -> step.alternatives().stream())
				.flatMap(alternative -> alternative.subunits().stream());
	}
}
