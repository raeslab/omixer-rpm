package org.omixer.rpm.parsers;

import org.omixer.rpm.model.BasicFeature;

/**
 * taxonomy, function, expression0..expressionN
 * 
 * 10776.Healthy	10850.Healthy	10543.Healthy	10246.Healthy	10156.Gastrectomy	10684.Healthy	10463.Healthy	10100.Healthy	10342.Gastrectomy
K00001	5.603	12.572	177.355	0.0	164.01	515.986	373.176	0.0	318.552
K00001|g__Acidaminococcus.s__Acidaminococcus_fermentans	0.0	12.572	0.0	0.0	0.0	0.0	0.0	0.0	0.0
K00001|g__Akkermansia.s__Akkermansia_muciniphila	0.0	0.0	17.695	0.0	0.0	33.505	1.976	0.0	0.0
K00001|g__Eggerthella.s__Eggerthella_lenta	0.0	0.0	7.67	0.0	0.0	0.0	5.746	0.0	0.0
K00001|g__Enterobacter.s__Enterobacter_cloacae	0.0	0.0	0.0	0.0	0.0	0.0	18.497	0.0	0.0

 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">omixer</a>
 *
 */
public class TaxonFunctionHumannLineProcessor extends FunctionLineProcessor {
		
	public TaxonFunctionHumannLineProcessor() {
		super(1);
	}
	
	@Override
	protected BasicFeature makeBasicFeature(String[] tokens, Double count) {
		// Split on |
		String[] koTaxon = tokens[0].split("\\|");
		// Create a feature without taxonomic annotation 
		BasicFeature feature = new BasicFeature("NA", koTaxon[0], count);
		// if length is 2 then a taxon was included
		if (koTaxon.length == 2) {
			// update taxon value
			feature.setTaxon(koTaxon[1]);
		}
		// return the feature
		return feature;
	}
}

