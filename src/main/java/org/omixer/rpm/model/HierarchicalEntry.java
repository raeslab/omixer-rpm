package org.omixer.rpm.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class HierarchicalEntry {

	Long id;
	int level;
	String name;
	
	public HierarchicalEntry(int level, String name) {
		super();
		this.level = level;
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	
	/**
	 * @param id the id to set
	 */
	public void setModuleHierarchyId(Long id) {
		this.id = id;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object o) {
		

		if (this == o) {
			return true;
		}

		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}

		HierarchicalEntry he = (HierarchicalEntry) o;
		
		EqualsBuilder eb = new  EqualsBuilder();
		
		eb.append(he.id, getId());
		eb.append(he.getLevel(), getLevel());
		eb.append(he.getName(), getName());

		return eb.isEquals();
	}
	
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(11, 31);

		hcb.append(getId());
		hcb.append(getLevel());
		hcb.append(getName());
		return hcb.toHashCode();
	}
	
	@Override
	public String toString() {
		
		ToStringBuilder tsb = new ToStringBuilder(this);
		tsb.append(level);
		tsb.append(name);
		return tsb.toString();
	}

		
}
