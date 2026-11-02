/**
 * The module database: immutable records as written in the file ({@link io.omixer.rpm.database.ModuleDefinition},
 * {@link io.omixer.rpm.database.Step}, {@link io.omixer.rpm.database.Alternative}), read by
 * {@link io.omixer.rpm.database.ModuleDatabaseReader}, then compiled once into
 * {@link io.omixer.rpm.database.CompiledDatabase}: orthologs as int indexes, which every profile and the scorer use.
 *
 * A module is a list of steps (reactions), all needed; a step is a list of alternatives, one is enough; an alternative
 * is one ortholog, or the orthologs of a complex (its subunits).
 */
package io.omixer.rpm.database;
