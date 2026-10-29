package io.omixer.rpm.engine

import io.omixer.rpm.domain.Alternative
import io.omixer.rpm.domain.ModuleDatabase
import io.omixer.rpm.domain.ModuleDefinition
import io.omixer.rpm.domain.Step
import spock.lang.Specification

class CompiledDatabaseSpec extends Specification {

	/** M1: K1|K2,K3 / K1 ; M2: K3 / K4 */
	static final CompiledDatabase DATABASE = CompiledDatabase.compile(new ModuleDatabase([
		new ModuleDefinition("M1", "first", [
			new Step([new Alternative(["K1"]), new Alternative(["K2", "K3"])]),
			new Step([new Alternative(["K1"])])]),
		new ModuleDefinition("M2", "second", [
			new Step([new Alternative(["K3"])]),
			new Step([new Alternative(["K4"])])])]))

	private static List<String> ids(CompiledModule module) {
		(0..<module.stepCount()).collect { s ->
			(0..<module.alternativeCount(s)).collect { a ->
				(0..<module.subunitCount(s, a)).collect { u -> DATABASE.ko(module.subunit(s, a, u)) }.join(",")
			}.join("|")
		}
	}

	def "every KO some module uses gets one index, and each module keeps its structure and database order"() {

		expect:
		DATABASE.koCount() == 4
		(0..<4).collect { DATABASE.ko(it) } as Set == ["K1", "K2", "K3", "K4"] as Set
		DATABASE.modules()*.id() == ["M1", "M2"]
		ids(DATABASE.modules()[0]) == ["K1|K2,K3", "K1"]
		ids(DATABASE.modules()[1]) == ["K3", "K4"]
	}

	def "a KO is counted once per module using it, however often it appears there"() {

		expect: "K1 twice in M1 only; K3 in M1 and M2"
		DATABASE.modulesUsing((0..<DATABASE.koCount()).find { DATABASE.ko(it) == ko }) == modules

		where:
		ko   | modules
		"K1" | 1
		"K2" | 1
		"K3" | 2
		"K4" | 1
	}
}
