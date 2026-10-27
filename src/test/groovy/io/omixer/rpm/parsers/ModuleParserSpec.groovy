package io.omixer.rpm.parsers


import java.nio.file.Path

import io.omixer.rpm.exceptions.ModuleFileFormatException
import io.omixer.rpm.model.Module;
import io.omixer.rpm.model.Ortholog
import spock.lang.Specification
import spock.lang.TempDir

public class ModuleParserSpec extends Specification {

	@TempDir
	Path tmp

	private File database(String content) {
		File file = tmp.resolve("modules.list").toFile()
		file.text = content
		return file
	}

	private static List<String> stepKos(Module module) {
		module.getOrthologs().collect { step -> step.flatten()*.getOrthologId().join("|") }
	}

	def "parseModuleFile ignores blank lines after the last module"() {

		when:
		List<Module> modules = ModuleParser.parseModuleFile(database("M1\tfirst\nK1\nK2\n///\n\n\n"))

		then:
		modules*.getModuleId() == ["M1"]
		stepKos(modules[0]) == ["K1", "K2"]
	}

	def "parseModuleFile does not read blank lines inside a module as steps"() {

		when:
		List<Module> modules = ModuleParser.parseModuleFile(database("M1\tfirst\nK1\n\n \t \nK2\n///\n"))

		then: "two steps, so coverage isn't diluted by empty steps"
		stepKos(modules[0]) == ["K1", "K2"]
	}

	def "parseModuleFile rejects a module header without a name, reporting its line"() {

		when:
		ModuleParser.parseModuleFile(database("M1\tfirst\nK1\n///\nM2\nK2\n///\n"))

		then:
		ModuleFileFormatException e = thrown()
		e.message.contains("line 4")
		e.message.contains("M2")
	}

	def "parseModuleFile rejects a /// where a module header is expected, reporting its line"() {

		when:
		ModuleParser.parseModuleFile(database(content))

		then:
		ModuleFileFormatException e = thrown()
		e.message.contains("line ${line}")

		where:
		content                                  | line
		"///\nM1\tfirst\nK1\n///\n"              | 1
		"M1\tfirst\nK1\n///\n///\n"              | 4
	}

	def "parseModuleFile rejects a module without steps, naming the module"() {

		when:
		ModuleParser.parseModuleFile(database(content))

		then:
		ModuleFileFormatException e = thrown()
		e.message.contains(module)

		where: "a header directly followed by ///, or a header at the end of the file"
		content                                         | module
		"M1\tfirst\n///\nM2\tsecond\nK1\n///\n"         | "M1"
		"M1\tfirst\nK1\n///\nM2\tsecond\n"              | "M2"
	}

	def "parseModuleFile accepts a last module without a closing ///"() {

		when:
		List<Module> modules = ModuleParser.parseModuleFile(database("M1\tfirst\nK1\n///\nM2\tsecond\nK2"))

		then:
		modules*.getModuleId() == ["M1", "M2"]
		stepKos(modules[1]) == ["K2"]
	}

	def "parseModuleFile ignores blank lines between modules"() {

		when:
		List<Module> modules = ModuleParser.parseModuleFile(database("M1\tfirst\nK1\n///\n\nM2\tsecond\nK2\n///\n"))

		then:
		modules*.getModuleId() == ["M1", "M2"]
		modules*.getName() == ["first", "second"]
		stepKos(modules[1]) == ["K2"]
	}

	def "parseModuleFile parses the flat file correctly"() {
		
		setup: "Read a module database"
		List<Module> modules = ModuleParser.parseModuleFile(new File("src/test/resources/module.list"))
		
		expect: "121 module read" 
		modules.size() == 121
		
		when: "First, a random and last modules read" 
		Module mf1 = modules[id];
		
		then: "Module Id, Name, and orthologs are parsed correctly"
		mf1.getModuleId() == moduleId
		mf1.getName() == name
		mf1.getOrthologs().size() == size 
				
		when: "Module is MF0004"
		Module mf4 = modules.get(3)
		then: "Each alternative is made out of 2 orthologs" 
		mf4.getOrthologs().every{ it.size() == 2 }
		
		where:
		id| moduleId | name | size
		0 | "MF0001" | "Ethanol production (formate pathway)" | 3
		3 | "MF0004" | "putrescine degradation" | 1
		120 | "MF0121" | "superoxide reductase" | 1
	}
	
	def "makeModuleStep parses the steps correctly"(){
		
		when: "TAB designates OR and ',' designates AND"
		List<List<Ortholog>> steps = ModuleParser.makeModuleStep("K00137,K09251	K09473,K09470", "\t", ",");
		then: "2 alternatives are found, and each is made of 2 orthologs"
		steps.size() == 2
		steps.every { it.size() == 2 }

		when: "TAB designates OR and '+' designates AND"
		steps = ModuleParser.makeModuleStep("K00006+K00007	K00008+K00009	K00010", "\t", "\\+");
		
		then: "3 alternatives are found: the first 2 are made of 2 orthologs while the last of only one"
		steps.size() == 3
		steps.get(0).size() == 2
		steps.get(2).size() == 1
	}
}
