package io.omixer.rpm.database;

import java.util.List;

/**
 * The reference modules, in database order (the order of the output rows).
 *
 * @param modules in database order
 */
public record ModuleDatabase(List<ModuleDefinition> modules) {

	public ModuleDatabase {
		modules = List.copyOf(modules);
	}
}
