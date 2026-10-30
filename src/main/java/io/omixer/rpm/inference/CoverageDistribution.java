package io.omixer.rpm.inference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

/**
 * The distribution of the coverage of every observed module (coverage &gt; 0) in every profile: the number of modules
 * recruited at each coverage cutoff, from the highest coverage down. Its knee, the coverage at the highest jump in
 * recruited modules, is the automatic cutoff. Immutable.
 */
public final class CoverageDistribution {

	/**
	 * @param coverage  an observed coverage
	 * @param recruited the number of observed modules with this coverage or a higher one
	 */
	public record Point(double coverage, double recruited) {
	}

	/** highest coverage first */
	private final List<Point> points;
	private final OptionalDouble knee;

	/**
	 * @param coverages the coverage of every observed module, in any order
	 */
	public CoverageDistribution(double[] coverages) {
		Map<Double, Integer> counts = new TreeMap<>(Comparator.reverseOrder());
		for (double coverage : coverages) {
			counts.merge(coverage, 1, Integer::sum);
		}
		List<Point> points = new ArrayList<>(counts.size());
		double recruited = 0;
		for (Map.Entry<Double, Integer> count : counts.entrySet()) {
			recruited += count.getValue();
			points.add(new Point(count.getKey(), recruited));
		}
		this.points = List.copyOf(points);
		this.knee = knee(this.points);
	}

	/**
	 * The highest coverage, unless a later jump in recruited modules is at least as high as every jump before it: on
	 * equal jumps the lower coverage is taken (remain stringent, but include a more aggressive coverage).
	 */
	private static OptionalDouble knee(List<Point> points) {
		if (points.isEmpty()) {
			return OptionalDouble.empty();
		}
		double knee = points.get(0).coverage();
		if (points.size() > 1) {
			double jump = points.get(1).recruited() - points.get(0).recruited();
			for (int i = 1; i < points.size() - 1; i++) {
				double next = points.get(i + 1).recruited() - points.get(i).recruited();
				if (next >= jump) {
					jump = next;
					knee = points.get(i).coverage();
				}
			}
		}
		return OptionalDouble.of(knee);
	}

	/** @return the observed coverages with their recruited modules, highest coverage first; empty without any */
	public List<Point> points() {
		return points;
	}

	/** @return the coverage at the highest jump in recruited modules; empty when no module was observed */
	public OptionalDouble knee() {
		return knee;
	}
}
