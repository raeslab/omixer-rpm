package io.omixer.rpm.exceptions;

import io.omixer.utils.readers.FileLine;

/**
 * Thrown when a line of an input file does not follow the expected format
 */
public class InputFormatException extends OmixerRpmException {

	private static final long serialVersionUID = 1L;

	/** A matrix line can hold thousands of samples: longer lines are shortened in the message */
	private static final int MAX_LINE_LENGTH = 80;

	/**
	 * @param line the offending line, with the file and line number it comes from
	 * @param expected the expected line format, e.g. KO&lt;TAB&gt;abundance
	 * @param cause the parsing error, if any
	 */
	public InputFormatException(FileLine line, String expected, Throwable cause) {
		super(line.file() + ", line " + line.number() + ": '" + shorten(line.text()) + "' is not " + expected, cause);
	}

	private static String shorten(String text) {
		return text.length() <= MAX_LINE_LENGTH ? text : text.substring(0, MAX_LINE_LENGTH) + "...";
	}
}
