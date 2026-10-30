package io.omixer.rpm.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.engine.CompiledModule;
import io.omixer.rpm.engine.Profiles;
import io.omixer.rpm.engine.ProfilesBuilder;
import io.omixer.rpm.io.DirectoryReader;
import io.omixer.rpm.io.InputFormat;
import io.omixer.rpm.io.MatrixReader;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.ModuleInferenceOptions;
import io.omixer.rpm.model.Modules;
import io.omixer.rpm.scoring.ModuleScore;
import io.omixer.rpm.scoring.ModuleScorer;
import io.omixer.rpm.service.ModuleManager;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 *
 * @author <a href="mailto:youssef.darzi@omixer.io">Youssef Darzi</a>
 *
 */
public class ModuleManagerImpl implements ModuleManager {

	protected final Log log = LogFactory.getLog(getClass());

	private InputFormat inputFormat = InputFormat.KO;
	boolean concurrent;

	public boolean isConcurrent() {
		return concurrent;
	}

	public InputFormat getInputFormat() {
		return inputFormat;
	}

	public void setConcurrent(boolean concurrent) {
		this.concurrent = concurrent;
	}

	public void setInputFormat(InputFormat inputFormat) {
		this.inputFormat = inputFormat;
	}

	public Map<String, Modules> inferModules(File input, ModuleInferenceOptions options,
			CompiledDatabase database) throws IncorrectNumberOfEntriesException, IOException {

		long start = System.currentTimeMillis();
		ProfilesBuilder builder = new ProfilesBuilder(database, options.isPerTaxon());
		if (input.isDirectory()) {
			new DirectoryReader(getInputFormat()).read(input.toPath(), builder);
		} else {
			new MatrixReader(getInputFormat()).read(input.toPath(), builder);
		}
		log.debug("Loaded samples in " + ((System.currentTimeMillis() - start) / 1000));
		warnAboutNumericFunctions(builder.numericFunctions());

		start = System.currentTimeMillis();
		Map<String, Modules> moduleInference = inferModules(builder.build(), options, database);
		log.debug("Inferred modules in " + ((System.currentTimeMillis() - start) / 1000));
		return moduleInference;
	}

	public Map<String, Modules> inferModules(Profiles profiles, ModuleInferenceOptions options,
			CompiledDatabase database) {
		Profiles resolved = options.getSharedKoResolution().apply(profiles, database);
		ModuleScorer scorer = new ModuleScorer(options.getScoringOptions());
		// sample => its profiles: the sample, or its taxa
		Map<String, List<Integer>> sampleProfiles = new LinkedHashMap<>();
		profiles.samples().forEach(sample -> sampleProfiles.put(sample, new ArrayList<>()));
		for (int profile = 0; profile < profiles.size(); profile++) {
			sampleProfiles.get(profiles.key(profile).sample()).add(profile);
		}
		Stream<Map.Entry<String, List<Integer>>> samples = isConcurrent() ? sampleProfiles.entrySet().parallelStream()
				: sampleProfiles.entrySet().stream();
		// collecting an ordered stream into a LinkedHashMap keeps the input sample order, also in parallel
		return samples.collect(Collectors.toMap(Map.Entry::getKey,
				entry -> sampleModules(entry.getValue(), resolved, options, database, scorer), (first, second) -> first,
				LinkedHashMap::new));
	}

	/** The modules of one sample: of the sample's profile, or of each of its taxa */
	private Modules sampleModules(List<Integer> profiles, Profiles resolved, ModuleInferenceOptions options,
			CompiledDatabase database, ModuleScorer scorer) {
		if (!options.isPerTaxon()) {
			return score(profiles.get(0), resolved, options, database, scorer);
		}
		Modules allModules = new Modules();
		allModules.setCoverageCutoff(options.getCoverage());
		for (int profile : profiles) {
			allModules.addModules(score(profile, resolved, options, database, scorer));
		}
		return allModules;
	}

	/** Scores every module of the database in one profile (a sample, or a taxon of a sample) */
	private Modules score(int profile, Profiles resolved, ModuleInferenceOptions options, CompiledDatabase database,
			ModuleScorer scorer) {
		double[] values = resolved.values(profile);
		String taxon = resolved.key(profile).taxon();
		List<Module> modules = new ArrayList<>(database.modules().size());
		for (CompiledModule module : database.modules()) {
			ModuleScore score = scorer.score(module, values);
			modules.add(new Module(module.id(), taxon, score.coverage(), score.abundance()));
		}
		return new Modules(modules, options.getCoverage());
	}

	/**
	 * Function ids that read as numbers usually mean an abundance column was read as the function column, i.e. the
	 * wrong input annotation format (-a). Only a warning: the ids are not checked against a fixed list, so that
	 * annotations from newer databases still work.
	 */
	private void warnAboutNumericFunctions(List<String> numericFunctions) {
		if (!numericFunctions.isEmpty()) {
			log.warn("Some function ids in the input are numbers (e.g. " + String.join(", ", numericFunctions)
					+ "): please check that the annotation format (-a) matches the input");
		}
	}
}
