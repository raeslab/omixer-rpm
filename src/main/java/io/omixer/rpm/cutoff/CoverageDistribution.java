package io.omixer.rpm.cutoff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.omixer.utils.utils.MathUtils;

/**
 * The distribution of the coverage of every observed module (coverage &gt; 0) in every profile: the number of modules
 * recruited at each coverage cutoff, from the highest coverage down. The automatic cutoff is the coverage at the highest
 * jump in recruited modules (the knee).
 */
public final class CoverageDistribution {

	// Module count for each observed coverage
	private List<Double> coverage;
	// the observed coverage values, highest first
	private List<Double> sortedCoverageValues;
	// Total number of recruited modules at each coverage cutoff (Total, Total -
	// first cutoff, and so on)
	private double[] accumulator;
	private Double optimalCoverage = null;

	/**
	 * @param coverage the coverage of every observed module, in any order
	 */
	public CoverageDistribution(List<Double> coverage) {
		this.coverage = coverage;
		init();
	}

	private void init() {

		Map<Double, Integer> coverageIncidence = coverage.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.reducing(0, e -> 1, Integer::sum)));
		
		sortedCoverageValues = new ArrayList<>(coverageIncidence.keySet());
		Collections.sort(sortedCoverageValues, new Comparator<Double>() {

			public int compare(Double arg0, Double arg1) {
				return -Double.compare(arg0, arg1);
			}
		});

		Integer[] sortedCoverageIncidence = new Integer[sortedCoverageValues.size()];
		for (int i = 0; i < sortedCoverageIncidence.length; i++) {
			sortedCoverageIncidence[i] = coverageIncidence.get(sortedCoverageValues.get(i));
		}

		// no observed module (e.g. an all-zero input): nothing to accumulate
		accumulator = sortedCoverageIncidence.length == 0 ? new double[0]
				: MathUtils.accumulate(sortedCoverageIncidence);
	}

	/**
	 * @return the coverage cutoff at the highest drop in recruited modules, or empty when no module was observed
	 *         (every coverage is 0), in which case there is no cutoff to learn
	 */
	public Optional<Double> findOptimalCoverage() {
		if (sortedCoverageValues.isEmpty()) {
			return Optional.empty();
		}
		if (optimalCoverage == null) {

			optimalCoverage = sortedCoverageValues.get(0);

			if (accumulator.length > 1) {
				double increase = accumulator[1] - accumulator[0];
				// find highest drop then return optimal
				for (int i = 1; i < accumulator.length - 1; i++) {
					double newIncrease = accumulator[i + 1] - accumulator[i];
					// remain stringent and include more aggressive coverage
					if (newIncrease >= increase) {
						increase = newIncrease;
						optimalCoverage = sortedCoverageValues.get(i);
					}
				}
			}
		}
		return Optional.of(optimalCoverage);
	}

	/**
	 * @throws IllegalStateException when no module was observed: there is no distribution to plot
	 */
	public String toSVG() {

		Double optimalCoverage = findOptimalCoverage()
				.orElseThrow(() -> new IllegalStateException("No module observed: no coverage distribution to plot"));
		// scale the values to the device length
		int width = 820;
		int height = 620;
		double xmax = sortedCoverageValues.get(0);
		double ymax = accumulator[accumulator.length - 1];
		int circleRadius = 4;

		SVGDevice svgDevice = new SVGDevice(width, height);
		int drawingAreaWidth = svgDevice.getDrawingAreaWidth();
		int drawingAreaHeight = svgDevice.getDrawingAreaHeight();

		
		for (int i = accumulator.length - 1; i > 0; i--) {

			double scaledX1 = (sortedCoverageValues.get(i) / xmax) * drawingAreaWidth;
			double scaledY1 = ((ymax - accumulator[i]) / ymax) * drawingAreaHeight;

			double scaledX2 = (sortedCoverageValues.get(i - 1) / xmax) * drawingAreaWidth;
			double scaledY2 = ((ymax - accumulator[i - 1]) / ymax) * drawingAreaHeight;

			svgDevice.drawLine(scaledX1, scaledY1, scaledX2, scaledY2);
		}

		Map<Double, Double> xTicks = new HashMap<>(); 
		Map<Double, Double> yTicks = new HashMap<>();
		for (int i = accumulator.length - 1; i >= 0; i--) {

			String fill = "white";
			if (Double.compare(sortedCoverageValues.get(i), optimalCoverage) == 0) {
				fill = "green";
			}
			double x = (sortedCoverageValues.get(i) / xmax) * drawingAreaWidth;
			double y = ((ymax - accumulator[i]) / ymax) * drawingAreaHeight;
			svgDevice.drawCircle(x,
					y, circleRadius, fill);
			xTicks.put(x, sortedCoverageValues.get(i));
			yTicks.put(y, accumulator[i]);
		}
		
		svgDevice.xaxis(xTicks);
		svgDevice.yaxis(yTicks);

		return svgDevice.toString();
	}
}