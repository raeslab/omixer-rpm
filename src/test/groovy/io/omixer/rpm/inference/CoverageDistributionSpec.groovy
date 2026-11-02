package io.omixer.rpm.inference

import spock.lang.Specification

class CoverageDistributionSpec extends Specification {

	/** coverage → number of observed modules with it */
	static final Map<Double, Integer> COUNTS = [0.1d: 10, 0.2d: 5, 0.3d: 10, 0.4d: 20, 0.5d: 30, 0.6d: 10, 0.8d: 20, 0.9d: 10]

	static double[] coverages(Map<Double, Integer> counts) {
		counts.collectMany { coverage, count -> [coverage] * count }.shuffled(new Random(1)) as double[]
	}

	def "the knee is the coverage at the highest jump in recruited modules"() {

		expect:
		new CoverageDistribution(coverages(COUNTS)).knee() == OptionalDouble.of(0.6d)
	}

	def "points go from the highest coverage down, with the modules recruited at or above each coverage"() {

		when:
		def points = new CoverageDistribution(coverages([0.5d: 3, 1d: 2, 0.25d: 4])).points()

		then:
		points == [new CoverageDistribution.Point(1d, 2d), new CoverageDistribution.Point(0.5d, 5d),
				new CoverageDistribution.Point(0.25d, 9d)]
	}

	def "on equal jumps the lower coverage is taken"() {

		expect: "jumps of 2 at 0.8 → 0.6 and at 0.6 → 0.4: the knee is 0.6, not 0.8"
		new CoverageDistribution(coverages([1d: 1, 0.8d: 1, 0.6d: 2, 0.4d: 2])).knee() == OptionalDouble.of(0.6d)
	}

	def "a single observed coverage is the knee"() {

		expect:
		new CoverageDistribution([0.5d, 0.5d] as double[]).knee() == OptionalDouble.of(0.5d)
	}

	def "a distribution without any observed module has no knee"() {

		when: "every module has coverage 0, e.g. an all-zero input"
		def empty = new CoverageDistribution(new double[0])

		then:
		empty.knee().isEmpty()
		empty.points().isEmpty()
	}
}
