package edu.wright.dase.lu;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.wcohen.ss.Levenstein;

import edu.wright.dase.Explanation;

public class Testing {

	OWLOntology source1;
	OWLOntology source2;
	static OWLOntology targetOnto;
	static OWLOntologyManager ontoManager;
	OWLDataFactory ontoDataFactory;
	static double threshold = 0.8;

	public Testing(OWLOntology source1, OWLOntology source2, OWLOntology targetOnto) {

		this.source1 = source1;
		this.source2 = source2;
		this.targetOnto = targetOnto;
		this.ontoManager = source1.getOWLOntologyManager();
		this.ontoDataFactory = source1.getOWLOntologyManager().getOWLDataFactory();
	}

	public static double computeConfidence(String labelA, String labelB) {
		double confidenceValue = 1 - (Math.abs(new Levenstein().score(labelA, labelB))
				/ (double) Math.max(labelA.length(), labelB.length()));

		return confidenceValue;
	}

	public static void saveOntology(OWLOntology ontology, String path) throws OWLOntologyStorageException {

		IRI owlDiskFileIRIForSave = IRI.create("file:" + path);
		ontoManager.saveOntology(ontology, owlDiskFileIRIForSave);

	}
	
	public static void doAlignMent() throws Exception {
		Set<OWLEntity> entities = new HashSet<>();

		Explanation.init();

		OWLOntology ontB = Explanation.loadOntology(new File("/home/sarker/Desktop/test/ADE_train_00002063.owl"));
		OWLOntology ontA = Explanation.loadOntology(new File("/home/sarker/Desktop/test/_aligned_.owl"));
		String saveTo = "/home/sarker/Desktop/test/_aligned_.owl";
		
		IRI ontoIRI = IRI.create("www.dase.org/hcbd#");
		
		ontoManager = ontA.getOWLOntologyManager();
		
		targetOnto = ontoManager.createOntology(ontoIRI);

		System.out.println("Loaded ontology: " + ontA);
		entities = ontA.getSignature();
		Set<OWLAxiom> axioms = new HashSet<>();

		ontoManager.addAxioms(targetOnto,ontA.getAxioms());
		
		// match classes and all axioms related to that class
//		for (OWLEntity a : ontA.getSignature()) {
//			
//			String lblA = a.getIRI().getShortForm();
////			System.out.println(lblA);
//			// need to preprocess
//			
			for (OWLEntity b : ontB.getObjectPropertiesInSignature()) {
				String lblB = b.getIRI().getShortForm();
				// need to preprocess
//				System.out.println(lblB);
//				double similarity = computeConfidence(lblA, lblB);
				//if (similarity >= threshold) {
				if(lblB.equals("Thing")) {
					axioms = ontA.getReferencingAxioms(b);
					//System.out.println(axioms);
				}
				//}else {
//					axioms = ontB.getReferencingAxioms(b);
//					System.out.println(axioms);
//					ontoManager.addAxioms(targetOnto, axioms);
				//}
			}
//		}
		
		
		// save to disk
		//saveOntology(targetOnto, saveTo);
		
	}

	public static void main(String[] args) throws Exception {
		doAlignMent();
	}
}
