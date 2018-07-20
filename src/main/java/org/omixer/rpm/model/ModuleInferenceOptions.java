package org.omixer.rpm.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.omixer.rpm.model.enums.ScalingMethod;


public class ModuleInferenceOptions {

	private Long id;
	private String algorithm;
	private double coverage;
	private long moduleSetId;
	private int promiscuousity;
	private ScalingMethod scalingMethod;
	private boolean normalizeByLength;
	private boolean perTaxon;
	/*
	 * Expertimental option
	 */
	private boolean distributeOrhologAbundance;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * @return the coverage
	 */
	public double getCoverage() {
		return coverage;
	}

	/**
	 * @return the moduleSetId
	 */
	public long getModuleSetId() {
		return moduleSetId;
	}

	public boolean isNormalizeByLength() {
		return normalizeByLength;
	}
	
	public boolean isPerTaxon() {
		return perTaxon;
	}
	
	public boolean isDistributeOrhologAbundance() {
		return distributeOrhologAbundance;
	}
	
	/**
	 * @return the promiscuousity
	 */
	public int getPromiscuousity() {
		return promiscuousity;
	}

	public ScalingMethod getScalingMethod() {
		return scalingMethod;
	}

	
	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @param algorithm
	 *            the algorithm to set
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * @param coverage
	 *            the coverage to set
	 */
	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}

	/**
	 * @param moduleSetId
	 *            the moduleSetId to set
	 */
	public void setModuleSetId(long moduleSetId) {
		this.moduleSetId = moduleSetId;
	}

	public void setNormalizeByLength(boolean normalizeByLength) {
		this.normalizeByLength = normalizeByLength;
	}
	
	/**
	 * @param promiscuousity
	 *            the promiscuousity to set
	 */
	public void setPromiscuousity(int promiscuousity) {
		this.promiscuousity = promiscuousity;
	}

	public void setPerTaxon(boolean perTaxon) {
		this.perTaxon = perTaxon;
	}
	
	public void setDistributeOrhologAbundance(boolean distributeOrhologAbundance) {
		this.distributeOrhologAbundance = distributeOrhologAbundance;
	}
	
	public void setScalingMethod(ScalingMethod scalingMethod) {
		this.scalingMethod = scalingMethod;
	}
	
	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
		tsb.append(getAlgorithm());
		tsb.append(getCoverage());
		tsb.append(getModuleSetId());
		tsb.append(getPromiscuousity());
		return tsb.toString();
	}


}
