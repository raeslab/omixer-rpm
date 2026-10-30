package io.omixer.rpm.database

import java.nio.file.Files
import java.nio.file.Path

import io.omixer.rpm.exceptions.ModuleFileFormatException
import spock.lang.Specification
import spock.lang.TempDir

class ModuleDatabaseReaderSpec extends Specification {

	@TempDir
	Path tmp

	private ModuleDatabase read(String content) {
		Path file = tmp.resolve("modules.list")
		Files.writeString(file, content)
		ModuleDatabaseReader.read(file)
	}

	/** The steps of a module as text: alternatives joined by |, subunits by + */
	private static List<String> steps(ModuleDefinition module) {
		module.steps().collect { step -> step.alternatives().collect { it.subunits().join("+") }.join("|") }
	}

	def "TAB separates alternatives and comma the subunits of a complex, in database order"() {

		when:
		def database = read("M1\tfirst\nK00137,K09251\tK09473,K09470\nK00006\tK00008,K00009\tK00010\n///\n")

		then:
		database.modules()*.id() == ["M1"]
		database.modules()[0].name() == "first"
		steps(database.modules()[0]) == ["K00137+K09251|K09473+K09470", "K00006|K00008+K00009|K00010"]
	}

	def "blank lines after the last module are ignored"() {

		expect:
		steps(read("M1\tfirst\nK1\nK2\n///\n\n\n").modules().first()) == ["K1", "K2"]
	}

	def "blank lines inside a module are not read as steps"() {

		expect: "two steps, so coverage isn't diluted by empty steps"
		steps(read("M1\tfirst\nK1\n\n \t \nK2\n///\n").modules().first()) == ["K1", "K2"]
	}

	def "blank lines between modules are ignored"() {

		when:
		def database = read("M1\tfirst\nK1\n///\n\nM2\tsecond\nK2\n///\n")

		then:
		database.modules()*.id() == ["M1", "M2"]
		database.modules()*.name() == ["first", "second"]
		steps(database.modules()[1]) == ["K2"]
	}

	def "the last module may end without ///"() {

		when:
		def database = read("M1\tfirst\nK1\n///\nM2\tsecond\nK2")

		then:
		database.modules()*.id() == ["M1", "M2"]
		steps(database.modules()[1]) == ["K2"]
	}

	def "a module header without a name is rejected, reporting its line"() {

		when:
		read("M1\tfirst\nK1\n///\nM2\nK2\n///\n")

		then:
		ModuleFileFormatException e = thrown()
		e.message == "modules.list, line 4: module header 'M2' has no name, expected ID<TAB>name"
	}

	def "a /// where a module header is expected is rejected, reporting its line"() {

		when:
		read(content)

		then:
		ModuleFileFormatException e = thrown()
		e.message == "modules.list, line ${line}: expected a module header (ID<TAB>name), found ///"

		where:
		content                     | line
		"///\nM1\tfirst\nK1\n///\n" | 1
		"M1\tfirst\nK1\n///\n///\n" | 4
	}

	def "a module without steps is rejected, naming the module"() {

		when:
		read(content)

		then:
		ModuleFileFormatException e = thrown()
		e.message == message

		where: "a header directly followed by ///, or a header at the end of the file"
		content                                 | message
		"M1\tfirst\n///\nM2\tsecond\nK1\n///\n" | "modules.list, line 2: module 'M1' has no steps"
		"M1\tfirst\nK1\n///\nM2\tsecond\n"      | "modules.list, line 4: module 'M2' has no steps"
	}

	def "an alternative without a KO is rejected, reporting its line"() {

		when:
		read("M1\tfirst\nK1\n,\tK2\n///\n")

		then:
		ModuleFileFormatException e = thrown()
		e.message == "modules.list, line 3: step ',\tK2' has an alternative without a KO"
	}

	def "the GMM database is read in database order"() {

		when:
		def modules = ModuleDatabaseReader.read(Path.of("src/test/resources/module.list")).modules()

		then:
		modules.size() == 121
		modules[index].id() == id
		modules[index].name() == name
		modules[index].steps().size() == stepCount

		and: "every alternative of MF0004 is a complex of two subunits"
		modules[3].steps().every { step -> step.alternatives().every { it.subunits().size() == 2 } }

		where:
		index | id       | name                                   | stepCount
		0     | "MF0001" | "Ethanol production (formate pathway)" | 3
		3     | "MF0004" | "putrescine degradation"               | 1
		120   | "MF0121" | "superoxide reductase"                 | 1
	}

	def "a database is immutable"() {

		given:
		def database = read("M1\tfirst\nK1\n///\n")

		when:
		database.modules()[0].steps()[0].alternatives()[0].subunits().add("K2")

		then:
		thrown(UnsupportedOperationException)
	}
}
