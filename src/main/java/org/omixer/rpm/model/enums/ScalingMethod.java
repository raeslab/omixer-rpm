package org.omixer.rpm.model.enums;

public enum ScalingMethod {

	NONE,
	// Divide by sample sum
	PROPORTIONS,
	// Multiply by a scaling factor of (Sample sum / Mean of samples )
	DATASET_MEAN_SCALING;
}
