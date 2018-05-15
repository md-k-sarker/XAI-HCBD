package org.dase.explanation.minidllearner;

import java.io.File;
import java.io.PrintStream;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * <p>
 * Simple example. Read an ontology, and display the class hierarchy. May use a
 * reasoner to calculate the hierarchy.
 * </p>
 * Author: Sean Bechhofer<br>
 * The University Of Manchester<br>
 * Information Management Group<br>
 * Date: 17-03-2007<br>
 * <br>
 */
public class SimpleHierarchyExample {
	private static int INDENT = 4;
	private final OWLReasonerFactory reasonerFactory;
	private final OWLOntology ontology;
	private final PrintStream out;
	private final static String ontoPath = "data/TestOntoTest.owl";

	public SimpleHierarchyExample(OWLReasonerFactory reasonerFactory, OWLOntology _ontology) {
		this.reasonerFactory = reasonerFactory;
		ontology = _ontology;
		out = System.out;
	}

	/**
	 * Print the class hierarchy for the given ontology from this class down,
	 * assuming this class is at the given level. Makes no attempt to deal sensibly
	 * with multiple inheritance.
	 */
	private void printHierarchy(OWLReasoner reasoner, OWLClass clazz) throws OWLException {

		printHierarchy(reasoner, clazz, 0);
		/* Now print out any unsatisfiable classes */
		for (OWLClass cl : ontology.getClassesInSignature()) {
			if (!reasoner.isSatisfiable(cl)) {
				out.println("XXX: " + labelFor(cl));
			}
		}
		reasoner.dispose();
	}

	private String labelFor(OWLEntity clazz) {
		/*
		 * Use a visitor to extract label annotations
		 */
		LabelExtractor le = new LabelExtractor();
		// Set<OWLAnnotation> annotations = clazz.getAnnotations(ontology);
		// for (OWLAnnotation anno : annotations) {
		// anno.accept(le);
		// }
		/* Print out the label if there is one. If not, just use the class URI */
		if (le.getResult() != null) {
			return le.getResult().toString();
		} else {
			return clazz.getIRI().toString();
		}
	}

	/**
	 * Print the class hierarchy from this class down, assuming this class is at the
	 * given level. Makes no attempt to deal sensibly with multiple inheritance.
	 */
	private void printHierarchy(OWLReasoner reasoner, OWLClass clazz, int level) throws OWLException {
		/*
		 * Only print satisfiable classes -- otherwise we end up with bottom everywhere
		 */
		if (reasoner.isSatisfiable(clazz)) {
			for (int i = 0; i < level * INDENT; i++) {
				out.print(" ");
			}

			/* Find the children and recurse */
			for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
				if (!child.equals(clazz)) {
					if (level == 1)
						return;
					printHierarchy(reasoner, child, level + 1);
				}
			}

			out.println(labelFor(clazz));

			// print direct individuals of this class
			for (OWLNamedIndividual indiv : reasoner.getInstances(clazz, true).getFlattened()) {
				out.println("  " + labelFor(indiv));
			}
		}
	}

	private void rawTest(OWLReasoner reasoner, OWLClass clazz, OWLObjectProperty obp) {

		out.println("Printing concepts related to imagecontains");
		for (OWLClass cls : reasoner.getObjectPropertyRanges(obp, false).getFlattened()) {
			out.println(labelFor(cls));
		}
	}

	@SuppressWarnings("javadoc")
	public static void main(String[] args)
			throws OWLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		File ontoFile = new File(ontoPath);

		System.out.println("path: " + ontoFile.getAbsolutePath());

		// String reasonerFactoryClassName = null;
		OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();

		// We first need to obtain a copy of an
		// OWLOntologyManager, which, as the name
		// suggests, manages a set of ontologies.
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		// We load an ontology
		// IRI documentIRI = IRI.create("file:/"+ontoFile.getAbsolutePath());
		System.out.println("Ontology loading...");

		// Now load the ontology.
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontoFile);

		// Report information about the ontology
		System.out.println("Ontology Loaded...");
		// System.out.println("Document IRI: " + documentIRI);
		System.out.println("Ontology : " + ontology.getOntologyID());
		System.out.println("Format : " + manager.getOntologyFormat(ontology));

		// / Create a new SimpleHierarchy object with the given reasoner.
		SimpleHierarchyExample simpleHierarchy = new SimpleHierarchyExample(reasonerFactory, ontology);

		// Get Thing
		OWLClass clazz = manager.getOWLDataFactory().getOWLThing();

		// inititate reasoner
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

		// System.out.println("Class : " + clazz);
		// Print the hierarchy below thing
		simpleHierarchy.printHierarchy(reasoner, clazz);

		// create objProperty
		IRI objectPropIri = IRI.create(ontology.getOntologyID().getOntologyIRI().get().toString(), "imageContains");
		OWLObjectProperty obp = manager.getOWLDataFactory().getOWLObjectProperty(objectPropIri);

		// do the raw test
		// simpleHierarchy.rawTest(reasoner, clazz, obp);
	}
}
