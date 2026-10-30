package io.omixer.rpm.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.omixer.rpm.cutoff.Cutoff;

/**
 * The module results of every profile, and the coverage cutoff that decides which are reported. Only observed modules
 * (coverage &gt; 0) are stored; a module with coverage 0 has abundance 0 with every estimator, and is generated on the
 * fly. Immutable; made with a {@link Builder}.
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
	private final Cutoff cutoff;
	/** sample → its profiles, in profile order */
	private final Map<String, List<Integer>> sampleProfiles = new LinkedHashMap<>();

	private Results(Builder builder, Cutoff cutoff) {
		this.profiles = builder.profiles;
		this.modules = builder.modules;
		this.observed = builder.observed;
		this.abundance = builder.abundance;
		this.coverage = builder.coverage;
		this.cutoff = Objects.requireNonNull(cutoff, "cutoff");
		profiles.samples().forEach(sample -> sampleProfiles.put(sample, new ArrayList<>()));
		for (int profile = 0; profile < profiles.size(); profile++) {
			sampleProfiles.get(profiles.key(profile).sample()).add(profile);
		}
	}

	/**
	 * @param modules the module ids, in database order
	 * @return a builder that takes the observed modules of every profile
	 */
	public static Builder builder(Profiles profiles, List<String> modules) {
		return new Builder(profiles, modules);
	}

	/** @return every sample of the input, in input order */
	public List<String> samples() {
		return profiles.samples();
	}

	/** @return true when results are per taxon of a sample */
	public boolean perTaxon() {
		return profiles.perTaxon();
	}

	/** @return the coverage cutoff, and the distribution it was learned from when it was */
	public Cutoff cutoff() {
		return cutoff;
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
		if (cutoff.value().isEmpty()) {
			return List.of();
		}
		double minimum = cutoff.value().getAsDouble();
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

	/**
	 * Takes the observed modules of every profile, each once and in any order; different profiles may be given from
	 * different threads.
	 */
	public static final class Builder {

		private final Profiles profiles;
		private final List<String> modules;
		private final int[][] observed;
		private final double[][] abundance;
		private final double[][] coverage;

		private Builder(Profiles profiles, List<String> modules) {
			this.profiles = Objects.requireNonNull(profiles, "profiles");
			this.modules = List.copyOf(modules);
			this.observed = new int[profiles.size()][];
			this.abundance = new double[profiles.size()][];
			this.coverage = new double[profiles.size()][];
		}

		/**
		 * Takes ownership of the arrays.
		 *
		 * @param modules   the indexes of the observed modules of a profile, ascending
		 * @param abundance their abundance
		 * @param coverage  their coverage, each &gt; 0
		 * @throws IllegalArgumentException when the arrays break one of these rules
		 * @throws IllegalStateException    when the profile was given before
		 */
		public void profile(int profile, int[] modules, double[] abundance, double[] coverage) {
			if (abundance.length != modules.length || coverage.length != modules.length) {
				throw new IllegalArgumentException("Profile " + profile + ": " + modules.length + " modules, "
						+ abundance.length + " abundances and " + coverage.length + " coverages");
			}
			for (int i = 0; i < modules.length; i++) {
				if (modules[i] < 0 || modules[i] >= this.modules.size() || i > 0 && modules[i] <= modules[i - 1]) {
					throw new IllegalArgumentException("Profile " + profile + ": module indexes must be ascending and in [0, "
							+ this.modules.size() + "): " + Arrays.toString(modules));
				}
				// the negated check also rejects NaN
				if (!(coverage[i] > 0)) {
					throw new IllegalArgumentException("Profile " + profile + ": module " + this.modules.get(modules[i])
							+ " is stored with coverage " + coverage[i] + "; only observed modules are");
				}
			}
			if (observed[profile] != null) {
				throw new IllegalStateException("Profile " + profile + " was given before");
			}
			observed[profile] = modules;
			this.abundance[profile] = abundance;
			this.coverage[profile] = coverage;
		}

		/** @return the coverage of every observed module given so far, profile by profile */
		public double[] observedCoverages() {
			return Arrays.stream(coverage).filter(Objects::nonNull).flatMapToDouble(Arrays::stream).toArray();
		}

		/**
		 * @throws IllegalStateException when a profile was not given
		 */
		public Results build(Cutoff cutoff) {
			for (int profile = 0; profile < observed.length; profile++) {
				if (observed[profile] == null) {
					throw new IllegalStateException("Profile " + profile + " was not given");
				}
			}
			return new Results(this, cutoff);
		}
	}
}
