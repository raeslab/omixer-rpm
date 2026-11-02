package io.omixer.rpm.profile.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.omixer.rpm.profile.ProfileSink;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * Reads an input and feeds every sample and value to a sink, in input order.
 */
public interface ProfileReader {

	/**
	 * Whether an input gives profiles per taxon: when they are asked for and the input has taxa. Only HUMAnN input
	 * may have none (a table of community totals only)
	 *
	 * @param input     a directory with one file per sample, or a matrix
	 * @param format    the layout of the input rows (-a)
	 * @param requested true when profiles per taxon are asked for: a taxon format without --ignore-taxonomic-info
	 * @return true for one profile per taxon of a sample
	 * @throws io.omixer.rpm.exceptions.InputFormatException if a HUMAnN input is a directory
	 * @throws IOException                                   when the input cannot be read
	 */
	static boolean perTaxon(Path input, InputFormat format, boolean requested) throws IOException {
		return requested && (format != InputFormat.HUMANN || HumannRowSelection.stratified(input));
	}

	/**
	 * @param input  a directory with one file per sample, or a matrix
	 * @param format the layout of the input rows (-a)
	 * @return the reader of the input
	 */
	static ProfileReader forInput(Path input, InputFormat format) {
		if (format == InputFormat.HUMANN) {
			return new HumannReader();
		}
		return Files.isDirectory(input) ? new DirectoryReader(format) : new MatrixReader(format);
	}

	/**
	 * What the reader of an input feeds: the profiles, or for a HUMAnN table its {@link HumannRowSelection} in front of
	 * them
	 *
	 * @param input    the input that will be read
	 * @param format   the layout of the input rows (-a)
	 * @param perTaxon true for one profile per taxon of a sample ({@link #perTaxon(Path, InputFormat, boolean)})
	 * @param profiles where the profiles are collected
	 * @return the sink for the reader
	 * @throws io.omixer.rpm.exceptions.InputFormatException if a HUMAnN input is a directory
	 * @throws IOException                                   when the input cannot be read
	 */
	static ProfileSink selection(Path input, InputFormat format, boolean perTaxon, ProfileSink profiles)
			throws IOException {
		return format == InputFormat.HUMANN ? HumannRowSelection.of(input, perTaxon, profiles) : profiles;
	}

	/**
	 * @throws io.omixer.rpm.exceptions.OmixerRpmException when the input does not follow its format, naming the file
	 *                                                      and line
	 * @throws IncorrectNumberOfEntriesException            when a matrix row has another number of values than samples
	 * @throws IOException                                  when the input cannot be read
	 */
	void read(Path input, ProfileSink sink) throws IOException, IncorrectNumberOfEntriesException;
}
