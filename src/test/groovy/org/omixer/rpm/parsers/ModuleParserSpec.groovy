package org.omixer.rpm.parsers;

import java.io.File;
import java.util.List;

import org.omixer.rpm.model.Module;
import org.omixer.rpm.model.Ortholog;
import org.omixer.rpm.parsers.ModuleParser;

import spock.lang.Specification

public class ModuleParserSpec extends Specification {

	def "parseModuleFile parses the flat file correctly"() {
		
		setup: "Read a module database"
		List<Module> modules = ModuleParser.parseModuleFile(new File("src/test/resources/module.list"));
		
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
