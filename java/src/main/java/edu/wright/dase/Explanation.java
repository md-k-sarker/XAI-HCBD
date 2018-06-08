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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.algorithms.celoe.PCELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
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
import edu.wright.dase.util.Constants;
import edu.wright.dase.util.OWLUtility;
import edu.wright.dase.util.Writer;
import uk.ac.manchester.cs.jfact.JFactFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * Explanation.java Creates explanation for each class.
 * <p>
 * It imports ontology from sumo and combine it with wordnet & ade20k ontology.
 * Then it call RunAlgorithmClass to get the explanation. Then it saves the
 * explanation in hard disk.
 *
 * @author sarker
 */

public class Explanation {

    private static final Logger logger = LoggerFactory.getLogger(Explanation.class);
    final static int maxNegativeInstances = 20;
    static int totalInstances = 0;
    static int totalClasses = 0;

    // file storages
    // static String sumoFilePath =
    // "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo_aligned/without_scores/sumo_aligned_without_score.owl";
    // for ning manual
    // static String rootPath =
    // "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";
    // static String rootOntoPath =
    // "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";
    // static String runDlConfWritings =
    // "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/b/";

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
    static String[] excludedFolders = {"images", "training", "validation", "a", "b", "c", "d", "e", "f", "g", "h", "i",
            "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    static String[] ningManualFolders = {"bathroom"};
    // , "bathroom", "butchers_shop", "bullpen", "bridge"
    static String[] OntologyFolders = {"bedroom", "bathroom"};
    static String posInstanceFolderPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/k/kitchen/";
    static String negInstanceFolderPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/training/l/living_room/";
    static String explanationForPath = "/Users/sarker/Workspaces/ProjectHCBD/experiments/Jun_08/neuron_activation_tracing/without_score/ning_v3/";
    static String backgroundOntology = "/Users/sarker/Workspaces/ProjectHCBD/datas/sumo_aligned/without_scores/sumo_aligned_without_score_minimal.owl";
    // "butchers_shop", "bullpen", "bridge"

    static String alreadyGotResultPath = "/home/sarker/MegaCloud/ProjectHCBD/experiments/ade_with_wn_sumo/automated/without_score_got_result/";

    static ArrayList<String> alreadyGotResult = new ArrayList<String>();

    private static int maxExecutionTimeInSeconds = (1 * 600);

    static ArrayList<Integer> randomClassIndex = new ArrayList<Integer>();

    static String[] posStrings = {"bathroom_ADE_train_00000006", "bathroom_ADE_train_00000007",
            "bathroom_ADE_train_00000008", "bathroom_ADE_train_00000009", "bathroom_ADE_train_00000010"};
    static String[] negStrings = {"bedroom_ADE_train_00000192", "bedroom_ADE_train_00000193",
            "conference_room_ADE_train_00000570", "conference_room_ADE_train_00005979",
            "dining_room_ADE_train_00006845", "dining_room_ADE_train_00006846", "hotel_room_ADE_train_00009520",
            "hotel_room_ADE_train_00009521", "kitchen_ADE_train_00000594", "kitchen_ADE_train_00000595",
            "living_room_ADE_train_00000651", "living_room_ADE_train_00000652"};

    static String logFile = "/Users/sarker/Workspaces/ProjectHCBD/experiments/may_13_2018/logs/logger_dl_learner.log";

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
        if (writeTo != null)
            Writer.writeInDisk(writeTo, "Removing " + ca.toString(), true);
        logger.info("Removing " + ca.toString(), true);
        System.out.println("Removing " + ca.toString());

        return combinedOntology;
    }

    /**
     * Alignment with sumo ontology
     *
     * @throws OWLOntologyCreationException
     */
    // public static void alignWithSumo() throws OWLOntologyCreationException {
    // sumoOntology = loadOntology(new File(sumoFilePath));
    //
    // // align Classes
    //
    // /**
    // * // there are a lot of considerations for alignment. So just combine them
    // now.
    // */
    //
    // sourceOntologies.add(sumoOntology);
    // OntologyMerger merger = new OntologyMerger(owlOntologyManager,
    // sourceOntologies, combinedOntology);
    // merger.mergeOntologies();
    //
    // // for (OWLClass classFromSumo : sumoOntology.getClassesInSignature()) {
    // // for (OWLClass classFromADE : combinedOntology.getClassesInSignature()) {
    // //
    // // double distance =
    // // Alignment.computeConfidence(classFromSumo.getIRI().getShortForm(),
    // // classFromADE.getIRI().getShortForm());
    // // if (distance > distanceThreshold) {
    // //
    // //
    // // // align individuals
    // // for (OWLIndividual indi : classFromADE.getIndividualsInSignature()) {
    // // OWLClassAssertionAxiom ax =
    // // owlDataFactory.getOWLClassAssertionAxiom(classFromSumo, indi);
    // // ChangeApplied ca = owlOntologyManager.addAxiom(sumoOntology, ax);
    // // }
    // //
    // // // // align object properties
    // // // for(OWLObjectProperty objProp:
    // // classFromADE.getObjectPropertiesInSignature())
    // // // {
    // // //
    // // // }
    // // }
    // // }
    // // }
    // //
    // // // // align object properties
    // // // for(OWLObjectProperty objProp:
    // // // combinedOntology.getObjectPropertiesInSignature()) {
    // // // objProp.
    // // // }
    // //
    // // // align data properties
    // //
    // // //
    // //
    // //
    // // combinedOntology = sumoOntology;
    // }

    /**
     * This function calls dl-learner to run
     *
     * @param path : folder
     * @throws ComponentInitException
     * @throws OWLOntologyCreationException
     * @throws OWLOntologyStorageException
     */
    public static void tryToCreateExplanations(Path path, String writeTo)
            throws ComponentInitException, OWLOntologyCreationException, OWLOntologyStorageException {

        sourceOntologies = new HashSet<OWLOntology>();

        // variables
        negIndiCounter = 0;
        posIndiCounter = 0;
        String folderName = path.getFileName().toString();
        // String writeTo = path.toAbsolutePath().toString() + "/" + folderName +
        // "_expl.txt";
        // System.out.println("writeTo: " + writeTo);

        logger.info("####### Explanantion for " + folderName + " class ########");
        System.out.println("####### Explanantion for " + folderName + " class ########");
        Writer.writeInDisk(writeTo, "####### Explanantion for " + folderName + " class ########", true);

        logger.info(" initializing load positive and negative instances...");
        System.out.println(" initializing load positive and negative instances...");
        Writer.writeInDisk(writeTo, "\n initializing load positive and negative instances...", true);
        /**
         * load positive and negative instances and corresponding ontologies to add them
         * in backgroundinformation
         */
        // do it manually now
        // loadPosInstancesAndAddForMerging(writeTo);
        // loadNegInstances(writeTo);
        posExamples.clear();
        negExamples.clear();

        for (String indi : posStrings) {
            indi = Constants.prefix + indi;
            IRI iri = IRI.create(indi);
            posExamples.add(owlDataFactory.getOWLNamedIndividual(iri));
        }
        for (String indi : negStrings) {
            indi = Constants.prefix + indi;
            IRI iri = IRI.create(indi);
            negExamples.add(owlDataFactory.getOWLNamedIndividual(iri));
        }

        logger.info(" initializing load positive and negative instances finished");
        System.out.println(" initializing load positive and negative instances finished");
        Writer.writeInDisk(writeTo, "\n initializing load positive and negative instances finished", true);

        logger.info(" loading background Ontology...");
        System.out.println(" loading background Ontology...");
        Writer.writeInDisk(writeTo, "\n loading background Ontology...", true);

        // just load background ontology
        combinedOntology = OWLUtility.loadOntology(new File(backgroundOntology));

        logger.info(" loading background Ontology finished");
        System.out.println(" loading background Ontology finished");
        Writer.writeInDisk(writeTo, "\n loading background Ontology finished", true);

        // reason over ontology
        // create resoner to reason
        owlReasoner = reasonerFactory.createNonBufferingReasoner(combinedOntology);
        totalClasses = combinedOntology.getClassesInSignature().size();
        totalInstances = combinedOntology.getIndividualsInSignature().size();

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
        DLLearner dlLearner = new DLLearner(combinedOntology, posExamples, posExamples, writeTo, maxExecutionTimeInSeconds);
        CELOE expl = dlLearner.run();

        counter++;

        writeStatistics(expl, writeTo);
        printStatus(path.toString());

    }

    /**
     * This function calls dl-learner to run
     *
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
        DLLearner dlLearner = new DLLearner(combinedOntology, posExamples, posExamples, writeTo, maxExecutionTimeInSeconds);
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
//	public static void iterateOverFolders(Path path)
//			throws OWLOntologyCreationException, OWLOntologyStorageException, ComponentInitException {
//
//		// no need to do explanation because these folders do not have any instances
//		for (String name : excludedFolders) {
//			if (path.getFileName().toString().equals(name)) {
//				System.out.println("Directory " + path + " is excluded");
//				return;
//			}
//		}
//
//		// make explanation for only ning suggested folders
//		for (String folder : ningManualFolders) {
//			if (path.getFileName().toString().equals(folder)) {
//
//				// tryToCreateExplanations(path);
//
//			}
//		}
//
//	}

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

            LinkedList best100Classes = (LinkedList) expl.getCurrentlyBestDescriptions(1000);


            System.out.println("Best " + best100Classes.size() + " Classes- using getCurrentlyBestDescriptions()");
            Writer.writeInDisk(fileName, "\nBest " + best100Classes.size() + " Classes- using getCurrentlyBestDescriptions()", true);
            Writer.writeInDisk(fileName, "\n# This is the best class descriptions found by the learning algorithm so far.\n", true);
            for (int i = 0; i < best100Classes.size(); i++) {
                //System.out.println("Best 100 Class- " + i + " :" + best100Classes.get(i));
                // writer.write(best100Classes.get(i) + "\n");
                Writer.writeInDisk(fileName, best100Classes.get(i) + "\n", true);
            }

            // Returns a sorted set of the best descriptions found so far. We assume that they are ordered such that the best ones come in last.
            // (In Java, iterators traverse a SortedSet in ascending order.)
            TreeSet bestEvalClasses = (TreeSet) expl.getCurrentlyBestEvaluatedDescriptions();
            Iterator it = bestEvalClasses.iterator();
            // writer.write("\nBest Eval Class- \n");
            Writer.writeInDisk(fileName, "\n\nBest Evaluated Class- using getCurrentlyBestEvaluatedDescriptions()\n", true);
            Writer.writeInDisk(fileName, "# Returns a sorted set of the best descriptions found so far. "
                    + "\n# We assume that they are ordered such that the best ones come in last."
                    + "\n# (In Java, iterators traverse a SortedSet in ascending order.\n)", true);
            while (it.hasNext()) {
                Object obj = it.next();
                //System.out.println("Best Eval Class- :" + obj);
                // writer.write(it.next()+"\n");
                Writer.writeInDisk(fileName, obj + "\n", true);
            }

            // writer.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    /**
     * Sort the solutions
     *
     * @param ascending
     * @return
     */
    public static void writeStatisticsSorted(CELOE expl, boolean ascending, String fileName) {

        Writer.writeInDisk(fileName, "\n\n####################Solutions####################:\n", true);

        solutionCounter = 0;

        ArrayList<? extends EvaluatedDescription<? extends Score>> solutionList = new ArrayList<>(expl.getCurrentlyBestEvaluatedDescriptions());

        expl.getCurrentlyBestEvaluatedDescriptions().stream().sorted(new Comparator<EvaluatedDescription<? extends Score>>() {
            @Override
            public int compare(EvaluatedDescription<? extends Score> o1, EvaluatedDescription<? extends Score> o2) {
                if (ascending) {
                    if (o1.getAccuracy() - o2.getAccuracy() > 0) {
                        return 1;
                    }
                    if (o1.getAccuracy() == o2.getAccuracy()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    if (o1.getAccuracy() - o2.getAccuracy() > 0) {
                        return -1;
                    }
                    if (o1.getAccuracy() == o2.getAccuracy()) {
                        return 0;
                    } else {
                        return 1;
                    }
                }

            }
        }).forEach(o -> {
            solutionCounter++;
            Writer.writeInDisk(fileName, "solution " + solutionCounter + ": " + String.valueOf(o.getDescription()) + "\n", true);
            Writer.writeInDisk(fileName, "\taccuracy_score: " + String.valueOf(o.getAccuracy()) + "\n", true);
        });

        Writer.writeInDisk(fileName, "\nTotal solutions found: " + solutionCounter, true);

    }


    static int solutionCounter = 0;

    /**
     * Write statistics to file/disk
     *
     * @param expl
     * @param fileName
     */
    public static void writeStatisticsV2(CELOE expl, String fileName) {
        BufferedWriter writer;
        try {

            // writer = new BufferedWriter( new FileWriter( fileName));

            Writer.writeInDisk(fileName, "\n\n####################Solutions####################:", true);

            solutionCounter = 0;

            // Writer.writeInDisk(fileName, "\n# These are the best class descriptions found by the learning algorithm so far.\n", true);


            expl.getCurrentlyBestEvaluatedDescriptions().stream().forEach(o -> {
                solutionCounter++;

                Writer.writeInDisk(fileName, "solution " + solutionCounter + ": " + String.valueOf(o.getDescription()) + "\n", true);
                Writer.writeInDisk(fileName, "\taccuracy_score: " + String.valueOf(o.getAccuracy()) + "\n", true);
            });

            Writer.writeInDisk(fileName, "\nTotal solutions found: " + solutionCounter, true);
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
            System.out.println("explaining instances from : " + status + " is successfull");
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
    private OWLOntology removeNonRelatedEntities() {
        return null;
    }

    /**
     * @param filePath
     * @throws IOException
     */
    public static void readExamplesFromConf(String filePath) throws IOException {

        //Set<OWLNamedIndividual> _posIndivs = new HashSet<OWLNamedIndividual>();
        //Set<OWLNamedIndividual> _negIndivs = new HashSet<OWLNamedIndividual>();
        posExamples = new HashSet<OWLNamedIndividual>();
        negExamples = new HashSet<OWLNamedIndividual>();

        try (BufferedReader buffRead = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = buffRead.readLine()) != null) {
                if (line.startsWith("lp.positiveExamples")) {
                    // remove first { and last }
                    line = line.substring(line.indexOf("{") + 1, line.length() - 1);
                    //System.out.println("line: " + line);
                    String[] indivs = line.split(",");
                    for (String eachIndi : indivs) {
                        eachIndi = eachIndi.trim();
                        // remove "ex: and last "
                        eachIndi = eachIndi.substring(eachIndi.indexOf(":") + 1, eachIndi.length() - 1);
                        IRI iri = IRI.create(Constants.prefix + eachIndi);
                        //System.out.println("eachIndi: "+eachIndi);
                        //System.out.println("iri: "+iri.toString());
                        posExamples.add(OWLManager.getOWLDataFactory().getOWLNamedIndividual(iri));
                    }
                } else if (line.startsWith("lp.negativeExamples")) {
                    // remove first { and last }
                    line = line.substring(line.indexOf("{") + 1, line.length() - 1);
                    //System.out.println("line: " + line);
                    String[] indivs = line.split(",");
                    for (String eachIndi : indivs) {
                        eachIndi = eachIndi.trim();
                        // remove "ex: and last "
                        eachIndi = eachIndi.substring(eachIndi.indexOf(":") + 1, eachIndi.length() - 1);
                        IRI iri = IRI.create(Constants.prefix + eachIndi);
                        negExamples.add(OWLManager.getOWLDataFactory().getOWLNamedIndividual(iri));
                    }
                }

            }
        } catch (Exception ex) {
            System.out.println("Fatal error: ");
            ex.printStackTrace();
        }

        // just for debug print
//		System.out.println("PosExamples: ");
//		for(OWLNamedIndividual indi: posExamples) {
//			System.out.println(indi);
//		}
//		
//		System.out.println("\nNegExamples: ");
//		for(OWLNamedIndividual indi: negExamples) {
//			System.out.println(indi);
//		}
    }

    /**
     * Will get each owl file path
     */
    private static void tryToCreateExplanationDemo(Path path) {

        String writeToPosNeg = path.toFile().getAbsolutePath().replace(".conf", "_expl_by_dl_learner.txt");
        try {

            // will get each file
            // now run explanation for each owl, i.e. for each image

            /**
             * Try to run Dl-Learner 2 times
             */

            // 1 for pos and neg
            //String confFilePath = path.toFile().getAbsolutePath().replace(".owl", ".conf");
            //String writeToPosNeg = path.toFile().getAbsolutePath().replace(".owl", "_expl_pos_neg.txt");

            DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy  HH.mm.ss a");
            Long programStartTime = System.currentTimeMillis();

            Writer.writeInDisk(writeToPosNeg, "Program started.................", false);
            Writer.writeInDisk(writeToPosNeg, "Program starts at: " + dateFormat.format(new Date()), true);
            Writer.writeInDisk(writeToPosNeg, "Working with confFile: " + path.toFile().getAbsolutePath(), true);
            Writer.writeInDisk(writeToPosNeg, "Ontology Path: " + backgroundOntology, true);

            System.out.println("Program started.................");
            System.out.println("Program starts at: " + OWLUtility.getCurrentTimeAsString());
            System.out.println("Working with confFile: " + path.toFile().getAbsolutePath());
            System.out.println("Ontology Path: " + backgroundOntology);


            OWLOntology _onto = null;
            /**
             * load background ontology
             */
            try {
                _onto = OWLUtility.loadOntology(new File(backgroundOntology));
                //reasonerFactory = new PelletReasonerFactory();
                reasonerFactory = new JFactFactory();
                owlReasoner = reasonerFactory.createNonBufferingReasoner(_onto);
            } catch (Exception ex) {
                System.out.println("Error in reading ontology");
                System.exit(-1);
            }
            /**
             * Get/load pos and neg examples from txt file
             */
            readExamplesFromConf(path.toFile().getAbsolutePath());
            Set<OWLNamedIndividual> allExamples = new HashSet<OWLNamedIndividual>();

            allExamples.addAll(posExamples);
            allExamples.addAll(negExamples);

            OWLOntology mod_onto = OWLUtility.removeNonRelatedIndividuals(_onto, owlReasoner, allExamples, null);
            owlReasoner = reasonerFactory.createNonBufferingReasoner(mod_onto);

            OWLOntology _mod_onto = OWLUtility.removeNonRelatedConcepts(mod_onto, owlReasoner);


            Long algoStartTime = System.currentTimeMillis();
            Writer.writeInDisk(writeToPosNeg, "Algorithm started.................", false);
            Writer.writeInDisk(writeToPosNeg, "\nAlgorithm starts at: " + dateFormat.format(new Date()), true);

            System.out.println("PosExamples: ");
            Writer.writeInDisk(writeToPosNeg, "\n\nPosExamples: ", true);
            for (OWLNamedIndividual indi : posExamples) {
                System.out.println("\t" + indi);
                Writer.writeInDisk(writeToPosNeg, "\n\t" + indi.getIRI().getShortForm(), true);
            }

            System.out.println("\nNegExamples: ");
            Writer.writeInDisk(writeToPosNeg, "\n\nNegExamples:", true);
            for (OWLNamedIndividual indi : negExamples) {
                System.out.println("\t" + indi);
                Writer.writeInDisk(writeToPosNeg, "\n\t" + indi.getIRI().getShortForm(), true);
            }

            Writer.writeInDisk(writeToPosNeg, "\n\n", true);

            // call to run dl-learner
            DLLearner dlLearner = new DLLearner(_mod_onto, posExamples, posExamples, writeToPosNeg, maxExecutionTimeInSeconds);
            CELOE expl = dlLearner.run(true);

            Long algoEndTime = System.currentTimeMillis();
            Writer.writeInDisk(writeToPosNeg, "\nAlgorithm ends at: " + dateFormat.format(new Date()), true);
            Writer.writeInDisk(writeToPosNeg, "\nAlgorithm duration: " + (algoEndTime - algoStartTime) / 1000 + " sec", true);

            counter++;

            writeStatisticsSorted(expl, false, writeToPosNeg);
            printStatus(path.toString());

//            Writer.writeInDisk(writeToPosNeg, "\nProgram " + "\n finished at: " + OWLUtility.getCurrentTimeAsString(), true);
//            System.out.println("Explanation for: " + path.toFile().getAbsolutePath() +
//                    "\n finished at: " + OWLUtility.getCurrentTimeAsString());

            // 1 for  pos and neg

            Long programEndTime = System.currentTimeMillis();
            Writer.writeInDisk(writeToPosNeg, "\nProgram ends at: " + dateFormat.format(new Date()), true);
            Writer.writeInDisk(writeToPosNeg, "\nProgram duration: " + (programEndTime - programStartTime) / 1000 + " sec", true);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Writer.writeInDisk(writeToPosNeg, "\n!!!!!!!!Fatal ERROR!!!!!!!!!!!\n" + OWLUtility.getStackTraceAsString(e), true);
        }
    }

    /**
     * Will get a folder
     */
    private static void iterateOverFolders(Path path) {


        if (explanationForPath.equals(path.toString() + "/")) {
            // System.out.println("equal");
            return;
        }

        //System.out.println("Folder: "+ path.toString());

        try {
            // iterate over the files of a folder
            Files.walk(path).filter(f -> f.toFile().isFile()).filter(f -> f.toFile().getAbsolutePath().endsWith(".conf")).limit(1).forEach(f -> {
                // will get each file
                if (alreadyGotResult.contains(f.toFile().getName())) {
                    System.out.println(f.toString() + " already has result not running it");
                } else {
                    //System.out.println("Not matched "+ f.toString());
                    tryToCreateExplanationDemo(f);
                }

            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Writer.writeInDisk(logFile, "!!!!!!!Fatal error!!!!!!!\n" + OWLUtility.getStackTraceAsString(e), true);
        }
    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {

        int i = 0;
        Writer.writeInDisk(logFile, "Main Program started at: " + OWLUtility.getCurrentTimeAsString(), false);
//        try {
//            Files.walk(Paths.get(alreadyGotResultPath)).filter(f -> f.toFile().isFile()).
//                    filter(f -> f.toFile().getAbsolutePath().endsWith(".txt")).forEach(f -> {
//                alreadyGotResult.add(f.toFile().getName().replaceAll("_expl_pos_only.txt", ".conf"));
//                Writer.writeInDisk(logFile, "\nAlready got result for: " + f.toString(), true);
//            });
//        } catch (Exception ex) {
//            Writer.writeInDisk(logFile, "\n\n!!!!!!!Fatal error!!!!!!!\n" + OWLUtility.getStackTraceAsString(ex), true);
//        }

        OWLOntology _onto;
        try {

            // start with root folder
            Files.walk(Paths.get(explanationForPath)).filter(d -> d.toFile().isDirectory()).forEach(d -> {
                try {
                    iterateOverFolders(d);
                    Writer.writeInDisk(logFile, "Running on folder: " + d.toString(), true);
                } catch (Exception ex) {
                    System.out.println("Error occurred");
                    ex.printStackTrace();
                    Writer.writeInDisk(logFile, "\n\n!!!!!!!Fatal error!!!!!!!\n" + OWLUtility.getStackTraceAsString(ex), true);
                    System.exit(0);
                }
            });

//			_onto = OWLUtility.loadOntology(new File(backgroundOntology));
//			reasonerFactory = new PelletReasonerFactory();
//			owlReasoner = reasonerFactory.createNonBufferingReasoner(_onto);
//			
//			String confFilePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ning_manual/DL_tensorflow_save_v3_txts_as_dirs_owl_without_score_without_wordnet/Bathroom/bathroom_ADE_train_00000006.conf";
//			readExamplesFromConf(confFilePath);
//			Set<OWLNamedIndividual> allExamples = new HashSet<OWLNamedIndividual>();
//			allExamples.addAll(posExamples);
//			allExamples.addAll(negExamples);
//			
//			OWLOntology mod_onto = OWLUtility.removeNonRelatedIndividuals(_onto, owlReasoner, allExamples, null);
//			owlReasoner = reasonerFactory.createNonBufferingReasoner(mod_onto);
//			
//			OWLOntology _mod_onto = OWLUtility.removeNonRelatedConcepts(mod_onto,owlReasoner);
//			
//			String saveTo = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo_aligned/without_scores/sumo_with_imgContains_without_score_without_wordnet_minimal_rnr__.owl";
//			
//			OWLUtility.saveOntology(_mod_onto, saveTo);
        }
//		catch (OWLOntologyCreationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}catch (OWLOntologyStorageException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        if (i == 0)
            return;

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

            tryToCreateExplanations(Paths.get(explanationForPath), writeTo);

            long endTime = System.currentTimeMillis();
            Long executionTimeInMinute = (endTime - startTime) / (60 * 1000);

            logger.info("Program finsihed after: " + executionTimeInMinute + " minutes");
            System.out.println("Program finsihed after: " + executionTimeInMinute + " minutes");
            Writer.writeInDisk(writeTo, "\n\n\n Program finsihed", true);
            Writer.writeInDisk(writeTo, "\nProgram run for: " + (endTime - startTime) / 1000 + " seconds", true);
            Writer.writeInDisk(writeTo, "\nProgram run for: " + executionTimeInMinute + " minutes", true);

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
