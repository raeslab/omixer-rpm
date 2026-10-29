package io.omixer.rpm.model;

/**
 * The result of one module in one profile: its abundance and coverage, for a taxon in per-taxon runs. Immutable.
 *
 * @author <a href="mailto:youssef.darzi@omixer.io">Youssef Darzi</a>
 */
public class Module {

	// The module id in the biological database (e.g. M00001)
	private final String moduleId;
	// null unless the results are per taxon
	private final String taxon;
	private final double coverage;
	private final double count;

	public Module(String moduleId, String taxon, double coverage, double count) {
		this.moduleId = moduleId;
		this.taxon = taxon;
		this.coverage = coverage;
		this.count = count;
	}

	public String getModuleId() {
		return moduleId;
	}

	public String getTaxon() {
		return taxon;
	}

	public double getCoverage() {
		return coverage;
	}

	/**
	 * @return the abundance
	 */
	public double getCount() {
		return count;
	}
}
