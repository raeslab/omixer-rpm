package io.omixer.rpm.exceptions;

/**
 * Thrown when a module database file does not follow the expected format
 */
public class ModuleFileFormatException extends OmixerRpmException {

	private static final long serialVersionUID = 1L;

	public ModuleFileFormatException(String message) {
		super(message);
	}
}
