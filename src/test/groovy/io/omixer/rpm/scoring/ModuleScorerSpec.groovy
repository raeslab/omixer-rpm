package io.omixer.rpm.scoring

import io.omixer.rpm.domain.Alternative
import io.omixer.rpm.domain.ModuleDatabase
import io.omixer.rpm.domain.ModuleDefinition
import io.omixer.rpm.domain.Step
import io.omixer.rpm.engine.CompiledDatabase
import spock.lang.Specification

class ModuleScorerSpec extends Specification {

	static final List<Estimator> ESTIMATORS = [new Estimator.Median(), new Estimator.Average(), new Estimator.Sum(),
		new Estimator.Min()]

	/** A one-module database: steps separated by " / ", alternatives by "|", subunits by "," */
	private static CompiledDatabase database(String module) {
		def steps = module.split(" / ").collect { step ->
			new Step(step.split("\\|").collect { new Alternative(it.split(",") as List) })
		}
		CompiledDatabase.compile(new ModuleDatabase([new ModuleDefinition("M1", "test", steps)]))
	}

	/** The profile as KO values indexed by the database's KO indexes; KOs not given are 0 */
	private static double[] values(CompiledDatabase database, Map<String, Double> profile) {
		double[] values = new double[database.koCount()]
		profile.each { ko, value ->
			if (database.indexOf(ko) >= 0) {
				values[database.indexOf(ko)] = value
			}
		}
		values
	}

	private static ModuleScore score(String module, Map<String, Double> profile, Estimator estimator,
			ComplexRule rule = ComplexRule.ANY, boolean normalize = false) {
		def db = database(module)
		new ModuleScorer(new ScoringOptions(estimator, rule, normalize)).score(db.modules().first(), values(db, profile))
	}

	def "the best path: each step takes its best observed alternative (REFACTORING.md example)"() {

		when:
		def result = score("K1|K2 / K3,K4|K5 / K6|K7", [K1: 4d, K3: 6d, K5: 2d], new Estimator.Median(), rule)

		then: "step 1 takes K1 (4); step 2 K3,K4 (mean 3) with any, K5 (2) with all; step 3 is not covered"
		result.coverage() == 2d / 3d
		result.abundance() == abundance

		where:
		rule            | abundance
		ComplexRule.ANY | 3.5d
		ComplexRule.ALL | 3d
	}

	def "with values ≥ 0, the best alternative per step gives the same result as every path, bit for bit"() {

		given: "random modules with repeated KOs, complexes, zeros (also -0.0) and ties"
		def random = new Random(20261029)
		def failures = []
		int observed = 0

		when:
		500.times {
			def kos = (0..<8).collect { "K" + it }
			def module = (1..(1 + random.nextInt(5))).collect {
				(1..(1 + random.nextInt(4))).collect {
					(1..(random.nextInt(10) < 7 ? 1 : 2 + random.nextInt(2))).collect { kos[random.nextInt(kos.size())] }.join(",")
				}.join("|")
			}.join(" / ")
			def profile = kos.collectEntries { ko ->
				int pick = random.nextInt(10)
				[ko, pick < 2 ? 0d : pick < 4 ? -0d : pick < 7 ? (double) (1 + random.nextInt(4)) : random.nextDouble() * 50]
			}
			def db = database(module)
			def compiled = db.modules().first()
			def values = values(db, profile)
			observed += (values as List).count { it > 0 }
			[ESTIMATORS, ComplexRule.values() as List, [false, true]].combinations().each { estimator, rule, normalize ->
				def scorer = new ModuleScorer(new ScoringOptions(estimator, rule, normalize))
				def perStep = scorer.scorePerStep(compiled, values)
				def allPaths = scorer.scoreAllPaths(compiled, values)
				if (Double.compare(perStep.abundance(), allPaths.abundance()) != 0
						|| Double.compare(perStep.coverage(), allPaths.coverage()) != 0) {
					failures << "$module $profile $estimator $rule -n=$normalize: per step $perStep, all paths $allPaths"
				}
			}
		}

		then:
		failures == []

		and: "the modules were scored on non-zero values"
		observed > 0
	}

	def "with a negative value every path is searched: the median's zero rule can favour a path that is not the best per step"() {

		given: """-n median. Step 1: K1 (2), or the complex K2,K3 (6, -6: mean 0, observed with any); step 2: K4 (5);
				step 3: K5, not observed (0 with -n). Best per step: [2, 5, 0] → median 2. The complex instead gives
				[0, 5, 0] → median 0, replaced by the smallest positive value: 5"""
		def module = "K1|K2,K3 / K4 / K5"
		def profile = [K1: 2d, K2: 6d, K3: -6d, K4: 5d]
		def db = database(module)
		def scorer = new ModuleScorer(new ScoringOptions(new Estimator.Median(), ComplexRule.ANY, true))

		expect:
		scorer.scorePerStep(db.modules().first(), values(db, profile)).abundance() == 2d
		score(module, profile, new Estimator.Median(), ComplexRule.ANY, true) == new ModuleScore(5d, 2d / 3d)
	}

	def "a module with no observed step has abundance 0 and coverage 0 with every estimator"() {

		expect:
		score("K1 / K2,K3", [K9: 4d], estimator, ComplexRule.ANY, normalize) == new ModuleScore(0d, 0d)

		where:
		[estimator, normalize] << [ESTIMATORS, [false, true]].combinations()
	}

	def "each estimator combines the step values of the best path"() {

		expect: "steps: K1 = 4, the complex K2,K3 = mean(6, 0) = 3, K4 not observed"
		score("K1 / K2,K3 / K4", [K1: 4d, K2: 6d], estimator, ComplexRule.ANY, normalize).abundance() == abundance

		where:
		estimator               | normalize | abundance
		new Estimator.Median()  | false     | 3.5d
		new Estimator.Median()  | true      | 3d
		new Estimator.Average() | false     | 3.5d
		new Estimator.Average() | true      | 7d / 3d
		new Estimator.Sum()     | false     | 7d
		new Estimator.Sum()     | true      | 7d / 3d
		new Estimator.Min()     | false     | 4d
		new Estimator.Min()     | true      | 4d
	}
}
