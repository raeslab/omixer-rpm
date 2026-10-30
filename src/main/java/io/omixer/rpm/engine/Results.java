package io.omixer.rpm.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import io.omixer.rpm.cutoff.CoverageDistribution;

/**
 * The module results of every profile, and the coverage cutoff that decides which are reported. Only observed modules
 * (coverage &gt; 0) are stored; a module with coverage 0 has abundance 0 with every estimator, and is generated on the
 * fly. Immutable.
 */
public final class Results {

	private final Profiles profiles;
	/** module ids, in database order */
	private final List<String> modules;
	/** profile → indexes of its observed modules, ascending */
	private final int[][] observed;
	/** profile → abundance of its observed modules */
	private final double[][] abundance;
	/** profile → coverage of its observed modules */
	private final double[][] coverage;
	private final OptionalDouble cutoff;
	private final CoverageDistribution coverageDistribution;
	/** sample → its profiles, in profile order */
	private final Map<String, List<Integer>> sampleProfiles = new LinkedHashMap<>();

	/**
	 * Takes ownership of the arrays.
	 *
	 * @param cutoff               the coverage cutoff, or empty when none could be learned (nothing is reported)
	 * @param coverageDistribution the distribution the cutoff was learned from, or null for a given cutoff
	 */
	public Results(Profiles profiles, List<String> modules, int[][] observed, double[][] abundance,
			double[][] coverage, OptionalDouble cutoff, CoverageDistribution coverageDistribution) {
		this.profiles = profiles;
		this.modules = List.copyOf(modules);
		this.observed = observed;
		this.abundance = abundance;
		this.coverage = coverage;
		this.cutoff = cutoff;
		this.coverageDistribution = coverageDistribution;
		profiles.samples().forEach(sample -> sampleProfiles.put(sample, new ArrayList<>()));
		for (int profile = 0; profile < profiles.size(); profile++) {
			sampleProfiles.get(profiles.key(profile).sample()).add(profile);
		}
	}

	/** @return every sample of the input, in input order */
	public List<String> samples() {
		return profiles.samples();
	}

	/** @return true when results are per taxon of a sample */
	public boolean perTaxon() {
		return profiles.perTaxon();
	}

	/** @return the coverage cutoff, or empty when none could be learned */
	public OptionalDouble cutoff() {
		return cutoff;
	}

	/** @return the distribution the cutoff was learned from, when it was */
	public Optional<CoverageDistribution> coverageDistribution() {
		return Optional.ofNullable(coverageDistribution);
	}

	/**
	 * @return every module of every profile of a sample: profiles in order, then modules in database order
	 */
	public List<ModuleResult> results(String sample) {
		List<ModuleResult> results = new ArrayList<>();
		for (int profile : sampleProfiles.get(sample)) {
			int next = 0;
			for (int module = 0; module < modules.size(); module++) {
				if (next < observed[profile].length && observed[profile][next] == module) {
					results.add(result(profile, module, abundance[profile][next], coverage[profile][next]));
					next++;
				} else {
					results.add(result(profile, module, 0d, 0d));
				}
			}
		}
		return results;
	}

	/**
	 * @return the results of a sample with a coverage at or above the cutoff, in the order of
	 *         {@link #results(String)}; none when there is no cutoff
	 */
	public List<ModuleResult> reported(String sample) {
		if (cutoff.isEmpty()) {
			return List.of();
		}
		double minimum = cutoff.getAsDouble();
		if (minimum <= 0) {
			// unobserved modules are reported too
			return results(sample).stream().filter(result -> result.coverage() >= minimum).toList();
		}
		List<ModuleResult> reported = new ArrayList<>();
		for (int profile : sampleProfiles.get(sample)) {
			for (int i = 0; i < observed[profile].length; i++) {
				if (coverage[profile][i] >= minimum) {
					reported.add(result(profile, observed[profile][i], abundance[profile][i], coverage[profile][i]));
				}
			}
		}
		return reported;
	}

	private ModuleResult result(int profile, int module, double abundance, double coverage) {
		ProfileKey key = profiles.key(profile);
		return new ModuleResult(key.sample(), key.taxon(), modules.get(module), abundance, coverage);
	}
}
