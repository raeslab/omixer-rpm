package io.omixer.rpm.output

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.TestResults
import spock.lang.Specification
import spock.lang.TempDir

class MatrixWriterSpec extends Specification {

	@TempDir
	Path tmp

	private List<String> abundance() {
		Files.readAllLines(tmp.resolve(MatrixWriter.ABUNDANCE_FILE))
	}

	private List<String> coverage() {
		Files.readAllLines(tmp.resolve(MatrixWriter.COVERAGE_FILE))
	}

	def "samples are columns in input order; modules are rows; an unreported module is 0.0"() {

		given: "samples and modules whose names a hash-based collection would reorder"
		List<String> modules = ["MF0012", "MF0003", "MF0101", "MF0040", "MF0007"]
		Map<String, List<List>> values = [:]
		["S3", "S1", "Z9", "S10", "A2"].eachWithIndex { sample, i ->
			values[sample] = modules.collect { [null, it, i as double, 1d] }
		}

		when:
		new MatrixWriter().write(TestResults.of(modules, values, false, 0.5d), tmp)

		then:
		abundance()[0] == "Module\tS3\tS1\tZ9\tS10\tA2"
		abundance().drop(1).collect { it.split("\t")[0] } == modules
		abundance()[1] == "MF0012\t0.0\t1.0\t2.0\t3.0\t4.0"
		coverage()[1] == "MF0012\t1.0\t1.0\t1.0\t1.0\t1.0"
	}

	def "modules are rows in the order they are first reported across samples, as in 1.x"() {

		given: "S1 reports only M3, S2 reports M1 and M3"
		def results = TestResults.of(["M1", "M2", "M3"],
				[S1: [[null, "M1", 5d, 0.2d], [null, "M3", 1d, 1d]], S2: [[null, "M1", 2d, 1d], [null, "M3", 3d, 1d]]],
				false, 0.5d)

		when:
		new MatrixWriter().write(results, tmp)

		then: "M1 is below the cutoff in S1, so M3 comes first; unreported cells are 0.0"
		abundance() == ["Module\tS1\tS2", "M3\t1.0\t3.0", "M1\t0.0\t2.0"]
		coverage() == ["Module\tS1\tS2", "M3\t1.0\t1.0", "M1\t0.0\t1.0"]
	}

	def "per taxon, a row is a taxon and a module; cells below the cutoff are 0.0"() {

		given:
		def results = TestResults.of(["M1", "M2", "M3"], [
				S1: [["Sp1", "M1", 1d, 0.7d], ["Sp2", "M1", 1d, 0.3d], ["Sp2", "M2", 1d, 0.7d], ["Sp2", "M3", 1d, 0.7d]],
				S2: [["Sp1", "M1", 2d, 0.7d], ["Sp2", "M1", 3d, 0.8d], ["Sp2", "M2", 1d, 0.3d]]], true, 0.6d)

		when:
		new MatrixWriter().write(results, tmp)

		then:
		abundance()[0] == "Taxon\tModule\tS1\tS2"
		abundance().drop(1) as Set == ["Sp1\tM1\t1.0\t2.0", "Sp2\tM1\t0.0\t3.0", "Sp2\tM2\t1.0\t0.0", "Sp2\tM3\t1.0\t0.0"] as Set
		coverage().drop(1) as Set == ["Sp1\tM1\t0.7\t0.7", "Sp2\tM1\t0.0\t0.8", "Sp2\tM2\t0.7\t0.0", "Sp2\tM3\t0.7\t0.0"] as Set
	}

	def "a taxon and a module whose names join to the same text as another pair keep their own values"() {

		given: "A + BM1 and AB + M1 both read ABM1"
		def results = TestResults.of(["BM1", "M1"], [S1: [["A", "BM1", 1d, 1d], ["AB", "M1", 2d, 0.5d]]], true, 0d)

		when:
		new MatrixWriter().write(results, tmp)

		then: "before, both shared one cell, so one of them was written as 0.0 (1.2: AB M1)"
		abundance().drop(1) as Set == ["A\tBM1\t1.0", "A\tM1\t0.0", "AB\tBM1\t0.0", "AB\tM1\t2.0"] as Set
		coverage().drop(1) as Set == ["A\tBM1\t1.0", "A\tM1\t0.0", "AB\tBM1\t0.0", "AB\tM1\t0.5"] as Set
	}

	def "each taxon's modules are rows in the order they are first reported"() {

		given:
		List<String> modules = ["MF0012", "MF0003", "MF0101", "MF0040", "MF0007"]
		def values = ["S3", "S1", "Z9", "S10", "A2"].collectEntries { [it, modules.collect { m -> ["Sp1", m, 1d, 1d] }] }

		when:
		new MatrixWriter().write(TestResults.of(modules, values, true, 0d), tmp)

		then:
		abundance()[0] == "Taxon\tModule\tS3\tS1\tZ9\tS10\tA2"
		abundance().drop(1).collect { it.split("\t")[1] } == modules
	}
}
