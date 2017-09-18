/**
 * 
 */
package edu.wright.dase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

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
 *
 */
public class DLLearner {

	private static final Logger logger = LoggerFactory.getLogger(DLLearner.class);

	public static final String ontoOrgnlFileName = "/Users/sarker/Dropbox/HCBD-Project/DL-Learner Approach/experiments/sumowithADE20K/warehouse (positive) vs workroom (negative)/sumo_hand_curated.owl";
	public static final String posExamplesFilePath = "/Users/sarker/Dropbox/HCBD-Project/DL-Learner Approach/experiments/sumowithADE20K/warehouse (positive) vs workroom (negative)/positive_indivs.txt";
	public static final String negExamplesFilePath = "/Users/sarker/Dropbox/HCBD-Project/DL-Learner Approach/experiments/sumowithADE20K/warehouse (positive) vs workroom (negative)/negative_indivs.txt";


	private static File ontoFile;
	private static OWLDataFactory df;
	private static OWLOntologyManager ontologyManager;

	private static OWLOntology ontology;
	
	private OWLOntology owlOntology;
	private Set<OWLIndividual> posExamples;
	private Set<OWLIndividual> negExamples;
	private OWLDataFactory owlDataFactory;
	
	/**
	 * Configurations
	 */
	private int maxExecutionTimeInSeconds = 6000;
	
	/**
	 * Constructor
	 * @param ontology
	 * @param posExamples
	 * @param negExamples
	 */
	public DLLearner(OWLOntology ontology,Set<OWLNamedIndividual> posExamples, Set<OWLNamedIndividual> negExamples) {
		this.owlOntology = ontology;
		this.owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
		
		Set<OWLIndividual> owlIndividuals = new HashSet<OWLIndividual>();
		for(OWLNamedIndividual indi: posExamples) {
			owlIndividuals.add((OWLIndividual) indi);
		}
		this.posExamples = owlIndividuals;
		
		owlIndividuals.clear();
		
		for(OWLNamedIndividual indi: negExamples) {
			owlIndividuals.add((OWLIndividual) indi);
		}
		this.negExamples = owlIndividuals;
		
		owlIndividuals.clear();
	}
	
	
	public CELOE run() throws ComponentInitException {
		KnowledgeSource ks = new OWLAPIOntology(this.owlOntology);

		ks.init();
		logger.info("finished initializing knowledge source");

		logger.info("initializing reasoner...");
		OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);

		baseReasoner.setReasonerImplementation(ReasonerImplementation.PELLET);
		// baseReasoner.setUseFallbackReasoner(true);
		baseReasoner.init();
		// Logger.getLogger(PelletReasoner.class).setLevel(Level.INFO);
		logger.info("finished initializing reasoner");

		logger.info("initializing reasoner component...");
		ClosedWorldReasoner rc = new ClosedWorldReasoner(ks);

		// rc.setReasonerComponent(baseReasoner);
		// rc.setHandlePunning(true);
		// rc.setMaterializeExistentialRestrictions(true);
		rc.init();
		logger.info("finished initializing reasoner");

		logger.info("initializing learning problem...");
		// PosOnlyLP lp = new PosOnlyLP(rc);
		PosNegLPStandard lp = new PosNegLPStandard(rc);
		lp.setPositiveExamples(this.posExamples);
		lp.setNegativeExamples(this.negExamples);
		lp.init();
		logger.info("finished initializing learning problem");

		logger.info("initializing learning algorithm...");
		AbstractCELA la;
		// OEHeuristicRuntime heuristic = new OEHeuristicRuntime();
		// heuristic.setExpansionPenaltyFactor(0.1);
		la = new CELOE(lp, rc);

		// OWLClassExpression startClass = new
		// OWLClassImpl(IRI.create(startClass));
		// startClass = new Intersection(
		// new NamedClass("http://dl-learner.org/smallis/Allelic_info"),
		// new ObjectSomeRestriction(new
		// ObjectProperty("http://dl-learner.org/smallis/has_phenotype"),
		// Thing.instance));
		((CELOE) la).setStartClass(this.owlDataFactory.getOWLThing());
		((CELOE) la).setMaxExecutionTimeInSeconds(maxExecutionTimeInSeconds);
		// ((CELOE) la).setNoisePercentage(80);
		// ((CELOE) la).setMaxNrOfResults(50);
		// ((CELOE) la).setWriteSearchTree(false);
		// ((CELOE) la).setReplaceSearchTree(true);
		// ((CELOE) la).setSearchTreeFile("log/mouse-diabetis.log");
		// ((CELOE) la).setHeuristic(heuristic);
		((CELOE) la).init();
		logger.info("finished initializing learning algorithm");
		
		long startTime = System.currentTimeMillis();
		la.start();
		long endTime = System.currentTimeMillis();
		logger.info("Algorithm run for: " + (endTime - startTime)/1000 + " seconds");
		
		return (CELOE) la;
	}
	
	
	/**
	 * long startTime = System.nanoTime();
methodToTime();
long endTime = System.nanoTime();

long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
	 */
	
	/**
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	
	private static Set<OWLIndividual> readNegativeExamples(String filePath) throws IOException {
		Set<OWLIndividual> indivs = new TreeSet<>();
		try (BufferedReader buffRead = new BufferedReader(new FileReader(new File(filePath)))) {
			String line;
			while ((line = buffRead.readLine()) != null) {
				line = line.trim().replace(",", "");
				// line = line.substring(1, line.length() - 1); // strip off
				// angle
				// brackets
				indivs.add(new OWLNamedIndividualImpl(IRI.create(line)));

				logger.info("negative individual: " + new OWLNamedIndividualImpl(IRI.create(line)).getIRI());
			}
		}
		return indivs;
	}

	private static Set<OWLIndividual> readPositiveExamples(String filePath) throws IOException {
		Set<OWLIndividual> indivs = new TreeSet<>();
		try (BufferedReader buffRead = new BufferedReader(new FileReader(new File(filePath)))) {
			String line;
			while ((line = buffRead.readLine()) != null) {
				line = line.trim().replace(",", "");
				// line = line.substring(1, line.length() - 1); // strip off
				// angle
				// brackets
				indivs.add(new OWLNamedIndividualImpl(IRI.create(line)));

				logger.info("positive individual: " + new OWLNamedIndividualImpl(IRI.create(line)).getIRI());
			}
		}
		return indivs;
	}

	private static void loadOntology() throws OWLOntologyCreationException {

		ontology = ontologyManager.loadOntologyFromOntologyDocument(ontoFile);

	}

	public static void main_(String[] args) {
		TreeSet ts = new TreeSet<Integer>();
		ts.add(1);
		ts.add(2);
		ts.add(3);
		ts.add(4);
		Iterator it = ts.iterator();

		while (it.hasNext()) {

			logger.info("item: " + it.next());
		}
	}

	public static void main_test(String[] args)
			throws IOException, ComponentInitException, OWLOntologyCreationException, OWLOntologyStorageException {

		logger.info("starting...");
		// IRI ontoFileIRI = IRI.create(ontoOrgnlFileName);
		ontoFile = new File(ontoOrgnlFileName);

		df = OWLManager.getOWLDataFactory();
		ontologyManager = OWLManager.createOWLOntologyManager();

		loadOntology();

		logger.info("reading positive and negative examples...");
		Set<OWLIndividual> posExamples = readPositiveExamples(posExamplesFilePath);

		Set<OWLIndividual> negExamples = readNegativeExamples(negExamplesFilePath);

		logger.info("finished reading examples");

		logger.info("initializing knowledge source...");
		KnowledgeSource ks = new OWLAPIOntology(ontology);

		ks.init();
		logger.info("finished initializing knowledge source");

		logger.info("initializing reasoner...");
		OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);

		baseReasoner.setReasonerImplementation(ReasonerImplementation.PELLET);
		// baseReasoner.setUseFallbackReasoner(true);
		baseReasoner.init();
		// Logger.getLogger(PelletReasoner.class).setLevel(Level.INFO);
		logger.info("finished initializing reasoner");

		logger.info("initializing reasoner component...");
		ClosedWorldReasoner rc = new ClosedWorldReasoner(ks);

		// rc.setReasonerComponent(baseReasoner);
		// rc.setHandlePunning(true);
		// rc.setMaterializeExistentialRestrictions(true);
		rc.init();
		logger.info("finished initializing reasoner");

		logger.info("initializing learning problem...");
		// PosOnlyLP lp = new PosOnlyLP(rc);
		PosNegLPStandard lp = new PosNegLPStandard(rc);
		lp.setPositiveExamples(posExamples);
		lp.setNegativeExamples(negExamples);
		lp.init();
		logger.info("finished initializing learning problem");

		logger.info("initializing learning algorithm...");
		AbstractCELA la;
		// OEHeuristicRuntime heuristic = new OEHeuristicRuntime();
		// heuristic.setExpansionPenaltyFactor(0.1);
		la = new CELOE(lp, rc);

		// OWLClassExpression startClass = new
		// OWLClassImpl(IRI.create(startClass));
		// startClass = new Intersection(
		// new NamedClass("http://dl-learner.org/smallis/Allelic_info"),
		// new ObjectSomeRestriction(new
		// ObjectProperty("http://dl-learner.org/smallis/has_phenotype"),
		// Thing.instance));
		((CELOE) la).setStartClass(df.getOWLThing());
		((CELOE) la).setMaxExecutionTimeInSeconds(6000);
		// ((CELOE) la).setNoisePercentage(80);
		// ((CELOE) la).setMaxNrOfResults(50);
		// ((CELOE) la).setWriteSearchTree(false);
		// ((CELOE) la).setReplaceSearchTree(true);
		// ((CELOE) la).setSearchTreeFile("log/mouse-diabetis.log");
		// ((CELOE) la).setHeuristic(heuristic);
		((CELOE) la).init();
		la.start();
		LinkedList bestClassesDescription = (LinkedList) la.getCurrentlyBestDescriptions();

		LinkedList best100Classes = (LinkedList) la.getCurrentlyBestDescriptions(100);

		TreeSet bestEvalClasses = (TreeSet) la.getCurrentlyBestEvaluatedDescriptions();

		for (int i = 0; i < best100Classes.size(); i++) {
			System.out.println("Best 100 Class- " + i + " :" + best100Classes.get(i));
		}
		Iterator it = bestEvalClasses.iterator();

		while (it.hasNext()) {
			System.out.println("Best Eval Class- :" + it.next());
		}

	}

}
