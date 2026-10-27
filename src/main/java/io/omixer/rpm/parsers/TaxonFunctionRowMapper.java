package io.omixer.rpm.parsers;

import java.util.function.Function;

import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.Constants;

public class TaxonFunctionRowMapper implements Function<String, BasicFeature> {
	
	public BasicFeature apply(String input) {

		String[] tokens = input.split(Constants.TAB);
		return new BasicFeature(tokens[0], tokens[1], Double.valueOf(tokens[2]));
	}
}
