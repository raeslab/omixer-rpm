package org.omixer.rpm.model;

import java.util.List;

public class PathwaySummary {

	private double coverage;
	private double abundance;
	private List<List<Ortholog>> pathway;

	public PathwaySummary(List<List<Ortholog>> pathway, double coverage, double abundance) {
		super();
		this.coverage = coverage;
		this.abundance = abundance;
		this.pathway = pathway;
	}

	/**
	 * @return the coverage
	 */
	public double getCoverage() {
		return coverage;
	}

	/**
	 * @return the abundance
	 */
	public double getAbundance() {
		return abundance;
	}

	/**
	 * @return the pathway
	 */
	public List<List<Ortholog>> getPathway() {
		return pathway;
	}

	/**
	 * @param coverage
	 *            the coverage to set
	 */
	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}

	/**
	 * @param abundance
	 *            the abundance to set
	 */
	public void setAbundance(double abundance) {
		this.abundance = abundance;
	}

	/**
	 * @param pathway
	 *            the pathway to set
	 */
	public void setPathway(List<List<Ortholog>> pathway) {
		this.pathway = pathway;
	}
}
