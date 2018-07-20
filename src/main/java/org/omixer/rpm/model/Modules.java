package org.omixer.rpm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.omixer.utils.Constants;

/**
 * 
 * A class to hold {@link Module}s inferred by a module inference
 * 
 * @author Youssef Darzi Created on Sep 21, 2016 at 9:56:04 PM
 *
 */
public class Modules {

	private List<Module> modules;
	private Double coverageCutoff;

	public Modules() {
		super();
		modules = new ArrayList<Module>();
	}

	public Modules(List<Module> modules, Double coverageCutoff) {
		this.modules = modules;
		this.coverageCutoff = coverageCutoff;

	}

	/**
	 * Append the list of modules in parameter into the existing list of modules.
	 * The coverage is not taken into account
	 * 
	 * @param modules
	 * @throws NullPointerException
	 *             if modules contains a null collection of modules or if the
	 *             receiving instance has a null module collection
	 */
	public void addModules(Modules modules) {
		this.modules.addAll(modules.getModules());
	}

	public List<Module> getModules() {
		return modules;
	}

	public void setModules(List<Module> modules) {
		this.modules = modules;
	}

	public void setCoverageCutoff(Double coverageCutoff) {
		this.coverageCutoff = coverageCutoff;
	}

	public List<Module> toAboveCutoffList() {
		return modules.stream().filter(x -> x.getCoverage() >= coverageCutoff).collect(Collectors.toList());
	}

	public List<Module> toAboveCutoffList(Double cutoff) {
		return modules.stream().filter(x -> x.getCoverage() > cutoff).collect(Collectors.toList());
	}

	public Double getCoverageCutoff() {
		return coverageCutoff;
	}

	/**
	 * Returns a list of observed coverage values
	 * 
	 * @return
	 */
	public List<Double> makeCoverageDistribution() {
		List<Double> distribution = new ArrayList<Double>();
		getModules().stream().filter(x -> Double.compare(x.getCoverage(), Constants.ZERO) > 0)
				.forEach(x -> distribution.add(x.getCoverage()));
		return distribution;
	}
}
