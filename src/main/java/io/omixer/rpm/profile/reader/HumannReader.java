package io.omixer.rpm.profile.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.omixer.rpm.exceptions.DuplicateSampleNameException;
import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.profile.ProfileSink;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * Reads a HUMAnN table (-a humann), e.g. gene families regrouped to KOs with humann_regroup_table and joined with
 * humann_join_tables: a matrix with a header (# Gene Family, then one column per sample), and per function a
 * community total row followed by one row per stratum (function|taxon, unclassified included).
 *
 * Every row is passed on, special rows (UNMAPPED, UNGROUPED, …) included: a total row without a taxon, a stratum with
 * its stratum as taxon, the function without its name. Which rows a run uses is {@link HumannRowSelection}. Sample
 * names are the header's column names without HUMAnN's _Abundance suffix (S1_Abundance-RPKs gives S1).
 */
public final class HumannReader implements ProfileReader {

	/** The suffix HUMAnN gives a sample's column: _Abundance, or _Abundance-<units> */
	private static final Pattern ABUNDANCE_SUFFIX = Pattern.compile("_Abundance(-[A-Za-z]+)?$");

	/**
	 * @throws InputFormatException         if the input is a directory, or a row does not follow the format
	 * @throws DuplicateSampleNameException if two columns give the same sample name
	 */
	@Override
	public void read(Path input, ProfileSink sink) throws IOException, IncorrectNumberOfEntriesException {
		new MatrixReader(InputFormat.HUMANN).read(table(input), new ProfileSink() {

			/** column → sample name */
			private final Map<String, String> names = new HashMap<>();
			/** sample name → the column that gave it */
			private final Map<String, String> columns = new HashMap<>();

			/**
			 * @throws DuplicateSampleNameException if another column gives the same name
			 */
			@Override
			public void sample(String column) {
				String sample = sampleName(column);
				String other = columns.putIfAbsent(sample, column);
				if (other != null) {
					throw new DuplicateSampleNameException(input.getFileName() + ": columns '" + other + "' and '" + column
							+ "' both give sample name '" + sample + "'");
				}
				names.put(column, sample);
				sink.sample(sample);
			}

			@Override
			public void add(String column, String taxon, String function, double value) {
				sink.add(names.get(column), taxon, function, value);
			}
		});
	}

	/**
	 * @return the input, a HUMAnN table
	 * @throws InputFormatException if the input is a directory: HUMAnN tables of several samples are joined first
	 */
	static Path table(Path input) {
		if (Files.isDirectory(input)) {
			throw new InputFormatException(input.getFileName() + " is a directory: -a humann reads one HUMAnN table (join"
					+ " per-sample tables with humann_join_tables)");
		}
		return input;
	}

	/** The sample name of a column: without HUMAnN's _Abundance suffix, unless nothing would be left */
	private static String sampleName(String column) {
		String sample = ABUNDANCE_SUFFIX.matcher(column).replaceFirst("");
		return sample.isEmpty() ? column : sample;
	}
}
