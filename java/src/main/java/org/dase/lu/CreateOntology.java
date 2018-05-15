package org.dase.lu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.SimpleIRIMapper;


public class CreateOntology {
	public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		loadAndSaveOntology();
	}
	
	public static void loadAndSaveOntology() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		// Load ontology
		File file = new File("./data/hcbdwsu.owl");
		
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
		System.out.println("Loaded ontology: " + ontology);
		
		// Add class
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		BufferedReader in = new BufferedReader(new FileReader("./data/terms.txt"));
		
		String str;
		Set<String> terms = new HashSet<>();
		
		while((str = in.readLine()) != null) {
			terms.add(str);
		}
	
		
		OWLClass clsThing = factory.getOWLClass(IRI.create("http://www.w3.org/2002/07/owl#Thing"));
		
		for (String t: terms) {
			OWLClass cls = factory
					.getOWLClass(IRI.create("http://www.semanticweb.org/luzhou/ontologies/2017/5/ADE20K/hcbdwsu#" + t.substring(0, 1).toUpperCase() + t.substring(1)));
			
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(cls, clsThing);
			AddAxiom addAxiom = new AddAxiom(ontology, axiom);
			manager.applyChange(addAxiom);
		}
		   
	    manager.saveOntology(ontology);
	}
}
