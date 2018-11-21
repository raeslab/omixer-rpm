package org.omixer.rpm.parsers;

import java.io.File;
import java.util.List;

import org.omixer.rpm.model.BasicFeature
import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.parsers.ModuleParser;
import org.omixer.utils.Constants
import org.omixer.utils.utils.FileUtils

import spock.lang.Specification

public class HumannKOSepeciesMatrixParserSpec extends Specification {

	def "taxonFunctionHumannLineProcessor parses entries correctly"() {

		setup: "Setup a Humann2 processor"
		TaxonFunctionHumannLineProcessor processor = new TaxonFunctionHumannLineProcessor()

		/**
		 * Expect species => KO|UNGROUPED, unclassified => KO|UNGROUPED, UNMAPPED
		 */
		when: "An entry is processed"
		List<BasicFeature> res = processor.process(entry, Constants.TAB)

		then: "KO Id, taxon, and abundances are parsed correctly"
		res.size() == size
		res.every{ it.getFunction() == ko }
		res.every{ it.getTaxon() == taxon }

		where:
		entry | ko | taxon | size
		"K00001|g__Acidaminococcus.s__Acidaminococcus_fermentans	0.0	12.572	0.0	0.0" | "K00001" | "g__Acidaminococcus.s__Acidaminococcus_fermentans" | 4
		"K19611|g__Enterobacter.s__Enterobacter_aerogenes	10.0	0.0	0.0	0.0" | "K19611" | "g__Enterobacter.s__Enterobacter_aerogenes" | 4
		"K19611|unclassified	10.0	0.0	0.0	0.0" | "K19611" | "unclassified" | 4
		"K19611	15.0	10.0	0.0	0.0" | "K19611" | "NA" | 4
		"UNGROUPED	518.5	3551	292.839	63.56" | "UNGROUPED" | "NA" | 4
		"UNGROUPED|g__Abiotrophia.s__Abiotrophia_defectiva	518.5	3551	292.839	63.56" | "UNGROUPED" | "g__Abiotrophia.s__Abiotrophia_defectiva" | 4
		"UNMAPPED	871.0	117.0	1235.0	121.0" | "UNMAPPED" | "NA" | 4
	}

	def "taxonFunctionHumannLineProcessor parses flat file correctly"() {
		
		setup: "Setup a Humann2 processor"
		def mlp = new TaxonFunctionHumannLineProcessor()
		
		when: "A matrix is processed"
		Map<String, List<BasicFeature>> matrix = FileUtils.readMatrix(new File("src/test/resources/humann2KoTaxa.tsv"), Constants.TAB, mlp);
		
		then: "9 Samples, 4 function (2 KOs, UNAMPPED, UNGROUPED) and 20 taxa (18, NA, unclassified) are returned"
		matrix.size() == 9
		matrix["10776.Healthy"].groupBy{it.getFunction()}.size() == 4
		matrix["10776.Healthy"].groupBy{it.getTaxon()}.size() == 20
	}
}
