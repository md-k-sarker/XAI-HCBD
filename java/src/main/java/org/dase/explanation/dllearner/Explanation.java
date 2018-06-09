package org.dase.explanation.dllearner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.dase.util.*;
import org.dase.util.Writer;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * Explanation_old.java Creates explanation for each class.
 * <p>
 * It imports ontology from sumo and combine it with wordnet & ade20k ontology.
 * Then it call RunAlgorithmClass to get the explanation. Then it saves the
 * explanation in hard disk.
 *
 * @author sarker
 */

public class Explanation {

    private static final Logger logger = LoggerFactory.getLogger(Explanation.class);


    private static Set<OWLOntology> sourceOntologies;
    private static OWLOntology sumoOntology;
    private static OWLOntology combinedOntology;
    private static OWLOntologyManager owlOntologyManager;
    private static OWLDataFactory owlDataFactory;
    private static OWLReasoner owlReasoner;
    private static OWLReasonerFactory reasonerFactory; // = new PelletReasonerFactory();
    private static PrintStream outPutStream;
    private static Monitor monitor;
    private static IRI owlDiskFileIRIForSave;


    private static Set<OWLNamedIndividual> posExamples = new HashSet<OWLNamedIndividual>();
    private static Set<OWLNamedIndividual> negExamples = new HashSet<OWLNamedIndividual>();
    private static int negIndiCounter = 0;
    private static int posIndiCounter = 0;
    private static int totalInstances = 0;
    private static int totalClasses = 0;
    private static int operationsCounter = 0;
    private static int maxExecutionTimeInSeconds = (1 * 6);


    private static String explanationForPath = "/Users/sarker/Workspaces/ProjectHCBD/experiments/Jun_08/neuron_activation_tracing/without_score/ning_v3/";
    private static String backgroundOntologyPath = "/Users/sarker/Workspaces/ProjectHCBD/datas/sumo_aligned/without_scores/sumo_aligned_without_score_minimal.owl";
    private static String alreadyGotResultPath = "/home/sarker/MegaCloud/ProjectHCBD/experiments/ade_with_wn_sumo/automated/without_score_got_result/";
    private static String logFile = "/Users/sarker/Workspaces/ProjectHCBD/experiments/may_13_2018/logs/logger_dl_learner.log";
    private static String[] excludedFolders = {"images", "training", "validation", "a", "b", "c", "d", "e", "f", "g", "h", "i",
            "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    private static ArrayList<String> alreadyGotResult = new ArrayList<String>();


    /**
     * Initializes various components
     *
     * @throws OWLOntologyCreationException
     */
    public static void init() throws OWLOntologyCreationException {
        owlOntologyManager = OWLManager.createConcurrentOWLOntologyManager();
        owlDataFactory = owlOntologyManager.getOWLDataFactory();
        IRI ontoIRI = IRI.create(ConfigParams.namespace);
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
        try {
            combinedOntology = Utility.loadOntology(backgroundOntologyPath);
        } catch (Exception ex) {
            logger.error("Ontology loading errror!!!!!!!!!!!!!!, program exiting");
            logger.error(Utility.getStackTraceAsString(ex));
            System.exit(-1);
        }
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

        operationsCounter++;

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
     * print status
     *
     * @param status
     */
    public static void printStatus(String status) {
        try {
            System.out.println("explaining instances from : " + status + " is successfull");
            System.out.println("Processed " + operationsCounter + " files");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    /**
     * Will get each confFilePath
     * @param confFilePath
     */
    private static void tryToCreateExplanationDemo(Path confFilePath) {

        String writeToPosNeg = confFilePath.toFile().getAbsolutePath().replace(".conf", "_expl_by_dl_learner.txt");
        try {

            DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy  HH.mm.ss a");
            Long programStartTime = System.currentTimeMillis();

            Writer.writeInDisk(writeToPosNeg, "Program started.................", false);
            Writer.writeInDisk(writeToPosNeg, "Program starts at: " + dateFormat.format(new Date()), true);
            Writer.writeInDisk(writeToPosNeg, "Working with confFile: " + confFilePath.toFile().getAbsolutePath(), true);
            Writer.writeInDisk(writeToPosNeg, "Ontology Path: " + backgroundOntologyPath, true);

            System.out.println("Program started.................");
            System.out.println("Program starts at: " + Utility.getCurrentTimeAsString());
            System.out.println("Working with confFile: " + confFilePath.toFile().getAbsolutePath());
            System.out.println("Ontology Path: " + backgroundOntologyPath);


            OWLOntology _onto = null;
            /**
             * load background ontology
             */
            try {
                _onto = Utility.loadOntology(backgroundOntologyPath);
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
            posExamples = Utility.readPosExamplesFromConf(confFilePath.toFile().getAbsolutePath());
            negExamples = Utility.readNegExamplesFromConf(confFilePath.toFile().getAbsolutePath());

            Set<OWLNamedIndividual> allExamples = new HashSet<OWLNamedIndividual>();

            allExamples.addAll(posExamples);
            allExamples.addAll(negExamples);

            OWLOntology mod_onto = Utility.removeNonRelatedIndividuals(_onto, owlReasoner, allExamples, null);
            owlReasoner = reasonerFactory.createNonBufferingReasoner(mod_onto);

            OWLOntology _mod_onto = Utility.removeNonRelatedConcepts(mod_onto, owlReasoner);


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

            operationsCounter++;

            writeStatisticsSorted(expl, false, writeToPosNeg);
            printStatus(confFilePath.toString());


            Long programEndTime = System.currentTimeMillis();
            Writer.writeInDisk(writeToPosNeg, "\nProgram ends at: " + dateFormat.format(new Date()), true);
            Writer.writeInDisk(writeToPosNeg, "\nProgram duration: " + (programEndTime - programStartTime) / 1000 + " sec", true);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Writer.writeInDisk(writeToPosNeg, "\n!!!!!!!!Fatal ERROR!!!!!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
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
                    // set parameters
                    ConfigParams.confFilePath = f.toString();
                    ConfigParams.ontoPath = Utility.readOntologyPathConf(ConfigParams.confFilePath);
                    String[] inpPaths = ConfigParams.confFilePath.split(File.separator);
                    //String fileName = inpPaths[inpPaths.length - 1].replace(".conf", "_expl_by_ecii.txt");
                    String fileName = inpPaths[inpPaths.length - 2] + "_expl_by_ecii.txt";
                    String folderName = inpPaths[inpPaths.length - 2];
                    ConfigParams.outputResultPath = ConfigParams.batchOutputResult + folderName + "/" + fileName;

                    initiateSingleDoOps(ConfigParams.outputResultPath);
                }

            });
        } catch (IOException e) {
            logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
            if (null != monitor) {
                monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
            } else {
                System.exit(0);
            }
        }
    }


    /**
     * @param outputResultPath
     */
    private static void initiateSingleDoOps(String outputResultPath) {

        try {
            // file to write
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputResultPath));
            PrintStream printStream = new PrintStream(bos, true);
            outPutStream = printStream;

            monitor = new Monitor(outPutStream);
            monitor.start("Program started.............", true);
            logger.info("Program started................");

            tryToCreateExplanationDemo(Paths.get(ConfigParams.confFilePath));

            monitor.stop(System.lineSeparator() + "Program finished.", true);
            logger.info("Program finished.");
            outPutStream.close();
        } catch (Exception e) {
            logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
            if (null != monitor) {
                monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
            } else {
                System.exit(0);
            }
        }
    }


    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {

        int i = 0;


        Writer.writeInDisk(logFile, "Main Program started at: " + Utility.getCurrentTimeAsString(), false);


        if (ConfigParams.batch) {
            try {

                // start with root folder
                Files.walk(Paths.get(explanationForPath)).filter(d -> d.toFile().isDirectory()).forEach(d -> {
                    try {
                        iterateOverFolders(d);
                        Writer.writeInDisk(logFile, "Running on folder: " + d.toString(), true);
                    } catch (Exception ex) {
                        System.out.println("Error occurred");
                        ex.printStackTrace();
                        Writer.writeInDisk(logFile, "\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(ex), true);
                        System.exit(0);
                    }
                });

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {

                initiateSingleDoOps(ConfigParams.outputResultPath);

                long startTime = System.currentTimeMillis();
                String folderName = Paths.get(explanationForPath).getFileName().toString();
                String writeTo = Paths.get(explanationForPath).toAbsolutePath().toString() + "/" + folderName + "_expl.txt";


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


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }

}
