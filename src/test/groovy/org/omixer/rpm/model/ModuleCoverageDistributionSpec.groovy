package org.omixer.rpm.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.omixer.rpm.model.ModuleCoverageDistribution;

import spock.lang.Shared
import spock.lang.Specification

/**
 * 
 * @author omixer
 *
 * Created on Jul 11, 2018
 */
public class ModuleCoverageDistributionSpec extends Specification {

	@Shared ModuleCoverageDistribution distribution

	def setup() {
		Map<Double, Integer> coverageDistibution = [0.1d:10, 0.2d:5, 0.3d:10, 0.4d:20, 0.5d:30, 0.6d:10, 0.8d:20, 0.9d:10]
		
		List<Double> coverage = [];
		coverageDistibution.each {k, v -> 
			for (int i = 0; i < v; i++) {
				coverage.add(k);
			}
		}
		distribution = new ModuleCoverageDistribution(coverage);
	}

	def "findOptimalCoverage return 0.6 based on the highest drop"() {

		when: "optimal coverage is the value that gives the highest decrease in module count"
		double optimalcoverage = distribution.findOptimalCoverage();
		
		then: "coverage should be 0.6"
		optimalcoverage == 0.6
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
