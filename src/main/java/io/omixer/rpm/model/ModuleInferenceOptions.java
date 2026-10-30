package io.omixer.rpm.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import io.omixer.rpm.model.enums.ScalingMethod;
import io.omixer.rpm.scoring.ComplexRule;
import io.omixer.rpm.scoring.Estimator;
import io.omixer.rpm.scoring.ScoringOptions;
import io.omixer.rpm.scoring.SharedKoResolution;


public class ModuleInferenceOptions {

	private Long id;
	private Estimator estimator = new Estimator.Median();
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
	/*
	 * How a partially observed complex step counts
	 */
	private ComplexRule complexRule = ComplexRule.ANY;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the score estimator (-s)
	 */
	public Estimator getEstimator() {
		return estimator;
	}

	/**
	 * @return how modules share a KO: split equally with --Xdistribute, otherwise not at all
	 */
	public SharedKoResolution getSharedKoResolution() {
		return distributeOrhologAbundance ? new SharedKoResolution.EqualSplit() : new SharedKoResolution.None();
	}

	/**
	 * @return the options the module scorer needs
	 */
	public ScoringOptions getScoringOptions() {
		return new ScoringOptions(estimator, complexRule, normalizeByLength);
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
	
	public ComplexRule getComplexRule() {
		return complexRule;
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
	 * @param estimator
	 *            the score estimator (-s)
	 */
	public void setEstimator(Estimator estimator) {
		this.estimator = estimator;
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
	
	public void setComplexRule(ComplexRule complexRule) {
		this.complexRule = complexRule;
	}
	
	public void setScalingMethod(ScalingMethod scalingMethod) {
		this.scalingMethod = scalingMethod;
	}
	
	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
		tsb.append(getEstimator());
		tsb.append(getCoverage());
		tsb.append(getModuleSetId());
		tsb.append(getPromiscuousity());
		return tsb.toString();
	}


}
