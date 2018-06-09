package org.dase;
/*
Written by sarker.
Written at 5/7/18.
*/


import org.dase.explanation.dllearner.DLLearner;
import org.dase.explanation.minidllearner.ConceptFinder;
import org.dase.util.*;
import org.dllearner.algorithms.celoe.CELOE;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    private static OWLOntology ontology;
    private static OWLOntologyManager manager;
    private static OWLDataFactory dataFacotry;
    private static OWLReasoner owlReasoner;
    private static PrintStream outPutStream;
    private static Monitor monitor;

    static ArrayList<String> alreadyGotResult = new ArrayList<String>();


    /**
     *
     */
    public static void logModifiedOntologyInfo() {
        logger.info("\nontology id: " + SharedDataHolder.owlOntology.getOntologyID().toString());
        logger.info("ontology manager:" + SharedDataHolder.owlOntologyManager.toString());
        logger.info("ontology datafactory: " + SharedDataHolder.owlDataFactory.toString());
        logger.info("owl reasoner name: " + SharedDataHolder.owlReasoner.getReasonerName());
        logger.info("ontology id using reasoner : " + SharedDataHolder.owlReasoner.getRootOntology().getOntologyID().toString());
        logger.info("total axioms: " + SharedDataHolder.owlOntology.getAxiomCount());
        logger.info("total named class: " + SharedDataHolder.owlOntology.getClassesInSignature().size());
        logger.info("total named individuals: " + SharedDataHolder.owlOntology.getIndividualsInSignature().size());
        logger.info("instances of owl:Thing using reasoner after init reasoning: " + SharedDataHolder.owlReasoner.getInstances(
                SharedDataHolder.owlThing, false).getFlattened().size());
    }



    public static void doOpsDLLearner(Path confPath) {

        int maxExecutionTimeInSeconds = (1 * 1800);
        String writeToDLLearnerResult = confPath.toFile().getAbsolutePath().replace(".conf", "_expl_using_dl_learner.txt");

        try {

            // load ontotology
            logger.info("\nloading base ontology started........ ");
            SharedDataHolder.owlOntology = Utility.loadOntology(ConfigParams.ontoPath, programMonitor);
            SharedDataHolder.owlOntologyManager = SharedDataHolder.owlOntology.getOWLOntologyManager();
            SharedDataHolder.owlDataFactory = SharedDataHolder.owlOntologyManager.getOWLDataFactory();
            logger.info("loading base ontology finished.");

            // String reasonerFactoryClassName = null;
            //OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
            // initiate owlReasoner
            //owlReasoner = reasonerFactory.createNonBufferingReasoner(owlOntology);
            /**
             * Problem of reasoners: https://github.com/stardog-union/pellet/wiki/FAQ#how-can-i-use-pellet-with-owl--api
             */

            // create the jfact++ owlReasoner
            //owlReasoner = Utility.initReasoner("jfact", owlOntology);


            // create the Pellet owlReasoner
            //reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(owlOntology);

            logger.info("\nInitializing reasoner on base ontology........ ");
            SharedDataHolder.owlReasoner = Utility.initReasoner("jfact", SharedDataHolder.owlOntology);
            logger.info("Initializing reasoner on base ontology finished.");

            logger.info("\nloading pos/neg examples from conf........ ");
            SharedDataHolder.posIndivs = Utility.readPosExamplesFromConf(confPath.toFile());
            SharedDataHolder.negIndivs = Utility.readNegExamplesFromConf(confPath.toFile());
            logger.info("\nloading pos/neg examples from conf finished.");


            // call to run dl-learner
            DLLearner dlLearner = new DLLearner(SharedDataHolder.owlOntology, SharedDataHolder.posIndivs, SharedDataHolder.negIndivs, writeToDLLearnerResult, maxExecutionTimeInSeconds);
            CELOE expl = dlLearner.run(true);


            Utility.writeStatistics(expl, writeToDLLearnerResult);


            Writer.writeInDisk(writeToDLLearnerResult, "\nExplanation_old for: " + confPath.toFile().getAbsolutePath() +
                    "\n finished at: " + Utility.getCurrentTimeAsString(), true);
            System.out.println("Explanation_old for: " + confPath.toFile().getAbsolutePath() +
                    "\n finished at: " + Utility.getCurrentTimeAsString());

        } catch (Exception ex) {
            logger.error("\n\n!!!!!!!Fatal error occurred while iterating dirs: " + ConfigParams.confFilePath + " !!!!!!!\n");
            logger.error(Utility.getStackTraceAsString(ex));
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
            doOps();

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


    public static void main(String[] args) throws OWLOntologyCreationException, IOException {

        if(ConfigParams.batch){

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

        }else{
            initiateSingleDoOps(ConfigParams.outputResultPath);
        }

        // global log file to write. this is different from log4j log file.
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(ConfigParams.logPath));
        PrintStream printStream = new PrintStream(bos);
        Main.printStream = printStream;

        programMonitor = new Monitor(Main.printStream);
        programMonitor.start("", true);
        logger.info("\nProgram started at :" + Utility.getCurrentDateTimeAsString() + " .......");

        try {

            doOpsDLLearner(Paths.get(ConfigParams.confFilePath));
            //doOpsMiniDLLearner();
            //doOpsEfficientConceptFinding();

            logger.info("\n..........Program finished successfully.......");
            //programMonitor.stop("", true);
        } catch (Exception ex) {
            logger.error("\n!!!!!!!!!!Program crashed!!!!!!!!!!" + Utility.getStackTraceAsString(ex));
            programMonitor.stop("", true);
        } finally {
            SharedDataHolder.owlReasoner.dispose();
            logger.info("System exiting.......");
            programMonitor.stopSystem("", true);
        }
    }

}
