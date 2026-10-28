package io.omixer.rpm.exceptions;

/**
 * Base class of the errors Omixer-RPM reports about its inputs and configuration
 *
 * Unchecked: thrown where the problem is detected and handled once, by the client (e.g. the command line app),
 * which reports the message. I/O failures remain {@link java.io.IOException}s.
 */
public class OmixerRpmException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OmixerRpmException(String message) {
		super(message);
	}

	public OmixerRpmException(String message, Throwable cause) {
		super(message, cause);
	}
}
