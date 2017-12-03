package org.dase.minidllearner;

import static org.testng.Assert.assertEquals;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SimpleHierarchyTest {

	String ontofile = "";
	String reasonerFactoryClassName = null;
	OWLOntology ontology;
	SimpleHierarchyExample he;

	@BeforeTest
	public void initialize() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		he = new SimpleHierarchyExample((OWLReasonerFactory) Class.forName(reasonerFactoryClassName).newInstance(),
				ontology);

	}

	@Test
	public void f() {
		String str = "A";
		assertEquals("A", str);
	}
}
