package io.omixer.rpm.scoring;

import io.omixer.rpm.engine.CompiledDatabase;
import io.omixer.rpm.engine.Profiles;

/**
 * Which share of a KO used by several modules each of them sees.
 */
public sealed interface SharedKoResolution {

	/** @return the profiles as every module sees them */
	Profiles apply(Profiles profiles, CompiledDatabase database);

	/** Every module sees a KO's whole value (default) */
	record None() implements SharedKoResolution {
		@Override
		public Profiles apply(Profiles profiles, CompiledDatabase database) {
			return profiles;
		}
	}

	/** A KO used by N distinct modules gives each of them its value ÷ N (--Xdistribute) */
	record EqualSplit() implements SharedKoResolution {
		@Override
		public Profiles apply(Profiles profiles, CompiledDatabase database) {
			return profiles.map((ko, value) -> {
				int modules = database.modulesUsing(ko);
				return modules > 1 ? value / modules : value;
			});
		}
	}
}
