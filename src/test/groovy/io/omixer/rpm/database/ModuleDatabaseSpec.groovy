package io.omixer.rpm.database

import spock.lang.Specification

class ModuleDatabaseSpec extends Specification {

	def "functions are every ortholog of every alternative of every step, in database order, with repeats"() {

		given: "M1: K1,K2 | K3 / K1; M2: K3"
		def database = new ModuleDatabase([
			new ModuleDefinition("M1", "first", [new Step([new Alternative(["K1", "K2"]), new Alternative(["K3"])]),
				new Step([new Alternative(["K1"])])]),
			new ModuleDefinition("M2", "second", [new Step([new Alternative(["K3"])])])])

		expect:
		database.functions().toList() == ["K1", "K2", "K3", "K1", "K3"]
	}
}
