package io.omixer.rpm.exceptions;

import io.omixer.rpm.parsers.InputLine;

/**
 * Thrown when a line of a sample file does not follow the expected format
 */
public class InputFormatException extends OmixerRpmException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param line the offending line, with the file and line number it comes from
	 * @param expected the expected line format, e.g. KO&lt;TAB&gt;abundance
	 * @param cause the parsing error, if any
	 */
	public InputFormatException(InputLine line, String expected, Throwable cause) {
		super(line.source() + ", line " + line.number() + ": '" + line.text() + "' is not " + expected, cause);
	}
}
