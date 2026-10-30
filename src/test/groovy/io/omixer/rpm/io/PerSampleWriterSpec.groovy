package io.omixer.rpm.io

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.TestResults
import spock.lang.Specification
import spock.lang.TempDir

class PerSampleWriterSpec extends Specification {

	@TempDir
	Path tmp

	def "one file per sample with its reported modules, also for a sample that reports none"() {

		given:
		def results = TestResults.of(["M1", "M2"], [S1: [[null, "M1", 2.5d, 1d], [null, "M2", 1d, 0.25d]], S2: []],
				false, 0.5d)

		when:
		new PerSampleWriter().write(results, tmp)

		then:
		Files.readAllLines(tmp.resolve("S1.modules")) == ["Module\tValue\tCoverage", "M1\t2.5\t1.0"]
		Files.readAllLines(tmp.resolve("S2.modules")) == ["Module\tValue\tCoverage"]
	}

	def "per taxon, the files have a taxon column"() {

		given:
		def results = TestResults.of(["M1"], [S1: [["Sp1", "M1", 2d, 1d]]], true, 0.5d)

		when:
		new PerSampleWriter().write(results, tmp)

		then:
		Files.readAllLines(tmp.resolve("S1.modules")) == ["Module\tTaxon\tValue\tCoverage", "M1\tSp1\t2.0\t1.0"]
	}
}
