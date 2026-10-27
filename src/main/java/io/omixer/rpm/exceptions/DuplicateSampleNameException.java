package io.omixer.rpm.exceptions;

/**
 * Thrown when two input files would give the same sample name, so that one would overwrite the other
 */
public class DuplicateSampleNameException extends OmixerRpmException {

	private static final long serialVersionUID = 1L;

	public DuplicateSampleNameException(String message) {
		super(message);
	}
}
