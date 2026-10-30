package io.omixer.rpm

import io.omixer.rpm.inference.ModuleResult
import io.omixer.rpm.inference.Results

/** The results of one sample */
class SampleResults {

	/** The sample name of a single-sample input */
	static final String SAMPLE = "S"

	final Results results
	final String sample

	SampleResults(Results results, String sample) {
		this.results = results
		this.sample = sample
	}

	/** @return every module of every profile of the sample */
	List<ModuleResult> all() {
		results.results(sample)
	}

	/** @return the modules at or above the cutoff */
	List<ModuleResult> reported() {
		results.reported(sample)
	}

	/** @return every module of the sample by module id (single profile) */
	Map<String, ModuleResult> byModule() {
		all().collectEntries { [it.module(), it] }
	}
}
