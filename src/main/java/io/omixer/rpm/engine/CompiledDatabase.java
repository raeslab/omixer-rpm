package io.omixer.rpm.engine;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.omixer.rpm.domain.Alternative;
import io.omixer.rpm.domain.ModuleDatabase;
import io.omixer.rpm.domain.ModuleDefinition;

/**
 * A module database prepared for scoring, built once and shared by every profile and thread: a dictionary of the KOs
 * that some module uses, and each module with its KOs as indexes of that dictionary. Immutable.
 */
public final class CompiledDatabase {

	/** KO index → KO id */
	private final List<String> kos;
	private final List<CompiledModule> modules;
	/** KO index → the number of distinct modules using it */
	private final int[] modulesUsing;

	private CompiledDatabase(List<String> kos, List<CompiledModule> modules, int[] modulesUsing) {
		this.kos = List.copyOf(kos);
		this.modules = List.copyOf(modules);
		this.modulesUsing = modulesUsing;
	}

	public static CompiledDatabase compile(ModuleDatabase database) {
		List<String> kos = new ArrayList<>();
		Map<String, Integer> index = new HashMap<>();
		List<CompiledModule> modules = new ArrayList<>();
		// each module's KOs, to count the distinct modules using a KO
		List<BitSet> moduleKos = new ArrayList<>();
		for (ModuleDefinition definition : database.modules()) {
			BitSet used = new BitSet();
			int[][][] steps = new int[definition.steps().size()][][];
			for (int s = 0; s < steps.length; s++) {
				List<Alternative> alternatives = definition.steps().get(s).alternatives();
				steps[s] = new int[alternatives.size()][];
				for (int a = 0; a < steps[s].length; a++) {
					List<String> subunits = alternatives.get(a).subunits();
					steps[s][a] = new int[subunits.size()];
					for (int u = 0; u < subunits.size(); u++) {
						int ko = index.computeIfAbsent(subunits.get(u), id -> {
							kos.add(id);
							return kos.size() - 1;
						});
						steps[s][a][u] = ko;
						used.set(ko);
					}
				}
			}
			modules.add(new CompiledModule(definition.id(), steps));
			moduleKos.add(used);
		}
		int[] modulesUsing = new int[kos.size()];
		for (BitSet used : moduleKos) {
			used.stream().forEach(ko -> modulesUsing[ko]++);
		}
		return new CompiledDatabase(kos, modules, modulesUsing);
	}

	/** @return the modules, in database order */
	public List<CompiledModule> modules() {
		return modules;
	}

	/** @return the number of KOs some module uses; KO indexes run from 0 to this - 1 */
	public int koCount() {
		return kos.size();
	}

	/** @return the KO id of an index */
	public String ko(int index) {
		return kos.get(index);
	}

	/** @return the number of distinct modules using the KO of an index */
	public int modulesUsing(int index) {
		return modulesUsing[index];
	}
}
