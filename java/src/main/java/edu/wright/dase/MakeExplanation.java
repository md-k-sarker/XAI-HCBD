package edu.wright.dase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

	private static final Logger logger = LoggerFactory.getLogger(MakeExplanation.class);
	final static int maxNegativeInstances = 20;
	final static int totalInstances = 640000;
	final static int totalClasses = 7500;
	
	// file storages
	static String sumoFilePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo/SUMO.owl";
	static String rootPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/";
	static String fullADE20KAsOntology = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/ade20k_full.owl";
	
	static OWLDataFactory owlDataFactory;
	static String prefix = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
	static OWLOntologyManager owlOntologyManager;
	static OWLReasonerFactory reasonerFactory; // = new PelletReasonerFactory();		
	static OWLReasoner owlReasoner;
	
	static OWLOntology sumoOntology;
	static OWLOntology ade20KOntology;
	static OWLOntology combinedOntology;
	static IRI owlDiskFileIRIForSave;
	static int counter = 0;
	static String [] excludedFolders = {"images","training","validation","a","b","c","d","e","f","g","h",
			"i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"}; 
	static ArrayList<Integer> randomClassIndex = new ArrayList<Integer>();
	
	/**
	 * Initializes various components
	 * @throws OWLOntologyCreationException 
	 */
	public static void init() throws OWLOntologyCreationException {
		owlOntologyManager = OWLManager.createConcurrentOWLOntologyManager();
		owlDataFactory = owlOntologyManager.getOWLDataFactory();
		IRI ontoIRI = IRI.create(fullADE20KAsOntology);
		combinedOntology = owlOntologyManager.createOntology(ontoIRI);
		reasonerFactory = new PelletReasonerFactory();
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
	
	
	// is it working without file:, i.e. fullPath = "file:" + fullPath ?;
	// Checked this saveOntology() function.  
	// do not save in disk for each combined ontology. it takes too much space in disk.
	// each combined ontology is being approximately 80MB.
	// 80 MB * 22000 = 2000,000 MB = 2000 GB
	// saveOntology(fullPath);
	
	/**
	 * Save combined ontology to file system
	 * @throws OWLOntologyStorageException
	 */
	public static void saveOntology(String path) throws OWLOntologyStorageException {
		
		owlDiskFileIRIForSave = IRI.create("file:" + path); 
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
	
	
	/**
	 * Iterate over folders to make positive and negative instances and then run DL-Learner
	 * @param path
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws ComponentInitException
	 */
	public static void iterateOverFolders(Path path) throws OWLOntologyCreationException, OWLOntologyStorageException, ComponentInitException {
		
		
		// no need to do explanation because these folders do not have any instances
		for (String name : excludedFolders) {
			if (path.getFileName().toString().equals(name)) {
				System.out.println("Directory " + path + " is excluded");
				return;
			}
		}
		

		//variables
		boolean tookPositiveExamples = false;
		Set<OWLNamedIndividual> posExamples = new HashSet<OWLNamedIndividual>();
		Set<OWLNamedIndividual> negExamples = new HashSet<OWLNamedIndividual>();
	
		int classCounter = 0;
		String folderName = path.getFileName().toString();
		String writeTo = path.toAbsolutePath().toString() +"/"+folderName+ "_expl.txt";
		System.out.println("writeTo: " + writeTo);
		String parentFolderName = path.getParent().getFileName().toString();
		String owl_class_name = folderName;
		
		
		//String parentFolderName = path.getParent().getFileName().toString();
		//String owl_super_class_name = "";
		
		// Condition
		// If parent name is misc, then parent folder name is misc
		// If grandparent is not a....z or outliers then class name should be
		// parent_name and grand_parent_name
		if (folderName.equals("misc")) {
			owl_class_name = "misc";
		} else if ((parentFolderName.length() == 1 || parentFolderName.equals("outliers"))) {
			owl_class_name = folderName;
		} else {
			owl_class_name = folderName + "_" + parentFolderName;
			//owl_super_class_name = parentFolderName;
		}
		
		
		//make positive class
		// create class
		IRI iriClass = IRI.create(prefix + owl_class_name);
		OWLClass thisOwlClass = owlDataFactory.getOWLClass(iriClass);
		System.out.println("Class: "+ thisOwlClass.getIRI().getShortForm());
		
		/**
		 * take instance for positive class
		 * i.e. from this class
		 * take negative instances
		 * i.e. from all other classes without this class
		 * take 10 instances randomly
		 */
		for (OWLClass owlClass : ade20KOntology.getClassesInSignature()) {
			if(owlClass.getIRI().getShortForm().equals(thisOwlClass.getIRI().getShortForm())) {
				
				posExamples = owlReasoner.getInstances(owlClass, false).getFlattened();
				tookPositiveExamples = true;
				
			}else if(randomClassIndex.contains(classCounter)){
				
				Set<OWLNamedIndividual> negExamples_ = owlReasoner.getInstances(owlClass, false).getFlattened();
				if(negExamples_.size() > 0) {
					negExamples.add(negExamples_.iterator().next());
					if(tookPositiveExamples && negExamples.size() >=10 ) {
						break;
					}
				}
				
			}
			classCounter++;
		}
		
		System.out.println("PosExamples: ");
		for(OWLNamedIndividual posIndi: posExamples) {
			System.out.println(posIndi.getIRI().toString());
		}
		System.out.println("\nNegExamples: ");
		for(OWLNamedIndividual negIndi: negExamples) {
			System.out.println(negIndi.getIRI().toString());
		}
		
		
		//call to run dl-learner
		DLLearner dlLearner = new DLLearner(combinedOntology, posExamples, posExamples);
		CELOE expl = dlLearner.run();
		
		counter++;
		
		writeStatistics(expl, writeTo);
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
	
	/**
	 * Write statistics to file/disk
	 * @param expl
	 * @param fileName
	 */
	public static void writeStatistics(CELOE expl, String fileName) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter( new FileWriter( fileName));
		
			writer.write("\n########################\n");
			
			LinkedList bestClassesDescription = (LinkedList) expl.getCurrentlyBestDescriptions();
	
			LinkedList best100Classes = (LinkedList) expl.getCurrentlyBestDescriptions(100);
			writer.write("Best 100 Class- \n");
			for (int i = 0; i < best100Classes.size(); i++) {
				System.out.println("Best 100 Class- " + i + " :" + best100Classes.get(i));
				writer.write(best100Classes.get(i) + "\n");
			}
			
			TreeSet bestEvalClasses = (TreeSet) expl.getCurrentlyBestEvaluatedDescriptions();
			Iterator it = bestEvalClasses.iterator();
			writer.write("\nBest Eval Class- \n");
			while (it.hasNext()) {
				System.out.println("Best Eval Class- :" + it.next());
				writer.write(it.next()+"\n");
			}
			
			writer.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * print status
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

	
	public static void main(String[] args) {
		try {
			
			init();
			
			//load sumo
			sumoOntology = loadOntology(new File(sumoFilePath));
			//load super ade20kfull.owl
			ade20KOntology = loadOntology(new File(fullADE20KAsOntology));
			
			Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
			ontologies.add(sumoOntology);
			ontologies.add(ade20KOntology);
			
			//merge ontoligies
			OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies, combinedOntology);
			merger.mergeOntologies();
			
			owlReasoner = reasonerFactory.createNonBufferingReasoner(combinedOntology);
			
			for(int i=0;i<maxNegativeInstances;i++) {
				randomClassIndex.add( ThreadLocalRandom.current().nextInt(0, totalClasses));
			}
			
			Files.walk(Paths.get(rootPath)).filter(d -> counter < 1).filter(d -> ! d.toFile().isFile()).forEach(d -> {
						try {
							
								iterateOverFolders(d);
							
						} catch (ComponentInitException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
						catch (OWLOntologyCreationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (OWLOntologyStorageException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
