package org.dase.explanation.dllearner;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.dase.util.*;
import org.dase.util.Writer;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
import org.dllearner.reasoning.ReasonerImplementation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
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

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


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
    private static int operationsCounter = 0;


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

        // is it working without file:, i.e. fullPath = "file:" + fullPath ?;
        // Checked this saveOntology() function.
        // do not save in disk for each combined ontology. it takes too much space in
        // disk.
        // each combined ontology is being approximately 80MB.
        // 80 MB * 22000 = 2000,000 MB = 2000 GB
        // saveOntology(fullPath);

        Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
        ontologies.add(sumoOntology);

        OntologyMerger merger = new OntologyMerger(owlOntologyManager, ontologies, combinedOntology);
        merger.mergeOntologies();
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

            monitor.displayMessage("\n\n########################\n", true);

            LinkedList bestClassesDescription = (LinkedList) expl.getCurrentlyBestDescriptions();

            LinkedList best100Classes = (LinkedList) expl.getCurrentlyBestDescriptions(1000);


            System.out.println("Best " + best100Classes.size() + " Classes- using getCurrentlyBestDescriptions()");
            monitor.displayMessage("\nBest " + best100Classes.size() + " Classes- using getCurrentlyBestDescriptions()", true);
            monitor.displayMessage("\n# This is the best class descriptions found by the learning algorithm so far.\n", true);
            for (int i = 0; i < best100Classes.size(); i++) {
                //System.out.println("Best 100 Class- " + i + " :" + best100Classes.get(i));
                // writer.write(best100Classes.get(i) + "\n");
                monitor.displayMessage(best100Classes.get(i) + "\n", true);
            }

            // Returns a sorted set of the best descriptions found so far. We assume that they are ordered such that the best ones come in last.
            // (In Java, iterators traverse a SortedSet in ascending order.)
            TreeSet bestEvalClasses = (TreeSet) expl.getCurrentlyBestEvaluatedDescriptions();
            Iterator it = bestEvalClasses.iterator();
            // writer.write("\nBest Eval Class- \n");
            monitor.displayMessage("\n\nBest Evaluated Class- using getCurrentlyBestEvaluatedDescriptions()\n", true);
            monitor.displayMessage("# Returns a sorted set of the best descriptions found so far. "
                    + "\n# We assume that they are ordered such that the best ones come in last."
                    + "\n# (In Java, iterators traverse a SortedSet in ascending order.\n)", true);
            while (it.hasNext()) {
                Object obj = it.next();
                //System.out.println("Best Eval Class- :" + obj);
                // writer.write(it.next()+"\n");
                monitor.displayMessage(obj + "\n", true);
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

        monitor.displayMessage("\n\n####################Solutions####################:\n", true);

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
            monitor.displayMessage("solution " + solutionCounter + ": " + String.valueOf(o.getDescription()), true);
            monitor.displayMessage("\t accuracy_score: " + String.valueOf(o.getAccuracy()), true);
        });

        monitor.displayMessage("\nTotal solutions found: " + solutionCounter, true);

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
     *
     * @param confFilePath
     */
    private static void createExplanationUsingDlLearner(Path confFilePath) {

        String writeToPosNeg = confFilePath.toFile().getAbsolutePath().replace(".conf", "_expl_by_dl_learner.txt");
        try {

            monitor.displayMessage("Working with confFile: " + confFilePath.toFile().getAbsolutePath(), true);
            logger.info("Working with confFile: " + ConfigParams.confFilePath);


            OWLOntology _onto = null;
            /**
             * load background ontology
             */
            try {
                _onto = Utility.loadOntology(ConfigParams.ontoPath);
                //reasonerFactory = new PelletReasonerFactory();
                reasonerFactory = new JFactFactory();
                owlReasoner = reasonerFactory.createNonBufferingReasoner(_onto);
            } catch (Exception ex) {
                logger.error(Utility.getStackTraceAsString(ex));
                monitor.stopSystem(Utility.getStackTraceAsString(ex), true);
            }

            logger.info("Ontology Path: " + ConfigParams.ontoPath);
            monitor.writeMessage("Ontology Path: " + ConfigParams.ontoPath);

            posExamples = Utility.readPosExamplesFromConf(confFilePath.toFile().getAbsolutePath());
            negExamples = Utility.readNegExamplesFromConf(confFilePath.toFile().getAbsolutePath());

            Set<OWLNamedIndividual> allExamples = new HashSet<OWLNamedIndividual>();

            allExamples.addAll(posExamples);
            allExamples.addAll(negExamples);

            OWLOntology mod_onto = Utility.removeNonRelatedIndividuals(_onto, owlReasoner, allExamples, null);
            owlReasoner = reasonerFactory.createNonBufferingReasoner(mod_onto);

            OWLOntology _mod_onto = Utility.removeNonRelatedConcepts(mod_onto, owlReasoner);


            Long algoStartTime = System.currentTimeMillis();
            monitor.displayMessage("Algorithm started.................", true);
            logger.info("Algorithm started.................");
            monitor.displayMessage("Algorithm starts at: " + ConfigParams.dateFormat.format(new Date()), true);
            logger.info("Algorithm starts at: " + ConfigParams.dateFormat.format(new Date()));

            logger.info("\nposIndivs from conf");
            monitor.writeMessage("\nposIndivs from conf");
            for (OWLNamedIndividual indi : posExamples) {
                logger.info("\t" + Utility.getShortName(indi));
                monitor.writeMessage("\t" + Utility.getShortName(indi));
            }

            logger.info("\nnegIndivs from conf");
            monitor.writeMessage("\nnegIndivs from conf");
            for (OWLNamedIndividual indi : negExamples) {
                logger.info("\t" + Utility.getShortName(indi));
                monitor.writeMessage("\t" + Utility.getShortName(indi));
            }


            // call to run dl-learner
            DLLearner dlLearner = new DLLearner(_mod_onto, posExamples, posExamples, writeToPosNeg);
            CELOE expl = dlLearner.run(false, ReasonerImplementation.JFACT,
                    ConfigParams.maxNrOfResults, ConfigParams.maxExecutionTimeInSeconds);

            Long algoEndTime = System.currentTimeMillis();
            monitor.displayMessage("\nAlgorithm ends at: " + ConfigParams.dateFormat.format(new Date()), true);
            logger.info("\nAlgorithm ends at: " + ConfigParams.dateFormat.format(new Date()), true);
            monitor.displayMessage("\nAlgorithm duration: " + (algoEndTime - algoStartTime) / 1000 + " sec", true);
            logger.info("\nAlgorithm duration: " + (algoEndTime - algoStartTime) / 1000 + " sec", true);

            operationsCounter++;

            writeStatisticsSorted(expl, false, writeToPosNeg);
            printStatus(confFilePath.toString());

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
     * Will get a folder
     *
     * @param dirPath
     */
    private static void iterateOverFolders(Path dirPath) {

        if (ConfigParams.batchConfFilePath.equals(dirPath.toString() + "/")) {
            // System.out.println("equal");
            return;
        }

        //System.out.println("Folder: "+ dirPath.toString());

        try {
            // iterate over the files of a folder
            Files.walk(dirPath).filter(f -> f.toFile().isFile()).filter(f -> f.toFile().getAbsolutePath().endsWith(".conf")).limit(1).forEach(f -> {
                // will get each file
                if (alreadyGotResult.contains(f.toFile().getName())) {
                    System.out.println(f.toString() + " already has result not running it");
                } else {
                    // set parameters
                    ConfigParams.confFilePath = f.toString();
                    ConfigParams.ontoPath = Utility.readOntologyPathConf(ConfigParams.confFilePath);
                    String[] inpPaths = ConfigParams.confFilePath.split(File.separator);
                    //String fileName = inpPaths[inpPaths.length - 1].replace(".conf", "_expl_by_ecii.txt");
                    String fileName = inpPaths[inpPaths.length - 2] + "_expl_by_dl_learner.txt";
                    String folderName = inpPaths[inpPaths.length - 2];
                    ConfigParams.outputResultPath = ConfigParams.batchOutputResult + folderName + "/" + fileName;
                    logger.info("ConfigParams.outputResultPath: " + ConfigParams.outputResultPath);
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

            Long programStartTime = System.currentTimeMillis();

            init();

            // file to write
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputResultPath));
            PrintStream printStream = new PrintStream(bos, true);
            outPutStream = printStream;

            monitor = new Monitor(outPutStream);
            monitor.start("Program started.............", true);
            logger.info("Program started................");

            createExplanationUsingDlLearner(Paths.get(ConfigParams.confFilePath));

            monitor.stop(System.lineSeparator() + "Program finished.", true);
            logger.info("Program finished.");

            Long programEndTime = System.currentTimeMillis();
            monitor.displayMessage("\nProgram ends at: " + ConfigParams.dateFormat.format(new Date()), true);
            monitor.displayMessage("\nProgram duration: " + (programEndTime - programStartTime) / 1000 + " sec", true);

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

        logger.info("Main Program started at: " + Utility.getCurrentTimeAsString());

        try {
            if (ConfigParams.batch) {

                // start with root folder
                Files.walk(Paths.get(ConfigParams.batchConfFilePath)).filter(d -> d.toFile().isDirectory()).forEach(d -> {
                    try {
                        iterateOverFolders(d);
                        logger.info("Running on folder: " + d.toString());
                    } catch (Exception e) {
                        logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
                        if (null != monitor) {
                            monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
                        } else {
                            System.exit(0);
                        }
                    }
                });
            } else {
                initiateSingleDoOps(ConfigParams.outputResultPath);
            }
        } catch (Exception ex) {
            logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(ex));
            if (null != monitor) {
                monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(ex), true);
            } else {
                System.exit(0);
            }
        }
    }

}
