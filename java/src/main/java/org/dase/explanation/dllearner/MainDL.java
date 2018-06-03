package org.dase.explanation.dllearner;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.dase.util.Constants;
import org.dase.util.SharedDataHolder;
import org.dase.util.Utility;
import org.dase.util.Writer;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Explanation.java Creates explanation for each class.
 * <p>
 * It imports ontology from sumo and combine it with wordnet & ade20k ontology.
 * Then it call RunAlgorithmClass to get the explanation. Then it saves the
 * explanation in hard disk.
 *
 * @author sarker
 */

public class MainDL {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    final static int maxNegativeInstances = 200;
    static int totalInstances = 0;
    static int totalClasses = 0;

    static final double distanceThreshold = 0.8;


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
    static OWLOntology combinedOntology;
    static int counter = 0;

    static String explanationForFolder = "/Users/sarker/Workspaces/ProjectHCBD/experiments/may_13_2018/river/";
    static String explanationForConfPath = "/Users/sarker/Workspaces/ProjectHCBD/experiments/may_13_2018/river/river_vs_5_10_from_r.conf";
    static String backgroundOntology = "/Users/sarker/Workspaces/ProjectHCBD/datas/sumo_may_13_2018/sumo_with_ade_training_r.owl";


    private static int maxExecutionTimeInSeconds = (1 * 1800);

    static String logFile = "/Users/sarker/Workspaces/ProjectHCBD/experiments/may_13_2018/logs/result_by_dl_learner.log";

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
     * Will get each owl file confPath
     */
    private static void tryToCreateExplanationUsingDLLearner(Path confPath, Path folderPath) {

        logger.info("working on file: " + confPath.toString());
        String writeToDLLearnerResult = confPath.toFile().getAbsolutePath().replace(".conf", "_expl_using_dl_learner.txt");

        if (!confPath.toFile().getAbsolutePath().endsWith(".conf")) {

            Writer.writeInDisk(writeToDLLearnerResult, "Explanation for: " + confPath.toFile().getAbsolutePath() +
                    "Can not run because it is not a .conf file.\n !!!!!!!!!!Explanation aborted!!!!!!!!!!", false);
            System.out.println("\n\n\nExplanation for: " + confPath.toFile().getAbsolutePath() +
                    "Can not run because it is not a .conf file. \n !!!!!!!!!!Explanation aborted!!!!!!!!!!");
            return;
        }

        try {

            // will get each file
            // now run explanation for each owl, i.e. for each image


            // for only pos
            Writer.writeInDisk(writeToDLLearnerResult, "Explanation for: " + confPath.toFile().getAbsolutePath() +
                    "\n started at: " + Utility.getCurrentTimeAsString(), false);
            System.out.println("\n\n\nExplanation for: " + confPath.toFile().getAbsolutePath() +
                    "\n started at: " + Utility.getCurrentTimeAsString());


            /**
             * load background ontology
             */
            System.out.println("loading ontology.........");
            OWLOntology _onto = Utility.loadOntology(backgroundOntology);
            System.out.println("loading ontology finished.");
            reasonerFactory = new PelletReasonerFactory();
            owlReasoner = reasonerFactory.createNonBufferingReasoner(_onto);

            /**
             * Get/load pos and neg examples from txt file
             */
            posExamples = Utility.readPosExamplesFromConf(confPath.toFile());
            negExamples = Utility.readNegExamplesFromConf(confPath.toFile());

            Set<OWLNamedIndividual> allExamples = new HashSet<OWLNamedIndividual>();

            allExamples.addAll(posExamples);
            allExamples.addAll(negExamples);

            //OWLOntology mod_onto = Utility.removeNonRelatedIndividuals(_onto, owlReasoner, allExamples, null);
            //owlReasoner = reasonerFactory.createNonBufferingReasoner(mod_onto);

            //OWLOntology _mod_onto = Utility.removeNonRelatedConcepts(_onto, owlReasoner);
            //owlReasoner = reasonerFactory.createNonBufferingReasoner(_mod_onto);

            System.out.println("PosExamples: ");
            Writer.writeInDisk(writeToDLLearnerResult, "\n\nPosExamples: ", true);
            for (OWLNamedIndividual indi : posExamples) {
                System.out.println(indi);
                Writer.writeInDisk(writeToDLLearnerResult, "\n" + indi.getIRI().getShortForm(), true);
            }

            System.out.println("\nNegExamples: ");
            Writer.writeInDisk(writeToDLLearnerResult, "\n\nNegExamples:", true);
            for (OWLNamedIndividual indi : negExamples) {
                System.out.println(indi);
                Writer.writeInDisk(writeToDLLearnerResult, "\n" + indi.getIRI().getShortForm(), true);
            }

            Writer.writeInDisk(writeToDLLearnerResult, "\n\n", true);

            // call to run dl-learner
            DLLearner dlLearner = new DLLearner(_onto, posExamples, posExamples, writeToDLLearnerResult, maxExecutionTimeInSeconds);
            CELOE expl = dlLearner.run(true);

            counter++;

            Utility.writeStatistics(expl, writeToDLLearnerResult);
            printStatus(confPath.toString());

            Writer.writeInDisk(writeToDLLearnerResult, "\nExplanation for: " + confPath.toFile().getAbsolutePath() +
                    "\n finished at: " + Utility.getCurrentTimeAsString(), true);
            System.out.println("Explanation for: " + confPath.toFile().getAbsolutePath() +
                    "\n finished at: " + Utility.getCurrentTimeAsString());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Writer.writeInDisk(writeToDLLearnerResult, "\n!!!!!!!!Fatal ERROR!!!!!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
        }
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
     * @param filePath
     * @param filePath : String
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
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {

        int i = 0;
        Writer.writeInDisk(logFile, "Main Program started at: " + Utility.getCurrentTimeAsString(), false);

        OWLOntology _onto;

        try {

            long startTime = System.currentTimeMillis();
            String folderName = Paths.get(explanationForFolder).getFileName().toString();
            String writeTo = Paths.get(explanationForFolder).toAbsolutePath().toString() + "/" + folderName + "_expl.txt";


            logger.info("Program started...........");
            System.out.println("Program started...........");
            Writer.writeInDisk(writeTo, "\n Program started...........\n", false);

            init();

            tryToCreateExplanationUsingDLLearner(Paths.get(explanationForConfPath),Paths.get(explanationForFolder));

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
