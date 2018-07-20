package org.omixer.rpm.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO merge with BasicFeature and provide a class hierarchy for basic modules,
 * inferred modules and so on...
 * 
 * @author <a href="mailto:youssef.darzi@gmail.com">Youssef Darzi</a>
 * 
 */
public class Module {

	private Long id;
	private boolean flatOrthologsHasChanged;
	// The module id in the biological database (e.g. M00001)
	private String moduleId;
	private String name;
	// TODO remove this from here since it doesn't make sense
	private String taxon;
	private double coverage;
	private double count;
	private Map<Integer, HierarchicalEntry> hierarchy;
	private List<List<List<Ortholog>>> orthologs;
	private List<Ortholog> flatOrthologs;

	public Module() {
		super();
		this.hierarchy = new HashMap<Integer, HierarchicalEntry>();
		this.orthologs = new LinkedList<List<List<Ortholog>>>();
	}

	public Module(String moduleId, String taxon, double coverage, double count,
			Map<Integer, HierarchicalEntry> hierarchy) {
		super();
		this.moduleId = moduleId;
		this.taxon = taxon;
		this.coverage = coverage;
		this.count = count;
		this.hierarchy = hierarchy;
		this.orthologs = new LinkedList<List<List<Ortholog>>>();
	}

	/**
	 * 
	 * @param position
	 *            the position in the reaction or step number
	 * @return the list of alternatives at this position
	 * @throws NullPointerException
	 *             when the orthologs list has not been initialized yet
	 * @throws IndexOutOfBoundsException
	 *             if index is not within bound
	 * 
	 */
	public List<List<Ortholog>> getAlternatives(int position) {
		return getOrthologs().get(position);
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the coverage
	 */
	public double getCoverage() {
		return coverage;
	}

	/**
	 * @return the count
	 */
	public double getCount() {
		return count;
	}

	/**
	 * Flattens the list of {@link Ortholog}s in this module.
	 * 
	 * Not thread safe
	 * 
	 * @return a {@link List} of {@link Ortholog}s
	 */
	public List<Ortholog> getFlatOrthologs() {
		if (flatOrthologs == null) {
			// flatten
			flattenOrthologs();
		} else if (flatOrthologsHasChanged){
			// flatten again
			flattenOrthologs();
			flatOrthologsHasChanged = false;
		}
		return flatOrthologs;
	}
	
	public Map<String, Ortholog> getFlatOrthologsAsMap() {
		Map<String, Ortholog> map = new HashMap<>();
		for (Ortholog ortholog : getFlatOrthologs()) {
			map.put(ortholog.getOrthologId(), ortholog);
		}
		return map;
	}
	
	/**
	 * @return the moduleId
	 */
	public String getModuleId() {
		return moduleId;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the hierarchy
	 */
	public Map<Integer, HierarchicalEntry> getHierarchy() {
		return hierarchy;
	}

	/**
	 * 
	 * @param level
	 * @return
	 */
	public HierarchicalEntry getHierarchyForLevel(Integer level) {
		return getHierarchy().get(level);
	}

	/**
	 * 
	 * @param position
	 *            the position in the reaction or step number
	 * @param alternative
	 *            the alternative index
	 * @param ortholog
	 *            the ortholog index (0 in case of simple enzymes and more in
	 *            case or complex enzymes)
	 * @return the {@link Ortholog} for the given positions
	 * @throws NullPointerException
	 *             when the orthologs list has not been initialized yet
	 * @throws IndexOutOfBoundsException
	 *             if position, alternative, or ortholog are not within bound
	 */
	public Ortholog getOrtholog(int position, int alternative, int ortholog) {
		return getOrthologs().get(position).get(alternative).get(ortholog);
	}

	/**
	 * The orthologs of this module. Modification of these orthologs will
	 * directly affect the returned orthologs and vice-versa
	 * 
	 * @return
	 */
	public List<List<List<Ortholog>>> getOrthologs() {
		return orthologs;
	}

	/**
	 * 
	 * @param position
	 *            the position in the reaction or step number
	 * @param alternative
	 *            the alternative index
	 * @return the reaction at this position/alternative. It's a simple enzyme
	 *         when size is 1 and a complex when more than one
	 * @throws NullPointerException
	 *             when the orthologs list has not been initialized yet
	 * @throws IndexOutOfBoundsException
	 *             if position or alternative are not within bound
	 */
	public List<Ortholog> getOrthologs(int position, int alternative) {
		return getOrthologs().get(position).get(alternative);
	}
	
	public String getTaxon() {
		return taxon;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @param moduleId
	 *            the moduleId to set
	 */
	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param coverage
	 *            the coverage to set
	 */
	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}

	/**
	 * @param count
	 *            the count to set
	 */
	public void setCount(double count) {
		this.count = count;
	}

	public void setOrthologs(List<List<List<Ortholog>>> orthologs) {
		this.orthologs = orthologs;
		this.flatOrthologsHasChanged = true;
	}

	/**
	 * @param hierarchy
	 *            the hierarchy to set
	 */
	public void setHierarchy(Map<Integer, HierarchicalEntry> hierarchy) {
		this.hierarchy = hierarchy;
	}

	public void setTaxon(String taxon) {
		this.taxon = taxon;
	}

	public void addOrthologs(List<List<Ortholog>> orthologs) {
		// add a step in the module
		this.orthologs.add(orthologs);
		// update flatOrthologs 
		if (flatOrthologs != null) {
			for (List<Ortholog> orthologList : orthologs) {
				for (Ortholog ortholog : orthologList) {
					flatOrthologs.add(ortholog);
				}
			}
		}
	}

	private void flattenOrthologs() {
		flatOrthologs = new ArrayList<>();
		for (List<List<Ortholog>> path : orthologs) {
			for (List<Ortholog> alternatives : path) {
				for (Ortholog ortholog : alternatives) {
					flatOrthologs.add(ortholog);
				}
			}
		}
	}
	
	/**
	 * 
	 *  
	 * 
	 * @param moduleOrthologs
	 * @return
	 */
	public List<List<List<Ortholog>>> makeAllPaths() {
		// all possible paths
		List<List<List<Ortholog>>> paths = new ArrayList<>();
		// each alternative
		for (List<List<Ortholog>> alternatives : getOrthologs()) {
			// create a new path combination place holder
			List<List<List<Ortholog>>> pathsUpdate = new LinkedList<>();
			// if not empty
			if (!paths.isEmpty()) {
				// each enzyme
				for (List<Ortholog> alternative : alternatives) {
					// each existing path copy append this alt
					for (List<List<Ortholog>> path : paths) {
						// copy existing path
						List<List<Ortholog>> pathTmp = new LinkedList<>(path);
						// add new alternative
						pathTmp.add(alternative);
						// add to list of paths
						pathsUpdate.add(pathTmp);
					}
				}
				// update the list of paths
				paths = pathsUpdate;
			} else { 
				//populates the list of paths with the first list of alternatives
				// this only happens at the first iteration
				for (List<Ortholog> alternative : alternatives) {
					List<List<Ortholog>> path = new LinkedList<>();
					path.add(alternative);
					paths.add(path);
				}
			}
		}
		return paths;
	}
	
	@Override
	public Module clone() {
		Module clone = new Module();
		clone.setCount(getCount());
		clone.setCoverage(getCoverage());
		clone.setId(getId());
		clone.setModuleId(getModuleId());
		clone.setName(getName());
		clone.setTaxon(getTaxon());
		// Add the module orthologs structure
		// for each reaction step
		for (List<List<Ortholog>> step : getOrthologs()) {
			// foreach alternative step in this reaction
			List<List<Ortholog>> reaction = new LinkedList<List<Ortholog>>();
			// add the step to the list of reaction in the module 
			clone.addOrthologs(reaction);
			// foreach alternative in this step
			for (List<Ortholog> alternative : step) {
				// a list of ortholog components for this alternative step
				List<Ortholog> orthologs = new LinkedList<Ortholog>();
				// add the alternative ortholog component
				reaction.add(orthologs);
				for (Ortholog ortholog : alternative) {
					// add a cloned ortholog to the orthologs of this alternative step
					orthologs.add(ortholog.clone());
				}
			}
		}
		// FIXME clone instead of shallow copying | OK for now as this is should be moved out of Module class as it is fixed data tha should not change 
		clone.getHierarchy().putAll(getHierarchy());
		return clone;
	}

	
	/* * Uncomment for debugging
	 * @Override*/
/*	public String toString() {
	
		String string = getModuleId() + " " + getName() + " =>  " + getOrthologs().size() + " coverage " + getCoverage() + "\n";
			for(List<List<Ortholog>> step : getOrthologs()){
				string += "step size " + step.size() + " ";
				for (List<Ortholog> ko : step) {
					// complex or alternative
					for (Ortholog ortholog : ko) {
						string += ortholog.toString() + "(" + ortholog.getCount() +"),";
					}
					string += "\t";
				}
				string += "\n";
			}
		string += "///";
		
		return string;
	}*/
}
