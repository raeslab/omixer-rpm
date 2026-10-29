package io.omixer.rpm.parsers;

import java.util.LinkedList;
import java.util.List;

import io.omixer.rpm.exceptions.InputFormatException;
import io.omixer.rpm.model.BasicFeature;
import io.omixer.utils.readers.FileLine;


/**
 * 
 * id, function, expression0..expressionN
 * 
 * @author <a href="mailto:youssef.darzi@omixer.io">Youssef Darzi</a>
 *
 */
public class FunctionLineProcessor extends AbstractLineProcessor {
	
	protected static final Double ZERO = 0.0;
	
	public FunctionLineProcessor() {
		super(1);
	}

	/**
	 * This is kept to allow subclass calling super(index)
	 * @param headerSampleStartIndex
	 */
	protected FunctionLineProcessor(int headerSampleStartIndex){
		super(headerSampleStartIndex);
	}
	
	protected BasicFeature makeBasicFeature(String[] tokens, Double count) {
		return new BasicFeature(null, tokens[0], count);
	}

	/**
	 * @return the expected line format, for error messages
	 */
	protected String expectedFormat() {
		return "KO<TAB>one abundance per sample";
	}

	/**
	 * process the line of format: id, functional annotation, values
	 *
	 * @throws InputFormatException if a value is not a number
	 */
	@Override
	public List<BasicFeature> process(FileLine line, String delimiter) {

		final List<BasicFeature> features = new LinkedList<BasicFeature>();

		final String[] tokens = line.text().split(delimiter);

		for (int i = getHeaderSampleStartIndex(); i < tokens.length; i++) {
			// check not empty nor equals ZERO
			if (!tokens[i].isEmpty()) {
				Double value;
				try {
					value = Double.valueOf(tokens[i]);
				} catch (NumberFormatException e) {
					// translate into an error that says which file and line
					throw new InputFormatException(line, expectedFormat(), e);
				}
				//if (value.compareTo(ZERO) != 0) { could be used to save memory
					features.add(makeBasicFeature(tokens, value));
				//}
			}
		}
		return features;
	}
}
