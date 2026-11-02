package io.omixer.rpm.database;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A module database prepared for scoring, built once and shared by every profile and thread: a dictionary of the
 * orthologs that some module uses (KO id → index, numbered in order of first appearance), and each module with its
 * orthologs as indexes of that dictionary. Immutable.
 *
 * Why indexes: the KO id of every input value is still looked up once, in this dictionary, while the profiles are
 * read ({@link #indexOf}); a design with a map of KO id → value per profile would make that same lookup. What the
 * indexes save comes afterwards. A profile stores its values as an index array and a value array (12 bytes a value,
 * against some 50 to 70 for a map entry with a boxed value), which keeps memory low with one profile per taxon of
 * every sample, and scoring reads a value with an array access instead of a hash lookup. The speed share of the
 * indexes alone was never measured: the 2.0 speed-up over 1.2 comes from several changes together (REFACTORING.md).
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
					List<String> orthologs = alternatives.get(a).orthologs();
					steps[s][a] = new int[orthologs.size()];
					for (int o = 0; o < orthologs.size(); o++) {
						// a new ortholog gets the next index
						int ortholog = index.computeIfAbsent(orthologs.get(o), id -> index.size());
						steps[s][a][o] = ortholog;
						used.set(ortholog);
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
