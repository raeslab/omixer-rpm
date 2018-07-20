package org.omixer.rpm.model;

/**
 * 
 * Holds entry information for http://www.raeslab.org/omixer/moduledb/display?id=1
 * 
 * @author omixer
 *
 * Created on Jul 20, 2018
 */
public class EggEntry {
	private long id;
	private byte svgId;
	private String name;
	private long moduleSetId;
	private String moduleId;

	public EggEntry() {
		super();
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the svgId
	 */
	public byte getSvgId() {
		return svgId;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the moduleSetId
	 */
	public long getModuleSetId() {
		return moduleSetId;
	}

	/**
	 * @return the moduleId
	 */
	public String getModuleId() {
		return moduleId;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @param svgId
	 *            the svgId to set
	 */
	public void setSvgId(byte svgId) {
		this.svgId = svgId;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param moduleSetId
	 *            the moduleSetId to set
	 */
	public void setModuleSetId(long moduleSetId) {
		this.moduleSetId = moduleSetId;
	}

	/**
	 * @param moduleId
	 *            the moduleId to set
	 */
	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}
}
