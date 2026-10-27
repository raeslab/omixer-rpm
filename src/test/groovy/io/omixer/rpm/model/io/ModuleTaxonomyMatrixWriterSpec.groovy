package io.omixer.rpm.model.io


import io.omixer.rpm.model.Module
import io.omixer.rpm.model.Modules
import spock.lang.Specification

public class ModuleTaxonomyMatrixWriterSpec extends Specification {

	def "writeCounts writes the samples as columns in the given order and each taxon's modules in reference order"() {

		setup:
		List<String> modules = ["MF0012", "MF0003", "MF0101", "MF0040", "MF0007"]
		Map<String, Modules> moduleInference = new LinkedHashMap<>()
		["S3", "S1", "Z9", "S10", "A2"].each { sample ->
			moduleInference[sample] = new Modules(modules.collect { new Module(it, "Sp1", 1d, 1d, null) }, 0d)
		}

		when:
		File out = File.createTempFile("ModuleTaxonomyMatrixWriterSpec", "out")
		new ModuleTaxonomyMatrixWriter().writeCounts(moduleInference, out)
		List<String> lines = out.readLines()

		then:
		lines[0] == "Taxon\tModule\tS3\tS1\tZ9\tS10\tA2"
		lines.drop(1).collect { it.split("\t")[1] } == modules
	}

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