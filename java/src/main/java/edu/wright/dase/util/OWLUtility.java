/**
 * 
 */
package edu.wright.dase.util;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.wcohen.ss.Levenstein;

/**
 * @author sarker
 *
 */
public class OWLUtility {

	
	/**
	 * Save combined ontology to file system
	 * 
	 * @throws OWLOntologyStorageException
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
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
