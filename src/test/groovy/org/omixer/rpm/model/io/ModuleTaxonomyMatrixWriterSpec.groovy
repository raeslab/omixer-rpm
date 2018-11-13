package org.omixer.rpm.model.io

import java.io.File
import java.io.IOException
import java.util.List
import java.util.Map
import java.util.Set

import org.omixer.rpm.model.Module
import org.omixer.rpm.model.Modules
import org.omixer.rpm.model.io.ModuleTaxonomyMatrixWriter

import spock.lang.Specification

public class ModuleTaxonomyMatrixWriterSpec extends Specification {

	def "writeCounts outputs a Matrix with a header containing taxon name, module id, followed by values"() {
		setup: "create a modules set for 2 samples"
		Map<String, Modules> moduleInference = [:]
		/* create 3 modules and 2 samples
		 * Taxon	Module	S1	S2
		 * Sp1	M1	1	1
		 * Sp2	M1	0	1
		 * Sp2	M2	1	0
		 * Sp2	M3	1	0
		 * 
		 */
		List<Module> s1Modules = [
				new Module("M1", "Sp1", 0.7, 1, null),
				new Module("M1", "Sp2", 0.3, 1, null),
				new Module("M2", "Sp2", 0.7, 1, null),
				new Module("M3", "Sp2", 0.7, 1, null)]
		
		List<Module> s2Modules = [
				new Module("M1", "Sp1", 0.7, 2, null),
				new Module("M1", "Sp2", 0.8, 3, null),
				new Module("M2", "Sp2", 0.3, 1, null)]
		
		moduleInference["S1"] = new Modules(s1Modules, 0.6)
		moduleInference["S2"] = new Modules(s2Modules, 0.6)
		
		def outlines = ["Taxon	Module	S1	S2", "Sp1	M1	1.0	2.0", "Sp2	M1	0.0	3.0", "Sp2	M2	1.0	0.0", "Sp2	M3	1.0	0.0"] as Set
		
		when:
		File outFile = File.createTempFile("ModuleTaxonomyMatrixWriterTestCase", "out");
		new ModuleTaxonomyMatrixWriter().writeCounts(moduleInference, outFile)
		
		
		then:
		// assert out matrix lines are matching
		outFile.readLines().every {
			// each line is in outlines, every remove will return true
			outlines.remove(it)
		}		// all expected lines were observed
		outlines.isEmpty() == true
	}
	
	def "exportModules outputs a Count and a Coverage Matrix with a header containing taxon name, module id, followed by values"() {
		setup: "create a modules set for 2 samples"
		Map<String, Modules> moduleInference = [:]
		/* create 3 modules and 2 samples
		 * Taxon	Module	S1	S2
		 * Sp1	M1	1	1
		 * Sp2	M1	0	1
		 * Sp2	M2	1	0
		 * Sp2	M3	1	0
		 *
		 */
		List<Module> s1Modules = [
				new Module("M1", "Sp1", 0.7, 1, null),
				new Module("M1", "Sp2", 0.3, 1, null),
				new Module("M2", "Sp2", 0.7, 1, null),
				new Module("M3", "Sp2", 0.7, 1, null)]
		
		List<Module> s2Modules = [
				new Module("M1", "Sp1", 0.7, 2, null),
				new Module("M1", "Sp2", 0.8, 3, null),
				new Module("M2", "Sp2", 0.3, 1, null)]
		
		moduleInference["S1"] = new Modules(s1Modules, 0.6)
		moduleInference["S2"] = new Modules(s2Modules, 0.6)
		
		def countLines = ["Taxon	Module	S1	S2", "Sp1	M1	1.0	2.0", "Sp2	M1	0.0	3.0", "Sp2	M2	1.0	0.0", "Sp2	M3	1.0	0.0"] as Set
		def coverageLines = ["Taxon	Module	S1	S2", "Sp1	M1	0.7	0.7", "Sp2	M1	0.0	0.8", "Sp2	M2	0.7	0.0", "Sp2	M3	0.7	0.0"] as Set
		
		when:
		File outCounts = File.createTempFile("ModuleTaxonomyMatrixWriterTestCase", "outCounts")
		File outCoverage = File.createTempFile("ModuleTaxonomyMatrixWriterTestCase", "outCoverage")
		new ModuleTaxonomyMatrixWriter().exportModules(moduleInference, outCounts, outCoverage)
		
		
		then:
		// assert out matrix lines are matching
		outCounts.readLines().every {
			// each line is in outlines, every remove will return true
			countLines.remove(it)
		}		// all expected lines were observed
		countLines.isEmpty() == true
		
		outCoverage.readLines().every {
			// each line is in outlines, every remove will return true
			coverageLines.remove(it)
		}		// all expected lines were observed
		coverageLines.isEmpty() == true
	}
}