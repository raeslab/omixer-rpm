package org.omixer.rpm.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * TODO extended by a complexKO class to avoid concatenating KO names
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class Ortholog implements Cloneable {

	private String entry;
	private String orthologId;
	private double count;

	public Ortholog() {
	}

	public Ortholog(String orthologId) {
		this.orthologId = orthologId;
	}

	public void setCount(double count) {
		this.count = count;
	}
	
	public void setEntry(String entry) {
		this.entry = entry;
	}

	public double getCount() {
		return count;
	}
	
	public String getEntry() {
		return entry;
	}

	/**
	 * @return the keggId
	 */
	public String getOrthologId() {
		return orthologId;
	}

	/**
	 * @param orthologId
	 *            the keggId to set
	 */
	public void setOrthologId(String orthologId) {
		this.orthologId = orthologId;
	}

	@Override
	protected Ortholog clone() {
		Ortholog clone = new Ortholog();
		clone.setEntry(getEntry());
		clone.setOrthologId(getOrthologId());
		clone.setCount(getCount());
		return clone;
	}
	
	@Override
	public String toString() {
		return orthologId;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(11, 31);
		hcb.append(orthologId);
		return hcb.toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}

		Ortholog ko = (Ortholog) o;

		EqualsBuilder eb = new EqualsBuilder();
		eb.append(this.orthologId, ko.getOrthologId());

		return eb.isEquals();
	}


}