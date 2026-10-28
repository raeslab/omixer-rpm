package io.omixer.rpm.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.omixer.rpm.exceptions.ModuleFileFormatException;
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.Ortholog;
import io.omixer.utils.Constants;

public class ModuleParser {

	/**
	 * Reads a module definition file to create a list of {@link Module}s
	 * 
	 * No comments allowed. Modules have to end by ///. Blank lines are ignored.
	 * Each module starts with a header line (ID TAB name) and has at least one step.
	 *
	 * @param file
	 * @return
	 * @throws ModuleFileFormatException if the file does not follow this format
	 * @throws IOException
	 */
	public static final List<Module> parseModuleFile(File file) throws IOException {
		final String MODULE_END = "///";
		final String alternativeSeparator = Constants.TAB;
		// a module header is expected at the start of the file and after each ///
		boolean newModule = true;
		// list of read modules
		final List<Module> modules = new LinkedList<Module>();
		// wrap the parsing in a try catch to be sure the fileReader is closed
		try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
			// initialize the reader

			String line = null;
			// the module at this point of iteration
			Module currentModule = null;
			// tokens holder for module name line
			String[] tokens = null;
			// 1-based line number, for error messages
			int lineNumber = 0;

			// parse the file
			while ((line = fileReader.readLine()) != null) {

				lineNumber++;
				line = line.trim();
				// blank lines are neither module headers nor steps
				if (line.isEmpty()) {
					continue;
				}
				if (newModule) {
					if (line.equals(MODULE_END)) {
						throw new ModuleFileFormatException(file.getName() + ", line " + lineNumber
								+ ": expected a module header (ID<TAB>name), found " + MODULE_END);
					}
					// the current line is its id
					tokens = line.split(Constants.TAB);
					if (tokens.length < 2) {
						throw new ModuleFileFormatException(file.getName() + ", line " + lineNumber + ": module header '"
								+ line + "' has no name, expected ID<TAB>name");
					}
					// add the current module to the list of modules
					modules.add(new Module());
					// create a new module
					currentModule = modules.get(modules.size() - 1);
					currentModule.setModuleId(tokens[0]);
					currentModule.setName(tokens[1]);
					newModule = false;
				} else if (line.equals(MODULE_END)) {
					checkHasSteps(currentModule, file, lineNumber);
					newModule = true;
				} else {
					// add the current line as a KO step
					currentModule.addOrthologs(makeModuleStep(line, alternativeSeparator, Constants.COMMA));
				}
			}
			// the last module may end without ///
			if (!newModule) {
				checkHasSteps(currentModule, file, lineNumber);
			}
		}
		// return the list of modules
		return modules;
	}

	/**
	 * A module without steps cannot be scored, so it is rejected when its end is reached
	 */
	private static void checkHasSteps(Module module, File file, int lineNumber) {
		if (module.getOrthologs().isEmpty()) {
			throw new ModuleFileFormatException(file.getName() + ", line " + lineNumber + ": module '"
					+ module.getModuleId() + "' has no steps");
		}
	}

	/**
	 * 
	 * @param line a reaction step of a module pathway
	 * @param alternativeSeparator a regular expression for alternative steps
	 * @param complexSeparator a regular expression for complex steps (i.e. those with sever orthologs aka complex)
	 * @return
	 */
	public static List<List<Ortholog>> makeModuleStep(String line, String alternativeSeparator,
			String complexSeparator) {

		List<List<Ortholog>> orthologs = new LinkedList<List<Ortholog>>();
		// split on alternatives
		String[] tokens = line.split(alternativeSeparator);
		// each alternative
		for (int i = 0; i < tokens.length; i++) {
			List<Ortholog> alternatives = new LinkedList<Ortholog>();
			// split on complex
			String[] subOrthologs = tokens[i].split(complexSeparator);
			for (int j = 0; j < subOrthologs.length; j++) {
				// add sub complex
				alternatives.add(new Ortholog(subOrthologs[j]));
			}
			orthologs.add(alternatives);
		}
		return orthologs;
	}
}
