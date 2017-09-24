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

import edu.wright.dase.util.Writer;

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
	static int totalInstances = 640000;
	static int totalClasses = 7500;

	// file storages
	static String sumoFilePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo/sumo_full.owl";
	// for ning manual
	static String rootPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";
	static String rootOntoPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";
	static String runDlConfWritings = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";

	// static String fullADE20KAsOntology =
	// "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/ade20k_full.owl";

	static OWLDataFactory owlDataFactory;
	static String prefix = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
	static OWLOntologyManager owlOntologyManager;
	static OWLReasonerFactory reasonerFactory; // = new PelletReasonerFactory();
	static OWLReasoner owlReasoner;

	static OWLOntology sumoOntology;
	// static OWLOntology ade20KOntology;
	static OWLOntology combinedOntology;
	static IRI owlDiskFileIRIForSave;
	static int counter = 0;
	static String[] excludedFolders = { "images", "training", "validation", "a", "b", "c", "d", "e", "f", "g", "h", "i",
			"j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };
	static String[] ningManualFolders = { "bedroom", "bathroom", "butchers_shop", "bullpen", "bridge" };
	static String[] OntologyFolders = { "bedroom", "bathroom", "butchers_shop", "bullpen", "bridge" };

	static ArrayList<Integer> randomClassIndex = new ArrayList<Integer>();

	/**
	 * Initializes various components
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static void init() throws OWLOntologyCreationException {
		owlOntologyManager = OWLManager.createConcurrentOWLOntologyManager();
		owlDataFactory = owlOntologyManager.getOWLDataFactory();
		IRI ontoIRI = IRI.create(rootOntoPath);
		combinedOntology = owlOntologyManager.createOntology(ontoIRI);
		reasonerFactory = new PelletReasonerFactory();
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
	 * This function calls dl-learner to run
	 * @param path
	 * @throws ComponentInitException 
	 */
	public static void tryToCreateExplanations(Path path) throws ComponentInitException {
		
		//variables 
		boolean tookPositiveExamples = false;
		Set<OWLNamedIndividual> posExamples = new HashSet<OWLNamedIndividual>();
		Set<OWLNamedIndividual> negExamples = new HashSet<OWLNamedIndividual>();

		int classCounter = 0;
		String folderName = path.getFileName().toString();
		String writeTo = path.toAbsolutePath().toString() + "/" + folderName + "_expl.txt";
		System.out.println("writeTo: " + writeTo);
		String parentFolderName = path.getParent().getFileName().toString();
		String owl_class_name = folderName;

		// String parentFolderName = path.getParent().getFileName().toString();
		// String owl_super_class_name = "";

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
			// owl_super_class_name = parentFolderName;
		}

		// make positive class
		// create class
		IRI iriClass = IRI.create(prefix + owl_class_name);
		OWLClass thisOwlClass = owlDataFactory.getOWLClass(iriClass);
		System.out.println("Class: " + thisOwlClass.getIRI().getShortForm());

		/**
		 * take instance for positive class i.e. from this class take negative instances
		 * i.e. from all other classes without this class take 10 instances randomly
		 */
		for (OWLClass owlClass : combinedOntology.getClassesInSignature()) {
			if (owlClass.getIRI().getShortForm().equals(thisOwlClass.getIRI().getShortForm())) {

				posExamples = owlReasoner.getInstances(owlClass, false).getFlattened();
				tookPositiveExamples = true;

			} else if (randomClassIndex.contains(classCounter)) {

				Set<OWLNamedIndividual> negExamples_ = owlReasoner.getInstances(owlClass, false).getFlattened();
				if (negExamples_.size() > 0) {
					negExamples.add(negExamples_.iterator().next());
					if (tookPositiveExamples && negExamples.size() >= 10) {
						break;
					}
				}

			}
			classCounter++;
		}

		// create explanation file
		Writer.writeInDisk(writeTo, "####### Explanantion for " + folderName + " class ########", false);

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
	private static void combineOntology() throws OWLOntologyCreationException {

		logger.info(" initializing combineOntology...");
		System.out.println(" initializing combineOntology...");
		Writer.writeInDisk(runDlConfWritings, "\n initializing combineOntology...", true);
		
		// declare ontology set
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();

		// load sumo
		sumoOntology = loadOntology(new File(sumoFilePath));
		ontologies.add(sumoOntology);

		// load specific ontology from ade20k dataset.
		for (String folder : OntologyFolders) {
			String ontoPath = rootOntoPath + folder +"/"+folder+ ".owl";
			File ontoFile = new File(ontoPath);
			if (ontoFile.exists()) {
				OWLOntology ontology = loadOntology(new File(ontoPath));
				ontologies.add(ontology);
				logger.info("\nOntoFile "+ ontoFile.getAbsolutePath() +" \n found. \n");
				System.out.println("\n loading OntoFile "+ ontoFile.getAbsolutePath() +" \n");
				Writer.writeInDisk(runDlConfWritings, "\n Adding OntoFile "+ ontoFile.getAbsolutePath() +" \n", true);
			}else {
				logger.info("\nOntoFile "+ ontoFile.getAbsolutePath() +" \n not found. \n");
				System.out.println("\nOntoFile "+ ontoFile.getAbsolutePath() +" \n not found. \n");
				Writer.writeInDisk(runDlConfWritings, "\nOntoFile "+ ontoFile.getAbsolutePath() +" \n not found. \n", true);
			}
		}

		// merge ontoligies
		OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies, combinedOntology);
		merger.mergeOntologies();

		logger.info("finished initializing combineOntology");
		System.out.println("finished initializing combineOntology");
		Writer.writeInDisk(runDlConfWritings, "\n finished initializing combineOntology", true);
	}

	public static void main(String[] args) {
		try {

			long startTime = System.currentTimeMillis();


			runDlConfWritings = runDlConfWritings+ "run_dl_in_b_folder.txt";
			logger.info("Program started...........");
			System.out.println("Program started...........");
			Writer.writeInDisk(runDlConfWritings, "\n Program started...........", false);
			
			init();

			// combine necessary ontologies
			combineOntology();

			// create resoner to reason
			owlReasoner = reasonerFactory.createNonBufferingReasoner(combinedOntology);

			totalClasses = combinedOntology.getClassesInSignature().size();

			for (int i = 0; i < maxNegativeInstances; i++) {
				randomClassIndex.add(ThreadLocalRandom.current().nextInt(0, totalClasses));
			}

			Files.walk(Paths.get(rootPath)).filter(d -> !d.toFile().isFile()).forEach(d -> {
				try {

					iterateOverFolders(d);

				} catch (ComponentInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OWLOntologyCreationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			
			long endTime = System.currentTimeMillis();
			
			logger.info("Program finsihed");
			System.out.println("Program finsihed");
			Writer.writeInDisk(runDlConfWritings, "\n Program finsihed", true);
			Writer.writeInDisk(runDlConfWritings, "\nProgram run for: " + (endTime - startTime)/1000 + " seconds", true);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
