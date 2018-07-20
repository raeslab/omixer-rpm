package org.omixer.rpm.parsers;

import java.util.LinkedList;
import java.util.List;

import org.omixer.rpm.model.BasicFeature;


/**
 * 
 * id, function, expression0..expressionN
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
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
		return new BasicFeature(tokens[0], null, tokens[0], count);
	}
	
	/**
	 * process the line of format: id, functional annotation, values
	 */
	@Override
	public List<BasicFeature> process(String line, String delimiter) {

		final List<BasicFeature> features = new LinkedList<BasicFeature>();

		final String[] tokens = line.split(delimiter);

		for (int i = getHeaderSampleStartIndex(); i < tokens.length; i++) {
			// check not empty nor equals ZERO
			if (!tokens[i].isEmpty()) {
				Double value = Double.valueOf(tokens[i]);
				//if (value.compareTo(ZERO) != 0) { could be used to save memory
					features.add(makeBasicFeature(tokens, value));	
				//}
			}
		}
		return features;
	}
}
