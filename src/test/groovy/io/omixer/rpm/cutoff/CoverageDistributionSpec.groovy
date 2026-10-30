package io.omixer.rpm.cutoff


import spock.lang.Shared
import spock.lang.Specification

/**
 * 
 * @author omixer
 *
 * Created on Jul 11, 2018
 */
public class CoverageDistributionSpec extends Specification {

	@Shared CoverageDistribution distribution

	def setup() {
		Map<Double, Integer> coverageDistibution = [0.1d:10, 0.2d:5, 0.3d:10, 0.4d:20, 0.5d:30, 0.6d:10, 0.8d:20, 0.9d:10]
		
		List<Double> coverage = [];
		coverageDistibution.each {k, v -> 
			for (int i = 0; i < v; i++) {
				coverage.add(k);
			}
		}
		distribution = new CoverageDistribution(coverage);
	}

	def "findOptimalCoverage return 0.6 based on the highest drop"() {

		when: "optimal coverage is the value that gives the highest decrease in module count"
		Optional<Double> optimalcoverage = distribution.findOptimalCoverage();

		then: "coverage should be 0.6"
		optimalcoverage == Optional.of(0.6d)
	}

	def "a distribution without any observed module has no optimal coverage"() {

		when: "every module has coverage 0, e.g. an all-zero input"
		def empty = new CoverageDistribution([])

		then: "no cutoff instead of an ArrayIndexOutOfBoundsException"
		empty.findOptimalCoverage() == Optional.empty()
	}

	def "toSVG produces the same plot as the predefined distribution plot"() throws IOException {
		
		when: "optimal coverage is computed and the result is returned in SVG"
		String plot = distribution.toSVG();
		println(plot);
		String expectedSVG  = new String(new File("src/test/resources/coverage-distribution-plot.svg").readBytes())
		
		then: "expect a monotonically decreasing line with the highest dicrease at 0.6" 
		expectedSVG ==  plot
	}
}
