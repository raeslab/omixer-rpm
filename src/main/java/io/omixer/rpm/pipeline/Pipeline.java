package io.omixer.rpm.pipeline;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.omixer.rpm.database.CompiledDatabase;
import io.omixer.rpm.database.ModuleDatabaseReader;
import io.omixer.rpm.inference.Results;
import io.omixer.rpm.output.plot.CoveragePlotWriter;
import io.omixer.rpm.profile.ProfilesBuilder;
import io.omixer.rpm.profile.reader.ProfileReader;
import io.omixer.utils.exceptions.IncorrectNumberOfEntriesException;

/**
 * A run: reads the module database and the input, infers the modules and writes the results. In order:
 * <ol>
 * <li>the module database is read and compiled (orthologs become indexes);</li>
 * <li>whether profiles are per taxon is decided before reading (a HUMAnN table without strata is per sample);</li>
 * <li>the reader feeds every input value to the profiles builder (for HUMAnN through its row selection);</li>
 * <li>the inference method gives the results and the coverage cutoff;</li>
 * <li>the coverage plot (automatic cutoff only) and the result files are written.</li>
 * </ol>
 */
public final class Pipeline {

	private static final Log LOG = LogFactory.getLog(Pipeline.class);

	/**
	 * @return the results, as written
	 * @throws io.omixer.rpm.exceptions.OmixerRpmException when the database or the input does not follow its format
	 * @throws IncorrectNumberOfEntriesException            when a matrix row has another number of values than samples
	 * @throws IOException                                  when a file cannot be read or written
	 */
	public Results run(Request request) throws IOException, IncorrectNumberOfEntriesException {
		// as 1.x: the output directory exists also when the input turns out to be wrong
		request.outputDir().toFile().mkdirs();
		CompiledDatabase database = CompiledDatabase.compile(ModuleDatabaseReader.read(request.database(), LOG::warn));
		boolean perTaxon = ProfileReader.perTaxon(request.input(), request.format(), request.perTaxon());
		if (request.perTaxon() && !perTaxon) {
			LOG.info(request.input().getFileName() + " has no stratified rows (function|taxon): one profile per sample");
		}
		ProfilesBuilder profiles = new ProfilesBuilder(database, perTaxon);
		ProfileReader.forInput(request.input(), request.format())
				.read(request.input(), ProfileReader.selection(request.input(), request.format(), perTaxon, profiles));
		warnAboutNumericFunctions(profiles.numericFunctions());

		Results results = request.inference().infer(database, profiles.build());
		if (results.cutoff().value().isEmpty()) {
			LOG.warn("No module was observed in any sample: no coverage cutoff can be learned and no module is reported");
		}
		new CoveragePlotWriter().write(results, request.outputDir());
		request.output().writer().write(results, request.outputDir());
		return results;
	}

	/**
	 * Function ids that read as numbers usually mean an abundance column was read as the function column, i.e. the
	 * wrong input annotation format (-a). Only a warning: the ids are not checked against a fixed list, so that
	 * annotations from newer databases still work.
	 */
	private static void warnAboutNumericFunctions(List<String> numericFunctions) {
		if (!numericFunctions.isEmpty()) {
			LOG.warn("Some function ids in the input are numbers (e.g. " + String.join(", ", numericFunctions)
					+ "): please check that the annotation format (-a) matches the input");
		}
	}
}
