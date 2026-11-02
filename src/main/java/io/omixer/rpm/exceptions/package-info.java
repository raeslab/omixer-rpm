/**
 * The errors Omixer-RPM reports about its inputs and options: unchecked subclasses of
 * {@link io.omixer.rpm.exceptions.OmixerRpmException}, thrown where the problem is found and caught only by the client
 * ({@link io.omixer.rpm.cli.OmixerRpmCommand}), which logs the message. Real I/O failures stay IOExceptions.
 */
package io.omixer.rpm.exceptions;
