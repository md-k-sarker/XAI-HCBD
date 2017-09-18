package edu.wright.dase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

/**
 * MakeExplanation.java
 * Creates explanation for each class.
 * 
 * It imports ontology from sumo and combine it with wordnet & ade20k ontology.
 * Then it call RunAlgorithmClass to get the explanation.
 * Then it saves the explanation in hard disk.
 * 
 * @author sarker
 *
 */

public class MakeExplanation {

	
	// file storages
	static String sumoFilePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo/SUMO.owl";
	static String rootPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/";
	static String fullADE20KAsOntology = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/ade20k_full.owl";
	
	static OWLDataFactory owlDataFactory;
	public static String prefix = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
	static OWLOntologyManager owlOntologyManager;
	static OWLOntology sumoOntology;
	static OWLOntology ade20KOntology;
	static OWLOntology combinedOntology;
	static IRI owlDiskFileIRIForSave;
	public static int counter = 0;
	static String [] excludedFolders = {"training","validation","a","b","c","d","e","f","g","h",
			"i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"}; 
	
	/**
	 * Initializes various components
	 * @throws OWLOntologyCreationException 
	 */
	public static void initOWLAPI() throws OWLOntologyCreationException {
		owlOntologyManager = OWLManager.createConcurrentOWLOntologyManager();
		owlDataFactory = owlOntologyManager.getOWLDataFactory();
		IRI ontoIRI = IRI.create(fullADE20KAsOntology);
		combinedOntology = owlOntologyManager.createOntology(ontoIRI);
	}
	
	/**
	 * It takes the ontologies and combine them with super fullADE20KAsOntology.owl ontology.
	 * @throws OWLOntologyCreationException 
	 */
	public static void combineOntology(String...owlFiles) throws OWLOntologyCreationException {
		
		if(owlFiles.length == 0) {
			combinedOntology = null;
			return;
		}
		
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		OWLOntology ontology;
		File ontoFile;
		
		// add sumo ontology. Sumo ontology is already loaded to memory
		// ontologies.add(sumoOntology);
		
		//load ontologies from files and add to set
		for(int i=0;i<owlFiles.length;i++) {
			ontoFile = new File(owlFiles[i]);
			ontology = loadOntology(ontoFile);
			ontologies.add(ontology);
		}
		
		OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies, combinedOntology);
		merger.mergeOntologies();
		
	}
	
	/**
	 * Combine sumo ontology with combinedOntology
	 */
	public static void combineSumoOntology() {
		
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		ontologies.add(sumoOntology);
		
		OntologyMerger merger = new OntologyMerger(owlOntologyManager,  ontologies, combinedOntology);
		merger.mergeOntologies();
	}
	
	/**
	 * Save combined ontology to file system
	 * @throws OWLOntologyStorageException
	 */
	public static void saveOntology(String path) throws OWLOntologyStorageException {
		
		owlDiskFileIRIForSave = IRI.create("file:" + path); 
				
		// Save Ontology
		owlOntologyManager.saveOntology(combinedOntology, owlDiskFileIRIForSave);
		
	}
	
	/**
	 * Load ontology from file system
	 * @throws OWLOntologyCreationException
	 */
	public static OWLOntology loadOntology(File ontoFile) throws OWLOntologyCreationException {

		OWLOntology ontology = owlOntologyManager.loadOntologyFromOntologyDocument(ontoFile);
		return ontology;

	}
	
	
	public static void iterateOverFolders(Path path) throws OWLOntologyCreationException, OWLOntologyStorageException {
		
		
		
		counter++;
		
		//variables
		File f = path.toFile();
		String folderName = path.getFileName().toString();
		String newFileName = "ADE_train_"+"m_w_sumo_"+folderName.replaceAll("ADE_train_", "");
		String fullPath = f.getAbsolutePath().toString();
		fullPath = fullPath.replaceAll(folderName, newFileName);
		//fullPath = "file:" + fullPath;
		
		String parent = path.getParent().getFileName().toString();
		String grandParent = path.getParent().getParent().getFileName().toString();
		String owl_class_name = "";
		String owl_super_class_name = "";

		// Condition
		// If parent name is misc, then parent folder name is misc
		// If grandparent is not a....z or outliers then class name should be
		// parent_name and grand_parent_name
		if (parent.equals("misc")) {
			owl_class_name = "misc";
		} else if ((grandParent.length() == 1 || grandParent.equals("outliers"))) {
			owl_class_name = parent;
		} else {
			owl_class_name = parent + "_" + grandParent;
			owl_super_class_name = grandParent;
		}
		
		
		
		//make positive class
		// create class
		IRI iriClass = IRI.create(prefix + owl_class_name);
		OWLClass owlClass = owlDataFactory.getOWLClass(iriClass);
		
		owlClass.getIndividualsInSignature();
		
		
		
		// take instance for positive class
		// i.e. from this class
		//Set<OWLNamedIndividual> posExamples = thisOntology.getIndividualsInSignature(Imports.INCLUDED);
				
		// take negative instances
		// i.e. from all other classes without this class
		// take 10 instances randomly
		
		
		//combine this ontology with super ontology
		combineOntology(path.toString());
		
		// Checked this function. it is working without file:, i.e. fullPath = "file:" + fullPath; 
		// do not save in disk. it takes too much space in disk.
		// each combined ontology is being approximately 80MB.
		// 80 MB * 22000 = 2000,000 MB = 2000 GB
		// saveOntology(fullPath);
		printStatus(path.toString());
		
		// setup configurations
		
		
		// feed dl-learner the configurations
		
		
		// print summary of the explanation
		
		// save all the explanations in disk
		
		

//
//		// call to create owl files
//		try {
//			// System.out.println("#########: "+ path.toString());
//			// D:\QQDownload\ADE20K_2016_07_26\ADE20K_2016_07_26\images\training\a\airport_terminal\ADE_train_00001150_atr.txt
//			createOWL(path, owl_class_name, shouldCreateSuperClass, owl_super_class_name);
//			printStatus(path.toString());
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}

	public static void printStatus(String status) {
		try {
			System.out.println("merging owl from file: " + status + " is successfull");
			System.out.println("Processed " + counter + " files");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public static void main(String[] args) {
		try {
			
			initOWLAPI();
			
			//load sumo
			sumoOntology = loadOntology(new File(sumoFilePath));
			//load super ade20kfull.owl
			ade20KOntology = loadOntology(new File(fullADE20KAsOntology));
			
			Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
			ontologies.add(sumoOntology);
			ontologies.add(ade20KOntology);
			
			//combine ontoligies
			OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies, combinedOntology);
			
			Files.walk(Paths.get(rootPath)).filter(d -> ! d.toFile().isFile()).forEach(d -> {
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
			
			//save combined Ontology to disk.
			saveOntology(fullADE20KAsOntology);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
