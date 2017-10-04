package edu.wright.dase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.algorithms.celoe.PCELOE;
import org.dllearner.core.ComponentInitException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import edu.wright.dase.lu.Alignment;
import edu.wright.dase.util.Writer;
import uk.ac.manchester.cs.jfact.JFactFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * Explanation.java Creates explanation for each class.
 * 
 * It imports ontology from sumo and combine it with wordnet & ade20k ontology.
 * Then it call RunAlgorithmClass to get the explanation. Then it saves the
 * explanation in hard disk.
 * 
 * @author sarker
 *
 */

public class Explanation {

	private static final Logger logger = LoggerFactory.getLogger(Explanation.class);
	final static int maxNegativeInstances = 20;
	static int totalInstances = 0;
	static int totalClasses = 0;

	// file storages
	static String sumoFilePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo/sumo_without_indi.owl";
	// for ning manual
	// static String rootPath =
	// "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";
	// static String rootOntoPath =
	// "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";
	static String runDlConfWritings = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";

	static final double distanceThreshold = 0.8;

	// static String fullADE20KAsOntology =
	// "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/ade20k_full.owl";

	static OWLDataFactory owlDataFactory;
	static String prefix = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
	static OWLOntologyManager owlOntologyManager;
	static OWLReasonerFactory reasonerFactory; // = new PelletReasonerFactory();
	static OWLReasoner owlReasoner;
	static Set<OWLNamedIndividual> posExamples = new HashSet<OWLNamedIndividual>();
	static Set<OWLNamedIndividual> negExamples = new HashSet<OWLNamedIndividual>();
	static int negIndiCounter = 0;
	static int posIndiCounter = 0;

	static OWLOntology sumoOntology;
	// static OWLOntology ade20KOntology;
	static OWLOntology combinedOntology;
	static Set<OWLOntology> sourceOntologies; // = new HashSet<OWLOntology>();
	static IRI owlDiskFileIRIForSave;
	static int counter = 0;
	static String[] excludedFolders = { "images", "training", "validation", "a", "b", "c", "d", "e", "f", "g", "h", "i",
			"j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };
	static String[] ningManualFolders = { "bathroom" };
	// , "bathroom", "butchers_shop", "bullpen", "bridge"
	static String[] OntologyFolders = { "bedroom", "bathroom" };
	static String posInstanceFolderPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/k/kitchen/";
	static String negInstanceFolderPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/l/living_room/";
	static String explanationForPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/k/kitchen/";
	// "butchers_shop", "bullpen", "bridge"

	static ArrayList<Integer> randomClassIndex = new ArrayList<Integer>();

	/**
	 * Initializes various components
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static void init() throws OWLOntologyCreationException {
		owlOntologyManager = OWLManager.createConcurrentOWLOntologyManager();
		owlDataFactory = owlOntologyManager.getOWLDataFactory();
		IRI ontoIRI = IRI.create(prefix);
		combinedOntology = owlOntologyManager.createOntology(ontoIRI);
		reasonerFactory = new PelletReasonerFactory();

		// reasoner comparison
		// http://dl.kr.org/ore2015/vip.cs.man.ac.uk_8008/results.html
		// jfact is based on fact++
		// reasonerFactory = new JFactFactory();

	}

	/**
	 * Combine sumo ontology with combinedOntology
	 */
	public static void combineSumoOntology() {

		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		ontologies.add(sumoOntology);

		OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies, combinedOntology);
		merger.mergeOntologies();
	}

	// is it working without file:, i.e. fullPath = "file:" + fullPath ?;
	// Checked this saveOntology() function.
	// do not save in disk for each combined ontology. it takes too much space in
	// disk.
	// each combined ontology is being approximately 80MB.
	// 80 MB * 22000 = 2000,000 MB = 2000 GB
	// saveOntology(fullPath);

	/**
	 * Save combined ontology to file system
	 * 
	 * @throws OWLOntologyStorageException
	 */
	public static void saveOntology(String path) throws OWLOntologyStorageException {

		owlDiskFileIRIForSave = IRI.create("file:" + path);
		owlOntologyManager.saveOntology(combinedOntology, owlDiskFileIRIForSave);

	}

	/**
	 * Load ontology from file system
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static OWLOntology loadOntology(File ontoFile) throws OWLOntologyCreationException {

		OWLOntology ontology = owlOntologyManager.loadOntologyFromOntologyDocument(ontoFile);
		return ontology;

	}

	/**
	 * Load positive instances and also add them to combined ontology
	 */
	public static void loadPosInstancesAndAddForMerging(String writeTo) {

		try {
			Files.walk(Paths.get(posInstanceFolderPath)).filter(f -> posIndiCounter <= 10)
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {
						// add to sourceOnto for combining with backgroundInfo
						try {
							OWLOntology ontology = loadOntology(f.toFile());
							sourceOntologies.add(ontology);
							logger.info(" adding " + f + " sumo to combineOntology...");
							System.out.println(" adding " + f + "  sumo to combineOntology...");
							Writer.writeInDisk(writeTo, "\n adding \"+ f +\" sumo to combineOntology...", true);
						} catch (OWLOntologyCreationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						// add to posIndi
						String name = f.getFileName().toString().replaceAll(".owl", "");
						IRI iriIndi = IRI.create(prefix + name);
						OWLNamedIndividual namedIndi = owlDataFactory.getOWLNamedIndividual(iriIndi);
						posExamples.add(namedIndi);
						posIndiCounter++;
					});
			// tookPositiveExamples = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Load positive instances and also add them to combined ontology
	 */
	public static void loadNegInstances(String writeTo) {
		try {
			Files.walk(Paths.get(negInstanceFolderPath)).filter(f -> negIndiCounter <= 10)
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {
						// add to sourceOnto for combining with backgroundInfo
						try {
							OWLOntology ontology = loadOntology(f.toFile());
							sourceOntologies.add(ontology);
							logger.info(" adding " + f + " sumo to combineOntology...");
							System.out.println(" adding " + f + "  sumo to combineOntology...");
							Writer.writeInDisk(writeTo, "\n adding \"+ f +\" sumo to combineOntology...", true);
						} catch (OWLOntologyCreationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						// add to negIndivs
						String name = f.getFileName().toString().replaceAll(".owl", "");
						IRI iriIndi = IRI.create(prefix + name);
						OWLNamedIndividual namedIndi = owlDataFactory.getOWLNamedIndividual(iriIndi);
						negExamples.add(namedIndi);
						negIndiCounter++;
					});
			// tookPositiveExamples = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * OWLEntityRemover entityRemover = new
	 * OWLEntityRemover(Collections.singleton(ontology)); for (OWLNamedIndividual
	 * individual : ontology.getIndividualsInSignature(Imports.INCLUDED)) { if
	 * (!individual.getIRI().getShortForm().contains("_Indi_")) {
	 * entityRemover.visit(individual); } }
	 * ontologyManager.applyChanges(entityRemover.getChanges());
	 */

	/**
	 * Removes concepts which do not have a single instance. alternatively keep
	 * those concepts which have at-least a single instance. TO-DO: FIX
	 */
	public static OWLOntology removeNonRelatedConcepts(OWLOntology combinedOntology, OWLReasoner owlReasoner,
			String writeTo) {

		OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

		for (OWLClass owlClass : combinedOntology.getClassesInSignature()) {
			if (owlReasoner.getInstances(owlClass, false).getFlattened().size() < 1) {
				entityRemover.visit(owlClass);
			}
		}
		ChangeApplied ca = owlOntologyManager.applyChanges(entityRemover.getChanges());

		Writer.writeInDisk(writeTo, "Removing " + ca.toString(), true);
		logger.info("Removing " + ca.toString(), true);
		System.out.println("Removing " + ca.toString());

		return combinedOntology;

	}

	/**
	 * Align with sumo ontology
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static void alignWithSumo() throws OWLOntologyCreationException {
		sumoOntology = loadOntology(new File(sumoFilePath));

		// align Classes

		/**
		 * // there are a lot of considerations for alignment. So just combine them now.
		 */

		sourceOntologies.add(sumoOntology);
		OntologyMerger merger = new OntologyMerger(owlOntologyManager, sourceOntologies, combinedOntology);
		merger.mergeOntologies();

		// for (OWLClass classFromSumo : sumoOntology.getClassesInSignature()) {
		// for (OWLClass classFromADE : combinedOntology.getClassesInSignature()) {
		//
		// double distance =
		// Alignment.computeConfidence(classFromSumo.getIRI().getShortForm(),
		// classFromADE.getIRI().getShortForm());
		// if (distance > distanceThreshold) {
		//
		//
		// // align individuals
		// for (OWLIndividual indi : classFromADE.getIndividualsInSignature()) {
		// OWLClassAssertionAxiom ax =
		// owlDataFactory.getOWLClassAssertionAxiom(classFromSumo, indi);
		// ChangeApplied ca = owlOntologyManager.addAxiom(sumoOntology, ax);
		// }
		//
		// // // align object properties
		// // for(OWLObjectProperty objProp:
		// classFromADE.getObjectPropertiesInSignature())
		// // {
		// //
		// // }
		// }
		// }
		// }
		//
		// // // align object properties
		// // for(OWLObjectProperty objProp:
		// // combinedOntology.getObjectPropertiesInSignature()) {
		// // objProp.
		// // }
		//
		// // align data properties
		//
		// //
		//
		//
		// combinedOntology = sumoOntology;
	}

	/**
	 * This function calls dl-learner to run
	 * 
	 * @param path
	 *            : folder
	 * @throws ComponentInitException
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 */
	public static void tryToCreateExplanations(Path path)
			throws ComponentInitException, OWLOntologyCreationException, OWLOntologyStorageException {

		sourceOntologies = new HashSet<OWLOntology>();

		// variables
		negIndiCounter = 0;
		posIndiCounter = 0;
		String folderName = path.getFileName().toString();
		String writeTo = path.toAbsolutePath().toString() + "/" + folderName + "_expl.txt";
		System.out.println("writeTo: " + writeTo);

		logger.info("####### Explanantion for " + folderName + " class ########");
		System.out.println("####### Explanantion for " + folderName + " class ########");
		Writer.writeInDisk(writeTo, "####### Explanantion for " + folderName + " class ########", true);

		logger.info(" initializing merzing combineOntology...");
		System.out.println(" initializing merzing combineOntology...");
		Writer.writeInDisk(writeTo, "\n initializing merzing combineOntology...", true);
		/**
		 * load positive and negative instances and corresponding ontologies to add them
		 * in backgroundinformation
		 */
		loadPosInstancesAndAddForMerging(writeTo);
		loadNegInstances(writeTo);

		// merge ontoligies
		OntologyMerger merger = new OntologyMerger(owlOntologyManager, sourceOntologies, combinedOntology);
		merger.mergeOntologies();

		logger.info(" initializing merzing combineOntology finished");
		System.out.println(" initializing merzing combineOntology finished");
		Writer.writeInDisk(writeTo, "\n initializing merzing combineOntology finished", true);

		// reason over ontology
		// create resoner to reason
		owlReasoner = reasonerFactory.createNonBufferingReasoner(combinedOntology);
		totalClasses = combinedOntology.getClassesInSignature().size();
		totalInstances = combinedOntology.getIndividualsInSignature().size();

		logger.info(" aligning sumo to combineOntology...");
		System.out.println(" aligning sumo to combineOntology...");
		Writer.writeInDisk(writeTo, "\n aligning sumo to combineOntology...", true);

		alignWithSumo();

		logger.info(" aligning sumo to combineOntology finished");
		System.out.println(" aligning sumo to combineOntology finished");
		Writer.writeInDisk(writeTo, "\n aligning sumo to combineOntology finished", true);

		// temporary it seems that we need to align ontology with sumo
		String saveinDesktopOrgn = "/home/sarker/Desktop/orginial_1.owl";
		String saveinDesktopRemv = "/home/sarker/Desktop/removedNonRelated_1.owl";
		saveOntology(saveinDesktopOrgn);

		logger.info("removing non related concepts from combineOntology...");
		logger.info("Before removing there are total: " + totalClasses + " classes in backgroundInformation");
		logger.info("Before removing there are total: " + totalInstances + " individuals in background Information");
		System.out.println("removing non related concepts from combineOntology...");
		System.out.println("Before removing there are total: " + totalClasses + " classes in backgroundInformation");
		System.out.println(
				"Before removing there are total: " + totalInstances + " individuals in background Information");
		Writer.writeInDisk(writeTo, "\n removing non related concepts from combineOntology...", true);
		Writer.writeInDisk(writeTo,
				"\nBefore removing there are total: " + totalClasses + " classes in background Information", true);
		Writer.writeInDisk(writeTo,
				"\nBefore removing there are total: " + totalInstances + " individuals in background Information",
				true);

		// remove non related concepts
		combinedOntology = removeNonRelatedConcepts(combinedOntology, owlReasoner, writeTo);
		totalClasses = combinedOntology.getClassesInSignature().size();
		totalInstances = combinedOntology.getIndividualsInSignature().size();

		logger.info("removing non related concepts from combineOntology finished.");
		logger.info("After removing there are total: " + totalClasses + " classes in background Information");
		logger.info("After removing there are total: " + totalInstances + " individuals in background Information");
		System.out.println("removing non related concepts from combineOntology finished");
		System.out.println("After removing there are total: " + totalClasses + " classes in background Information");
		System.out.println(
				"After removing there are total: " + totalInstances + " individuals in background Information");

		Writer.writeInDisk(writeTo, "\nremoving non related concepts from combineOntology finished.", true);
		Writer.writeInDisk(writeTo,
				"\nAfter removing there are total: " + totalClasses + " classes in background Information", true);
		Writer.writeInDisk(writeTo,
				"\nAfter removing there are total: " + totalInstances + " individuals in background Information", true);

		saveOntology(saveinDesktopRemv);

		logger.info("finished initializing combineOntology finished.");
		System.out.println("finished initializing combineOntology");
		Writer.writeInDisk(writeTo, "\n finished initializing combineOntology", true);

		Writer.writeInDisk(writeTo, "\n####### Positive examples: \t", true);
		System.out.println("PosExamples: ");
		for (OWLNamedIndividual posIndi : posExamples) {
			System.out.println(posIndi.getIRI().toString());
			Writer.writeInDisk(writeTo, posIndi.getIRI().getShortForm() + ", ", true);
		}

		Writer.writeInDisk(writeTo, "\n####### Negative examples: \t", true);
		System.out.println("\nNegExamples: ");
		for (OWLNamedIndividual negIndi : negExamples) {
			System.out.println(negIndi.getIRI().toString());
			Writer.writeInDisk(writeTo, negIndi.getIRI().getShortForm() + ", ", true);
		}

		// call to run dl-learner
		DLLearner dlLearner = new DLLearner(combinedOntology, posExamples, posExamples, writeTo);
		CELOE expl = dlLearner.run();

		counter++;

		writeStatistics(expl, writeTo);
		printStatus(path.toString());

		// // variables
		// boolean tookPositiveExamples = false;
		//
		// int classCounter = 0;
		//
		//
		// String parentFolderName = path.getParent().getFileName().toString();
		// String owl_class_name = folderName;

		// String parentFolderName = path.getParent().getFileName().toString();
		// String owl_super_class_name = "";

		// // Condition
		// // If parent name is misc, then parent folder name is misc
		// // If grandparent is not a....z or outliers then class name should be
		// // parent_name and grand_parent_name
		// if (folderName.equals("misc")) {
		// owl_class_name = "misc";
		// } else if ((parentFolderName.length() == 1 ||
		// parentFolderName.equals("outliers"))) {
		// owl_class_name = folderName;
		// } else {
		// owl_class_name = folderName + "_" + parentFolderName;
		// // owl_super_class_name = parentFolderName;
		// }

		// // make positive class
		// // create class
		// IRI iriClass = IRI.create(prefix + owl_class_name);
		// OWLClass thisOwlClass = owlDataFactory.getOWLClass(iriClass);
		// System.out.println("Class: " + thisOwlClass.getIRI().getShortForm());

		/*
		 * take instance for positive class i.e. from this folder/class
		 */
		// make positive instances

		// try {
		// Files.walk(path).filter(f -> posIndiCounter <= 10)
		// .filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {
		//
		// String name = f.getFileName().toString().replaceAll(".owl", "");
		// IRI iriIndi = IRI.create(prefix + name);
		// OWLNamedIndividual namedIndi = owlDataFactory.getOWLNamedIndividual(iriIndi);
		// posExamples.add(namedIndi);
		// posIndiCounter++;
		// });
		// tookPositiveExamples = true;
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// /**
		// * take negative instances i.e. take 10 instances randomly
		// */
		//
		// for (OWLNamedIndividual indi : combinedOntology.getIndividualsInSignature())
		// {
		// if (negIndiCounter >= 10) {
		// break;
		// }
		// if ((!posExamples.contains(indi)) &&
		// (indi.getIRI().getShortForm().startsWith("ADE_train_"))) {
		// negExamples.add(indi);
		// negIndiCounter++;
		// }
		// }

		// /**
		// * take instance for positive class i.e. from this class take negative
		// instances
		// * i.e. from all other classes without this class take 10 instances randomly
		// */
		// for (OWLClass owlClass : combinedOntology.getClassesInSignature()) {
		//// if
		// (owlClass.getIRI().getShortForm().equals(thisOwlClass.getIRI().getShortForm()))
		// {
		////
		//// posExamples = owlReasoner.getInstances(owlClass, false).getFlattened();
		//// tookPositiveExamples = true;
		////
		//// } else if
		// if(randomClassIndex.contains(classCounter)) {
		//
		// Set<OWLNamedIndividual> negExamples_ = owlReasoner.getInstances(owlClass,
		// false).getFlattened();
		// if (negExamples_.size() > 0) {
		// negExamples.add(negExamples_.iterator().next());
		// if (tookPositiveExamples && negExamples.size() >= 10) {
		// break;
		// }
		// }
		//
		// }
		// classCounter++;
		// }

		// create explanation file

	}

	/**
	 * This function calls dl-learner to run
	 * 
	 * @param path
	 *            : folder
	 * @throws ComponentInitException
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 */
	public static void tryToCreateExplanations(OWLOntology ontology, Set<OWLNamedIndividual> posExamples,
			Set<OWLNamedIndividual> negExamples, String writeTo, String explanationFor)
			throws ComponentInitException, OWLOntologyCreationException, OWLOntologyStorageException {

		System.out.println("writeTo: " + writeTo);

		logger.info("####### Explanantion for " + explanationFor + " class ########");
		System.out.println("####### Explanantion for " + explanationFor + " class ########");
		Writer.writeInDisk(writeTo, "####### Explanantion for " + explanationFor + " class ########", true);

		// reason over ontology
		// create resoner to reason
		owlReasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		totalClasses = ontology.getClassesInSignature().size();
		totalInstances = ontology.getIndividualsInSignature().size();

		logger.info("removing non related concepts from combineOntology...");
		logger.info("Before removing there are total: " + totalClasses + " classes in backgroundInformation");
		logger.info("Before removing there are total: " + totalInstances + " individuals in background Information");
		System.out.println("removing non related concepts from combineOntology...");
		System.out.println("Before removing there are total: " + totalClasses + " classes in backgroundInformation");
		System.out.println(
				"Before removing there are total: " + totalInstances + " individuals in background Information");
		Writer.writeInDisk(writeTo, "\n removing non related concepts from combineOntology...", true);
		Writer.writeInDisk(writeTo,
				"\nBefore removing there are total: " + totalClasses + " classes in background Information", true);
		Writer.writeInDisk(writeTo,
				"\nBefore removing there are total: " + totalInstances + " individuals in background Information",
				true);

		// remove non related concepts
		combinedOntology = removeNonRelatedConcepts(ontology, owlReasoner, writeTo);
		totalClasses = combinedOntology.getClassesInSignature().size();
		totalInstances = combinedOntology.getIndividualsInSignature().size();

		logger.info("removing non related concepts from combineOntology finished.");
		logger.info("After removing there are total: " + totalClasses + " classes in background Information");
		logger.info("After removing there are total: " + totalInstances + " individuals in background Information");
		System.out.println("removing non related concepts from combineOntology finished");
		System.out.println("After removing there are total: " + totalClasses + " classes in background Information");
		System.out.println(
				"After removing there are total: " + totalInstances + " individuals in background Information");

		Writer.writeInDisk(writeTo, "\nremoving non related concepts from combineOntology finished.", true);
		Writer.writeInDisk(writeTo,
				"\nAfter removing there are total: " + totalClasses + " classes in background Information", true);
		Writer.writeInDisk(writeTo,
				"\nAfter removing there are total: " + totalInstances + " individuals in background Information", true);

		logger.info("finished initializing combineOntology finished.");
		System.out.println("finished initializing combineOntology");
		Writer.writeInDisk(writeTo, "\n finished initializing combineOntology", true);

		Writer.writeInDisk(writeTo, "\n####### Positive examples: \t", true);
		System.out.println("PosExamples: ");
		for (OWLNamedIndividual posIndi : posExamples) {
			System.out.println(posIndi.getIRI().toString());
			Writer.writeInDisk(writeTo, posIndi.getIRI().getShortForm() + ", ", true);
		}

		Writer.writeInDisk(writeTo, "\n####### Negative examples: \t", true);
		System.out.println("\nNegExamples: ");
		for (OWLNamedIndividual negIndi : negExamples) {
			System.out.println(negIndi.getIRI().toString());
			Writer.writeInDisk(writeTo, negIndi.getIRI().getShortForm() + ", ", true);
		}

		// call to run dl-learner
		DLLearner dlLearner = new DLLearner(combinedOntology, posExamples, posExamples, writeTo);
		CELOE expl = dlLearner.run();

		writeStatistics(expl, writeTo);
		printStatus(explanationFor);

	}

	/**
	 * Iterate over folders to make positive and negative instances and then run
	 * DL-Learner
	 * 
	 * @param path
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws ComponentInitException
	 */
	public static void iterateOverFolders(Path path)
			throws OWLOntologyCreationException, OWLOntologyStorageException, ComponentInitException {

		// no need to do explanation because these folders do not have any instances
		for (String name : excludedFolders) {
			if (path.getFileName().toString().equals(name)) {
				System.out.println("Directory " + path + " is excluded");
				return;
			}
		}

		// make explanation for only ning suggested folders
		for (String folder : ningManualFolders) {
			if (path.getFileName().toString().equals(folder)) {

				tryToCreateExplanations(path);

			}
		}

	}

	/**
	 * Write statistics to file/disk
	 * 
	 * @param expl
	 * @param fileName
	 */
	public static void writeStatistics(CELOE expl, String fileName) {
		BufferedWriter writer;
		try {
			// writer = new BufferedWriter( new FileWriter( fileName));

			Writer.writeInDisk(fileName, "\n\n########################\n", true);

			LinkedList bestClassesDescription = (LinkedList) expl.getCurrentlyBestDescriptions();

			LinkedList best100Classes = (LinkedList) expl.getCurrentlyBestDescriptions(100);
			Writer.writeInDisk(fileName, "\nBest 100 Class- \n", true);
			for (int i = 0; i < best100Classes.size(); i++) {
				System.out.println("Best 100 Class- " + i + " :" + best100Classes.get(i));
				// writer.write(best100Classes.get(i) + "\n");
				Writer.writeInDisk(fileName, best100Classes.get(i) + "\n", true);
			}

			TreeSet bestEvalClasses = (TreeSet) expl.getCurrentlyBestEvaluatedDescriptions();
			Iterator it = bestEvalClasses.iterator();
			// writer.write("\nBest Eval Class- \n");
			Writer.writeInDisk(fileName, "\nBest Eval Class- \n", true);
			while (it.hasNext()) {
				System.out.println("Best Eval Class- :" + it.next());
				// writer.write(it.next()+"\n");
				Writer.writeInDisk(fileName, it.next() + "\n", true);
			}

			// writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static Set<OWLIndividual> loadPosExamples(String posExamplesFilePath) throws IOException {
		return readExamples(posExamplesFilePath);
	}

	public static Set<OWLIndividual> loadNegExamples(String negExamplesFilePath) throws IOException {
		return readExamples(negExamplesFilePath);
	}

	public static Set<OWLIndividual> readExamples(String filePath) throws IOException {
		Set<OWLIndividual> indivs = new TreeSet<>();
		try (BufferedReader buffRead = new BufferedReader(new FileReader(new File(filePath)))) {
			String line;
			while ((line = buffRead.readLine()) != null) {
				line = line.trim();
				line = line.substring(1, line.length() - 1); // strip off angle
																// brackets
				indivs.add(new OWLNamedIndividualImpl(IRI.create(line)));
			}
		}
		return indivs;
	}

	public static void readExamples(String filePath, boolean both) throws IOException {
		
		Set<OWLNamedIndividual> _posIndivs = new HashSet<OWLNamedIndividual>();
		Set<OWLNamedIndividual> _negIndivs = new HashSet<OWLNamedIndividual>();

		try (BufferedReader buffRead = new BufferedReader(new FileReader(new File(filePath)))) {
			String line;
			while ((line = buffRead.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("+") && line.length() > 2) {
					line = line.substring(1, line.length());
					IRI iri = IRI.create(line);

					_posIndivs.add(owlDataFactory.getOWLNamedIndividual(iri));
				} else if (line.startsWith("-")) {
					line = line.substring(1, line.length());
					IRI iri = IRI.create(line);

					_negIndivs.add(owlDataFactory.getOWLNamedIndividual(iri));
				}
			}
		}
		posExamples = _posIndivs;
		negExamples = _negIndivs;
	}

	/**
	 * print status
	 * 
	 * @param status
	 */
	public static void printStatus(String status) {
		try {
			System.out.println("explaining instances from class : " + status + " is successfull");
			System.out.println("Processed " + counter + " files");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Combine ontologies
	 * 
	 * @throws OWLOntologyCreationException
	 */
	// private static void combineOntology() throws OWLOntologyCreationException {
	//
	// logger.info(" initializing combineOntology...");
	// System.out.println(" initializing combineOntology...");
	// Writer.writeInDisk(runDlConfWritings, "\n initializing combineOntology...",
	// true);
	//
	// // declare ontology set
	// Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
	//
	// // TO-DO: later load sumo
	// // sumoOntology = loadOntology(new File(sumoFilePath));
	// // ontologies.add(sumoOntology);
	//
	// // load specific ontology from ade20k dataset.
	// for (String folder : OntologyFolders) {
	// String ontoPath = rootOntoPath + folder + "/" + folder + ".owl";
	// File ontoFile = new File(ontoPath);
	// if (ontoFile.exists()) {
	// OWLOntology ontology = loadOntology(new File(ontoPath));
	// ontologies.add(ontology);
	// logger.info("\nOntoFile " + ontoFile.getAbsolutePath() + " \n found. \n");
	// System.out.println("\n loading OntoFile " + ontoFile.getAbsolutePath() + "
	// \n");
	// Writer.writeInDisk(runDlConfWritings, "\n Adding OntoFile " +
	// ontoFile.getAbsolutePath() + " \n", true);
	// } else {
	// logger.info("\nOntoFile " + ontoFile.getAbsolutePath() + " \n not found.
	// \n");
	// System.out.println("\nOntoFile " + ontoFile.getAbsolutePath() + " \n not
	// found. \n");
	// Writer.writeInDisk(runDlConfWritings, "\nOntoFile " +
	// ontoFile.getAbsolutePath() + " \n not found. \n",
	// true);
	// }
	// }
	//
	// // merge ontoligies
	// OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies,
	// combinedOntology);
	// merger.mergeOntologies();
	//
	// logger.info("finished initializing combineOntology");
	// System.out.println("finished initializing combineOntology");
	// Writer.writeInDisk(runDlConfWritings, "\n finished initializing
	// combineOntology", true);
	// }

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			long startTime = System.currentTimeMillis();
			String folderName = Paths.get(explanationForPath).getFileName().toString();
			String writeTo = Paths.get(explanationForPath).toAbsolutePath().toString() + "/" + folderName + "_expl.txt";

			// runDlConfWritings = runDlConfWritings + "bathroom/" +
			// "bathroom_run_dl_in_b_folder.txt";
			logger.info("Program started...........");
			System.out.println("Program started...........");
			Writer.writeInDisk(writeTo, "\n Program started...........\n", false);

			init();

			tryToCreateExplanations(Paths.get(explanationForPath));

			long endTime = System.currentTimeMillis();

			logger.info("Program finsihed");
			System.out.println("Program finsihed");
			Writer.writeInDisk(writeTo, "\n\n\n Program finsihed", true);
			Writer.writeInDisk(writeTo, "\nProgram run for: " + (endTime - startTime) / 1000 + " seconds", true);

			// combine necessary ontologies
			// combineOntology();

			// create resoner to reason
			// owlReasoner = reasonerFactory.createNonBufferingReasoner(combinedOntology);

			// totalClasses = combinedOntology.getClassesInSignature().size();

			// for (int i = 0; i < maxNegativeInstances; i++) {
			// randomClassIndex.add(ThreadLocalRandom.current().nextInt(0, totalClasses));
			// }
			//
			// Files.walk(Paths.get(rootPath)).filter(d -> !d.toFile().isFile()).forEach(d
			// -> {
			// try {
			//
			// iterateOverFolders(d);
			//
			// } catch (ComponentInitException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (OWLOntologyCreationException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (OWLOntologyStorageException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// });

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
