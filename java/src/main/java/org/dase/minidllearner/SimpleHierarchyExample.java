package org.dase.minidllearner;

import java.io.File;
import java.io.PrintStream;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import org.dase.minidllearner.LabelExtractor;

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
	private void printHierarchy(OWLClass clazz) throws OWLException {
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		printHierarchy(reasoner, clazz, 0);
		/* Now print out any unsatisfiable classes */
		for (OWLClass cl : ontology.getClassesInSignature()) {
			if (!reasoner.isSatisfiable(cl)) {
				out.println("XXX: " + labelFor(cl));
			}
		}
		reasoner.dispose();
	}

	private String labelFor(OWLClass clazz) {
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
			out.println(labelFor(clazz));
			/* Find the children and recurse */
			for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
				if (!child.equals(clazz)) {
					printHierarchy(reasoner, child, level + 1);
				}
			}
		}
	}

	@SuppressWarnings("javadoc")
	public static void main(String[] args)
			throws OWLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		String path = "";
		File f = new File(path);

		System.out.println("path: "+ f.getAbsolutePath());

		// String reasonerFactoryClassName = null;
		// // We first need to obtain a copy of an
		// // OWLOntologyManager, which, as the name
		// // suggests, manages a set of ontologies.
		// OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		// // We load an ontology from the URI specified
		// // on the command line
		// System.out.println(args[0]);
		// IRI documentIRI = IRI.create(args[0]);
		// // Now load the ontology.
		// OWLOntology ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
		// // Report information about the ontology
		// System.out.println("Ontology Loaded...");
		// System.out.println("Document IRI: " + documentIRI);
		// System.out.println("Ontology : " + ontology.getOntologyID());
		// System.out.println("Format : " + manager.getOntologyFormat(ontology));
		// // / Create a new SimpleHierarchy object with the given reasoner.
		// SimpleHierarchyExample simpleHierarchy = new SimpleHierarchyExample(
		// (OWLReasonerFactory) Class.forName(reasonerFactoryClassName)
		// .newInstance(), ontology);
		// // Get Thing
		// OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
		// System.out.println("Class : " + clazz);
		// // Print the hierarchy below thing
		// simpleHierarchy.printHierarchy(clazz);
	}
}
