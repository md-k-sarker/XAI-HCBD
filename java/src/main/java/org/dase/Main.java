package org.dase;
/*
Written by sarker.
Written at 5/7/18.
*/


import org.dase.explanation.dllearner.DLLearner;
import org.dase.util.Monitor;
import org.dase.util.*;
import org.dase.explanation.minidllearner.*;
import org.dllearner.algorithms.celoe.CELOE;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
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

public class Main {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    private static PrintStream printStream;
    private static Monitor programMonitor;
    private static Monitor eciiMonitor;
    private static Monitor dlMonitor;
    private static Monitor miniDlMonitor;
    //private static ConceptFinderComplex conceptFinderComplex;

    /**
     * Generate explanation using minidl learner
     *
     * @throws IOException
     */
    public static void explanationUsingMiniDlLearner(Path confPath) throws IOException {

        /**
         * For the examples to run: I'd say simply pick a pre-defined scene class for the positive examples (take all images from a given scene),
         * and pick negative examples from other scenes (and I'd say pick many more than the positive examples).
         * Run a few dozen of these at least. In order to have time to ingest this,
         * I would really need this soon (say, end of next week if possible).
         */
        // create objProperty
        IRI objectPropIri = IRI.create("http://www.daselab.org/ontologies/ADE20K/hcbdwsu#", "imageContains");
        OWLObjectProperty imgContains = SharedDataHolder.owlDataFactory.getOWLObjectProperty(objectPropIri);


        // result file for org.dase.minidllearner to write. will write in log file now.
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(ConfigParams.miniDlLearnerResultPath));
        PrintStream printStream = new PrintStream(bos);

        miniDlMonitor = new Monitor(printStream);

        // Create a new ConceptFinder object with the given owlReasoner.
        ConceptFinder findConceptsObj = new ConceptFinder(printStream, miniDlMonitor);

        logger.info("\nExplanation using Mini DL Learner started..........");
        miniDlMonitor.start("", true);
        try {
            findConceptsObj.findConcepts(SharedDataHolder.posIndivs, SharedDataHolder.negIndivs, ConfigParams.tolerance, imgContains);
            logger.info("Explanation using Mini DL Learner finished successfully.");
        } catch (Exception ex) {
            logger.error("\n!!!!!!!!!!Explanation using Mini DL Learner crashed!!!!!!!!!! while explaining: " + confPath.toString() + "\n");
            logger.error(Utility.getStackTraceAsString(ex));
            logger.error("\nreturning control to parent caller.........");
        }

    }

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

    /**
     * Operations for minimal dl-learner
     *
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public static void doOpsMiniDLLearner() throws OWLOntologyCreationException, IOException {


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


        // are being loaded by readExamplesFromConf
        //        HashSet<OWLNamedIndividual> posIndivs = getPosIndivs();
        //        HashSet<OWLNamedIndividual> negIndivs = getNegIndivs();


        try {
            Files.walk(Paths.get(ConfigParams.confFilePath)).filter(f -> f.toFile().isFile()).
                    filter(f -> f.toFile().getAbsolutePath().endsWith(".conf")).forEach(f -> {

                logger.info("\nExplaining for " + f.toFile() + " started..........");

                try {
                    logger.info("\nloading pos/neg examples from conf........ ");
                    SharedDataHolder.posIndivs = Utility.readPosExamplesFromConf(f.toFile());
                    SharedDataHolder.negIndivs = Utility.readNegExamplesFromConf(f.toFile());
                    logger.info("\nloading pos/neg examples from conf finished.");

                    logger.info("\nPositive Individuals: ");
                    SharedDataHolder.posIndivs.forEach(owlNamedIndividual -> {
                        logger.info("\t" + owlNamedIndividual.getIRI().getShortForm(), true);
                    });
                    logger.info("\nNegative Individuals: ");
                    SharedDataHolder.negIndivs.forEach(owlNamedIndividual -> {
                        logger.info("\t" + owlNamedIndividual.getIRI().getShortForm(), true);
                    });

                    //explanationUsingDlLearner();

                    // this minidl functionanlity is in conceptmathcer project now.
                    //explanationUsingMiniDlLearner(f);

                } catch (IOException iex) {
                    logger.error("\n\n!!!!!!!IOException occurred while explaining: " + f.toString() + " !!!!!!!\n" + Utility.getStackTraceAsString(iex));
                }
            });
        } catch (Exception ex) {
            logger.error("\n\n!!!!!!!Fatal error occurred while iterating dirs: " + ConfigParams.confFilePath + " !!!!!!!\n");
            logger.error(Utility.getStackTraceAsString(ex));
        }

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


            Writer.writeInDisk(writeToDLLearnerResult, "\nExplanation for: " + confPath.toFile().getAbsolutePath() +
                    "\n finished at: " + Utility.getCurrentTimeAsString(), true);
            System.out.println("Explanation for: " + confPath.toFile().getAbsolutePath() +
                    "\n finished at: " + Utility.getCurrentTimeAsString());

        } catch (Exception ex) {
            logger.error("\n\n!!!!!!!Fatal error occurred while iterating dirs: " + ConfigParams.confFilePath + " !!!!!!!\n");
            logger.error(Utility.getStackTraceAsString(ex));
        }
    }

    public static void main(String[] args) throws OWLOntologyCreationException, IOException {

        ConfigParams.init();

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
