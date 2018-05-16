package org.dase.explanation.dllearner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import org.dase.util.Constants;
import org.dase.util.Utility;

public class Alignment {

	static OWLOntology targetOnto;
	static OWLOntologyManager ontoManager;
	static OWLDataFactory ontoDataFactory;
	static double threshold = 0.8;
	static String sumoPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo_aligned/without_scores/sumo_with_imgContains_without_score_without_wordnet.owl";
	static final String adePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ning_manual/DL_tensorflow_save_v3_txts_as_dirs_owl_without_score_without_wordnet/";
	static int counter = 0;
	// static String targetPath =
	// "/Users/sarker/Mega_Cloud/ProjectHCBD/datas/sumo_aligned/sumo_with_imgContain_others_aligned.owl";

	public static void doAlignMent(String pathA, String pathB, String pathTarget) throws Exception {

		// Explanation.init();

		OWLOntology ontA = Utility.loadOntology(pathA);
		System.out.println("Loaded ontology: " + ontA.getOntologyID());
		OWLOntology ontB = Utility.loadOntology(pathB);
		System.out.println("Loaded ontology: " + ontB.getOntologyID());
		OWLReasonerFactory reasonerFactory = new PelletReasonerFactory();
		OWLReasoner owlReasoner = reasonerFactory.createNonBufferingReasoner(ontA);

		String saveTo = pathTarget;
		IRI ontoIRI = IRI.create(Constants.prefix, "aligned");

		ontoManager = OWLManager.createOWLOntologyManager();
		ontoDataFactory = ontoManager.getOWLDataFactory();

		targetOnto = OWLManager.createOWLOntologyManager().createOntology(ontoIRI);

		// load all from onto A to target onto
		ontoManager.addAxioms(targetOnto, ontA.getAxioms());

		ChangeApplied changeApplied = null;
		double similarity = 0;

		// match classes and all axioms related to that class
		for (OWLClass b : ontB.getClassesInSignature()) {

			Set<OWLAxiom> axioms = new HashSet<>();
			axioms = ontB.getReferencingAxioms(b);

			String lblB = b.getIRI().getShortForm();

			// System.out.println(lblB);
			// need to preprocess
			OWLClass matchedClass = null;

			for (OWLClass a : ontA.getClassesInSignature()) {
				String lblA = a.getIRI().getShortForm();
				// need to preprocess
				// System.out.println(lblA);

				 similarity = Utility.computeConfidence(lblA,
						lblB.startsWith("WN_") ? lblB.substring(3) : lblB);
				if (similarity >= threshold) {
					matchedClass = a;
					break;
				}
			}

			// found a matching class
			if (matchedClass != null) {
				// starts with WN_
				if (lblB.startsWith("WN_")) {
					// make it exact subclass only if correct match
					if(similarity == 1) {
						Set<OWLClass> directSuperClasses = owlReasoner.getSuperClasses(matchedClass, true).getFlattened();
						OWLClass immediateSuperClass = null;
						System.out.println("Class: " + matchedClass +" \twn: " +lblB);
						for (OWLClass cls : directSuperClasses) {
							immediateSuperClass = cls;
							System.out.println("SuperClass: " + cls);
						}
						System.out.println("Selected SuperClass: " + immediateSuperClass);
						System.out.println("");
						// make it subclass of immediateSuperClass
						changeApplied = ontoManager.addAxiom(targetOnto,
								ontoDataFactory.getOWLSubClassOfAxiom(b, immediateSuperClass));
					}else {
						// make it subclass of immediateSuperClass
						changeApplied = ontoManager.addAxiom(targetOnto,
								ontoDataFactory.getOWLSubClassOfAxiom(b, ontoDataFactory.getOWLThing()));
					}
					

				}
				changeApplied = ontoManager.addAxioms(targetOnto, axioms);

			} else {
				// did not found a matching class
				// declare class b as subclass of OWL:Thing
				OWLSubClassOfAxiom subClassAxiom = ontoDataFactory.getOWLSubClassOfAxiom(b,
						ontoDataFactory.getOWLThing());
				AddAxiom addAxiom = new AddAxiom(targetOnto, subClassAxiom);
				changeApplied = ontoManager.applyChange(addAxiom);
				// System.out.println("class assertion changeapplied: " + changeApplied);
				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
				// System.out.println("not matchedClass changeapplied: " + changeApplied);

			}
		}

		// match objectProperties and all axioms related to that objectproperties
		for (OWLObjectProperty b : ontB.getObjectPropertiesInSignature()) {

			Set<OWLAxiom> axioms = new HashSet<>();
			axioms = ontB.getReferencingAxioms(b);

			String lblB = b.getIRI().getShortForm();
			// System.out.println(lblB);
			// need to preprocess
			OWLObjectProperty matchedProperty = null;

			for (OWLObjectProperty a : ontA.getObjectPropertiesInSignature()) {
				String lblA = a.getIRI().getShortForm();
				// need to preprocess
				// System.out.println(lblA);

				 similarity = Utility.computeConfidence(lblA, lblB);
				if (similarity >= threshold) {
					matchedProperty = a;
					break;
				}
			}

			// found a matching objectProperty
			if (matchedProperty != null) {
				ontoManager.addAxioms(targetOnto, axioms);
				// System.out.println(" matched ObjProperty changeapplied: " + changeApplied);
			} else {

				// did not found a matching object property
				// declare objProp b as subclass of OWL:TopObjectProperty
				OWLSubObjectPropertyOfAxiom subObjPropAxiom = ontoDataFactory.getOWLSubObjectPropertyOfAxiom(b,
						ontoDataFactory.getOWLTopObjectProperty());
				AddAxiom addAxiom = new AddAxiom(targetOnto, subObjPropAxiom);
				changeApplied = ontoManager.applyChange(addAxiom);
				// System.out.println("objProp assertion changeapplied: " + changeApplied);
				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
				// System.out.println(" not matchedObjProperty changeapplied: " +
				// changeApplied);

				// this should never happen
				System.out.println("$$$$$$$$$$$$$$$$$$$$$$ ERROR $$$$$$$$$$$$$$$$$$$$$");
				throw new RuntimeException("Object property not matched");

			}
		}

		// match dataProperties and all axioms related to that dataproperties
		for (OWLDataProperty b : ontB.getDataPropertiesInSignature()) {

			Set<OWLAxiom> axioms = new HashSet<>();
			axioms = ontB.getReferencingAxioms(b);

			String lblB = b.getIRI().getShortForm();
			// System.out.println(lblB);
			// need to preprocess
			OWLDataProperty matchedProperty = null;

			for (OWLDataProperty a : ontA.getDataPropertiesInSignature()) {
				String lblA = a.getIRI().getShortForm();
				// need to preprocess
				// System.out.println(lblA);

				 similarity = Utility.computeConfidence(lblA, lblB);
				if (similarity >= threshold) {
					matchedProperty = a;
					break;
				}
			}

			// found a matching dataProperty
			if (matchedProperty != null) {
				ontoManager.addAxioms(targetOnto, axioms);
				// System.out.println(" matched DataProperty changeapplied: " + changeApplied);
			} else {

				// did not found a matching object property
				// declare objProp b as subclass of OWL:TopObjectProperty
				OWLSubDataPropertyOfAxiom subDataPropAxiom = ontoDataFactory.getOWLSubDataPropertyOfAxiom(b,
						ontoDataFactory.getOWLTopDataProperty());
				AddAxiom addAxiom = new AddAxiom(targetOnto, subDataPropAxiom);
				changeApplied = ontoManager.applyChange(addAxiom);
				// System.out.println("dataProp assertion changeapplied: " + changeApplied);
				changeApplied = ontoManager.addAxioms(targetOnto, axioms);
				// System.out.println(" not matched DataProperty changeapplied: " +
				// changeApplied);

				// this should never happen
				System.out.println("$$$$$$$$$$$$$$$$$$$$$$ ERROR $$$$$$$$$$$$$$$$$$$$$");
				throw new RuntimeException("Data property not matched");

			}
		}

		System.out.println("debug: saveTo: " + saveTo);
		// save to disk
		Utility.saveOntology(targetOnto, saveTo);

		counter++;
		printStatus(pathB);

	}

	public static void printStatus(String status) {
		try {

			System.out.println("aligning owl from file: " + status + " is successfull");
			System.out.println("Processed " + counter + " files");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static int c = 0;

	public static void main(String[] args) {
		String pathA = sumoPath;

		// TODO Auto-generated method stub
		try {
			Files.walk(Paths.get(adePath)).filter(f -> f.toFile().isFile())
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".owl")).forEach(f -> {
						try {
							c++;
							doAlignMent(pathA, f.toFile().getAbsolutePath(), pathA);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Alignment with sumo ontology
	 * 
	 * @throws OWLOntologyCreationException
	 */
	// public static void alignWithSumo() throws OWLOntologyCreationException {
	// sumoOntology = loadOntology(new File(sumoFilePath));
	//
	// // align Classes
	//
	// /**
	// * // there are a lot of considerations for alignment. So just combine them
	// now.
	// */
	//
	// sourceOntologies.add(sumoOntology);
	// OntologyMerger merger = new OntologyMerger(owlOntologyManager,
	// sourceOntologies, combinedOntology);
	// merger.mergeOntologies();
	//
	// // for (OWLClass classFromSumo : sumoOntology.getClassesInSignature()) {
	// // for (OWLClass classFromADE : combinedOntology.getClassesInSignature()) {
	// //
	// // double distance =
	// // Alignment.computeConfidence(classFromSumo.getIRI().getShortForm(),
	// // classFromADE.getIRI().getShortForm());
	// // if (distance > distanceThreshold) {
	// //
	// //
	// // // align individuals
	// // for (OWLIndividual indi : classFromADE.getIndividualsInSignature()) {
	// // OWLClassAssertionAxiom ax =
	// // owlDataFactory.getOWLClassAssertionAxiom(classFromSumo, indi);
	// // ChangeApplied ca = owlOntologyManager.addAxiom(sumoOntology, ax);
	// // }
	// //
	// // // // align object properties
	// // // for(OWLObjectProperty objProp:
	// // classFromADE.getObjectPropertiesInSignature())
	// // // {
	// // //
	// // // }
	// // }
	// // }
	// // }
	// //
	// // // // align object properties
	// // // for(OWLObjectProperty objProp:
	// // // combinedOntology.getObjectPropertiesInSignature()) {
	// // // objProp.
	// // // }
	// //
	// // // align data properties
	// //
	// // //
	// //
	// //
	// // combinedOntology = sumoOntology;
	// }

}
