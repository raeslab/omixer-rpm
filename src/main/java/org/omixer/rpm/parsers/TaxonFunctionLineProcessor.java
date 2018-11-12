package org.omixer.rpm.parsers;

import org.omixer.rpm.model.BasicFeature;

/**
 * taxonomy, function, expression0..expressionN
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 *
 */
public class TaxonFunctionLineProcessor extends FunctionLineProcessor {
		
	public TaxonFunctionLineProcessor() {
		super(2);
	}
	
	@Override
	protected BasicFeature makeBasicFeature(String[] tokens, Double count) {
		return new BasicFeature(tokens[0], tokens[1], count);
	}	
}

