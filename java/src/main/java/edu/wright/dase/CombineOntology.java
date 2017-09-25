package edu.wright.dase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * Combine/Merge ontologies from different folders
 * 
 * @author sarker
 *
 */

public class CombineOntology {

	private static final Logger logger = LoggerFactory.getLogger(CombineOntology.class);

	static String rootPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/";
	
	static String customLogFile = "/home/sarker/MegaCloud/ProjectHCBD/experiments/ade_with_wn_sumo/logs/";
	
	static OWLDataFactory owlDataFactory;
	static String prefix = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
	static OWLOntologyManager owlOntologyManager;
	static OntologyMerger ontologyMerger;

	// static OWLOntology sumoOntology;
	static int fileCounter = 0;
	static int folderCounter = 0;

	static String[] folders = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
			"r", "s", "t", "u", "v", "w", "x", "y", "z", "misc", "outliers" };
	
	static String[] ningManualFolders = {  "b" };

	static String[] excludedFolders = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
			"q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "misc", "outliers" };

	static Set<OWLOntology> sourceOntologies;
	static OWLOntology targetOntology;

	static int limit = 0;

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
	public static void saveOntology(String path, OWLOntology ontology) throws OWLOntologyStorageException {

		IRI owlDiskFileIRIForSave;
		owlDiskFileIRIForSave = IRI.create("file:" + path);

		// Save Ontology
		owlOntologyManager.saveOntology(ontology, owlDiskFileIRIForSave);
		System.out.println("File saved as: " + owlDiskFileIRIForSave);
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
	 * Initializes various components
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static void init() throws OWLOntologyCreationException {
		owlOntologyManager = OWLManager.createConcurrentOWLOntologyManager();
		owlDataFactory = owlOntologyManager.getOWLDataFactory();
		Long currentTimeinSeconds = System.currentTimeMillis()/1000;
		LocalDate localdate = LocalDate.now();
		
		customLogFile = customLogFile+"CombineOntology_"+localdate.getMonth()+"_"+localdate.getDayOfMonth()+"_"+localdate.getYear()+"_"+currentTimeinSeconds+".txt";
	}

	/**
	 * This function will load each ontology from disk and add them to
	 * sourceontologis
	 * 
	 * @param path
	 * @throws OWLOntologyCreationException
	 */
	public static void iterateOverFiles(Path path) throws OWLOntologyCreationException {

		// variables
		File f = path.toFile();
		String parentName = f.getParent();
		System.out.println("Path: " + path);

		OWLOntology onto = loadOntology(f);
		if (!sourceOntologies.contains(onto)) {
			sourceOntologies.add(onto);
		}

		fileCounter++;

		printStatus(path.toString());
	}

	/**
	 * This function will try to run each folders inside the a, b, c etc. like for a folders will be
	 * abbey,access_road, .. etc
	 * 
	 * Caveat: some folders have multiple inner folder. like: arena folder has
	 * basketball, Football etc.. folders inside it.
	 * 
	 * @param path
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 */
	public static void iterateOverFolders(Path path) throws OWLOntologyCreationException, OWLOntologyStorageException {

		String folderName = path.getFileName().toString();

		for (String excludedFolder : excludedFolders) {
			if (folderName.equals(excludedFolder)) {
				// System.out.println("excluded path: "+ path);
				return;
			}
		}

		String ontoId = "";
		String parentFolder = path.getParent().getFileName().toString();
		String savingPath = "";
		ArrayList<String> exF = new ArrayList<>(Arrays.asList(excludedFolders));

		if (exF.contains(parentFolder)) {
			savingPath = path.toFile().getAbsolutePath() + "/" + folderName + ".owl";
		} else {
			savingPath = path.toFile().getAbsolutePath() + "/" + folderName + "_" + parentFolder + ".owl";
		}

		// initialize newly created ontology and set of source ontology
		sourceOntologies = new HashSet<OWLOntology>();
		IRI ontoIRI = IRI.create(path.toString());
		targetOntology = owlOntologyManager.createOntology(ontoIRI);

		// System.out.println("saving path: " + savingPath);
		
		//here combine only those owl files which are atomic. i.e. which has ADE in names
		try {
			Files.walk(path).filter(f -> f.toFile().isFile()).filter(d -> limit < 2)
					.filter(f -> f.getFileName().toString().contains("ADE"))
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {
						try {
							// here some of them may be folder why ?
							iterateOverFiles(f);
						} catch (OWLOntologyCreationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					});
		} catch (IOException e) {

			e.printStackTrace();
		}

		// merge them
		ontologyMerger = new OntologyMerger(owlOntologyManager, sourceOntologies, targetOntology);
		ontologyMerger.mergeOntologies();

		// save the new ontology
		saveOntology(savingPath, targetOntology);

		folderCounter++;

	}

	/**
	 * This function will try to run each of a, b, c etc. folders one by one.
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static void runOneByOne() throws OWLOntologyCreationException {

		for (String folder : ningManualFolders) {
			limit++;
			if (limit > 1) {
				break;
			}
			String path = rootPath + "training/" + folder + "/";

			try {
				Files.walk(Paths.get(path)).filter(d -> !d.toFile().isFile()).filter(d -> limit < 2).forEach(d -> {
					try {
						iterateOverFolders(d);
					} catch (OWLOntologyCreationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (OWLOntologyStorageException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			} catch (Exception e) {

			}
		}
	}

	/**
	 * Print status
	 * 
	 * @param status
	 */
	public static void printStatus(String status) {
		try {
			System.out.println("loading owl from file: " + status + " is successfull");
			System.out.println("Processed " + folderCounter + " folders");
			System.out.println("Processed " + fileCounter + " files");
			System.out.println("Size(sourceOntologies): " + sourceOntologies.size());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {

			//init();

			//runOneByOne();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
