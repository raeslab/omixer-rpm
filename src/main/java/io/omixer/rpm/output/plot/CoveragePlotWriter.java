package io.omixer.rpm.output.plot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.omixer.rpm.inference.CoverageDistribution;
import io.omixer.rpm.inference.Results;
import io.omixer.rpm.output.ResultWriter;

/**
 * Writes coverage-plot.svg: the coverage distribution an automatic cutoff was learned from, with the cutoff in green;
 * nothing for a given cutoff.
 */
public final class CoveragePlotWriter implements ResultWriter {

	public static final String PLOT_FILE = "coverage-plot.svg";

	private static final int CIRCLE_RADIUS = 4;

	@Override
	public void write(Results results, Path outputDir) throws IOException {
		Optional<CoverageDistribution> distribution = results.cutoff().learnedFrom();
		if (distribution.isPresent()) {
			Files.write(outputDir.resolve(PLOT_FILE), svg(distribution.get()).getBytes());
		}
	}

	/**
	 * @return the plot of a distribution with a knee: recruited modules (y) by coverage (x), scaled to the drawing area
	 */
	static String svg(CoverageDistribution distribution) {
		List<CoverageDistribution.Point> points = distribution.points();
		double knee = distribution.knee().orElseThrow();
		double xmax = points.get(0).coverage();
		double ymax = points.get(points.size() - 1).recruited();
		SVGDevice svg = new SVGDevice();
		for (int i = points.size() - 1; i > 0; i--) {
			svg.drawLine(x(points.get(i), xmax), y(points.get(i), ymax), x(points.get(i - 1), xmax),
					y(points.get(i - 1), ymax));
		}
		// as 1.x: the ticks are written in the iteration order of a HashMap
		Map<Double, Double> xTicks = new HashMap<>();
		Map<Double, Double> yTicks = new HashMap<>();
		for (int i = points.size() - 1; i >= 0; i--) {
			CoverageDistribution.Point point = points.get(i);
			double x = x(point, xmax);
			double y = y(point, ymax);
			svg.drawCircle(x, y, CIRCLE_RADIUS, Double.compare(point.coverage(), knee) == 0 ? "green" : "white");
			xTicks.put(x, point.coverage());
			yTicks.put(y, point.recruited());
		}
		svg.xaxis(xTicks);
		svg.yaxis(yTicks);
		return svg.toString();
	}

	private static double x(CoverageDistribution.Point point, double xmax) {
		return (point.coverage() / xmax) * SVGDevice.DRAWING_AREA_WIDTH;
	}

	private static double y(CoverageDistribution.Point point, double ymax) {
		return ((ymax - point.recruited()) / ymax) * SVGDevice.DRAWING_AREA_HEIGHT;
	}
}
