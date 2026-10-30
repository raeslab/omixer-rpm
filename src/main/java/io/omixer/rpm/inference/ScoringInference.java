package io.omixer.rpm.inference;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import io.omixer.rpm.cutoff.CoverageCutoff;
import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.engine.CompiledModule;
import io.omixer.rpm.engine.Profiles;
import io.omixer.rpm.engine.Results;
import io.omixer.rpm.scoring.ModuleScore;
import io.omixer.rpm.scoring.ModuleScorer;
import io.omixer.rpm.scoring.ScoringOptions;
import io.omixer.rpm.scoring.SharedKoResolution;

/**
 * Infers modules by scoring: resolves shared KOs, scores every module of every profile with a {@link ModuleScorer},
 * then resolves the coverage cutoff from every observed coverage.
 */
public final class ScoringInference implements ModuleInference {

	private final ScoringOptions scoring;
	private final SharedKoResolution resolution;
	private final CoverageCutoff cutoff;
	private final int threads;

	/**
	 * @param threads the number of threads scoring profiles in parallel (-t); 1 scores them in turn
	 */
	public ScoringInference(ScoringOptions scoring, SharedKoResolution resolution, CoverageCutoff cutoff, int threads) {
		this.scoring = Objects.requireNonNull(scoring, "scoring");
		this.resolution = Objects.requireNonNull(resolution, "resolution");
		this.cutoff = Objects.requireNonNull(cutoff, "cutoff");
		if (threads < 1) {
			throw new IllegalArgumentException("threads must be at least 1, not " + threads);
		}
		this.threads = threads;
	}

	@Override
	public Results infer(CompiledDatabase database, Profiles profiles) {
		Profiles resolved = resolution.apply(profiles, database);
		ModuleScorer scorer = new ModuleScorer(scoring);
		List<CompiledModule> modules = database.modules();
		Results.Builder results = Results.builder(profiles, modules.stream().map(CompiledModule::id).toList());
		// each profile is given once, so profiles can be scored in parallel without locks
		forEachProfile(profiles.size(), profile -> {
			double[] values = resolved.values(profile);
			int[] indexes = new int[modules.size()];
			double[] abundances = new double[modules.size()];
			double[] coverages = new double[modules.size()];
			int count = 0;
			for (int module = 0; module < modules.size(); module++) {
				ModuleScore score = scorer.score(modules.get(module), values);
				// coverage 0 means no observed step, and so abundance 0: not stored
				if (score.coverage() > 0) {
					indexes[count] = module;
					abundances[count] = score.abundance();
					coverages[count] = score.coverage();
					count++;
				}
			}
			results.profile(profile, Arrays.copyOf(indexes, count), Arrays.copyOf(abundances, count),
					Arrays.copyOf(coverages, count));
		});
		return results.build(cutoff.resolve(results.observedCoverages()));
	}

	private void forEachProfile(int size, IntConsumer action) {
		if (threads == 1) {
			IntStream.range(0, size).forEach(action);
			return;
		}
		ForkJoinPool pool = new ForkJoinPool(threads);
		try {
			pool.submit(() -> IntStream.range(0, size).parallel().forEach(action)).join();
		} finally {
			pool.shutdown();
		}
	}
}
