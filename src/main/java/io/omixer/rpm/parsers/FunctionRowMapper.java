package io.omixer.rpm.parsers;

import java.util.function.Function;

import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.Constants;

/**
 * Maps a line of a headerless sample file: KO TAB abundance
 *
 * Stateless, so a single instance can be shared
 */
public class FunctionRowMapper implements Function<InputLine, BasicFeature> {

	private static final String EXPECTED = "KO<TAB>abundance";

	/**
	 * @throws InputFormatException if the line is not KO TAB abundance
	 */
	@Override
	public BasicFeature apply(InputLine line) {
		String[] tokens = line.text().split(Constants.TAB);
		if (tokens.length < 2) {
			throw new InputFormatException(line, EXPECTED, null);
		}
		BasicFeature bf = new BasicFeature();
		bf.setFunction(tokens[0]);
		try {
			bf.setCount(Double.valueOf(tokens[1]));
		} catch (NumberFormatException e) {
			// translate into an error that says which file and line
			throw new InputFormatException(line, EXPECTED, e);
		}
		return bf;
	}
}
