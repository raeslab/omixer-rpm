package io.omixer.rpm.parsers;

import java.util.function.Function;

import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.Constants;

/**
 * Maps a line of a headerless sample file: taxon TAB KO TAB abundance
 *
 * Stateless, so a single instance can be shared
 */
public class TaxonFunctionRowMapper implements Function<InputLine, BasicFeature> {

	private static final String EXPECTED = "taxon<TAB>KO<TAB>abundance";

	/**
	 * @throws InputFormatException if the line is not taxon TAB KO TAB abundance
	 */
	@Override
	public BasicFeature apply(InputLine line) {
		String[] tokens = line.text().split(Constants.TAB);
		if (tokens.length < 3) {
			throw new InputFormatException(line, EXPECTED, null);
		}
		try {
			return new BasicFeature(tokens[0], tokens[1], Double.valueOf(tokens[2]));
		} catch (NumberFormatException e) {
			// translate into an error that says which file and line
			throw new InputFormatException(line, EXPECTED, e);
		}
	}
}
