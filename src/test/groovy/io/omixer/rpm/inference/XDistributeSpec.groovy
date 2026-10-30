package io.omixer.rpm.inference

import io.omixer.rpm.Settings
import io.omixer.rpm.SampleResults
import io.omixer.rpm.engine.ModuleResult
import java.nio.file.Path

import io.omixer.rpm.scoring.Estimator
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.io.InputFormat
import io.omixer.rpm.io.ModuleDatabaseReader

import spock.lang.Specification

/**
 * Regression tests for the --Xdistribute option (a KO shared by N modules gives each module 1/N of its abundance).
 * Bug IDs refer to BUGS.md.
 *
 * Reference modules (src/test/resources/xdistribute-modules.list):
 *   X001: K10001 / K10002 / K10001                      K10001 twice, in no other module
 *   X002: (K10010,K10013 | K10010,K10014) / K10015      K10010 in both alternatives of step 1
 *   X003: K10010 / K10016                                K10010 shared with X002
 */
class XDistributeSpec extends Specification {

	def database
	def options

	def setup() {
		database = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/xdistribute-modules.list")))
		options = new Settings()
		options.setEstimator(new Estimator.Median())
		options.setCoverage(0d)
		options.setNormalizeByLength(false)
		options.setDistribute(true)
	}

	private static List<List> features(String taxon, Map<String, Double> abundances) {
		abundances.collect { ko, value -> Settings.row(taxon, ko, value) }
	}

	private static Map<String, ModuleResult> byId(SampleResults results) {
		results.byModule()
	}

	def "a KO repeated inside one module, and used by no other module, is not split"() {

		when:
		Map<String, ModuleResult> result = byId(options.infer(features(null, [K10001: 7d, K10002: 1d]), database))

		then: "steps are 7, 1, 7, so the median is 7 (before the fix: K10001 was split against itself and the median was 3.5)"
		result["X001"].abundance() == 7d
		result["X001"].coverage() == 1d
	}

	def "a KO repeated inside one module and used by one other module is split in two, at every occurrence"() {

		when:
		Map<String, ModuleResult> result = byId(options.infer(features(null, [K10010: 8d, K10013: 4d, K10014: 4d, K10015: 4d, K10016: 4d]), database))

		then: """K10010 is used by 2 modules (X002, X003), so each gets 8/2 = 4, in both alternatives of X002.
				X002: step 1 = mean(4, 4) = 4 in either alternative, step 2 = 4 -> 4.
				X003: steps 4, 4 -> 4.
				Before the fix: N counted 3 occurrences (8/3), only X002's last alternative was reduced,
				so X002 picked the unreduced alternative (5.0) and X003 got 3.33."""
		result["X002"].abundance() == 4d
		result["X003"].abundance() == 4d
	}

	def "every occurrence of a shared KO carries the reduced value"() {

		when:
		Map<String, ModuleResult> result = byId(options.infer(features(null, [K10010: 8d, K10013: 4d, K10014: 8d, K10015: 4d]), database))

		then: """K10010 = 8 / 2 = 4 in both alternatives of X002's first step: mean(4, 4) = 4 and mean(4, 8) = 6, so the
				step takes 6 and the median of [6, 4] is 5. With an unreduced occurrence that alternative would be
				mean(8, 8) = 8 and the median 6"""
		result["X002"].abundance() == 5d
	}

	def "per-taxon --Xdistribute reports every taxon, each identical to running that taxon alone"() {

		setup:
		Map<String, Map<String, Double>> taxa = [
				T1: [K10001: 7d, K10002: 1d],
				T2: [K10010: 8d, K10013: 4d, K10015: 4d, K10016: 2d],
				T3: [K10001: 3d, K10002: 9d, K10010: 6d, K10016: 6d]]
		List<List> all = taxa.collectMany { taxon, abundances -> features(taxon, abundances) }

		when:
		options.setPerTaxon(true)
		SampleResults perTaxon = options.infer(all, database)

		then: "every taxon is present (before the fix: only the last taxon was kept)"
		perTaxon.all()*.taxon().toSet() == taxa.keySet()
		perTaxon.all().size() == taxa.size() * database.modules().size()

		and: "each taxon's results equal a separate run on that taxon's data"
		taxa.every { taxon, abundances ->
			options.setPerTaxon(false)
			Map<String, ModuleResult> alone = byId(options.infer(features(taxon, abundances), CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/xdistribute-modules.list")))))
			perTaxon.all().findAll { it.taxon() == taxon }.every {
				it.abundance() == alone[it.module()].abundance() && it.coverage() == alone[it.module()].coverage()
			}
		}
	}

	def "per-taxon --Xdistribute on the legacy taxon matrix reports every taxon and yields an automatic cutoff"() {

		setup:
		def gmms = CompiledDatabase.compile(ModuleDatabaseReader.read(Path.of("src/test/resources/module.list")))
		options.setPerTaxon(true)
		options.setCoverage(-1d)

		when:
		def results = options.infer(new File("src/test/resources/taxon_matrix.tsv"), InputFormat.TAXON_KO, gmms)
		def inference = results.samples().collectEntries { [it, new SampleResults(results, it)] }
		double cutoff = results.cutoff().orElseThrow()

		then: """every taxon is present in every sample. Before the fix, each sample held only the last taxon,
				whose modules all had coverage 0, so the automatic cutoff got an empty distribution and threw
				ArrayIndexOutOfBoundsException (reported by the CLI as a wrong --annotation value)"""
		inference.keySet() == ["S1", "S2"] as Set
		inference.values().every { sample ->
			sample.all()*.taxon().toSet() == ["Sp1", "Sp2", "Sp3"] as Set
		}
		cutoff > 0d
	}
}
