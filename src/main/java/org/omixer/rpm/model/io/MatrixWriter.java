/**
 * 
 */
package org.omixer.rpm.model.io;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.Modules;

/**
 * @author omixer
 *
 *         Created on Jul 19, 2018
 */
public abstract class MatrixWriter {
	
	public void writeCounts(Map<String, Modules> moduleInference, File outfile) throws IOException {
		writeMatrix(moduleInference, outfile, Module::getCount);
	}

	public void writeCoverage(Map<String, Modules> moduleInference, File outfile) throws IOException {
		writeMatrix(moduleInference, outfile, Module::getCoverage);
	}

	public abstract void writeMatrix(Map<String, Modules> moduleInference, File outfile, Function<Module, Double> f)
			throws IOException;
}
