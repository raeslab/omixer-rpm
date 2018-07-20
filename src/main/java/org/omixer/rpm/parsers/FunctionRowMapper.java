package org.omixer.rpm.parsers;

import java.util.function.Function;

import org.omixer.rpm.model.BasicFeature;
import org.omixer.utils.Constants;

public class FunctionRowMapper implements Function<String, BasicFeature> {
	public BasicFeature apply(String input) {
		String[] tokens = input.split(Constants.TAB);
		BasicFeature bf = new BasicFeature();
		bf.setFunction(tokens[0]);
		bf.setCount(Double.valueOf(tokens[1]));
		return bf;
	}
}
