/**
 * The command line client: parses and checks the options, builds a {@link io.omixer.rpm.pipeline.Request}, runs the
 * {@link io.omixer.rpm.pipeline.Pipeline} and turns errors into a logged message and an exit status. The only place
 * where errors are caught.
 */
package io.omixer.rpm.cli;
