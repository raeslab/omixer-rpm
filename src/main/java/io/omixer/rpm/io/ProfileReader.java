package io.omixer.rpm.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.omixer.rpm.engine.ProfileSink;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * Reads an input and feeds every sample and value to a sink, in input order.
 */
public interface ProfileReader {

	/**
	 * @param input  a directory with one file per sample, or a matrix
	 * @param format the layout of the input rows (-a)
	 * @return the reader of the input
	 */
	static ProfileReader forInput(Path input, InputFormat format) {
		return Files.isDirectory(input) ? new DirectoryReader(format) : new MatrixReader(format);
	}

	/**
	 * @throws io.omixer.rpm.exceptions.OmixerRpmException when the input does not follow its format, naming the file
	 *                                                      and line
	 * @throws IncorrectNumberOfEntriesException            when a matrix row has another number of values than samples
	 * @throws IOException                                  when the input cannot be read
	 */
	void read(Path input, ProfileSink sink) throws IOException, IncorrectNumberOfEntriesException;
}
