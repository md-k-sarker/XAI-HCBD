package edu.wright.daselab.inference;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubObjectPropertyAxiomGenerator;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import edu.wright.dase.util.Monitor;

import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.model.IRI;

public class InferredAxioms {

	private static OWLOntology ontology;
	private static OWLOntologyManager manager;
	private static OWLDataFactory dataFacotry;
	private static List<InferredAxiomGenerator<? extends OWLAxiom>> gens;
	private static OWLReasoner reasoner;

	private static void loadOntology() throws OWLOntologyCreationException, IOException {

		Path ontoPath = Paths.get(ConfigInferParams.ontoPath).toAbsolutePath();

		File ontoFile = null;
		if (Files.isSymbolicLink(ontoPath)) {
			System.out.println("Path is symbolic: " + Files.isSymbolicLink(ontoPath));
			ontoFile = Files.readSymbolicLink(ontoPath).toFile();
		} else {
			ontoFile = ontoPath.toFile();
		}
		ontoFile = ontoFile.toPath().toRealPath().toFile();
		System.out.println("Ontology Path " + ontoFile.getCanonicalPath());

		// We first need to obtain a copy of an OWLOntologyManager, which, as the name
		// suggests, manages a set of ontologies.
		manager = OWLManager.createOWLOntologyManager();
		dataFacotry = manager.getOWLDataFactory();

		// load the ontology.
		System.out.println("Ontology loading...");
		ontology = manager.loadOntologyFromOntologyDocument(ontoFile);

		// Report information about the ontology
		System.out.println("Ontology Loaded");
		System.out.println("Ontology path: " + ontoFile.getAbsolutePath());
		System.out.println("Ontology id : " + ontology.getOntologyID());
		System.out.println("Format : " + manager.getOntologyFormat(ontology));
	}

	private static void generateInferredAxioms() throws Exception {

		// String reasonerFactoryClassName = null;
		// OWLReasonerFactory.class.getDeclaredConstructor().newInstance().createNonBufferingReasoner(ontology)
		// reasonerFactory = new Reasoner.ReasonerFactory();
		// initiate reasoner
		try {
			// String reasonerFactoryClassName = null;
			OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
			// initiate reasoner
			reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception();
		}

		gens = new ArrayList<>();
		gens.add(new InferredSubClassAxiomGenerator());
		gens.add(new InferredClassAssertionAxiomGenerator());
		gens.add(new InferredDisjointClassesAxiomGenerator());
		gens.add(new InferredEquivalentClassAxiomGenerator());
		gens.add(new InferredEquivalentDataPropertiesAxiomGenerator());
		gens.add(new InferredEquivalentObjectPropertyAxiomGenerator());
		gens.add(new InferredInverseObjectPropertiesAxiomGenerator());
		gens.add(new InferredObjectPropertyCharacteristicAxiomGenerator());
		gens.add(new InferredPropertyAssertionGenerator());
		gens.add(new InferredSubDataPropertyAxiomGenerator());
		gens.add(new InferredSubObjectPropertyAxiomGenerator());

		// manager.saveOntology(infOnt,new RDFXMLDocumentFormat(),IRI.create(new
		// File("D://file.owl")));
	}

	private static void writeInferredAxioms() throws OWLOntologyCreationException, OWLOntologyStorageException {
		InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, gens);
		OWLOntology infOnt = manager.createOntology();
		iog.fillOntology(dataFacotry, infOnt);
		// new file solved the problem
		IRI outputDiskFileIRI = IRI.create(new File(ConfigInferParams.outputOntoPath));
		manager.saveOntology(infOnt, outputDiskFileIRI);
	}

	private static void doOps() throws Exception {
		loadOntology();
		generateInferredAxioms();
		writeInferredAxioms();
	}

	public static void main(String[] args)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
		try {
			Monitor.start("Program started");
			doOps();
			Monitor.stop("Program finished");
		} catch (Exception E) {
			E.printStackTrace();
			Monitor.displayMessage("Program crashed");
			Monitor.stop(E.getMessage());
		}
	}

}
