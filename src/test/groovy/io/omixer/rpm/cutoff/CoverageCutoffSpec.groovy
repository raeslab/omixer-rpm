package io.omixer.rpm.cutoff

import spock.lang.Specification

class CoverageCutoffSpec extends Specification {

	static final double[] COVERAGES = CoverageDistributionSpec.coverages(CoverageDistributionSpec.COUNTS)

	def "a fixed cutoff is its value, learned from nothing"() {

		when:
		Cutoff cutoff = new CoverageCutoff.Fixed(0.3d).resolve(COVERAGES)

		then:
		cutoff.value() == OptionalDouble.of(0.3d)
		cutoff.learnedFrom().isEmpty()
	}

	def "the automatic cutoff is the knee of the coverage distribution, which it keeps"() {

		when:
		Cutoff cutoff = new CoverageCutoff.Automatic().resolve(COVERAGES)

		then:
		cutoff.value() == OptionalDouble.of(0.6d)
		cutoff.learnedFrom().get().knee() == OptionalDouble.of(0.6d)
	}

	def "without any observed module the automatic cutoff has no value and no distribution"() {

		when:
		Cutoff cutoff = new CoverageCutoff.Automatic().resolve(new double[0])

		then:
		cutoff.value().isEmpty()
		cutoff.learnedFrom().isEmpty()
	}

	def "a fixed cutoff outside [0, 1] is rejected"() {

		when:
		new CoverageCutoff.Fixed(value)

		then:
		thrown(IllegalArgumentException)

		where:
		value << [-0.1d, 1.1d, Double.NaN]
	}
}
