/**
 * 
 */
package edu.wright.dase.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.dllearner.algorithms.celoe.CELOE;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import com.wcohen.ss.Levenstein;

/**
 * @author sarker
 *
 */
public class Utility {

	/**
	 * 
	 * Save combined ontology to file system
	 * 
	 * @throws OWLOntologyStorageException
	 * 
	 *             DO-NOT-SAVE- Each Combined Ontology do not save in disk for each
	 *             combined ontology. it takes too much space in disk. Each combined
	 *             ontology is being approximately 80MB. 80 MB *22000 = 2000,000 MB
	 *             = 2000 GB
	 */
	public static void saveOntology(OWLOntology ontology, String path) throws OWLOntologyStorageException {

		IRI owlDiskFileIRIForSave = IRI.create("file:" + path);
		ontology.getOWLOntologyManager().saveOntology(ontology, owlDiskFileIRIForSave);

	}

	/**
	 * Load ontology from file system
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public static OWLOntology loadOntology(File ontoFile) throws OWLOntologyCreationException {

		OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontoFile);
		return ontology;

	}

	public static double computeConfidence(String labelA, String labelB) {
		double confidenceValue = 1 - (Math.abs(new Levenstein().score(labelA, labelB))
				/ (double) Math.max(labelA.length(), labelB.length()));

		return confidenceValue;
	}

	/**
	 * Removes concepts which do not have a single instance. alternatively keep
	 * those concepts which have at-least a single instance. TO-DO: FIX
	 */
	public static OWLOntology removeNonRelatedConcepts(OWLOntology combinedOntology, OWLReasoner owlReasoner) {

		return removeNonRelatedConcepts(combinedOntology, owlReasoner, null);

	}

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

		ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
		if (writeTo != null)
			Writer.writeInDisk(writeTo, "Removing nonrelatedConcepts" + ca.toString(), true);
		// logger.info("Removing " + ca.toString(), true);
		System.out.println("Removing " + ca.toString());

		return combinedOntology;
	}

	/**
	 * Removes concepts which do not have a single instance. alternatively keep
	 * those concepts which have at-least a single instance. TO-DO: FIX
	 */
	public static OWLOntology removeNonRelatedIndividuals(OWLOntology combinedOntology, OWLReasoner owlReasoner,
			Set<OWLNamedIndividual> excludedIndivs, String writeTo) {

		OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));
		Set<OWLAxiom> relatedAxioms = new HashSet<OWLAxiom>();

		IRI iri = IRI.create(Constants.prefix + "imageContains");
		// System.out.println("objProp: "+ iri);
		OWLObjectProperty objProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(iri);

		for (OWLNamedIndividual owlIndi : combinedOntology.getIndividualsInSignature()) {
			if ((!excludedIndivs.contains(owlIndi)) && (!owlIndi.getIRI().getShortForm().startsWith("obj_"))) {
				entityRemover.visit(owlIndi);

				Set<OWLNamedIndividual> _relatedIndi = owlReasoner.getObjectPropertyValues(owlIndi, objProp)
						.getFlattened();
				// System.out.println(" indi: "+ owlIndi);
				for (OWLNamedIndividual _eachRelatedIndi : _relatedIndi) {
					entityRemover.visit(_eachRelatedIndi);
					// System.out.println("related indi: "+ _eachRelatedIndi);
				}
				// relatedAxioms.addAll(combinedOntology.getReferencingAxioms(owlIndi));
				// System.out.println("OWLIndi: "+ owlIndi);
				// for(OWLObjectProperty _objProp: owlIndi.getObjectPropertiesInSignature()) {
				// System.out.println("insignature_obj_prop: "+_objProp);
				// }
				// for(OWLNamedIndividual _indi: owlIndi.getIndividualsInSignature()) {
				// System.out.println("inSignature_indi: "+ _indi.getIRI().getShortForm());
				// }
			}

		}

		ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
		System.out.println("Removing non related individuals " + ca.toString());
		for (OWLAxiom ax : relatedAxioms) {
			// System.out.println("axiom: "+ ax);
		}
		ca = combinedOntology.getOWLOntologyManager().removeAxioms(combinedOntology, relatedAxioms);

		System.out.println("Removing non related classes again " + ca.toString());

		if (writeTo != null)
			Writer.writeInDisk(writeTo, "Removing " + ca.toString(), true);
		// logger.info("Removing " + ca.toString(), true);

		return combinedOntology;
	}

	// positive individual counter;
	static int posIndiCounter = 0;

	/**
	 * Load positive instances and also add them to combined ontology
	 */
	public static Set<OWLNamedIndividual> loadPosInstancesAndAddForMerging(String posInstanceFolderPath,
			int negIndiLimit, String writeTo) {

		Set<OWLNamedIndividual> posExamples = new HashSet<OWLNamedIndividual>();

		try {

			Files.walk(Paths.get(posInstanceFolderPath)).filter(f -> negIndiCounter <= negIndiLimit)
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {

						// add to negIndivs
						String name = f.getFileName().toString().replaceAll(".owl", "");
						IRI iriIndi = IRI.create(Constants.prefix + name);
						OWLNamedIndividual namedIndi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(iriIndi);
						posExamples.add(namedIndi);
						posIndiCounter++;
					});
			posIndiCounter = 0;
			return posExamples;
		} catch (IOException e) {
			posIndiCounter = 0;
			return posExamples;
		}

	}

	// nagative individual counter;
	static int negIndiCounter = 0;

	/**
	 * Load positive instances and also add them to combined ontology
	 */
	public static Set<OWLNamedIndividual> loadNegInstances(String negInstanceFolderPath, int negIndiLimit,
			String writeTo) {

		Set<OWLNamedIndividual> negExamples = new HashSet<OWLNamedIndividual>();

		try {

			Files.walk(Paths.get(negInstanceFolderPath)).filter(f -> negIndiCounter <= negIndiLimit)
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {

						// add to negIndivs
						String name = f.getFileName().toString().replaceAll(".owl", "");
						IRI iriIndi = IRI.create(Constants.prefix + name);
						OWLNamedIndividual namedIndi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(iriIndi);
						negExamples.add(namedIndi);
						negIndiCounter++;
					});
			negIndiCounter = 0;
			return negExamples;
		} catch (IOException e) {
			negIndiCounter = 0;
			return negExamples;
		}
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
			Writer.writeInDisk(fileName,
					"\nBest " + best100Classes.size() + " Classes- using getCurrentlyBestDescriptions()", true);
			Writer.writeInDisk(fileName,
					"\n# This is the best class descriptions found by the learning algorithm so far.\n", true);
			for (int i = 0; i < best100Classes.size(); i++) {
				// System.out.println("Best 100 Class- " + i + " :" + best100Classes.get(i));
				// writer.write(best100Classes.get(i) + "\n");
				Writer.writeInDisk(fileName, best100Classes.get(i) + "\n", true);
			}

			// Returns a sorted set of the best descriptions found so far. We assume that
			// they are ordered such that the best ones come in last.
			// (In Java, iterators traverse a SortedSet in ascending order.)
			TreeSet bestEvalClasses = (TreeSet) expl.getCurrentlyBestEvaluatedDescriptions();
			Iterator it = bestEvalClasses.iterator();
			// writer.write("\nBest Eval Class- \n");
			Writer.writeInDisk(fileName, "\n\nBest Evaluated Class- using getCurrentlyBestEvaluatedDescriptions()\n",
					true);
			Writer.writeInDisk(fileName,
					"# Returns a sorted set of the best descriptions found so far. "
							+ "\n# We assume that they are ordered such that the best ones come in last."
							+ "\n# (In Java, iterators traverse a SortedSet in ascending order.\n)",
					true);
			while (it.hasNext()) {
				Object obj = it.next();
				// System.out.println("Best Eval Class- :" + obj);
				// writer.write(it.next()+"\n");
				Writer.writeInDisk(fileName, obj + "\n", true);
			}

			// writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String getCurrentTimeAsString() {
		String time = new SimpleDateFormat("MM.dd.yyyy  HH.mm.ss a").format(new Date());
		return time;
	}

	public static String getStackTraceAsString(Exception e) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString(); // stack trace as a string
		return sStackTrace;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * OWLEntityRemover entityRemover = new
	 * OWLEntityRemover(Collections.singleton(ontology)); for (OWLNamedIndividual
	 * individual : ontology.getIndividualsInSignature(Imports.INCLUDED)) { if
	 * (!individual.getIRI().getShortForm().contains("_Indi_")) {
	 * entityRemover.visit(individual); } }
	 * ontologyManager.applyChanges(entityRemover.getChanges());
	 */

}
