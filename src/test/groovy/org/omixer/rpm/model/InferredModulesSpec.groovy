package org.omixer.rpm.model

import java.util.HashMap
import java.util.List
import java.util.Map

import org.omixer.rpm.model.InferredModules
import org.omixer.rpm.model.Module
import org.omixer.rpm.model.Modules

import spock.lang.Specification

/**
 * @author omixer
 *
 * Created on Jul 1, 2018
 */
class InferredModulesSpec extends Specification {

	def "moduleDistribution computes the incidence of coverage instances for the whole module inference"(){
		when: "3 datasets have one module per coverage value"
		// Add Modules to 3 datasets with increasing coverage
		def inferredModules = (0..2).collectEntries { i -> 
			List<Module> modules = (0..10).collect {new Module([coverage: it/10d])}
			[(i): new Modules(modules, null)]
		}
		def im = new InferredModules();
		im.setInferedModules(inferredModules); 
		Map<Double, Integer> distribution = im.computeCoverageDistribution();
		
		then: "each coverage is observed 3 times"
		(0..10).every{ distribution.get(it/10d) == 3 } 
	}

}
