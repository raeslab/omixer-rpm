package io.omixer.rpm.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Collects the input into {@link Profiles}: one profile per sample, or per taxon of a sample in per-taxon runs. Only
 * observed values of KOs in the database are stored; every value still passes here, so that information on the rest
 * of the input (e.g. how much of it maps to the database) can be gathered in this one place.
 *
 * As in 1.x: duplicate values of a KO in a profile are summed in input order; a profile with no observed value is
 * kept (its modules score 0); the taxa of a sample follow the order of a HashMap of their names.
 */
public final class ProfilesBuilder implements ProfileSink {

	/** How many numeric function ids {@link #numericFunctions()} gives */
	private static final int NUMERIC_FUNCTION_EXAMPLES = 3;

	private final CompiledDatabase database;
	private final boolean perTaxon;
	/** sample → taxon (null unless per taxon) → observed values; samples in input order */
	private final Map<String, Map<String, Observations>> samples = new LinkedHashMap<>();
	private final Set<String> numericFunctions = new LinkedHashSet<>();
	/** Function ids already checked: NumberUtils.isCreatable throws and catches an exception for every non-number */
	private final Set<String> checkedFunctions = new HashSet<>();

	/**
	 * @param perTaxon true for one profile per taxon of a sample, false for one profile per sample (taxa summed)
	 */
	public ProfilesBuilder(CompiledDatabase database, boolean perTaxon) {
		this.database = database;
		this.perTaxon = perTaxon;
	}

	@Override
	public void sample(String sample) {
		Map<String, Observations> profiles = samples.computeIfAbsent(sample, s -> new HashMap<>());
		if (!perTaxon) {
			profiles.computeIfAbsent(null, t -> new Observations());
		}
	}

	@Override
	public void add(String sample, String taxon, String function, double value) {
		if (numericFunctions.size() < NUMERIC_FUNCTION_EXAMPLES && checkedFunctions.add(function)
				&& NumberUtils.isCreatable(function)) {
			numericFunctions.add(function);
		}
		Observations profile = samples.computeIfAbsent(sample, s -> new HashMap<>())
				.computeIfAbsent(perTaxon ? taxon : null, t -> new Observations());
		// an unobserved value is 0 anyway; a function outside the database is not scored
		int ko = database.indexOf(function);
		if (value != 0 && ko >= 0) {
			profile.add(ko, value);
		}
	}

	/**
	 * @return up to 3 function ids that read as numbers, in input order: an abundance column read as the function
	 *         column, i.e. the wrong input annotation format, gives such ids
	 */
	public List<String> numericFunctions() {
		return List.copyOf(numericFunctions);
	}

	public Profiles build() {
		List<ProfileKey> keys = new ArrayList<>();
		List<int[]> kos = new ArrayList<>();
		List<double[]> values = new ArrayList<>();
		samples.forEach((sample, profiles) -> profiles.forEach((taxon, observations) -> {
			keys.add(new ProfileKey(sample, taxon));
			observations.merge(kos, values);
		}));
		return new Profiles(database.koCount(), perTaxon, new ArrayList<>(samples.keySet()), keys,
				kos.toArray(int[][]::new), values.toArray(double[][]::new));
	}

	/** The observed values of one profile, in input order */
	private static final class Observations {

		private int[] kos = new int[16];
		private double[] values = new double[16];
		private int size;

		void add(int ko, double value) {
			if (size == kos.length) {
				kos = Arrays.copyOf(kos, size * 2);
				values = Arrays.copyOf(values, size * 2);
			}
			kos[size] = ko;
			values[size] = value;
			size++;
		}

		/** Appends one value per KO, ascending, summing the values of a KO in input order (the first as is) */
		void merge(List<int[]> mergedKos, List<double[]> mergedValues) {
			// KO in the high bits, input position in the low bits: sorting gives KO order, then input order
			long[] order = new long[size];
			for (int i = 0; i < size; i++) {
				order[i] = (long) kos[i] << 32 | i;
			}
			Arrays.sort(order);
			int[] uniqueKos = new int[size];
			double[] sums = new double[size];
			int unique = -1;
			for (long entry : order) {
				int ko = (int) (entry >>> 32);
				double value = values[(int) entry];
				if (unique >= 0 && uniqueKos[unique] == ko) {
					sums[unique] += value;
				} else {
					unique++;
					uniqueKos[unique] = ko;
					sums[unique] = value;
				}
			}
			mergedKos.add(Arrays.copyOf(uniqueKos, unique + 1));
			mergedValues.add(Arrays.copyOf(sums, unique + 1));
		}
	}
}
