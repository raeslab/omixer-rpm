package io.omixer.rpm.model.io

import io.omixer.rpm.model.Module
import io.omixer.rpm.model.Modules

import spock.lang.Specification

class ModuleMatrixWriterSpec extends Specification {

	def "writeCounts writes the samples as columns in the given order and the modules as rows in reference order"() {

		setup: "samples and modules whose names a hash-based collection would reorder"
		List<String> modules = ["MF0012", "MF0003", "MF0101", "MF0040", "MF0007"]
		Map<String, Modules> moduleInference = new LinkedHashMap<>()
		["S3", "S1", "Z9", "S10", "A2"].eachWithIndex { sample, i ->
			moduleInference[sample] = new Modules(modules.collect { new Module(it, null, 1d, i) }, 0d)
		}

		when:
		File out = File.createTempFile("ModuleMatrixWriterSpec", "out")
		new ModuleMatrixWriter().writeCounts(moduleInference, out)
		List<String> lines = out.readLines()

		then:
		lines[0] == "Module\tS3\tS1\tZ9\tS10\tA2"
		lines.drop(1).collect { it.split("\t")[0] } == modules
		lines[1] == "MF0012\t0.0\t1.0\t2.0\t3.0\t4.0"
	}
}
