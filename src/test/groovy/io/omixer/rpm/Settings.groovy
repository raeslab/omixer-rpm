package io.omixer.rpm

import java.nio.file.Files

import io.omixer.rpm.database.CompiledDatabase
import io.omixer.rpm.inference.CoverageCutoff
import io.omixer.rpm.inference.Results
import io.omixer.rpm.inference.scoring.ComplexRule
import io.omixer.rpm.inference.scoring.Estimator
import io.omixer.rpm.inference.scoring.ScoringInference
import io.omixer.rpm.inference.scoring.ScoringOptions
import io.omixer.rpm.inference.scoring.SharedKoResolution
import io.omixer.rpm.profile.ProfilesBuilder
import io.omixer.rpm.profile.reader.DirectoryReader
import io.omixer.rpm.profile.reader.InputFormat
import io.omixer.rpm.profile.reader.MatrixReader

/**
 * The settings of a run in a spec, and the run itself: input read through the real readers and ProfilesBuilder, then
 * scored by ScoringInference. A coverage of -1 is the automatic cutoff.
 */
class Settings {

	Estimator estimator = new Estimator.Median()
	ComplexRule complexRule = ComplexRule.ANY
	boolean normalizeByLength = false
	boolean perTaxon = false
	boolean distribute = false
	double coverage = 0d
	int threads = 1

	/** A row of one sample's input: [taxon, function, value] */
	static List row(String taxon, String function, double value) {
		[taxon, function, value]
	}

	ScoringInference inference() {
		new ScoringInference(new ScoringOptions(estimator, complexRule, normalizeByLength),
				distribute ? new SharedKoResolution.EqualSplit() : new SharedKoResolution.None(),
				coverage == -1d ? new CoverageCutoff.Automatic() : new CoverageCutoff.Fixed(coverage), threads)
	}

	/** @return the results of one sample made of the given rows */
	SampleResults infer(List<List> rows, CompiledDatabase database) {
		def builder = new ProfilesBuilder(database, perTaxon)
		builder.sample(SampleResults.SAMPLE)
		rows.each { taxon, function, value -> builder.add(SampleResults.SAMPLE, taxon, function, value) }
		new SampleResults(inference().infer(database, builder.build()), SampleResults.SAMPLE)
	}

	/** @return the results of a matrix or directory */
	Results infer(File input, InputFormat format, CompiledDatabase database) {
		def builder = new ProfilesBuilder(database, perTaxon)
		def reader = input.isDirectory() ? new DirectoryReader(format) : new MatrixReader(format)
		reader.read(input.toPath(), builder)
		inference().infer(database, builder.build())
	}

	/** @return the results of every sample of a matrix or directory, in input order */
	Map<String, SampleResults> inferSamples(File input, InputFormat format, CompiledDatabase database) {
		def results = infer(input, format, database)
		results.samples().collectEntries { [it, new SampleResults(results, it)] }
	}
}
