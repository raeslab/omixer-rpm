package io.omixer.rpm.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.omixer.rpm.exceptions.ModuleFileFormatException;
import io.omixer.utils.Constants;

/**
 * Reads a module database: one module per block, a header line {@code ID<TAB>name}, then one line per step, where
 * TAB separates alternatives and comma the subunits of a complex. A block ends with {@code ///}; the last one may
 * end with the file. Blank lines are ignored.
 */
public final class ModuleDatabaseReader {

	private static final String MODULE_END = "///";

	private ModuleDatabaseReader() {
	}

	/**
	 * @throws ModuleFileFormatException naming the file and line, when the file does not follow the format
	 * @throws IOException               when the file cannot be read
	 */
	public static ModuleDatabase read(Path file) throws IOException {
		String fileName = file.getFileName().toString();
		List<ModuleDefinition> modules = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile(), StandardCharsets.UTF_8))) {
			// the module being read: its header fields and steps; null when a header is expected
			String[] header = null;
			List<Step> steps = new ArrayList<>();
			int lineNumber = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (header == null) {
					header = readHeader(line, fileName, lineNumber);
				} else if (line.equals(MODULE_END)) {
					modules.add(module(header, steps, fileName, lineNumber));
					header = null;
					steps = new ArrayList<>();
				} else {
					steps.add(readStep(line, fileName, lineNumber));
				}
			}
			if (header != null) {
				modules.add(module(header, steps, fileName, lineNumber));
			}
		}
		return new ModuleDatabase(modules);
	}

	private static String[] readHeader(String line, String fileName, int lineNumber) {
		if (line.equals(MODULE_END)) {
			throw new ModuleFileFormatException(fileName + ", line " + lineNumber
					+ ": expected a module header (ID<TAB>name), found " + MODULE_END);
		}
		String[] fields = line.split(Constants.TAB);
		if (fields.length < 2) {
			throw new ModuleFileFormatException(fileName + ", line " + lineNumber + ": module header '" + line
					+ "' has no name, expected ID<TAB>name");
		}
		return fields;
	}

	private static Step readStep(String line, String fileName, int lineNumber) {
		List<Alternative> alternatives = new ArrayList<>();
		for (String alternative : line.split(Constants.TAB)) {
			String[] subunits = alternative.split(Constants.COMMA);
			if (subunits.length == 0) {
				throw new ModuleFileFormatException(fileName + ", line " + lineNumber + ": step '" + line
						+ "' has an alternative without a KO");
			}
			alternatives.add(new Alternative(Arrays.asList(subunits)));
		}
		return new Step(alternatives);
	}

	/** A module without steps cannot be scored, so it is rejected when its end is reached */
	private static ModuleDefinition module(String[] header, List<Step> steps, String fileName, int lineNumber) {
		if (steps.isEmpty()) {
			throw new ModuleFileFormatException(fileName + ", line " + lineNumber + ": module '" + header[0]
					+ "' has no steps");
		}
		return new ModuleDefinition(header[0], header[1], steps);
	}
}
