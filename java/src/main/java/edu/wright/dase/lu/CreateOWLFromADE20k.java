package edu.wright.dase.lu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

public class CreateOWLFromADE20k {

	public static String prefix = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
	public static String rootPath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/";
									//"/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/"

	public static String partLevelDataPropertyName = "partLevel";
	public static String isOccludedDataPropertyName = "isOccluded";
	public static String hasAttributeDataPropertyName = "hasAttribute";
	public static String imageContainsObjPropertyName = "imageContains";

	public static int counter = 0;

	public static void main(String[] args) {
		try {
			Files.walk(Paths.get(rootPath)).filter(f -> f.toFile().isFile())
					.filter(f -> f.toFile().getAbsolutePath().endsWith(".txt")).forEach(f -> iterateOverFiles(f));
			// createOWL(
			// "D:/QQDownload/ADE20K_2016_07_26/ADE20K_2016_07_26/images/training/a/abbey/ADE_train_00000970_atr.txt",
			// "Abbey");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void iterateOverFiles(Path path) {
		String parent = path.getParent().getFileName().toString();
		String grandParent = path.getParent().getParent().getFileName().toString();
		String owl_class_name = "";
		String owl_super_class_name = "";
		boolean shouldCreateSuperClass = false;

		// Condition
		// If parent name is misc, then parent folder name is misc
		// If grandparent is not a....z or outliers then class name should be
		// parent_name and grand_parent_name
		if (parent.equals("misc")) {
			owl_class_name = "misc";
		} else if ((grandParent.length() == 1 || grandParent.equals("outliers"))) {
			owl_class_name = parent;
		} else {
			owl_class_name = parent + "_" + grandParent;
			owl_super_class_name = grandParent;
			shouldCreateSuperClass = true;
		}

		// call to create owl files
		try {
			// System.out.println("#########: "+ path.toString());
			// D:\QQDownload\ADE20K_2016_07_26\ADE20K_2016_07_26\images\training\a\airport_terminal\ADE_train_00001150_atr.txt
			createOWL(path, owl_class_name, shouldCreateSuperClass, owl_super_class_name);
			printStatus(path.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void printStatus(String status) {
		try {
			counter++;
			System.out.println("creating owl from file: " + status + " is successfull");
			System.out.println("Processed " + counter + " files");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * 
	 */
	public static void createOWL(Path filePath, String owl_class_name, boolean shouldCreateSuperClass,
			String owl_super_class_name) throws Exception {

		File f = filePath.toFile();
		String imageName = f.getName().replaceAll("_atr.txt", "");
		// System.out.println("debug: imageName: " + imageName);

		// Create Ontology
		OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();

		OWLDataFactory owlDataFactory = owlManager.getOWLDataFactory();

		IRI ontologyIRI = IRI.create(imageName.replaceAll("_atr", ".owl"));

		String temp = f.getAbsolutePath().replaceAll("_atr.txt", ".owl");
		String diskFileName = temp.replace("\\", "/");
		IRI owlDiskFileIRI = IRI.create("file:" + diskFileName);

//		 System.out.println("debug: temp: "+ temp);
//		 System.out.println("debug: diskFileName: "+ diskFileName);
//		 System.out.println("debug: owlDiskFileIRI: " + owlDiskFileIRI);
		OWLOntology ontology = owlManager.createOntology(ontologyIRI);
		// System.out.println("created ontology: " + ontology);

		// create individual
		IRI iriIndi = IRI.create(prefix + imageName);
		OWLNamedIndividual namedIndiImage = owlDataFactory.getOWLNamedIndividual(iriIndi);

		// create class and super class
		IRI iriClass = IRI.create(prefix + owl_class_name);
		OWLClass owlClass = owlDataFactory.getOWLClass(iriClass);

		if (shouldCreateSuperClass) {
			assert (owl_super_class_name.length() > 0);
			IRI iriSuperClass = IRI.create(prefix + owl_super_class_name);
			OWLClass owlSuperClass = owlDataFactory.getOWLClass(iriSuperClass);
			OWLAxiom axiom = owlDataFactory.getOWLSubClassOfAxiom(owlClass, owlSuperClass);
			AddAxiom addAxiom = new AddAxiom(ontology, axiom);
			owlManager.applyChange(addAxiom);
		}

		// create object property
		IRI iriObjectProp = IRI.create(prefix + imageContainsObjPropertyName);
		OWLObjectProperty owlObjPropImageContains = owlDataFactory.getOWLObjectProperty(iriObjectProp);
//		OWLDataMinCardinality owlDataMinCardinality = owlDataFactory.getOWLDataMinCardinality(0,
//				owlDataPropertyPartLevel);
//		OWLAxiom axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyPartLevel,
//				owlDataFactory.getIntegerOWLDatatype());
//		AddAxiom addAxiom = new AddAxiom(ontology, axiom);
//		owlManager.applyChange(addAxiom);
		
		// create data property
		IRI iriDataProperty = IRI.create(prefix + partLevelDataPropertyName);
		OWLDataProperty owlDataPropertyPartLevel = owlDataFactory.getOWLDataProperty(iriDataProperty);
		OWLDataMinCardinality owlDataMinCardinality = owlDataFactory.getOWLDataMinCardinality(0,
				owlDataPropertyPartLevel);
		OWLAxiom axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyPartLevel,
				owlDataFactory.getIntegerOWLDatatype());
		AddAxiom addAxiom = new AddAxiom(ontology, axiom);
		owlManager.applyChange(addAxiom);

		iriDataProperty = IRI.create(prefix + isOccludedDataPropertyName);
		OWLDataProperty owlDataPropertyIsOccluded = owlDataFactory.getOWLDataProperty(iriDataProperty);
		axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyIsOccluded,
				owlDataFactory.getBooleanOWLDatatype());
		addAxiom = new AddAxiom(ontology, axiom);
		owlManager.applyChange(addAxiom);

		iriDataProperty = IRI.create(prefix + hasAttributeDataPropertyName);
		OWLDataProperty owlDataPropertyHasAttribute = owlDataFactory.getOWLDataProperty(iriDataProperty);
		axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyHasAttribute,
				owlDataFactory.getOWLDatatype(XSDVocabulary.STRING.getIRI()));
		addAxiom = new AddAxiom(ontology, axiom);
		owlManager.applyChange(addAxiom);

		// assign individual to class
		// do not assign it the corresponding class, instead assign it to OWL:Thing
		System.out.println("Individual Name: "+ namedIndiImage.getIRI().toString());
		OWLClassAssertionAxiom owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlDataFactory.getOWLThing(), namedIndiImage);
		addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
		owlManager.applyChange(addAxiom);

		// Read files and Parse data
		FileReader reader = new FileReader(filePath.toString());
		BufferedReader bfr = new BufferedReader(reader);

		String line;
		// Set<String> terms = new HashSet<>();

		while ((line = bfr.readLine()) != null) {
			// this is a single line
			// example 017 # 0 # 0 # plant, flora, plant life # plants # ""
			String[] column = line.split("#");

			for (int i = 0; i < column.length; i++) {
				column[i] = column[i].trim();

			}

			// create namedIndividual
			// column[0]
			String instanceName = "obj_" + column[0] + "_" + column[1] + "_" + imageName;
			iriIndi = IRI.create(prefix + instanceName);
			OWLNamedIndividual namedIndiObject = owlDataFactory.getOWLNamedIndividual(iriIndi);
			// Assign this objects to the image by using imagecontains object property
			OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(owlObjPropImageContains, namedIndiImage, namedIndiObject);
			addAxiom = new AddAxiom(ontology, owlObjectPropertyAssertionAxiom);
			owlManager.applyChange(addAxiom);
			
			// create class and assign individual to class
			// Column[3] Wordnet
			String[] classes = column[3].split(",");
			for (String eachClass : classes) {

				eachClass = eachClass.trim().replace(" ", "_");
				eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);
				// create class
				iriClass = IRI.create(prefix + "WN_" + eachClass);
				owlClass = owlDataFactory.getOWLClass(iriClass);
				// assign individual to class
				owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
				addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
				owlManager.applyChange(addAxiom);
			}

			// Column[4] Raw name
			String[] rawClasses = column[4].split(",");
			for (String eachClass : rawClasses) {

				eachClass = eachClass.trim().replace(" ", "_");
				eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);
				// create class
				iriClass = IRI.create(prefix + eachClass);
				owlClass = owlDataFactory.getOWLClass(iriClass);
				// assign individual to class
				owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
				addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
				owlManager.applyChange(addAxiom);
			}

			// Column[1] part level
			// hasAttribute
			// this contains the part of relation
			// this can be merged without if-else
			// for our understanding keep this now.
			if (column[1].equals("0")) {
				axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyPartLevel, namedIndiObject,
						Integer.parseInt(column[1]));
				addAxiom = new AddAxiom(ontology, axiom);
				owlManager.applyChange(addAxiom);
			} else {
				// column[1] = 1, 2, 3 or more
				// assign dataProperty
				axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyPartLevel, namedIndiObject,
						Integer.parseInt(column[1]));
				addAxiom = new AddAxiom(ontology, axiom);
				owlManager.applyChange(addAxiom);
			}

			// Column[2]
			// isOccluded
			if (column[2].equals("0")) {
				axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyIsOccluded, namedIndiObject,
						owlDataFactory.getOWLLiteral(false));
				addAxiom = new AddAxiom(ontology, axiom);
				ChangeApplied ca = owlManager.applyChange(addAxiom);

			} else if (column[2].equals("1")) {
				axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyIsOccluded, namedIndiObject,
						owlDataFactory.getOWLLiteral(true));
				addAxiom = new AddAxiom(ontology, axiom);
				ChangeApplied ca = owlManager.applyChange(addAxiom);

			}

			// column[5] attributes
			column[5] = column[5].replaceAll("\"", "");
			if (column[5].length() > 0) {
				String[] attributes = column[5].split(",");
				for (String attribute : attributes) {
					attribute = attribute.trim();
					axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyHasAttribute, namedIndiObject,
							owlDataFactory.getOWLLiteral(attribute));
					addAxiom = new AddAxiom(ontology, axiom);
					ChangeApplied ca = owlManager.applyChange(addAxiom);
					// for (OWLOntologyChange eca : ca) {
					// System.out.println("change: " + eca);
					// }
				}
			}

		}

		// Save Ontology
		owlManager.saveOntology(ontology, owlDiskFileIRI);

		// System.out.println("saved on file: " + owlDiskFileIRI +
		// "\nSuccessfull");
	}
}
