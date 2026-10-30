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

	/** KO id → KO index */
	private final Map<String, Integer> index;
	private final List<CompiledModule> modules;
	/** KO index → the number of distinct modules using it */
	private final int[] modulesUsing;

	private CompiledDatabase(Map<String, Integer> index, List<CompiledModule> modules, int[] modulesUsing) {
		this.index = Map.copyOf(index);
		this.modules = List.copyOf(modules);
		this.modulesUsing = modulesUsing;
	}

	public static CompiledDatabase compile(ModuleDatabase database) {
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
						// a new KO gets the next index
						int ko = index.computeIfAbsent(subunits.get(u), id -> index.size());
						steps[s][a][u] = ko;
						used.set(ko);
					}
				}
			}
			modules.add(new CompiledModule(definition.id(), steps));
			moduleKos.add(used);
		}
		int[] modulesUsing = new int[index.size()];
		for (BitSet used : moduleKos) {
			used.stream().forEach(ko -> modulesUsing[ko]++);
		}
		return new CompiledDatabase(index, modules, modulesUsing);
	}

	/** @return the modules, in database order */
	public List<CompiledModule> modules() {
		return modules;
	}

	/** @return the number of KOs some module uses; KO indexes run from 0 to this - 1 */
	public int koCount() {
		return index.size();
	}

	/** @return the index of a KO, or -1 when no module uses it */
	public int indexOf(String ko) {
		return index.getOrDefault(ko, -1);
	}

	/** @return the number of distinct modules using the KO of an index */
	public int modulesUsing(int index) {
		return modulesUsing[index];
	}
}
