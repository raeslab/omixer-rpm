package io.omixer.rpm.profile;

import java.util.List;

/**
 * The input, ready for scoring: profiles in input order (all profiles of a sample together), each holding only its
 * observed values. Unobserved values are 0 and generated on the fly. Immutable.
 */
public final class Profiles {

	/** A function of an observed value of KO index ko */
	@FunctionalInterface
	public interface KoValueFunction {
		double apply(int ko, double value);
	}

	private final int koCount;
	private final boolean perTaxon;
	private final List<String> samples;
	private final List<ProfileKey> keys;
	/** profile → KO indexes of its observed values, ascending */
	private final int[][] kos;
	/** profile → its observed values, matching kos */
	private final double[][] values;

	Profiles(int koCount, boolean perTaxon, List<String> samples, List<ProfileKey> keys, int[][] kos,
			double[][] values) {
		this.koCount = koCount;
		this.perTaxon = perTaxon;
		this.samples = List.copyOf(samples);
		this.keys = List.copyOf(keys);
		this.kos = kos;
		this.values = values;
	}

	/** @return true for one profile per taxon of a sample, false for one profile per sample */
	public boolean perTaxon() {
		return perTaxon;
	}

	/** @return every sample of the input in input order, also one without profiles (per taxon, nothing observed) */
	public List<String> samples() {
		return samples;
	}

	public int size() {
		return keys.size();
	}

	public ProfileKey key(int profile) {
		return keys.get(profile);
	}

	/** @return a profile's values, indexed by the KO indexes of the database; 0 where nothing was observed */
	public double[] values(int profile) {
		double[] dense = new double[koCount];
		for (int i = 0; i < kos[profile].length; i++) {
			dense[kos[profile][i]] = values[profile][i];
		}
		return dense;
	}

	/** @return a copy in which every observed value is replaced by function(KO index, value) */
	public Profiles map(KoValueFunction function) {
		double[][] mapped = new double[values.length][];
		for (int p = 0; p < values.length; p++) {
			mapped[p] = new double[values[p].length];
			for (int i = 0; i < values[p].length; i++) {
				mapped[p][i] = function.apply(kos[p][i], values[p][i]);
			}
		}
		return new Profiles(koCount, perTaxon, samples, keys, kos, mapped);
	}
}
