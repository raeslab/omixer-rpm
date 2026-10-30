package io.omixer.rpm

import io.omixer.rpm.domain.ModuleDatabase
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.engine.ProfilesBuilder
import io.omixer.rpm.engine.Results

/** Results with chosen values, for the writer specs */
class TestResults {

	/**
	 * @param modules the module ids, in database order
	 * @param values  sample → list of [taxon, module, abundance, coverage]; taxon null unless per taxon
	 */
	static Results of(List<String> modules, Map<String, List<List>> values, boolean perTaxon, double cutoff) {
		def builder = new ProfilesBuilder(CompiledDatabase.compile(new ModuleDatabase([])), perTaxon)
		values.each { sample, rows ->
			builder.sample(sample)
			rows.each { taxon, module, abundance, coverage -> builder.add(sample, taxon, "-", 0d) }
		}
		def profiles = builder.build()
		int size = profiles.size()
		int[][] observed = new int[size][]
		double[][] abundance = new double[size][]
		double[][] coverage = new double[size][]
		(0..<size).each { p ->
			def key = profiles.key(p)
			def rows = values[key.sample()].findAll { it[0] == key.taxon() }.sort { modules.indexOf(it[1]) }
			observed[p] = rows.collect { modules.indexOf(it[1]) } as int[]
			abundance[p] = rows.collect { it[2] as double } as double[]
			coverage[p] = rows.collect { it[3] as double } as double[]
		}
		new Results(profiles, modules, observed, abundance, coverage, OptionalDouble.of(cutoff), null)
	}
}
