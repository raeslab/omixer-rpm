package org.omixer.rpm.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.Ortholog;
import org.omixer.utils.Constants;

public class ModuleParser {

	/**
	 * Reads a module definition file to create a list of {@link Module}s
	 * 
	 * No comments allowed. Modules have to end by ///
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static final List<Module> parseModuleFile(File file) throws IOException {
		final String MODULE_END = "///";
		final String alternativeSeparator = Constants.TAB;
		boolean newModule = false;
		// list of read modules
		final List<Module> modules = new LinkedList<Module>();
		// wrap the parsing in a try catch to be sure the fileReader is closed
		try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
			// initialize the reader
			
			String line = null;
			// the module at this point of iteration
			Module currentModule = null;
			// tokens holder for module name line
			String tokens[] = null;
			// read the first module
			if (fileReader.ready()) {
				line = fileReader.readLine().trim();
				// add the current module to the list of modules
				modules.add(new Module());
				// create a new module
				currentModule = modules.get(modules.size() - 1);
				// the current line is its id
				tokens = line.split(Constants.TAB);
				currentModule.setModuleId(tokens[0]);
				currentModule.setName(tokens[1]);
			}

			// parse the file
			while (fileReader.ready()) {

				line = fileReader.readLine().trim();
				if (newModule) {
					// add the current module to the list of modules
					modules.add(new Module());
					// create a new module
					currentModule = modules.get(modules.size() - 1);
					// the current line is its id
					tokens = line.split(Constants.TAB);
					currentModule.setModuleId(tokens[0]);
					currentModule.setName(tokens[1]);
					newModule = false;
				} else if (line.equals(MODULE_END)) {
					newModule = true;
				} else {
					if (currentModule != null) {
						// add the current line as a KO step
						currentModule.addOrthologs(makeModuleStep(line, alternativeSeparator, Constants.COMMA));
					}
				}
			}
		}
		// return the list of modules
		return modules;
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
