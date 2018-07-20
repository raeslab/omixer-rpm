package org.omixer.rpm.model.enums;

public enum ModuleInferenceOptimizers {
	
	ABUNDANCE_COVERAGE_REACTION_BASED,
	ABUNDANCE_COVERAGE_ORTHOLOG_BASED,
	ABUNDANCE_COVERAGE_MEDIAN_BASED;
	
	public String displayName() {
				
		switch (this) {
		case ABUNDANCE_COVERAGE_ORTHOLOG_BASED:
			return "ABUNDANCE_COVERAGE_ORTHOLOG_BASED";
		default:
			return toString();
		}

	}

	public static ModuleInferenceOptimizers forName(String name) {
	
		if (name.equals("ABUNDANCE_COVERAGE_ORTHOLOG_BASED")) {
			return ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_ORTHOLOG_BASED;
		} else if (name.equals("ABUNDANCE_COVERAGE_REACTION_BASED")) {
			return ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED;
		} else if (name.equals("ABUNDANCE_COVERAGE_MEDIAN_BASED")) {
			return ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED;
		} 
		return null;
	}
}
