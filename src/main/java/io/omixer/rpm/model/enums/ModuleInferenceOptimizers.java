package io.omixer.rpm.model.enums;

public enum ModuleInferenceOptimizers {
	
	ABUNDANCE_COVERAGE_REACTION_BASED,
	ABUNDANCE_COVERAGE_ORTHOLOG_BASED,
	ABUNDANCE_COVERAGE_MEDIAN_BASED,
	SUM,
	MIN;
	
	public String displayName() {

        if (this == ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_ORTHOLOG_BASED) {
            return "ABUNDANCE_COVERAGE_ORTHOLOG_BASED";
        }
        return toString();

    }

	public static ModuleInferenceOptimizers forName(String name) {

        return switch (name) {
            case "ABUNDANCE_COVERAGE_ORTHOLOG_BASED" -> ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_ORTHOLOG_BASED;
            case "ABUNDANCE_COVERAGE_REACTION_BASED" -> ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_REACTION_BASED;
            case "ABUNDANCE_COVERAGE_MEDIAN_BASED" -> ModuleInferenceOptimizers.ABUNDANCE_COVERAGE_MEDIAN_BASED;
            default -> null;
        };
    }
}
