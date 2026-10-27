package io.omixer.rpm.service

import io.omixer.rpm.model.BasicFeature
import io.omixer.rpm.model.Module
import io.omixer.rpm.model.ModuleCoverageDistribution
import io.omixer.rpm.model.ModuleInferenceOptions
import io.omixer.rpm.model.Modules
import io.omixer.rpm.model.Ortholog
import io.omixer.rpm.model.enums.ModuleInferenceOptimizers
import io.omixer.rpm.parsers.ModuleParser
import io.omixer.rpm.service.impl.ModuleManagerImpl

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

	def manager
	def referenceModules
	def options

	def setup() {
		manager = new ModuleManagerImpl()
		referenceModules = ModuleParser.parseModuleFile(new File("src/test/resources/xdistribute-modules.list"))
		options = new ModuleInferenceOptions()
		options.setAlgorithm(ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED.displayName())
		options.setCoverage(0d)
		options.setNormalizeByLength(false)
		options.setDistributeOrhologAbundance(true)
	}

	private static List<BasicFeature> features(String taxon, Map<String, Double> abundances) {
		abundances.collect { ko, value -> new BasicFeature(taxon, ko, value) }
	}

	private static Map<String, Module> byId(Modules modules) {
		modules.getModules().collectEntries { [(it.getModuleId()): it] }
	}

	def "a KO repeated inside one module, and used by no other module, is not split"() {

		when:
		Map<String, Module> result = byId(manager.inferModulesWithDistributedOrthologAbundance(
				features(null, [K10001: 7d, K10002: 1d]), options, referenceModules))

		then: "steps are 7, 1, 7, so the median is 7 (before the fix: K10001 was split against itself and the median was 3.5)"
		result["X001"].getCount() == 7d
		result["X001"].getCoverage() == 1d
	}

	def "a KO repeated inside one module and used by one other module is split in two, at every occurrence"() {

		when:
		Map<String, Module> result = byId(manager.inferModulesWithDistributedOrthologAbundance(
				features(null, [K10010: 8d, K10013: 4d, K10014: 4d, K10015: 4d, K10016: 4d]), options, referenceModules))

		then: """K10010 is used by 2 modules (X002, X003), so each gets 8/2 = 4, in both alternatives of X002.
				X002: step 1 = mean(4, 4) = 4 in either alternative, step 2 = 4 -> 4.
				X003: steps 4, 4 -> 4.
				Before the fix: N counted 3 occurrences (8/3), only X002's last alternative was reduced,
				so X002 picked the unreduced alternative (5.0) and X003 got 3.33."""
		result["X002"].getCount() == 4d
		result["X003"].getCount() == 4d
	}

	def "every occurrence of a shared KO in the chosen path carries the reduced value"() {

		when:
		Map<String, Module> result = byId(manager.inferModulesWithDistributedOrthologAbundance(
				features(null, [K10010: 8d, K10013: 4d, K10014: 4d, K10015: 4d, K10016: 4d]), options, referenceModules))

		then:
		List<Ortholog> chosen = result["X002"].getOrthologs().collectMany { step -> step.flatten() } as List<Ortholog>
		chosen.findAll { it.getOrthologId() == "K10010" }*.getCount().every { it == 4d }
	}

	def "per-taxon --Xdistribute reports every taxon, each identical to running that taxon alone"() {

		setup:
		Map<String, Map<String, Double>> taxa = [
				T1: [K10001: 7d, K10002: 1d],
				T2: [K10010: 8d, K10013: 4d, K10015: 4d, K10016: 2d],
				T3: [K10001: 3d, K10002: 9d, K10010: 6d, K10016: 6d]]
		List<BasicFeature> all = taxa.collectMany { taxon, abundances -> features(taxon, abundances) }

		when:
		options.setPerTaxon(true)
		Modules perTaxon = manager.inferModules(all, options, referenceModules)

		then: "every taxon is present (before the fix: only the last taxon was kept)"
		perTaxon.getModules()*.getTaxon().toSet() == taxa.keySet()
		perTaxon.getModules().size() == taxa.size() * referenceModules.size()

		and: "each taxon's results equal a separate run on that taxon's data"
		taxa.every { taxon, abundances ->
			options.setPerTaxon(false)
			Map<String, Module> alone = byId(manager.inferModules(features(taxon, abundances), options,
					ModuleParser.parseModuleFile(new File("src/test/resources/xdistribute-modules.list"))))
			perTaxon.getModules().findAll { it.getTaxon() == taxon }.every {
				it.getCount() == alone[it.getModuleId()].getCount() && it.getCoverage() == alone[it.getModuleId()].getCoverage()
			}
		}
	}

	def "The --Xdistribute leaves the reference modules it was given unchanged"() {

		setup:
		def before = referenceModules.collect { it.getOrthologs().toString() }

		when:
		manager.inferModulesWithDistributedOrthologAbundance(
				features(null, [K10010: 8d, K10013: 4d, K10015: 4d, K10016: 4d]), options, referenceModules)

		then: "no alternative was collapsed and no abundance was written into the references"
		referenceModules.collect { it.getOrthologs().toString() } == before
		referenceModules.every { m -> m.getFlatOrthologs().every { it.getCount() == 0d } }
	}

	def "per-taxon --Xdistribute on the legacy taxon matrix reports every taxon and yields an automatic cutoff"() {

		setup:
		def gmms = ModuleParser.parseModuleFile(new File("src/test/resources/module.list"))
		manager.setInputFormat("2")
		options.setPerTaxon(true)

		when:
		def inference = manager.inferModules(new File("src/test/resources/taxon_matrix.tsv"), options, gmms)
		Modules all = new Modules()
		inference.values().each { all.addModules(it) }
		Double cutoff = new ModuleCoverageDistribution(all).findOptimalCoverage()

		then: """every taxon is present in every sample. Before the fix, each sample held only the last taxon,
				whose modules all had coverage 0, so the automatic cutoff got an empty distribution and threw
				ArrayIndexOutOfBoundsException (reported by the CLI as a wrong --annotation value)"""
		inference.keySet() == ["S1", "S2"] as Set
		inference.values().every { sample ->
			sample.getModules()*.getTaxon().toSet() == ["Sp1", "Sp2", "Sp3"] as Set
		}
		cutoff > 0d
	}
}
