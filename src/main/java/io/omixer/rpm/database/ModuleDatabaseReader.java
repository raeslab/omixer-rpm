package io.omixer.rpm.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.math.NumberUtils;

import io.omixer.rpm.exceptions.ModuleFileFormatException;
import io.omixer.utils.Constants;

/**
 * Reads a module database: one module per block, a header line {@code ID<TAB>name}, then one line per step, where
 * TAB separates alternatives and comma the subunits of a complex. A block ends with {@code ///}; the last one may
 * end with the file (as in GBMs.v1.0), with a warning, but a file in which no block ends with {@code ///} is not a
 * module database. Blank lines are ignored.
 */
public final class ModuleDatabaseReader {

	private static final String MODULE_END = "///";
	/** How many numeric function ids a warning gives */
	private static final int NUMERIC_FUNCTION_EXAMPLES = 3;

	private ModuleDatabaseReader() {
	}

	/**
	 * @param warnings receives what is accepted but looks wrong: a last module without {@code ///}, function ids that
	 *                 read as numbers (a matrix given as the module database)
	 * @throws ModuleFileFormatException naming the file and line, when the file does not follow the format
	 * @throws IOException               when the file cannot be read
	 */
	public static ModuleDatabase read(Path file, Consumer<String> warnings) throws IOException {
		String fileName = file.getFileName().toString();
		List<ModuleDefinition> modules = new ArrayList<>();
		// the module being read: its header fields and line, and steps; header null when a header is expected
		String[] header = null;
		int headerLine = 0;
		List<Step> steps = new ArrayList<>();
		int lineNumber = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (header == null) {
					header = readHeader(line, fileName, lineNumber);
					headerLine = lineNumber;
				} else if (line.equals(MODULE_END)) {
					modules.add(module(header, steps, fileName, lineNumber));
					header = null;
					steps = new ArrayList<>();
				} else {
					steps.add(readStep(line, fileName, lineNumber));
				}
			}
		}
		if (header != null) {
			// the file ended inside the last module
			if (modules.isEmpty()) {
				throw notAModuleDatabase(fileName);
			}
			modules.add(module(header, steps, fileName, lineNumber));
			warnings.accept(fileName + ", line " + headerLine + ": the last module, '" + header[0]
					+ "', does not end with " + MODULE_END + ": if the file was cut off, modules are missing");
		} else if (modules.isEmpty()) {
			throw notAModuleDatabase(fileName);
		}
		ModuleDatabase database = new ModuleDatabase(modules);
		// values of a matrix read as a database give such ids; only a warning, so that any database still works
		List<String> numericFunctions = database.functions().distinct().filter(NumberUtils::isCreatable)
				.limit(NUMERIC_FUNCTION_EXAMPLES).toList();
		if (!numericFunctions.isEmpty()) {
			warnings.accept("Some function ids in " + fileName + " are numbers (e.g. " + String.join(", ", numericFunctions)
					+ "): please check that it is a module database (-d)");
		}
		return database;
	}

	private static ModuleFileFormatException notAModuleDatabase(String fileName) {
		return new ModuleFileFormatException(fileName + ": no module ends with " + MODULE_END
				+ ", so it is not a module database (a module is a header ID<TAB>name, one line per step, then "
				+ MODULE_END + ")");
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
