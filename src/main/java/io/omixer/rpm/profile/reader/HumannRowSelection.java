package io.omixer.rpm.profile.reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import io.omixer.rpm.profile.ProfileSink;
import io.omixer.utils.Constants;

/**
 * The rows of a HUMAnN table that a run uses, passed on to the profiles; {@link HumannReader} gives every row.
 *
 * Totals and strata are never added together: per taxon, only the strata are used; per sample, only the totals, or
 * the strata of a table that has no totals (humann_split_stratified_table). A table is read per taxon only if it is
 * {@link #stratified(Path)} ({@link ProfileReader#perTaxon}). Special rows (UNMAPPED, UNGROUPED, …) are not functions
 * and are never used.
 */
public final class HumannRowSelection implements ProfileSink {

	/** Rows that hold no function: unmapped reads, and abundance that maps to no group or pathway */
	private static final Set<String> SPECIAL_ROWS = Set.of("UNMAPPED", "READS_UNMAPPED", "UNGROUPED", "UNINTEGRATED");

	private final ProfileSink profiles;
	/** true if the strata are used, false for the totals */
	private final boolean strata;

	private HumannRowSelection(ProfileSink profiles, boolean strata) {
		this.profiles = profiles;
		this.strata = strata;
	}

	/**
	 * @param table    the HUMAnN table that will be read
	 * @param perTaxon true for one profile per taxon of a sample, false for one per sample
	 * @param profiles where the used rows go
	 * @return the selection of the table's rows for the run
	 * @throws io.omixer.rpm.exceptions.InputFormatException if the input is a directory
	 * @throws IOException                                   when the table cannot be read
	 */
	public static HumannRowSelection of(Path table, boolean perTaxon, ProfileSink profiles) throws IOException {
		Rows rows = Rows.of(HumannReader.table(table));
		// per sample, the totals when the table has them (or has no row at all), otherwise the strata
		return new HumannRowSelection(profiles, perTaxon || !rows.totals() && rows.strata());
	}

	/**
	 * Whether a table has taxa: none when it has only community totals (humann --remove-stratified-output, or the
	 * unstratified half of humann_split_stratified_table)
	 *
	 * @param table a HUMAnN table
	 * @return false if the table has community totals and no stratified rows, true otherwise
	 * @throws io.omixer.rpm.exceptions.InputFormatException if the input is a directory
	 * @throws IOException                                   when the table cannot be read
	 */
	public static boolean stratified(Path table) throws IOException {
		Rows rows = Rows.of(HumannReader.table(table));
		return !rows.totals() || rows.strata();
	}

	@Override
	public void sample(String sample) {
		profiles.sample(sample);
	}

	@Override
	public void add(String sample, String taxon, String function, double value) {
		if (!SPECIAL_ROWS.contains(function) && (taxon != null) == strata) {
			profiles.add(sample, taxon, function, value);
		}
	}

	/**
	 * Which kinds of function rows a table has, found before reading it
	 *
	 * @param totals true if it has unstratified rows (community totals)
	 * @param strata true if it has stratified rows (function|taxon)
	 */
	private record Rows(boolean totals, boolean strata) {

		/** Reads the first column of each row, up to the first row of each kind */
		static Rows of(Path table) throws IOException {
			boolean totals = false;
			boolean strata = false;
			try (BufferedReader reader = new BufferedReader(new FileReader(table.toFile()))) {
				// the header is not a row
				String text = reader.readLine();
				while (!(totals && strata) && (text = reader.readLine()) != null) {
					String[] key = { text.split(Constants.TAB, 2)[0] };
					if (!SPECIAL_ROWS.contains(InputFormat.HUMANN.function(key))) {
						boolean stratified = InputFormat.HUMANN.taxon(key) != null;
						totals |= !stratified;
						strata |= stratified;
					}
				}
			}
			return new Rows(totals, strata);
		}
	}
}
