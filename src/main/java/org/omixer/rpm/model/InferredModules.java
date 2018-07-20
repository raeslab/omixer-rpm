package org.omixer.rpm.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class InferredModules {

	private long id;
	private String name;
	private long moduleSetId;
	private long datasetId;
	private ModuleInferenceOptions inferenceOptions;
	// the sample to inferred modules mapping
	private Map<Long, Modules> inferedModules;
	private Map<Integer, Set<HierarchicalEntry>> observedHierarchies;

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
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
	 * @return the datasetId
	 */
	public long getDatasetId() {
		return datasetId;
	}

	/**
	 * @return the inferenceOptions
	 */
	public ModuleInferenceOptions getInferenceOptions() {
		return inferenceOptions;
	}

	/**
	 * @return the inferedModules
	 */
	public Map<Long, Modules> getInferedModules() {
		return inferedModules;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(long id) {
		this.id = id;
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
	 * @param datasetId
	 *            the datasetId to set
	 */
	public void setDatasetId(long datasetId) {
		this.datasetId = datasetId;
	}

	/**
	 * @param inferenceOptions
	 *            the inferenceOptions to set
	 */
	public void setModuleInferenceOptions(ModuleInferenceOptions inferenceOptions) {
		this.inferenceOptions = inferenceOptions;
	}

	/**
	 * @param inferedModuleList
	 *            the inferedModules to set
	 */
	public void setInferedModules(Map<Long, Modules> inferedModuleList) {
		this.inferedModules = inferedModuleList;
		// with a new list of modules the hierarchies will be different
		this.observedHierarchies = null;
	}

	/**
	 * Generates the list of observed hierarchies for above cutoff Modules
	 * 
	 * @return
	 */
	public Map<Integer, Set<HierarchicalEntry>> getObservedHierarchies() {

		if (observedHierarchies == null) {

			Map<Integer, Set<HierarchicalEntry>> hierarchy = new HashMap<Integer, Set<HierarchicalEntry>>();

			// a cache for the processed modules
			Set<String> processedModules = new HashSet<String>();
			// each sample
			for (Entry<Long, Modules> sampleToModules : getInferedModules().entrySet()) {
				// each module
				for (Module module : sampleToModules.getValue().toAboveCutoffList()) {
					// was this module processed?
					if (!processedModules.contains(module.getModuleId())) {
						// register as processed
						processedModules.add(module.getModuleId());
						// get the hierarchy
						Map<Integer, HierarchicalEntry> moduleHierarchy = module.getHierarchy();
						if (moduleHierarchy != null) {
							// on each level to name of the hierarchy
							for (Entry<Integer, HierarchicalEntry> levelToHierarchy : moduleHierarchy.entrySet()) {
								// get name for this level
								Set<HierarchicalEntry> hierarchicalEntries = hierarchy.get(levelToHierarchy.getKey());
								// is it the first time we see this level ?
								if (hierarchicalEntries == null) {
									// initialize it's names
									hierarchicalEntries = new HashSet<HierarchicalEntry>();
									// register it
									hierarchy.put(levelToHierarchy.getKey(), hierarchicalEntries);
								}
								// add the new hierarchy for this level
								hierarchicalEntries.add(levelToHierarchy.getValue());
							}
						}
					}
				}
			}
			observedHierarchies = hierarchy;
		}
		return observedHierarchies;
	}

	/**
	 * 
	 * Generates a module coverage distribution by counting the incidence of each
	 * observed coverage value
	 * 
	 * @return A Map of coverage values incidence
	 */
	public Map<Double, Integer> computeCoverageDistribution() {
		// Pool all inferred modules
		List<Module> mappedModules = getInferedModules().values().stream().flatMap(x -> x.getModules().stream())
				.collect(Collectors.toList());

		// Get the incidence of each coverage value
		Map<Double, Integer> distribution = mappedModules.stream()
				.collect(Collectors.groupingBy(Module::getCoverage, Collectors.reducing(0, e -> 1, Integer::sum)));

		return distribution;
	}

	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this);
		tsb.append(getName());
		tsb.append(getModuleSetId());
		tsb.append(getDatasetId());
		tsb.append(getInferenceOptions());
		return tsb.toString();
	}
}
