package io.omixer.rpm.inference.scoring

import io.omixer.rpm.SampleResults
import io.omixer.rpm.Settings
import io.omixer.rpm.database.CompiledDatabase
import io.omixer.rpm.database.ModuleDatabaseReader
import io.omixer.rpm.inference.ModuleResult
import io.omixer.rpm.profile.reader.InputFormat
import java.nio.file.Path

import io.omixer.utils.Constants;

import spock.lang.Specification

class ScoringInferenceSpec extends Specification {

	def database
	def options
	
	def setup() {
		database = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module-inference.list")))
		options = new Settings()
		options.setCoverage(0.66)
		options.setNormalizeByLength(false)
	}
	
	def "scoring selects the path with higher abundance when there are ties in the coverage"() {

		setup:
		List<List> orthologs = [Settings.row(null, "K00004", 2d), Settings.row(null, "K00005", 6d), Settings.row(null, "K00011", 2d)]
		
		when:
		options.setCoverage(0.65)
		options.setEstimator(new Estimator.Median())
		SampleResults modulePrediction = options.infer(orthologs, database)
		List<ModuleResult> modules = modulePrediction.reported()
		
		then:
		modules.size() == 1
		def module = modules[0]
		module.module().equals("MF0003")
		module.abundance() == 4
		module.coverage() == 2d/3d

	}

	def "scoring normalizes complex reaction steps by their orthologs counts"() {
		
		setup:
		List<List> orthologs = [Settings.row(null, "K00001", 2d), Settings.row(null, "K00002", 2d)]
		
		when: "A complex step is found (i.e. several orthologs form an enzyme) "
		options.setNormalizeByLength(true);
		options.setEstimator(new Estimator.Average())
		SampleResults modulePrediction = options.infer(orthologs, database)
		List<ModuleResult> modules = modulePrediction.reported()
		
		then: "its abudance is divided by it orthologs counts"
		modules.size() == 1
		modules[0].module().equals("MF0001")
		modules[0].abundance() == (2 + 2/2) / 2
		modules[0].coverage() == 1
	}
	
	def "scoring returns correct coverage and average"() {
		
		setup:
		options.setNormalizeByLength(true)
		options.setEstimator(new Estimator.Average())
				
		when: "Every step of MF0003 has a KO"
		List<List> orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 2d]].collect{Settings.row(null, it[0], it[1])}
		SampleResults modulePrediction = options.infer(orthologs, database);
		List<ModuleResult> modules = modulePrediction.reported();

		then: "MF0003 has 100% coverage"
		modules.size() == 1
		modules[0].module() == "MF0003"
		modules[0].abundance() == (2 + 8 + 2 ) / 3
		modules[0].coverage() == 1
		
		when: "2/3 of a module"
		orthologs = [["K00004", 8d], ["K00005", 8d], ["K00011", 8d]].collect{Settings.row(null, it[0], it[1])}
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: "coverage is 2/3"
		modules.size() == 1
		modules[0].abundance() == 16d/3d
		modules[0].coverage() == 2d/3d
		
		when: "A one step module is matched"
		orthologs = [Settings.row(null, "K00012", 8d)]
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].module() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).abundance() == 8d
		
		and: "a 100% coverage"
		modules.get(0).coverage() == 1d
		
		
		when:
		options.setNormalizeByLength(false);
		orthologs = [["K00004", 8d], ["K00005", 8d], ["K00011", 8d]].collect{Settings.row(null, it[0], it[1])}
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: ""
		modules.size() == 1
		modules.get(0).abundance() == 16d/2d
		modules.get(0).coverage() == 2d/3d
		
	}
	
	def "scoring returns correct coverage and median"() {
		
		setup:
		List<List> orthologs
		options.setNormalizeByLength(true)
		options.setEstimator(new Estimator.Median())
		
		when: "A one step module is matched"
		orthologs = [Settings.row(null, "K00012", 8d)]
		SampleResults modulePrediction = options.infer(orthologs, database);
		List<ModuleResult> modules = modulePrediction.reported();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].module() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).abundance() == 8d
		
		and: "a 100% coverage"
		modules.get(0).coverage() == 1d
		
		
		when: "Every step of MF0003 has a KO"
		orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 10d]].collect{Settings.row(null, it[0], it[1])}
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: "MF0003 is selected"
		modules.size() == 1
		modules[0].module() == "MF0003"
		
		and: "Its count equals the median of {2,8,10}"
		modules.get(0).abundance() == 8
		
		and: "it has 100% coverage"
		modules.get(0).coverage() == 1
		
	}
		
	def "scoring returns correct coverage and sum"() {
		
		setup:
		List<List> orthologs
		options.setNormalizeByLength(false)
		options.setEstimator(new Estimator.Sum())
		
		when: "A one step module is matched"
		orthologs = [Settings.row(null, "K00012", 8d)]
		SampleResults modulePrediction = options.infer(orthologs, database);
		List<ModuleResult> modules = modulePrediction.reported();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].module() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).abundance() == 8d
		
		and: "a 100% coverage"
		modules.get(0).coverage() == 1d
		
		
		when: "Every step of MF0003 has a KO"
		orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 10d]].collect{Settings.row(null, it[0], it[1])}
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: "MF0003 is selected"
		modules.size() == 1
		modules[0].module() == "MF0003"
		
		and: "Its count equals the Sum of {2,8,10}"
		modules.get(0).abundance() == 20
		
		and: "it has 100% coverage"
		modules.get(0).coverage() == 1
	}
	
	def "scoring returns correct coverage and min"() {
		
		setup:
		List<List> orthologs
		options.setNormalizeByLength(false)
		options.setEstimator(new Estimator.Min())
		
		when: "A one step module is matched"
		orthologs = [Settings.row(null, "K00012", 8d)]
		SampleResults modulePrediction = options.infer(orthologs, database);
		List<ModuleResult> modules = modulePrediction.reported();
		
		then: "Only MF0004 is selected"
		modules.size() == 1
		modules[0].module() == "MF0004"
		
		and: "module count equals ortholog count"
		modules.get(0).abundance() == 8d
		
		and: "a 100% coverage"
		modules.get(0).coverage() == 1d
		
		
		when: "Every step of MF0003 has a KO"
		orthologs = [["K00005", 2d], ["K00010", 8d], ["K00011", 10d]].collect{Settings.row(null, it[0], it[1])}
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: "MF0003 is selected"
		modules.size() == 1
		modules[0].module() == "MF0003"
		
		and: "Its count equals the Minimum of {2,8,10}"
		modules.get(0).abundance() == 2
		
		and: "it has 100% coverage"
		modules.get(0).coverage() == 1
	}
	
	def "scoring replaces 0 median by the trimmed minimum"() {
		
		setup:
		List<List> orthologs = [["K00013", 2d], ["K00014", 3d], ["K00015", 8d]].collect{Settings.row(null, it[0], it[1])}
		options.setEstimator(new Estimator.Median())
		options.setCoverage(0.33d)
		
		when: "Normalize by length is ON or OFF"
		options.setNormalizeByLength(normalizeByLength)
		SampleResults modulePrediction = options.infer(orthologs, database)
		List<ModuleResult> modules = modulePrediction.reported()

		then: "A diffrent value for count is returned"
		modules.size() == size
		modules.get(0).abundance() == count
		modules.get(0).coverage() == coverage
		
		where: "Count value depends on normalizeByLength"
		normalizeByLength | size | count | coverage | comment
		     true         |  1   |  2d   | 3d/7d    | "Instead of the median of {0, 0, 0, 0, 2, 3, 8}, then trimmed minimum (min({2, 3, 8})) is returned"
		     false        |  1   |  3d   | 3d/7d    | "module count is the median of {2, 3, 8}"
	}
	
	def "scoring from matrix and NormalizeByLength is true"(){
		
		when: "modules are inferred from a matrix"
		options.setEstimator(new Estimator.Average())
		options.setNormalizeByLength(true)
		Map<String, SampleResults> inference = options.inferSamples(new File("src/test/resources/matrix.tsv"), InputFormat.KO, database)

		then:
		inference.size() == 2
		
		when: "cutoff is applied"
		def inferenceS1 = inference.get("S1").reported().groupBy{ it.module() }
		
		then: "MF0001 and MF0003"
		inferenceS1.size() == 2
		
		when:
		def module = inferenceS1.get(moduleId)[0]
		
		then: "MF0001 and MF0003"
		module.module() == moduleId
		module.abundance() == count
		module.coverage() == coverage
				
		where:
		moduleId | count | coverage
		"MF0001" | (1 + (2/2))/2 | 1
		"MF0003" | 12d/3d | 1
		
		
	}
	
	def "scoring from matrix and NormalizeByLength is false"(){
		
		when: "modules are inferred from a matrix"
		options.setEstimator(new Estimator.Average())
		options.setNormalizeByLength(false)
		Map<String, SampleResults> inference = options.inferSamples(new File("src/test/resources/matrix.tsv"), InputFormat.KO, database)
		def inferenceS2 = inference.get("S2").reported().groupBy{ it.module() }

		then: 	
		inferenceS2.size() == 3
				
		when:
		def module = inferenceS2.get(moduleId)[0]
		
		then: "MF0003, MF0004, and MF0005"
		module.module() == moduleId
		module.abundance() == count
		module.coverage() == coverage
		
		where:
		moduleId | count | coverage
		"MF0003" | (8+8) / 2 | 2d/3d
		"MF0004" | 8 | 1
		"MF0005" | 29d/5d | 5d/7d
		
	}
	
	def "scoring from Species matrix returns correct number of samples"() {
		
		setup:
		options.setEstimator(new Estimator.Average())
		options.setNormalizeByLength(true)
		options.setPerTaxon(true)
				
		when: "A matrix wih species annotation and a 2 samples"
		Map<String, SampleResults> inference = options.inferSamples(new File("src/test/resources/taxon_matrix.tsv"), InputFormat.TAXON_KO, database)
		
		then: "There is an inference for each sample"
		inference.size() == 2
		
		when: "Sample S1 is selected and cutoff is applied"
		def s1 = inference.get("S1").reported()
		def inferenceS1 = s1.groupBy{ it.module() + Constants.UNDERSCORE + it.taxon() }
				
		then: "2 Modules are found"
		inferenceS1.size() == 2
		
		when:
		def module = inferenceS1.get(id)[0]
		
		then:
		module.module() == moduleId
		module.taxon() == species
		module.abundance() == count
		module.coverage() == coverage
		
		where:
		id | moduleId | species | count | coverage
		"MF0001_Sp1" | "MF0001" | "Sp1" | (1 + (2/2))/2 | 1
		"MF0003_Sp2" | "MF0003" | "Sp2" | 12d/3d | 1
	}
	
	def "scoring from directory returns correct number of samples"() {
		
		when: "a directory with 2 input files is input"
		options.setEstimator(new Estimator.Average())
		options.setNormalizeByLength(true)
				
		Map<String, SampleResults> inference = options.inferSamples(new File("src/test/resources/input_directory/"), InputFormat.KO, database)

		then:"expect 2 samples to be returned"
		inference.size() == 2

		when: "cutoff is applied"
		def inferenceS1 = inference.get("S1").reported().groupBy{ it.module() }

		then: "MF0001 and MF0003"
		inferenceS1.size() == 2

		when:
		def module = inferenceS1.get(moduleId)[0]

		then: "MF0001 and MF0003"
		module.module() == moduleId
		module.abundance() == count
		module.coverage() == coverage

		where:
		moduleId | count | coverage
		"MF0001" | (1 + (2/2))/2 | 1
		"MF0003" | 12d/3d | 1
	}

	def "scoring with distributed ortholog abundance divides shared KOs"() throws IOException {

		setup:
		List<List> orthologs = [Settings.row(null, "K00001", 2d), Settings.row(null, "K00002", 2d)]
		
		when:
		Settings options = new Settings();
		options.setEstimator(new Estimator.Average());
		options.setCoverage(0.66);
		options.setNormalizeByLength(true);
		options.setDistribute(true);
		SampleResults modulePrediction = options.infer(orthologs, database);
		List<ModuleResult> modules = modulePrediction.reported();
		
		then: "One module is matched"
		modules.size() == 1
		and: """K1 is divided by 2 as it is found in two modules
				K2 is divided by 2 as the step where it is found is a 2 KO complex step.
				The score is ( 2/2 + 2/2 ) / 2. The last division by 2 is because the module size is 2"""
		modules[0].abundance() == 1d
	}

	def "scoring correclty quantifies lactoseGalactoseDegradation: debugging for a specific example"() {
		
		setup:
		List<List> orthologs = [["K01220", 2d], ["K01819", 4d], ["K00917", 2d]].collect{ Settings.row(null, it[0], it[1]) }
		
		when: "Above 50% no ties in coverage"
		options.setEstimator(new Estimator.Median());
		options.setCoverage(0.55);
		SampleResults modulePrediction = options.infer(orthologs, database);
		List<ModuleResult> modules = modulePrediction.reported();
		
		then:
		modules.size() == 1
		
		modules[0].module().equals("MF0007")
		modules[0].abundance() == 2
		modules[0].coverage() == 3d/5d

		when: "Above 50% with ties in coverage"
		orthologs.add(Settings.row(null, "K08302", 2d))
		orthologs.add(Settings.row(null, "K01635", 5d))
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		
		then:
		modules.size() == 1
		modules[0].module().equals("MF0007")
		modules[0].abundance() == 3
		modules[0].coverage() == 4d/5d
				
		
		when: "A third of the complex part included"
		orthologs.add(Settings.row(null, "K02788", 1d))
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then:
		modules.size() == 1
		modules[0].module().equals("MF0007")
		modules[0].abundance() == 2
		modules[0].coverage() == 1
		
		
		when: "all the complex part included"
		orthologs.add(Settings.row(null, "K02787", 5d))
		orthologs.add(Settings.row(null, "K02786", 9d))
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then: 
		modules.size() == 1
		modules[0].module().equals("MF0007")
		modules[0].abundance() == 4
		modules[0].coverage() == 1
		
		when:"Lower than 50% coverage"
		options.setCoverage(0.30);
		orthologs = [Settings.row(null, "K01819", 4d), Settings.row(null, "K08302", 2d)]
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();
		
		then : "Above 30% no ties in coverage"
		modules.size() == 1
		modules[0].module().equals("MF0007")
		modules[0].abundance() == 3
		modules[0].coverage() == 2d/5d
		
		when: "Above 30% with ties in coverage"
		orthologs.add(Settings.row(null, "K01635", 4d));
		modulePrediction = options.infer(orthologs, database);
		modules = modulePrediction.reported();

		then: 
		modules.size() == 1
		modules[0].module().equals("MF0007")
		modules[0].abundance() == 4
		modules[0].coverage() == 2d/5d
	}
}