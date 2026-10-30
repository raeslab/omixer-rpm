package io.omixer.rpm.io;

import java.io.IOException;
import java.nio.file.Path;

import io.omixer.rpm.engine.ProfileSink;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * Reads an input and feeds every sample and value to a sink, in input order.
 */
public interface ProfileReader {

	/**
	 * @throws io.omixer.rpm.exceptions.OmixerRpmException when the input does not follow its format, naming the file
	 *                                                      and line
	 * @throws IncorrectNumberOfEntriesException            when a matrix row has another number of values than samples
	 * @throws IOException                                  when the input cannot be read
	 */
	void read(Path input, ProfileSink sink) throws IOException, IncorrectNumberOfEntriesException;
}
