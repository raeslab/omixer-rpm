package org.omixer.rpm.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.omixer.rpm.model.BasicFeature;
import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.ModuleInferenceOptions;
import org.omixer.rpm.model.Modules;
import org.omixer.rpm.model.enums.ModuleInferenceOptimizers;
import org.omixer.rpm.parsers.ModuleParser;
import org.omixer.rpm.service.impl.ModuleManagerImpl;
import org.omixer.utils.Constants;

import spock.lang.Specification

public class ModuleManagerSpecs extends Specification {

	def manager
	def referenceModules
	def options
	
	def setup() {
		manager = new ModuleManagerImpl()
		referenceModules = ModuleParser.parseModuleFile(new File("src/test/resources/module-inference.list"))
		options = new ModuleInferenceOptions()
		options.setCoverage(0.66)
		options.setNormalizeByLength(false)
		options.setPromiscuousity(0)
	}
	
	def "inferModules selects the path with higher abundance when there are ties in the coverage"() {

		setup:
		List<BasicFeature> orthologs = [new BasicFeature(null, "K00004", 2d), new BasicFeature(null, "K00005", 6d), new BasicFeature(null, "K00011", 2d)]
		
		when:
		options.setCoverage(0.65)
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName())
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules)
		List<Module> modules = modulePrediction.toAboveCutoffList()
		
		then:
		modules.size() == 1
		def module = modules[0]
		module.getModuleId().equals("MF0003")
		module.getCount() == 4
		module.getCoverage() == 2d/3d

	}

	def "inferModules normalizes complex reaction steps by their orthologs counts"() {
		
		setup:
		List<BasicFeature> orthologs = [new BasicFeature(null, "K00001", 2d), new BasicFeature(null, "K00002", 2d)]
		
		when: "A complex step is found (i.e. several orthologs form an enzyme) "
		options.setNormalizeByLength(true);
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules)
		List<Module> modules = modulePrediction.toAboveCutoffList()
		
		then: "its abudance is divided by it orthologs counts"
		modules.size() == 1
		modules[0].getModuleId().equals("MF0001")
		modules[0].getCount() == (2 + 2/2) / 2
		modules[0].getCoverage() == 1
	}
	
	def "inferModules returns correct coverage and average"() {
		
		setup:
		options.setNormalizeByLength(true)
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())
				
		when: "Every step of MF0003 has a KO"
		List<BasicFeature> orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 2d]].collect{new BasicFeature(null, it[0], it[1])}
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules);						
		List<Module> modules = modulePrediction.toAboveCutoffList();

		then: "MF0003 has 100% coverage"
		modules.size() == 1
		modules[0].getModuleId() == "MF0003"
		modules[0].getCount() == (2 + 8 + 2 ) / 3
		modules[0].getCoverage() == 1
		
		when: "2/3 of a module"
		orthologs = [["K00004", 8d], ["K00005", 8d], ["K00011", 8d]].collect{new BasicFeature(null, it[0], it[1])}
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: "coverage is 2/3"
		modules.size() == 1
		modules[0].getCount() == 16d/3d
		modules[0].getCoverage() == 2d/3d
		
		when: "A one step module is matched"
		orthologs = [new BasicFeature(null, "K00012", 8d)]
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).getCount() == 8d
		
		and: "a 100% coverage"
		modules.get(0).getCoverage() == 1d
		
		
		when:
		options.setNormalizeByLength(false);
		orthologs = [["K00004", 8d], ["K00005", 8d], ["K00011", 8d]].collect{new BasicFeature(null, it[0], it[1])}
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: ""
		modules.size() == 1
		modules.get(0).getCount() == 16d/2d
		modules.get(0).getCoverage() == 2d/3d
		
	}
	
	def "inferModules returns correct coverage and median"() {
		
		setup:
		List<BasicFeature> orthologs
		options.setNormalizeByLength(true)
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName())
		
		when: "A one step module is matched"
		orthologs = [new BasicFeature(null, "K00012", 8d)]
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		List<Module> modules = modulePrediction.toAboveCutoffList();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).getCount() == 8d
		
		and: "a 100% coverage"
		modules.get(0).getCoverage() == 1d
		
		
		when: "Every step of MF0003 has a KO"
		orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 10d]].collect{new BasicFeature(null, it[0], it[1])}
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: "MF0003 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0003"
		
		and: "Its count equals the median of {2,8,10}"
		modules.get(0).getCount() == 8
		
		and: "it has 100% coverage"
		modules.get(0).getCoverage() == 1
		
	}
		
	def "inferModules returns correct coverage and sum"() {
		
		setup:
		List<BasicFeature> orthologs
		options.setNormalizeByLength(false)
		options.setAlgorithm(ModuleInferenceOptimizers.SUM.displayName())
		
		when: "A one step module is matched"
		orthologs = [new BasicFeature(null, "K00012", 8d)]
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		List<Module> modules = modulePrediction.toAboveCutoffList();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).getCount() == 8d
		
		and: "a 100% coverage"
		modules.get(0).getCoverage() == 1d
		
		
		when: "Every step of MF0003 has a KO"
		orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 10d]].collect{new BasicFeature(null, it[0], it[1])}
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: "MF0003 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0003"
		
		and: "Its count equals the Sum of {2,8,10}"
		modules.get(0).getCount() == 20
		
		and: "it has 100% coverage"
		modules.get(0).getCoverage() == 1
	}
	
	def "inferModules returns correct coverage and min"() {
		
		setup:
		List<BasicFeature> orthologs
		options.setNormalizeByLength(false)
		options.setAlgorithm(ModuleInferenceOptimizers.MIN.displayName())
		
		when: "A one step module is matched"
		orthologs = [new BasicFeature(null, "K00012", 8d)]
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		List<Module> modules = modulePrediction.toAboveCutoffList();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).getCount() == 8d
		
		and: "a 100% coverage"
		modules.get(0).getCoverage() == 1d
		
		
		when: "Every step of MF0003 has a KO"
		orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 10d]].collect{new BasicFeature(null, it[0], it[1])}
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: "MF0003 is selected"
		modules.size() == 1
		modules[0].getModuleId() == "MF0003"
		
		and: "Its count equals the Minimum of {2,8,10}"
		modules.get(0).getCount() == 2
		
		and: "it has 100% coverage"
		modules.get(0).getCoverage() == 1
	}
	
	def "inferModules replaces 0 median by the trimmed minimum"() {
		
		setup:
		List<BasicFeature> orthologs = [["K00013", 2d], ["K00014", 3d], ["K00015", 8d]].collect{new BasicFeature(null, it[0], it[1])}
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName())
		options.setCoverage(0.33d)
		
		when: "Normalize by length is ON or OFF"
		options.setNormalizeByLength(normalizeByLength)
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules)
		List<Module> modules = modulePrediction.toAboveCutoffList()

		then: "A diffrent value for count is returned"
		modules.size() == size
		modules.get(0).getCount() == count
		modules.get(0).getCoverage() == coverage
		
		where: "Count value depends on normalizeByLength"
		normalizeByLength | size | count | coverage | comment
		     true         |  1   |  2d   | 3d/7d    | "Instead of the median of {0, 0, 0, 0, 2, 3, 8}, then trimmed minimum (min({2, 3, 8})) is returned"
		     false        |  1   |  3d   | 3d/7d    | "module count is the median of {2, 3, 8}"
	}
	
	def "inferModules from matrix and NormalizeByLength is true"(){
		
		when: "modules are inferred from a matrix"
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())
		options.setNormalizeByLength(true)
		manager.setInputFormat("1")
		ConcurrentHashMap<String, Modules> inference = manager.inferModules(new File("src/test/resources/matrix.tsv"), options, referenceModules)

		then:
		inference.size() == 2
		
		when: "cutoff is applied"
		def inferenceS1 = inference.get("S1").toAboveCutoffList().groupBy{ it.getModuleId() }
		
		then: "MF0001 and MF0003"
		inferenceS1.size() == 2
		
		when:
		def module = inferenceS1.get(moduleId)[0]
		
		then: "MF0001 and MF0003"
		module.getModuleId() == moduleId
		module.getCount() == count
		module.getCoverage() == coverage
				
		where:
		moduleId | count | coverage
		"MF0001" | (1 + (2/2))/2 | 1
		"MF0003" | 12d/3d | 1
		
		
	}
	
	def "inferModules from matrix and NormalizeByLength is false"(){
		
		when: "modules are inferred from a matrix"
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())
		options.setNormalizeByLength(false)
		manager.setInputFormat("1")
		ConcurrentHashMap<String, Modules> inference = manager.inferModules(new File("src/test/resources/matrix.tsv"), options, referenceModules)
		def inferenceS2 = inference.get("S2").toAboveCutoffList().groupBy{ it.getModuleId() }

		then: 	
		inferenceS2.size() == 3
				
		when:
		def module = inferenceS2.get(moduleId)[0]
		
		then: "MF0003, MF0004, and MF0005"
		module.getModuleId() == moduleId
		module.getCount() == count
		module.getCoverage() == coverage
		
		where:
		moduleId | count | coverage
		"MF0003" | (8+8) / 2 | 2d/3d
		"MF0004" | 8 | 1
		"MF0005" | 29d/5d | 5d/7d
		
	}
	
	def "inferModules from Species matrix returns correct number of samples"() {
		
		setup:
		manager.setInputFormat("2")
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())
		options.setNormalizeByLength(true)
		options.setPerTaxon(true)
				
		when: "A matrix wih species annotation and a 2 samples"
		ConcurrentHashMap<String, Modules> inference = manager.inferModules(new File("src/test/resources/taxon_matrix.tsv"), options, referenceModules)
		
		then: "There is an inference for each sample"
		inference.size() == 2
		
		when: "Sample S1 is selected and cutoff is applied"
		def s1 = inference.get("S1").toAboveCutoffList()
		def inferenceS1 = s1.groupBy{ it.getModuleId() + Constants.UNDERSCORE + it.getTaxon() }
				
		then: "2 Modules are found"
		inferenceS1.size() == 2
		
		when:
		def module = inferenceS1.get(id)[0]
		
		then:
		module.getModuleId() == moduleId
		module.getTaxon() == species
		module.getCount() == count
		module.getCoverage() == coverage
		
		where:
		id | moduleId | species | count | coverage
		"MF0001_Sp1" | "MF0001" | "Sp1" | (1 + (2/2))/2 | 1
		"MF0003_Sp2" | "MF0003" | "Sp2" | 12d/3d | 1
	}
	
	def "inferModules from directory returns correct number of samples"() {
		
		when: "a directory with 2 input files is input"
		manager.setInputFormat("1")
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName())
		options.setNormalizeByLength(true)
				
		ConcurrentHashMap<String, Modules> inference = manager.inferModules(new File("src/test/resources/input_directory/"), options, referenceModules)

		then:"expect 2 samples to be returned"
		inference.size() == 2

		when: "cutoff is applied"
		def inferenceS1 = inference.get("S1").toAboveCutoffList().groupBy{ it.getModuleId() }

		then: "MF0001 and MF0003"
		inferenceS1.size() == 2

		when:
		def module = inferenceS1.get(moduleId)[0]

		then: "MF0001 and MF0003"
		module.getModuleId() == moduleId
		module.getCount() == count
		module.getCoverage() == coverage

		where:
		moduleId | count | coverage
		"MF0001" | (1 + (2/2))/2 | 1
		"MF0003" | 12d/3d | 1
	}

	def "withDistributedOrthologAbundance distributes KO abundances"() throws IOException {

		setup:
		List<BasicFeature> orthologs = [new BasicFeature(null, "K00001", 2d), new BasicFeature(null, "K00002", 2d)]
		
		when:
		ModuleInferenceOptions options = new ModuleInferenceOptions();
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED.displayName());
		options.setCoverage(0.66);
		options.setNormalizeByLength(true);
		options.setPromiscuousity(0);
		Modules modulePrediction = manager.inferModulesWithDistributedOrthologAbundance(orthologs, options, referenceModules);
		List<Module> modules = modulePrediction.toAboveCutoffList();
		
		then: "One module is matched"
		modules.size() == 1
		and: """K1 is divided by 2 as it is found in two modules
				K2 is divided by 2 as the step where it is found is a 2 KO complex step.
				The score is ( 2/2 + 2/2 ) / 2. The last division by 2 is because the module size is 2"""
		modules[0].getCount() == 1d
	}

	def "inferModules correclty quantifies lactoseGalactoseDegradation: debugging for a specific example"() {
		
		setup:
		List<BasicFeature> orthologs = [["K01220", 2d], ["K01819", 4d], ["K00917", 2d]].collect{ new BasicFeature(null, it[0], it[1]) }
		
		when: "Above 50% no ties in coverage"
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName());
		options.setCoverage(0.55);
		Modules modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		List<Module> modules = modulePrediction.toAboveCutoffList();
		
		then:
		modules.size() == 1
		
		modules[0].getModuleId().equals("MF0007")
		modules[0].getCount() == 2
		modules[0].getCoverage() == 3d/5d

		when: "Above 50% with ties in coverage"
		orthologs.add(new BasicFeature(null, "K08302", 2d))
		orthologs.add(new BasicFeature(null, "K01635", 5d))
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		
		then:
		modules.size() == 1
		modules[0].getModuleId().equals("MF0007")
		modules[0].getCount() == 3
		modules[0].getCoverage() == 4d/5d
				
		
		when: "A third of the complex part included"
		orthologs.add(new BasicFeature(null, "K02788", 1d))
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then:
		modules.size() == 1
		modules[0].getModuleId().equals("MF0007")
		modules[0].getCount() == 2
		modules[0].getCoverage() == 1
		
		
		when: "all the complex part included"
		orthologs.add(new BasicFeature(null, "K02787", 5d))
		orthologs.add(new BasicFeature(null, "K02786", 9d))
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then: 
		modules.size() == 1
		modules[0].getModuleId().equals("MF0007")
		modules[0].getCount() == 4
		modules[0].getCoverage() == 1
		
		when:"Lower than 50% coverage"
		options.setCoverage(0.30);
		orthologs = [new BasicFeature(null, "K01819", 4d), new BasicFeature(null, "K08302", 2d)]
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();
		
		then : "Above 30% no ties in coverage"
		modules.size() == 1
		modules[0].getModuleId().equals("MF0007")
		modules[0].getCount() == 3
		modules[0].getCoverage() == 2d/5d
		
		when: "Above 30% with ties in coverage"
		orthologs.add(new BasicFeature(null, "K01635", 4d));
		modulePrediction = manager.inferModules(orthologs, options, referenceModules);
		modules = modulePrediction.toAboveCutoffList();

		then: 
		modules.size() == 1
		modules[0].getModuleId().equals("MF0007")
		modules[0].getCount() == 4
		modules[0].getCoverage() == 2d/5d
	}
}