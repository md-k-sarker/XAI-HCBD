package edu.wright.dase.lu;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import com.wcohen.ss.Levenstein;

import edu.wright.dase.Explanation;
import edu.wright.dase.util.Constants;

public class Testing {

	OWLOntology source1;
	OWLOntology source2;
	static OWLOntology targetOnto;
	static OWLOntologyManager ontoManager;
	static OWLDataFactory ontoDataFactory;
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

//	public static void doAlignMent() throws Exception {
//		
//		Explanation.init();
//
//		OWLOntology ontB = Explanation.loadOntology(new File("/Users/sarker/Mega_Cloud/ProjectHCBD/datas/ning_manual/DL_tensorflow_save_2_txts_as_dirs/AirportTerminal_/airport_terminal_ADE_train_00001092.owl"));
//		OWLOntology ontA = Explanation.loadOntology(new File("/Users/sarker/Desktop/test/_aligned__.owl"));
//		String saveTo = "/Users/sarker/Desktop/test/_aligned__.owl";
//
//		IRI ontoIRI = IRI.create(Constants.prefix , "aligned");
//
//		ontoManager = ontA.getOWLOntologyManager();
//		ontoDataFactory = ontoManager.getOWLDataFactory();
//
//		targetOnto = OWLManager.createOWLOntologyManager().createOntology(ontoIRI);
//
//		System.out.println("Loaded ontology: " + ontA);
//		
//		// load all from onto a to target onto
//		ontoManager.addAxioms(targetOnto, ontA.getAxioms());
//
//		ChangeApplied changeApplied = null;
//		
//		// match classes and all axioms related to that class
//		for (OWLClass b : ontB.getClassesInSignature()) {
//
//			Set<OWLAxiom> axioms = new HashSet<>();
//			axioms = ontB.getReferencingAxioms(b);
//
//			String lblB = b.getIRI().getShortForm();
//			//System.out.println(lblB);
//			// need to preprocess
//			OWLClass matchedClass = null;
//
//			for (OWLClass a : ontA.getClassesInSignature()) {
//				String lblA = a.getIRI().getShortForm();
//				// need to preprocess
//				//System.out.println(lblA);
//
//				double similarity = computeConfidence(lblA, lblB);
//				if (similarity >= threshold) {
//					matchedClass = a;
//					break;
//				}
//			}
//
//			// found a matching class
//			if (matchedClass != null) {
//				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
//				System.out.println(" matchedClass changeapplied: " + changeApplied);
//			} else {
//				// did not found a matching class
//				// declare class b as subclass of OWL:Thing
//				OWLSubClassOfAxiom subClassAxiom = ontoDataFactory.getOWLSubClassOfAxiom(b,
//						ontoDataFactory.getOWLThing());
//				AddAxiom addAxiom = new AddAxiom(targetOnto, subClassAxiom);
//				changeApplied = ontoManager.applyChange(addAxiom);
//				System.out.println("class assertion changeapplied: " + changeApplied);
//				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
//				System.out.println("not matchedClass changeapplied: " + changeApplied);
//
//			}
//		}
//
//		// match objectProperties and all axioms related to that objectproperties
//		for (OWLObjectProperty b : ontB.getObjectPropertiesInSignature()) {
//
//			Set<OWLAxiom> axioms = new HashSet<>();
//			axioms = ontB.getReferencingAxioms(b);
//
//			String lblB = b.getIRI().getShortForm();
//			//System.out.println(lblB);
//			// need to preprocess
//			OWLObjectProperty matchedProperty = null;
//
//			for (OWLObjectProperty a : ontA.getObjectPropertiesInSignature()) {
//				String lblA = a.getIRI().getShortForm();
//				// need to preprocess
//				//System.out.println(lblA);
//
//				double similarity = computeConfidence(lblA, lblB);
//				if (similarity >= threshold) {
//					matchedProperty = a;
//					break;
//				}
//			}
//
//			// found a matching objectProperty
//			if (matchedProperty != null) {
//				ontoManager.addAxioms(targetOnto, axioms);
//				System.out.println(" matched ObjProperty changeapplied: " + changeApplied);
//			} else {
//				
//				// did not found a matching object property
//				// declare objProp b as subclass of OWL:TopObjectProperty
//				OWLSubObjectPropertyOfAxiom subObjPropAxiom = ontoDataFactory.getOWLSubObjectPropertyOfAxiom(b,
//						ontoDataFactory.getOWLTopObjectProperty());
//				AddAxiom addAxiom = new AddAxiom(targetOnto, subObjPropAxiom);
//				changeApplied = ontoManager.applyChange(addAxiom);
//				System.out.println("objProp assertion changeapplied: " + changeApplied);
//				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
//				System.out.println(" not matchedObjProperty changeapplied: " + changeApplied);
//				
//				// this should never happen
//				System.out.println("$$$$$$$$$$$$$$$$$$$$$$ ERROR $$$$$$$$$$$$$$$$$$$$$");
//				throw new RuntimeException("Object property not matched");
//
//			}
//		}
//
//		// match dataProperties and all axioms related to that dataproperties
//		for (OWLDataProperty b : ontB.getDataPropertiesInSignature()) {
//
//			Set<OWLAxiom> axioms = new HashSet<>();
//			axioms = ontB.getReferencingAxioms(b);
//
//			String lblB = b.getIRI().getShortForm();
//			//System.out.println(lblB);
//			// need to preprocess
//			OWLDataProperty matchedProperty = null;
//
//			for (OWLDataProperty a : ontA.getDataPropertiesInSignature()) {
//				String lblA = a.getIRI().getShortForm();
//				// need to preprocess
//				// System.out.println(lblA);
//
//				double similarity = computeConfidence(lblA, lblB);
//				if (similarity >= threshold) {
//					matchedProperty = a;
//					break;
//				}
//			}
//
//			// found a matching dataProperty
//			if (matchedProperty != null) {
//				ontoManager.addAxioms(targetOnto, axioms);
//				System.out.println(" matched DataProperty changeapplied: " + changeApplied);
//			} else {
//				
//				// did not found a matching object property
//				// declare objProp b as subclass of OWL:TopObjectProperty
//				OWLSubDataPropertyOfAxiom subDataPropAxiom = ontoDataFactory.getOWLSubDataPropertyOfAxiom(b,
//						ontoDataFactory.getOWLTopDataProperty());
//				AddAxiom addAxiom = new AddAxiom(targetOnto, subDataPropAxiom);
//				changeApplied = ontoManager.applyChange(addAxiom);
//				System.out.println("dataProp assertion changeapplied: " + changeApplied);
//				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
//				System.out.println(" not matched DataProperty changeapplied: " + changeApplied);
//				
//				// this should never happen
//				System.out.println("$$$$$$$$$$$$$$$$$$$$$$ ERROR $$$$$$$$$$$$$$$$$$$$$");
//				throw new RuntimeException("Data property not matched");
//
//			}
//		}
//
//		// save to disk
//		saveOntology(targetOnto, saveTo);
//
//	}

	public static void main(String[] args) throws Exception {
		//doAlignMent();
		
		String a = "abcdefg";
		double i = 0;
		String time = new SimpleDateFormat("MM.dd.yyyy  HH.mm.ss a").format(new Date());
		System.out.println(time);
	}
}
