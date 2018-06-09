///**
// *
// */
//package org.dase.util;
//
//import java.io.File;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.text.SimpleDateFormat;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashSet;
//import java.util.Set;
//
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.model.IRI;
//import org.semanticweb.owlapi.model.OWLAxiom;
//import org.semanticweb.owlapi.model.OWLClass;
//import org.semanticweb.owlapi.model.OWLNamedIndividual;
//import org.semanticweb.owlapi.model.OWLObjectProperty;
//import org.semanticweb.owlapi.model.OWLOntology;
//import org.semanticweb.owlapi.model.OWLOntologyCreationException;
//import org.semanticweb.owlapi.model.OWLOntologyStorageException;
//import org.semanticweb.owlapi.model.parameters.ChangeApplied;
//import org.semanticweb.owlapi.reasoner.OWLReasoner;
//import org.semanticweb.owlapi.util.OWLEntityRemover;
//
//import com.wcohen.ss.Levenstein;
//
///**
// * @author sarker
// *
// */
//public class OWLUtility {
//
//
//	/**
//	 * Save combined ontology to file system
//	 *
//	 * @throws OWLOntologyStorageException
//	 */
//	public static void saveOntology(OWLOntology ontology, String path) throws OWLOntologyStorageException {
//
//		IRI owlDiskFileIRIForSave = IRI.create("file:" + path);
//		ontology.getOWLOntologyManager().saveOntology(ontology, owlDiskFileIRIForSave);
//
//	}
//
//	/**
//	 * Load ontology from file system
//	 *
//	 * @throws OWLOntologyCreationException
//	 */
//	public static OWLOntology loadOntology(File ontoFile) throws OWLOntologyCreationException {
//
//		OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontoFile);
//		return ontology;
//
//	}
//
//	public static double computeConfidence(String labelA, String labelB) {
//		double confidenceValue = 1 - (Math.abs(new Levenstein().score(labelA, labelB))
//				/ (double) Math.max(labelA.length(), labelB.length()));
//
//		return confidenceValue;
//	}
//
//	/**
//	 * Removes concepts which do not have a single instance. alternatively keep
//	 * those concepts which have at-least a single instance. TO-DO: FIX
//	 */
//	public static OWLOntology removeNonRelatedConcepts(OWLOntology combinedOntology, OWLReasoner owlReasoner,
//			String writeTo) {
//
//		OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));
//
//		for (OWLClass owlClass : combinedOntology.getClassesInSignature()) {
//			if (owlReasoner.getInstances(owlClass, false).getFlattened().size() < 1) {
//				entityRemover.visit(owlClass);
//			}
//		}
//
//		ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
//		if (writeTo != null)
//			Writer.writeInDisk(writeTo, "Removing nonrelatedConcepts" + ca.toString(), true);
//		//logger.info("Removing " + ca.toString(), true);
//		System.out.println("Removing " + ca.toString());
//
//		return combinedOntology;
//	}
//
//	/**
//	 * Removes concepts which do not have a single instance. alternatively keep
//	 * those concepts which have at-least a single instance. TO-DO: FIX
//	 */
//	public static OWLOntology removeNonRelatedIndividuals(OWLOntology combinedOntology, OWLReasoner owlReasoner, Set<OWLNamedIndividual> excludedIndivs,
//			String writeTo) {
//
//		OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));
//		Set<OWLAxiom> relatedAxioms = new HashSet<OWLAxiom>();
//
//		IRI iri = IRI.create(Constants.prefix + "imageContains");
//		//System.out.println("objProp: "+ iri);
//		OWLObjectProperty objProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(iri);
//
//
//		for (OWLNamedIndividual owlIndi : combinedOntology.getIndividualsInSignature()) {
//			if((!excludedIndivs.contains(owlIndi)) && (!owlIndi.getIRI().getShortForm().startsWith("obj_"))) {
//				entityRemover.visit(owlIndi);
//
//				Set<OWLNamedIndividual> _relatedIndi = owlReasoner.getObjectPropertyValues(owlIndi, objProp).getFlattened();
//				//System.out.println(" indi: "+ owlIndi);
//				for(OWLNamedIndividual _eachRelatedIndi: _relatedIndi) {
//					entityRemover.visit(_eachRelatedIndi);
//					//System.out.println("related indi: "+ _eachRelatedIndi);
//				}
////				relatedAxioms.addAll(combinedOntology.getReferencingAxioms(owlIndi));
////				System.out.println("OWLIndi: "+ owlIndi);
////				for(OWLObjectProperty _objProp: owlIndi.getObjectPropertiesInSignature()) {
////					System.out.println("insignature_obj_prop: "+_objProp);
////				}
////				for(OWLNamedIndividual _indi: owlIndi.getIndividualsInSignature()) {
////					System.out.println("inSignature_indi: "+ _indi.getIRI().getShortForm());
////				}
//			}
//
//		}
//
//		ChangeApplied ca =  combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
//		System.out.println("Removing non related individuals " + ca.toString());
//		for(OWLAxiom ax: relatedAxioms) {
//			//System.out.println("axiom: "+ ax);
//		}
//		ca = combinedOntology.getOWLOntologyManager().removeAxioms(combinedOntology, relatedAxioms);
//
//		System.out.println("Removing non related classes again " + ca.toString());
//
//		if (writeTo != null)
//			Writer.writeInDisk(writeTo, "Removing " + ca.toString(), true);
//		//logger.info("Removing " + ca.toString(), true);
//
//		return combinedOntology;
//	}
//
//
//	/**
//	 * Removes concepts which do not have a single instance. alternatively keep
//	 * those concepts which have at-least a single instance. TO-DO: FIX
//	 */
//	public static OWLOntology removeNonRelatedConcepts(OWLOntology combinedOntology,
//													   OWLReasoner owlReasoner) {
//
//		OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));
//
//		for (OWLClass owlClass : combinedOntology.getClassesInSignature()) {
//			if (owlReasoner.getInstances(owlClass, false).getFlattened().size() < 1) {
//				entityRemover.visit(owlClass);
//			}
//		}
//
//		ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
//
//		//logger.info("Removing " + ca.toString(), true);
//		System.out.println("Removing " + ca.toString());
//
//		return combinedOntology;
//	}
//
//	public static String getCurrentTimeAsString() {
//		String time = new SimpleDateFormat("MM.dd.yyyy  HH.mm.ss a").format(new Date());
//		return time;
//	}
//
//	public static String getStackTraceAsString(Exception e) {
//
//		StringWriter sw = new StringWriter();
//		PrintWriter pw = new PrintWriter(sw);
//		e.printStackTrace(pw);
//		String sStackTrace = sw.toString(); // stack trace as a string
//		return sStackTrace;
//	}
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//
//	}
//
//}
