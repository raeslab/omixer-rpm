package io.omixer.rpm.profile.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.profile.ProfileSink;
import io.omixer.utils.Constants;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;
import io.omixer.utils.readers.FileLine;
import io.omixer.utils.readers.MatrixRowHandler;
import io.omixer.utils.utils.FileUtils;

/**
 * Reads a matrix: a header with one column per sample, then one row per function (and taxon), in an
 * {@link InputFormat}. Empty cells are skipped and the values are given to the samples in order, as in 1.x: a row must
 * still have one value per sample.
 */
public final class MatrixReader implements ProfileReader {

	private final InputFormat format;

	public MatrixReader(InputFormat format) {
		this.format = format;
	}

	@Override
	public void read(Path input, ProfileSink sink) throws IOException, IncorrectNumberOfEntriesException {
		FileUtils.readMatrixRows(input.toFile(), Constants.TAB, format.keyColumns(), new MatrixRowHandler() {

			private List<String> samples;

			@Override
			public void columns(List<String> columns) {
				samples = columns;
				samples.forEach(sink::sample);
			}

			/**
			 * @throws InputFormatException if a value is not a number
			 */
			@Override
			public int row(FileLine line, String[] fields) {
				if (fields.length <= format.keyColumns()) {
					return 0;
				}
				String taxon = format.taxon(fields);
				String function = format.function(fields);
				// the values, in sample order; empty cells are skipped
				int values = 0;
				for (int i = format.keyColumns(); i < fields.length; i++) {
					if (!fields[i].isEmpty()) {
						double value;
						try {
							value = Double.parseDouble(fields[i]);
						} catch (NumberFormatException e) {
							// translate into an error that says which file and line
							throw new InputFormatException(line, format.matrixRow(), e);
						}
						// a row with more values than samples is rejected by the matrix reader
						if (values < samples.size()) {
							sink.add(samples.get(values), taxon, function, value);
						}
						values++;
					}
				}
				return values;
			}
		});
	}
}
