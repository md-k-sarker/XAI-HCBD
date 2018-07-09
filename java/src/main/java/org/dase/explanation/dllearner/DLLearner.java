/**
 *
 */
package org.dase.explanation.dllearner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.dase.util.ConfigParams;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.AbstractCELA;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.reasoning.OWLAPIReasoner;
import org.dllearner.reasoning.ReasonerImplementation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * @author sarker
 */
public class DLLearner {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private OWLOntology owlOntology;
    private Set<OWLIndividual> posExamples;
    private Set<OWLIndividual> negExamples;
    private OWLDataFactory owlDataFactory;
    private String writeTo;

    /**
     * Constructor
     *
     * @param ontology
     * @param posExamples
     * @param negExamples
     * @param writeTo
     */
    public DLLearner(OWLOntology ontology, Set<OWLNamedIndividual> posExamples, Set<OWLNamedIndividual> negExamples,
                     String writeTo) {
        this.owlOntology = ontology;
        this.owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        this.posExamples = new HashSet<OWLIndividual>();
        this.negExamples = new HashSet<OWLIndividual>();

        for (OWLNamedIndividual indi : posExamples) {
            this.posExamples.add((OWLIndividual) indi);
        }

        if (negExamples != null) {
            for (OWLNamedIndividual indi : negExamples) {
                this.negExamples.add((OWLIndividual) indi);
            }
        }

        this.writeTo = writeTo;
    }

    //@formatter:off
	/**
	 *  http://dl.kr.org/ore2015/vip.cs.man.ac.uk_8008/results.html
	 * 1. Konclude:  is not supported.
	 * 2. Fact++: * In dl-learner they have fact++ support. 
	 * link: https://github.com/SmartDataAnalytics/DL-Learner/blob/develop/components-core/src/main/java/org/dllearner/reasoning/OWLAPIReasoner.java
	 * 
	 *  but Exception in thread "main" java.lang.UnsatisfiedLinkError: no FaCTPlusPlusJNI in java.library.path
	 *  dl-learner not running without fact++jni.
	 * 
	 */
	//@formatter:on
    public CELOE run(boolean posOnly) throws ComponentInitException {

        return run(posOnly, ReasonerImplementation.JFACT, ConfigParams.maxNrOfResults, ConfigParams.maxExecutionTimeInSeconds);
    }

    /**
     * @param posOnly
     * @param reasonerName
     * @return
     * @throws ComponentInitException
     */
    public CELOE run(boolean posOnly, ReasonerImplementation reasonerName, int maxNumberOfResults, int maxExecutionTimeInSeconds)
            throws ComponentInitException {
        KnowledgeSource ks = new OWLAPIOntology(this.owlOntology);

        ks.init();
        logger.info("finished initializing knowledge source");
        //Writer.writeInDisk(writeTo, "\n\n\nfinished initializing knowledge source", true);

        logger.info("initializing reasoner...");
        //Writer.writeInDisk(writeTo, "\ninitializing reasoner...", true);
        OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);

        // ReasonerImplementation.JFACT
        baseReasoner.setReasonerImplementation(reasonerName);
        // baseReasoner.setUseFallbackReasoner(true);
        baseReasoner.init();
        // Logger.getLogger(HermitReasoner.class).setLevel(Level.INFO);
        logger.info("finished initializing reasoner");
        //Writer.writeInDisk(writeTo, "\nfinished initializing reasoner", true);

        logger.info("initializing reasoner component...");
        //Writer.writeInDisk(writeTo, "\ninitializing reasoner component...", true);
        ClosedWorldReasoner rc = new ClosedWorldReasoner(ks);

        rc.setReasonerComponent(baseReasoner);
        // rc.setHandlePunning(true);
        // rc.setMaterializeExistentialRestrictions(true);
        rc.init();
        logger.info("finished initializing reasoner");
        //Writer.writeInDisk(writeTo, "\nfinished initializing reasoner", true);

        logger.info("initializing learning problem...");
        //Writer.writeInDisk(writeTo, "\ninitializing learning problem...", true);


        PosOnlyLP posOnlyLP = null;
        PosNegLPStandard posNegLP = null;

        if (posOnly) {
            posOnlyLP = new PosOnlyLP(rc);
            posOnlyLP.setPositiveExamples(posExamples);
            posOnlyLP.init();
        } else {
            posNegLP = new PosNegLPStandard(rc);
            posNegLP.setPositiveExamples(posExamples);
            posNegLP.setNegativeExamples(negExamples);
            posNegLP.init();
        }


        logger.info("finished initializing learning problem");
        //Writer.writeInDisk(writeTo, "\nfinished initializing learning problem", true);

        logger.info("initializing learning algorithm...");
        //Writer.writeInDisk(writeTo, "\ninitializing learning algorithm...", true);

        AbstractCELA la;
        // OEHeuristicRuntime heuristic = new OEHeuristicRuntime();
        // heuristic.setExpansionPenaltyFactor(0.1);
        if (posOnly) {
            la = new CELOE(posOnlyLP, rc);
        } else {
            la = new CELOE(posNegLP, rc);
        }
        // OWLClassExpression startClass = new
        // OWLClassImpl(IRI.create(startClass));
        // startClass = new Intersection(
        // new NamedClass("http://dl-learner.org/smallis/Allelic_info"),
        // new ObjectSomeRestriction(new
        // ObjectProperty("http://dl-learner.org/smallis/has_phenotype"),
        // Thing.instance));
        ((CELOE) la).setStartClass(this.owlDataFactory.getOWLThing());
        ((CELOE) la).setMaxExecutionTimeInSeconds(maxExecutionTimeInSeconds);
        // ((CELOE) la).setNrOfThreads(maxNoOfThreads);
        //((CELOE) la).setNoisePercentage(80);
        ((CELOE) la).setMaxNrOfResults(maxNumberOfResults);
        //((CELOE) la).setMaxClassExpressionTests(10);
        // ((CELOE) la).setWriteSearchTree(false);
        // ((CELOE) la).setReplaceSearchTree(true);
        // ((CELOE) la).setSearchTreeFile("log/mouse-diabetis.log");
        // ((CELOE) la).setHeuristic(heuristic);
        ((CELOE) la).init();
        logger.info("finished initializing learning algorithm");
        //Writer.writeInDisk(writeTo, "\nfinished initializing learning algorithm", true);

//        long startTime = System.currentTimeMillis();
        la.start();
//        long endTime = System.currentTimeMillis();
//        logger.info("Algorithm run for: " + (endTime - startTime) / 1000 + " seconds");

        return (CELOE) la;
    }


}
