package io.omixer.rpm.output.plot

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.TestResults
import io.omixer.rpm.inference.CoverageCutoff
import io.omixer.rpm.inference.CoverageDistribution
import io.omixer.rpm.inference.CoverageDistributionSpec
import spock.lang.Specification
import spock.lang.TempDir

class CoveragePlotWriterSpec extends Specification {

	@TempDir
	Path tmp

	static final double[] COVERAGES = CoverageDistributionSpec.coverages(CoverageDistributionSpec.COUNTS)

	def "the plot is the same as in 1.x, byte for byte"() {

		expect: "a decreasing line with the knee (0.6) in green"
		CoveragePlotWriter.svg(new CoverageDistribution(COVERAGES)) ==
				new String(Files.readAllBytes(Path.of("src/test/resources/coverage-distribution-plot.svg")))
	}

	def "a learned cutoff is plotted from the distribution it was learned from"() {

		given:
		def cutoff = new CoverageCutoff.Automatic().resolve(COVERAGES)

		when:
		new CoveragePlotWriter().write(TestResults.of(["M1"], [S1: [[null, "M1", 1d, 1d]]], false, cutoff), tmp)

		then:
		Files.readString(tmp.resolve(CoveragePlotWriter.PLOT_FILE)) == CoveragePlotWriter.svg(cutoff.learnedFrom().get())
	}

	def "nothing is plotted for a given cutoff, or when no cutoff could be learned"() {

		when:
		new CoveragePlotWriter().write(TestResults.of(["M1"], [S1: []], false, cutoff.resolve(coverages)), tmp)

		then:
		Files.notExists(tmp.resolve(CoveragePlotWriter.PLOT_FILE))

		where:
		cutoff                          | coverages
		new CoverageCutoff.Fixed(0.5d)  | COVERAGES
		new CoverageCutoff.Automatic()  | new double[0]
	}
}
