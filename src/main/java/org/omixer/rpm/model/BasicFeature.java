package org.omixer.rpm.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A basic feature with name, taxon, score, and one other function so far
 * TODO add an annotation Map to allow for more that one annotation
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class BasicFeature {
	
	private static final String NA = "NA";
	private static final String NONE = "none";
	private static final String BLANK = "";
	
	/**
	 * 
	 */
	private Long id;
	private String featureId;
	private String function;
	private Double count;
	private String taxon;

	/**
	 * Full constructor
	 * 
	 * @param featureId
	 * @param function
	 * @param count
	 * @param taxon
	 */
	public BasicFeature(String featureId, String taxon, String function, Double count) {
		super();
		this.featureId = featureId;
		this.function = function;
		this.count = count;
		this.taxon = taxon;
	}

	public BasicFeature() {
		super();
	}

	/**
	 * This is a unique identifier for the feature when it's stored in a database
	 * @return
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the function
	 */
	public String getFunction() {
		return function;
	}

	/**
	 * @return the id
	 */
	public String getFeatureId() {
		return featureId;
	}

	/**
	 * @return the taxon
	 */
	public String getTaxon() {
		return taxon;
	}

	/**
	 * @return the score
	 */
	public Double getCount() {
		return count;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @param function
	 *            the function to set
	 */
	public void setFunction(String function) {
		this.function = function;
	}

	/**
	 * @param featureId
	 *            the id to set
	 */
	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	/**
	 * @param count
	 *            the score to set
	 */
	public void setCount(Double count) {
		this.count = count;
	}

	/**
	 * @param taxon
	 *            the taxon to set
	 */
	public void setTaxon(String taxon) {
		this.taxon = taxon;
	}

	/**
	 * 
	 * @return true if not null nor "", nor NA, nor none 
	 */
	public boolean haveValidFunction() {
		if (getFunction() != null) {
			if (!getFunction().equals(BLANK) && !getFunction().equals(NA) && !getFunction().equals(NONE)) {
				return true;
			}	
		}
		return false;
	}
	
	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
		tsb.append(featureId);
		tsb.append(taxon);
		tsb.append(function);
		tsb.append(count);
		return tsb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}

		BasicFeature bf = (BasicFeature) o;
		EqualsBuilder eb = new EqualsBuilder();

		eb.append(getFeatureId(), bf.getFeatureId());
		eb.append(getTaxon(), bf.getTaxon());
		eb.append(getFunction(), bf.getFunction());
		eb.append(getCount(), bf.getCount());

		return eb.isEquals();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(11, 31);
		hcb.append(featureId);
		hcb.append(taxon);
		hcb.append(function);
		hcb.append(count);
		return hcb.toHashCode();
	}
}
