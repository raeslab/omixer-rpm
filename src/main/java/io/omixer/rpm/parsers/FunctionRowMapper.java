package io.omixer.rpm.parsers;

import java.util.function.Function;

import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.Constants;

public class FunctionRowMapper implements Function<String, BasicFeature> {
	public BasicFeature apply(String input) {
		String[] tokens = input.split(Constants.TAB);
		BasicFeature bf = new BasicFeature();
		bf.setFunction(tokens[0]);
		bf.setCount(Double.valueOf(tokens[1]));
		return bf;
	}
}
