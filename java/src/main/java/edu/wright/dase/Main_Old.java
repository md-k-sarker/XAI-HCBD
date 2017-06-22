package edu.wright.dase;
//package edu.wright.dase;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Set;
//import java.util.TreeSet;
//
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.ModelFactory;
//import org.apache.jena.riot.Lang;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.dllearner.algorithms.celoe.CELOE;
//import org.dllearner.algorithms.celoe.OEHeuristicRuntime;
//import org.dllearner.core.AbstractCELA;
//import org.dllearner.core.AbstractReasonerComponent;
//import org.dllearner.core.ComponentInitException;
//import org.dllearner.core.KnowledgeSource;
//import org.dllearner.core.StringRenderer;
//import org.dllearner.core.StringRenderer.Rendering;
//import org.dllearner.kb.OWLAPIOntology;
//import org.dllearner.learningproblems.PosNegLPStandard;
//import org.dllearner.reasoning.ClosedWorldReasoner;
//import org.dllearner.reasoning.OWLAPIReasoner;
//import org.dllearner.refinementoperators.RhoDRDown;
////import org.dllearner.scripts.MouseDiabetesCBD;
//import org.dllearner.utilities.OwlApiJenaUtils;
//import org.semanticweb.elk.owlapi.ElkReasoner;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.model.IRI;
//import org.semanticweb.owlapi.model.OWLClass;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.semanticweb.owlapi.model.OWLIndividual;
//import org.semanticweb.owlapi.model.OWLOntology;
//import org.semanticweb.owlapi.model.OWLOntologyCreationException;
//
//import javafx.beans.property.ObjectProperty;
//import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
//import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
//
//public class Main {
//
//	private static final Logger logger = Logger.getLogger(Main.class);
//	private static final String dir = "/tmp/smallis/";
//	private static final String kbFilePath = dir + "monarch_module_exp1.owl";
//	private static final String genoDiseaseFilePath = dir + "mgi_geno_disease.ttl";
//	private static final String genoNotDiseaseFilePath = dir + "mgi_geno_notdisease.ttl";
//	public static final String posExamplesFilePath = dir + "pos_uris.txt";
//	public static final String negExamplesFilePath = dir + "neg_uris.txt";
//
//	static boolean useCBD = false;
//
//	public static void main(String[] args) throws OWLOntologyCreationException, IOException, ComponentInitException {
//		setUp();
//		logger.debug("starting...");
//		OWLOntology ontology;
//		if (useCBD) {
//			ontology = readCBD();
//		} else {
//			ontology = readDumpFiles();
//		}
//
//		logger.debug("reading positive and negative examples...");
//		Set<OWLIndividual> posExamples = readExamples(posExamplesFilePath);
//		Set<OWLIndividual> negExamples = readExamples(negExamplesFilePath);
//		if (useCBD) {
//			//posExamples = new HashSet<>(new ArrayList<>(posExamples).subList(0, MouseDiabetesCBD.nrOfPosExamples));
//			//negExamples = new HashSet<>(new ArrayList<>(negExamples).subList(0, MouseDiabetesCBD.nrOfNegExamples));
//		}
//		logger.debug("finished reading examples");
//
//		logger.debug("initializing knowledge source...");
//		KnowledgeSource ks = new OWLAPIOntology(ontology);
//		ks.init();
//		logger.debug("finished initializing knowledge source");
//
//		logger.debug("initializing reasoner...");
//		OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);
//		baseReasoner.setUseFallbackReasoner(true);
//		baseReasoner.init();
//		Logger.getLogger(ElkReasoner.class).setLevel(Level.OFF);
//		logger.debug("finished initializing reasoner");
//		logger.debug("initializing reasoner component...");
//		ClosedWorldReasoner rc = new ClosedWorldReasoner(ks);
//		rc.setReasonerComponent(baseReasoner);
//		rc.setHandlePunning(true);
//		rc.setMaterializeExistentialRestrictions(true);
//		rc.init();
//		logger.debug("finished initializing reasoner");
//
//		// Reasoner myObject = (Reasoner) MonProxyFactory.monitor(rc);
//
//		logger.debug("initializing learning problem...");
//		PosNegLPStandard lp = new PosNegLPStandard(rc);
//		lp.setPositiveExamples(posExamples);
//		lp.setNegativeExamples(negExamples);
//		lp.init();
//		logger.debug("finished initializing learning problem");
//
//		logger.debug("initializing learning algorithm...");
//		AbstractCELA la;
//		OEHeuristicRuntime heuristic = new OEHeuristicRuntime();
//		heuristic.setExpansionPenaltyFactor(0.1);
//		la = new CELOE(lp, rc);
//		((CELOE) la).setHeuristic(heuristic);
//		((CELOE) la).setMaxExecutionTimeInSeconds(1800);
//		((CELOE) la).setNoisePercentage(80);
//		((CELOE) la).setMaxNrOfResults(50);
//		((CELOE) la).setWriteSearchTree(false);
//		((CELOE) la).setReplaceSearchTree(true);
//		((CELOE) la).setSearchTreeFile("log/mouse-diabetis.log");
//		
//		OWLClassExpression startClass = new OWLClassImpl(IRI.create("http://dl-learner.org/smallis/Allelic_info"));
//		 startClass = new Intersection(
//		 new NamedClass("http://dl-learner.org/smallis/Allelic_info"),
//		 new ObjectSomeRestriction(new
//		 ObjectProperty("http://dl-learner.org/smallis/has_phenotype"),
//		 Thing.instance));
//		((CELOE) la).setStartClass(startClass);
//		logger.debug("finished initializing learning algorithm");
//		logger.debug("initializing operator...");
//		RhoDRDown op = new RhoDRDown();
//		op.setInstanceBasedDisjoints(true);
//		op.setUseNegation(false);
//		op.setUseHasValueConstructor(true);
//		op.setUseAllConstructor(false);
//		op.setStartClass(startClass);
//		op.setReasoner(rc);
//		op.setSubHierarchy(rc.getClassHierarchy());
//		op.setObjectPropertyHierarchy(rc.getObjectPropertyHierarchy());
//		op.setDataPropertyHierarchy(rc.getDatatypePropertyHierarchy());
//		op.init();
//		logger.debug("finished initializing operator");
//		((CELOE) la).setOperator(op);
//
//		// Set<Description> refinements = op.refine(d, d.getLength()+4);
//		// for (Description ref : refinements) {
//		// System.out.println(ref + ":" + lp.getAccuracyOrTooWeak(ref, 1.0));
//		// }
//
//		// SortedSet<Description> subClasses = rc.getSubClasses(Thing.instance);
//		// for (Description sub : subClasses) {
//		// System.out.println(sub + ":" + rc.getIndividuals(sub).size() + "
//		// instances");
//		// }
//
//		//
//		// OWLOntologyManager man = OWLManager.createOWLOntologyManager();
//		// OWLDataFactory dataFactory = man.getOWLDataFactory();
//		// OWLReasonerFactory reasonerFactory =
//		// PelletReasonerFactory.getInstance();
//		// OWLReasoner reasoner =
//		// reasonerFactory.createNonBufferingReasoner(ontology);
//		// reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//		// NodeSet<OWLClass> subClasses2 = reasoner.getSuperClasses(
//		// dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MP_0001265")),
//		// false);
//		// for (OWLClass cls : subClasses2.getFlattened()) {
//		// System.out.println(cls);
//		// }
//
//		la.init();
//		la.start();
//	}
//
//	public static Set<OWLIndividual> loadPosExamples() throws IOException {
//		return readExamples(posExamplesFilePath);
//	}
//
//	public static Set<OWLIndividual> loadNegExamples() throws IOException {
//		return readExamples(negExamplesFilePath);
//	}
//
//	public static Set<OWLIndividual> readExamples(String filePath) throws IOException {
//		Set<OWLIndividual> indivs = new TreeSet<>();
//		try (BufferedReader buffRead = new BufferedReader(new FileReader(new File(filePath)))) {
//			String line;
//			while ((line = buffRead.readLine()) != null) {
//				line = line.trim();
//				line = line.substring(1, line.length() - 1); // strip off angle
//																// brackets
//				indivs.add(new OWLNamedIndividualImpl(IRI.create(line)));
//			}
//		}
//		return indivs;
//	}
//
//	private static void setUp() {
//		logger.setLevel(Level.DEBUG);
//		Logger.getLogger(AbstractReasonerComponent.class).setLevel(Level.OFF);
//		StringRenderer.setRenderer(Rendering.DL_SYNTAX);
//	}
//
//	public static OWLOntology readCBD() {
//		logger.debug("reading CBD-based knowledge base (" + kbFilePath + ")...");
//		try (FileInputStream is = new FileInputStream(new File(MouseDiabetesCBD.cbdFilePath))) {
//			OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(is);
//			logger.debug("finished reading files");
//
//			if (containsErrorNamedClasses(ontology)) {
//				System.exit(0);
//			}
//
//			return ontology;
//		} catch (IOException | OWLOntologyCreationException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//
//	public static OWLOntology readDumpFiles() throws IOException {
//		Model model = ModelFactory.createDefaultModel();
//
//		logger.debug("reading main knowledge base (" + kbFilePath + ")...");
//		try (InputStream is = new FileInputStream(new File(kbFilePath))) {
//			model.read(is, null, Lang.RDFXML.getName());
//		}
//
//		logger.debug("reading positive examples data (" + genoDiseaseFilePath + ")...");
//		try (InputStream is = new FileInputStream(new File(genoDiseaseFilePath))) {
//			model.read(is, null, Lang.TURTLE.getName());
//		}
//
//		logger.debug("reading negative examples data (" + genoNotDiseaseFilePath + ")...");
//		try (InputStream is = new FileInputStream(new File(genoNotDiseaseFilePath))) {
//			model.read(is, null, Lang.TURTLE.getName());
//		}
//
//		logger.debug("finished reading files");
//
//		// convert JENA model to OWL API ontology
//		logger.debug("converting to OWLApi ontology...");
//		OWLOntology ontology = OwlApiJenaUtils.getOWLOntology(model);
//		logger.debug("finished conversion");
//
//		// sanity check
//		if (containsErrorNamedClasses(ontology)) {
//			System.exit(0);
//		}
//		return ontology;
//	}
//
//	private static boolean containsErrorNamedClasses(OWLOntology ontology) {
//		for (OWLClass cls : ontology.getClassesInSignature()) {
//			if (cls.toStringID().startsWith("http://org.semanticweb.owlapi/error#")) {
//				return true;
//			}
//		}
//		return false;
//	}

// public void createAnnotation(){
//
//
// OWLAnnotationProperty annotationProperty;
// annotationProperty = datafactory.getOWLAnnotationProperty(
// Constants.FIXED_ANNOTATION_NAME,
// PrefixUtilities.getPrefixOWLOntologyFormat(ontology));
//
// String value;
//
// if (newlyCreatedObjectProperitesList != null &&
// newlyCreatedObjectProperitesList.size() > 0) {
// value = getRuleName() + "___" + getRuleText() + "___" + getComment();
// for (OWLObjectProperty owlObjectProperty :
// newlyCreatedObjectProperitesList) {
// value = value + "___" + owlObjectProperty.getIRI();
// // System.out.println("value: "+ value);
// }
// } else {
// value = getRuleName() + "___" + getRuleText() + "___" + getComment();
// }
//
// OWLAnnotationValue owlLiteral =
// Constants.owlDataFactoryAsStaticReference.getOWLLiteral(value);
// OWLAnnotation annotation =
// Constants.owlDataFactoryAsStaticReference.getOWLAnnotation(fixedAnnotationProperty,
// owlLiteral);
//
// return annotation;
// }
//
// public OWLAnnotation getAnnotation(){
// OWLAnnotation annotation
// }
//
// public OWLAxiom addAxiomAnnotation(OWLAxiom axiom) {
//
// Set<OWLAnnotation> newAnnotations = new HashSet<OWLAnnotation>();
//
// newAnnotations.add(annotation);
//
// OWLAxiom annotatedAxiom = axiom.getAnnotatedAxiom(newAnnotations);
//
// return annotatedAxiom;
// }



//}
