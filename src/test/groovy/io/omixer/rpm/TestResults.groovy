package io.omixer.rpm

import io.omixer.rpm.cutoff.CoverageCutoff
import io.omixer.rpm.cutoff.Cutoff
import io.omixer.rpm.domain.ModuleDatabase
import io.omixer.rpm.engine.CompiledDatabase
import io.omixer.rpm.engine.ProfilesBuilder
import io.omixer.rpm.engine.Results

/** Results with chosen values, for the writer specs */
class TestResults {

	/**
	 * @param modules the module ids, in database order
	 * @param values  sample → list of [taxon, module, abundance, coverage]; taxon null unless per taxon
	 * @param cutoff  a given coverage cutoff
	 */
	static Results of(List<String> modules, Map<String, List<List>> values, boolean perTaxon, double cutoff) {
		of(modules, values, perTaxon, new CoverageCutoff.Fixed(cutoff).resolve(new double[0]))
	}

	static Results of(List<String> modules, Map<String, List<List>> values, boolean perTaxon, Cutoff cutoff) {
		def builder = new ProfilesBuilder(CompiledDatabase.compile(new ModuleDatabase([])), perTaxon)
		values.each { sample, rows ->
			builder.sample(sample)
			rows.each { taxon, module, abundance, coverage -> builder.add(sample, taxon, "-", 0d) }
		}
		def profiles = builder.build()
		def results = Results.builder(profiles, modules)
		(0..<profiles.size()).each { p ->
			def key = profiles.key(p)
			def rows = values[key.sample()].findAll { it[0] == key.taxon() }.sort { modules.indexOf(it[1]) }
			results.profile(p, rows.collect { modules.indexOf(it[1]) } as int[], rows.collect { it[2] as double } as double[],
					rows.collect { it[3] as double } as double[])
		}
		results.build(cutoff)
	}
}
