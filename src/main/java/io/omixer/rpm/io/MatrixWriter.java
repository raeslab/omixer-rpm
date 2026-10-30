package io.omixer.rpm.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import io.omixer.rpm.engine.ModuleResult;
import io.omixer.rpm.engine.Results;
import io.omixer.utils.Constants;

/**
 * Writes the reported results as an abundance and a coverage matrix: a column per sample, a row per module (or per
 * taxon and module), 0.0 where a sample does not report it. Rows follow 1.x: modules in the order they are first
 * reported across samples; taxa in the order of a HashMap of their names.
 */
public final class MatrixWriter implements ResultWriter {

	public static final String ABUNDANCE_FILE = "modules.tsv";
	public static final String COVERAGE_FILE = "modules-coverage.tsv";

	/** A row of the matrix; taxon is empty for results that are not per taxon */
	private record Row(String taxon, String module) {
	}

	@Override
	public void write(Results results, Path outputDir) throws IOException {
		boolean perTaxon = results.perTaxon();
		// sample => its reported results by row
		Map<String, Map<Row, ModuleResult>> samples = new LinkedHashMap<>();
		// the rows: taxon => its modules, both in 1.x order
		Map<String, Set<String>> rows = perTaxon ? new HashMap<>() : new LinkedHashMap<>();
		for (String sample : results.samples()) {
			Map<Row, ModuleResult> reported = new HashMap<>();
			for (ModuleResult result : results.reported(sample)) {
				String taxon = result.taxon() == null ? Constants.EMPTY_STRING : result.taxon();
				rows.computeIfAbsent(taxon, t -> new LinkedHashSet<>()).add(result.module());
				reported.put(new Row(taxon, result.module()), result);
			}
			samples.put(sample, reported);
		}
		String keys = perTaxon ? "Taxon\tModule" : "Module";
		write(outputDir.resolve(ABUNDANCE_FILE), keys, samples, rows, perTaxon, ModuleResult::abundance);
		write(outputDir.resolve(COVERAGE_FILE), keys, samples, rows, perTaxon, ModuleResult::coverage);
	}

	private static void write(Path file, String keys, Map<String, Map<Row, ModuleResult>> samples,
			Map<String, Set<String>> rows, boolean perTaxon, ToDoubleFunction<ModuleResult> measure) throws IOException {
		List<String> header = new ArrayList<>(List.of(keys));
		header.addAll(samples.keySet());
		try (BufferedWriter out = new BufferedWriter(new FileWriter(file.toFile()))) {
			out.write(String.join(Constants.TAB, header) + Constants.NEW_LINE);
			for (Map.Entry<String, Set<String>> taxon : rows.entrySet()) {
				for (String module : taxon.getValue()) {
					StringBuilder line = new StringBuilder(perTaxon ? taxon.getKey() + Constants.TAB + module : module);
					Row row = new Row(taxon.getKey(), module);
					for (Map<Row, ModuleResult> reported : samples.values()) {
						ModuleResult result = reported.get(row);
						line.append(Constants.TAB).append(result == null ? 0d : measure.applyAsDouble(result));
					}
					out.write(line.toString());
					out.newLine();
				}
			}
		}
	}
}
