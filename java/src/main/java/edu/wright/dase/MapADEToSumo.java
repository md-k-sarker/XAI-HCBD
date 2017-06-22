package edu.wright.dase;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dllearner.core.OntologyFormat;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormatImpl;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

import edu.wright.dase.util.Constants;

public class MapADEToSumo extends ModifyOntology {

	private static final Logger logger = LoggerFactory.getLogger(MapADEToSumo.class);

	private HashMap<String, Integer> concept_indi_mapping;
	private HashMap<String, Integer> no_of_items_in_image;
	private static OWLObjectProperty imgContains;
	private static HashMap<String, HashSet<String>> file_browser_mapping;

	/**
	 * @return the imgContains
	 */
	public OWLObjectProperty getImgContains() {
		return imgContains;
	}

	/**
	 * @param imgContains
	 *            the imgContains to set
	 */
	public void setImgContains(OWLObjectProperty imgContains) {
		this.imgContains = imgContains;
	}

	/**
	 * @return the file_browser_mapping
	 */
	public static HashMap<String, HashSet<String>> getFile_mapping() {
		return file_browser_mapping;
	}

	/**
	 * @param file_browser_mapping
	 *            the file_browser_mapping to set
	 */
	public static void setFile_mapping(HashMap<String, HashSet<String>> file_mapping) {
		MapADEToSumo.file_browser_mapping = file_mapping;
	}

	public MapADEToSumo() {
		super();
	}

	@Override
	public void init() {
		super.init();
		concept_indi_mapping = new HashMap<>();
		file_browser_mapping = new HashMap<>();
	}

	//@formatter:off
	/*
	 * column 1=instance number, 
	 * column 2=part level (0 for objects), 
	 * column 3=occluded (1 for true), 
	 * column 4=class name (parsed using wordnet),
	 * column 5=original raw name (might provide a more detailed categorization), 
	 * column 6=comma separated attributes list.
	 */
	//@formatter:on
	public void extractTagText(Path path, Integer individualNo) {
		no_of_items_in_image = new HashMap<String, Integer>();

		try {
			List<String> lines = Files.readAllLines(path);
			Integer no_of_concept = 0;
			++no_of_concept;

			// this individual
			String folderName = path.getParent().getFileName().toString().trim().replace(" ", "_");
			folderName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, folderName);
			IRI indiIri = IRI.create(defaultOntologyIRIPrefix, folderName + "_indi_" + individualNo);
			OWLNamedIndividual thisIndi = datafactory.getOWLNamedIndividual(indiIri);

			for (String line : lines) {
				String[] columns = line.split("#");
				String[] col_4_concepts_in_image = columns[3].trim().toLowerCase().split(",");
				String[] col_6_attributes_for_concepts = columns[5].trim().replace("\"", "").split(",");

				for (String concept : col_4_concepts_in_image) {
					// trim the concept names of the tag in text file
					concept = concept.trim().replace(" ", "_");
					concept = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, concept);

					if (no_of_items_in_image.containsKey(concept)) {
						no_of_items_in_image.put(concept, (no_of_items_in_image.get(concept) + 1));
					} else {
						no_of_items_in_image.put(concept, 1);
					}

					/*
					 * If an image has multiple apple menthioned in multiple
					 * line of tag text then that information is lost. Need to
					 * preserve it. Need to fix.
					 */
					IRI indiFromLineIri = IRI.create(defaultOntologyIRIPrefix, concept + "_indi_" + 1);
					OWLNamedIndividual indiFromLine = datafactory.getOWLNamedIndividual(indiFromLineIri);

					// Set<OWLNamedIndividual> individuals =
					// ontology.getIndividualsInSignature();
					// for(OWLNamedIndividual eachIndi: individuals){
					// if(eachIndi.equals(thisIndi)){
					//
					// break;
					// }
					// }
					// search for individual

					// create object property axioms
					OWLObjectPropertyAssertionAxiom assAxiom = datafactory
							.getOWLObjectPropertyAssertionAxiom(getImgContains(), thisIndi, indiFromLine);
					ontologyChanges.add(new AddAxiom(ontology, assAxiom));
				}
			}
		} catch (IOException e) {

			logger.error("IOException: ", e);
		}
	}

	/**
	 * 
	 */
	public void createObjectProperty() {

		IRI objIRI = IRI.create(defaultOntologyIRIPrefix, Constants.objPropImgConName);

		imgContains = datafactory.getOWLObjectProperty(objIRI);

	}

	/**
	 * Check wether a conceptName (got from folder structure of ADE20K dataset)
	 * already exists in the ontology. If found return the OWLCLass else return
	 * null
	 * 
	 * @param conceptName
	 * @return OWLClass or null
	 */
	public OWLClass getExistingClassIfExist(String conceptName) {

		Set<OWLClass> existingClasses = ontology.getClassesInSignature(Imports.INCLUDED);

		for (OWLClass owlClass : existingClasses) {
			String shortForm = owlClass.getIRI().getShortForm();
			/*
			 * underscore is deleted from folder name to match with existing
			 * sumo ontology. Example: water_treatment_plant
			 */
			if (conceptName.trim().equalsIgnoreCase(shortForm.trim())) {
				return owlClass;
			}
		}

		return null;

	}

	/**
	 * Select a owlClass which is as close as possible to the new class
	 * 
	 * @return
	 */
	public OWLClass getNearestClass(String conceptName) {

		Set<OWLClass> existingClasses = ontology.getClassesInSignature(Imports.INCLUDED);
		/*
		 * Example name : indoor.wrestling_ring
		 */
		String[] conceptNames = conceptName.replace("_", "").split("\\.");
		if (conceptNames.length == 2) {
			for (OWLClass owlClass : existingClasses) {
				String shortForm = owlClass.getIRI().getShortForm();
				if (conceptNames[1].trim().equalsIgnoreCase(shortForm.trim())) {
					// need to return superclass not the matching class
					Configuration configuration = new Configuration();
					Reasoner reasoner = new Reasoner(configuration, ontology);
					NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(owlClass, true);

					for (Node superClass : superClasses) {
						OWLClass cls = (OWLClass) superClass.getRepresentativeElement();
						logger.info("owlClass: " + owlClass.getIRI());
						logger.info("sup: " + cls.getIRI());
					}
					return owlClass;
				}
			}
		} else if (conceptNames.length == 1) {
			for (OWLClass owlClass : existingClasses) {
				String shortForm = owlClass.getIRI().getShortForm();

				if (conceptNames[0].trim().equalsIgnoreCase(shortForm.trim())) {
					// need to return superclass not the matching class
					Reasoner reasoner = new Reasoner(null, ontology);
					NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(owlClass, true);
					logger.info("owlClass: " + owlClass.getIRI());
					for (Node superClass : superClasses) {
						OWLClass cls = (OWLClass) superClass.getRepresentativeElement();
						logger.info("sup: " + cls.getIRI());
					}
					return owlClass;
				}
			}
		} else {
			logger.info("Extreme case: conceptname: " + conceptName + "\t" + conceptNames.length);
		}

		return null;
	}

	/**
	 * Create class hierarchy by combining newly created class with the existing
	 * classes.
	 * 
	 * @precondition ADE20K dataset already traversed.
	 * @param
	 * @return
	 */
	public void createClassIndiHierarchy() {

		logger.info("createClassIndiHierarchy started");

		if (concept_indi_mapping != null && !concept_indi_mapping.isEmpty()) {
			for (String a_concept_name : concept_indi_mapping.keySet()) {
				List<OWLOntologyChange> changes = new ArrayList<>();

				// check wether the concept already exist
				String concept_name = a_concept_name.replace("_", "").replace(".", "");
				OWLClass existingClass = getExistingClassIfExist(concept_name);
				OWLClass owlClass;

				if (existingClass != null) {
					owlClass = existingClass;
				} else {
					// concept not found in the existing ontology. Creating new
					// concept/class
					IRI conceptIri = IRI.create(defaultOntologyIRIPrefix, concept_name);
					owlClass = datafactory.getOWLClass(conceptIri);

					/*
					 * Need to insert it into proper class hierarchy by string
					 * matching. Don't make it subclass of OWLThing. Current
					 * implementation is performing 100% string matching to find
					 * nearest class. Need to fix it with Lu
					 * 
					 */
					OWLAxiom classAxiom;
					OWLClass nearestClass = getNearestClass(concept_name);
					if (nearestClass != null) {
						classAxiom = datafactory.getOWLSubClassOfAxiom(owlClass, nearestClass);
					} else {
						classAxiom = datafactory.getOWLSubClassOfAxiom(owlClass, datafactory.getOWLThing());
					}
					// classAxiom = addAxiomAnnotation(classAxiom);
					changes.add(new AddAxiom(ontology, classAxiom));
				}

				// Need to test and check it
				Integer noOfIndi = concept_indi_mapping.get(a_concept_name);
				for (Integer i = 0; i < noOfIndi; i++) {

					// create individual
					IRI indiIri = IRI.create(defaultOntologyIRIPrefix, concept_name + "_indi_" + i);
					OWLNamedIndividual indiv = datafactory.getOWLNamedIndividual(indiIri);
					OWLAxiom classAssertionAxiom = datafactory.getOWLClassAssertionAxiom(owlClass, indiv);
					changes.add(new AddAxiom(ontology, classAssertionAxiom));
				}

				// write the change immediately to be able to find in next
				// concpet name search
				if (!changes.isEmpty()) {
					ontologyManager.applyChanges(changes);
					changes.clear();
				}
			}
		}

		logger.info("createClassIndiHierarchy finished");
	}

	/*
	 * If the folder has name indoor or outdoor it does not mean anything by
	 * itself. It should to be appended with parent folder name to get actual
	 * folder name. example: w/waterfall/fan example: w/wrestling_ring/indoor
	 */
	public String getProperFolderName(String path) {

		String[] pathParts = path.toString().replace(Constants.ade20kRootDirTest, "").split("/");

		String pathsPartName = "";
		if (pathParts.length == 4) { // pathparts has 2 layer inside that folder
			pathsPartName = pathParts[2] + "." + pathParts[1] + "." + pathParts[0];
		} else if (pathParts.length == 3) { // has indoor or outdoor name
			pathsPartName = pathParts[1] + "." + pathParts[0];
		} else if (pathParts.length == 2) {
			pathsPartName = pathParts[0];
		}

		pathsPartName = pathsPartName.trim();
		// pathsPartName =
		// CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
		// pathsPartName);
		return pathsPartName;
	}

	public void processFiles(Path path) {

		String pathsPartName = getProperFolderName(path.toString());
		logger.info("Folder/PathsPart/Concept name: " + pathsPartName);

		/*
		 * Java file walk is not synchronised. So we need to keep track of
		 * files. Which file contains which word file_browser_mapping
		 */
		HashSet<String> fileNames = new HashSet<>();
		fileNames.add(path.toString());
		if (file_browser_mapping.containsKey(pathsPartName)) {
			fileNames.addAll(file_browser_mapping.get(pathsPartName));
			file_browser_mapping.put(pathsPartName, fileNames);
		} else {
			file_browser_mapping.put(pathsPartName, fileNames);
		}

		/*
		 * How many instances each concept has. Does it mean how many text files
		 * a folder has? yes
		 */
		if (concept_indi_mapping.containsKey(pathsPartName)) {
			concept_indi_mapping.put(pathsPartName, (concept_indi_mapping.get(pathsPartName) + 1));
		} else {
			concept_indi_mapping.put(pathsPartName, 1);
		}

	}

	public void processDirs(Path path) {
		String[] pathParts = path.toString().replace(Constants.ade20kRootDirTest, "").split("/");

		String pathsPartName = "";

		List<OWLOntologyChange> changes = new ArrayList<>();
		if (pathParts.length == 1) {
			/*
			 * single folder name
			 */
			pathsPartName = pathParts[0];
		} else if (pathParts.length == 2) {
			/*
			 * has indoor or outdoor name or 1 layer example
			 * wresling_ring/indoor/
			 */
			if (pathParts[1].equalsIgnoreCase("indoor") || pathParts[1].equalsIgnoreCase("outdoor")) {
				// to check wether the concept already exist
				String concept_name = pathParts[1] + pathParts[0].replace("_", "");
				// dot version for similar concept finding
				String concept_name_with_dot = pathParts[1] + "." + pathParts[0].replace("_", "");
				// check wether the concept already exist
				OWLClass existingClass = getExistingClassIfExist(concept_name);
				OWLClass owlClass;

				if (existingClass != null) {
					owlClass = existingClass;
				} else {
					// concept not found in the existing ontology. Creating new
					// concept/class
					IRI conceptIri = IRI.create(defaultOntologyIRIPrefix, concept_name);
					owlClass = datafactory.getOWLClass(conceptIri);

					/*
					 * Need to insert it into proper class hierarchy by string
					 * matching. Don't make it subclass of OWLThing. Current
					 * implementation is performing 100% string matching to find
					 * nearest class. Need to fix it with Lu
					 */
					OWLClass nearestClass = getNearestClass(concept_name_with_dot);
					OWLAxiom classAxiom;
					if (nearestClass != null) {
						classAxiom = datafactory.getOWLSubClassOfAxiom(owlClass, nearestClass);
					} else {
						classAxiom = datafactory.getOWLSubClassOfAxiom(owlClass, datafactory.getOWLThing());
					}
					// classAxiom = addAxiomAnnotation(classAxiom);
					changes.add(new AddAxiom(ontology, classAxiom));
				}
			} else {
				// just make sure that child folder is subclass of parent folder
				// to check wether the concept already exist
				String concept_name = pathParts[0].replace("_", "");
				String parent_concept_name = pathParts[1].replace("_", "");

				// dot version for similar concept finding
				String concept_name_with_dot = pathParts[1] + "." + pathParts[0].replace("_", "");
				// check wether the concept already exist
				OWLClass existingClass = getExistingClassIfExist(concept_name);
				// existingParentClass should not be null.
				// If null happens then need to test java file travering api.
				OWLClass existingParentClass = getExistingClassIfExist(parent_concept_name);
				OWLClass owlClass;

				if (existingClass != null) {
					owlClass = existingClass;
				} else {
					// concept not found in the existing ontology. Creating new
					// concept/class
					IRI conceptIri = IRI.create(defaultOntologyIRIPrefix, concept_name);
					owlClass = datafactory.getOWLClass(conceptIri);

					/*
					 * Need to insert it into proper class hierarchy by string
					 * matching. Don't make it subclass of OWLThing. Current
					 * implementation is performing 100% string matching to find
					 * nearest class. Need to fix it with Lu
					 */
					OWLClass nearestClass;
					//nearestClass.getSuperClasses()
					// If null happens then test java file travering
					// api.
					if (existingParentClass != null) {
						nearestClass = existingParentClass;
					} else {
						logger.info("Extreme case test java file travering, bfs or dfs");
						nearestClass = getNearestClass(concept_name_with_dot);
					}
					OWLAxiom classAxiom;
					if (nearestClass != null) {
						classAxiom = datafactory.getOWLSubClassOfAxiom(owlClass, nearestClass);
					} else {
						classAxiom = datafactory.getOWLSubClassOfAxiom(owlClass, datafactory.getOWLThing());
					}
					// classAxiom = addAxiomAnnotation(classAxiom);
					changes.add(new AddAxiom(ontology, classAxiom));
				}
			}
		} else if (pathParts.length == 3) { // pathparts has 2 layer inside that
											// folder
			logger.info("Extreme case");
		}

		// write the change immediately to be able to find in next
		// concpet name search
		if (!changes.isEmpty()) {
			ontologyManager.applyChanges(changes);
			changes.clear();
		}

	}

	public void iteratedOverFiles() throws IOException {

		// walk over the directory
		Files.walk(Paths.get(Constants.ade20kRootDirTest)).filter(f -> f.toFile().isDirectory())
				.forEach(f -> processDirs(f));

		// walk over files
		// Files.walk(Paths.get(Constants.ade20kRootDirTest)).filter(f ->
		// f.toString().endsWith(".txt"))
		// .forEach(f -> processFiles(f));
		//
		// createClassIndiHierarchy();

		//
		// for (String folderName : file_browser_mapping.keySet()) {
		//
		// ArrayList<String> txtFile = new
		// ArrayList<String>(file_browser_mapping.get(folderName));
		//
		// for (int i = 0; i < txtFile.size(); i++) {
		// extractTagText(Paths.get(txtFile.get(i)), i);
		// }
		// }
		//
		// writeChanges();

	}




	public static void main(String[] args) {

		// stringTest();

		try {

			MapADEToSumo mapADEToSumo = new MapADEToSumo();

			mapADEToSumo.init();
			mapADEToSumo.loadOntology();
			// mapADEToSumo.createObjectProperty();
			// mapADEToSumo.iteratedOverFiles();
			// mapADEToSumo.saveOntology();
			mapADEToSumo.writeforConfigFiles();

		}
		// catch (OWLOntologyCreationException ex) {
		// logger.error("OWLOntologyCreationException", ex);
		// } catch (IOException ex) {
		// logger.error("IOException", ex);
		// } catch (OWLOntologyStorageException ex) {
		// logger.error("OWLOntologyStorageException", ex);
		// }
		catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}

	/*
	 * Test various aspect of string
	 */
	public static void stringTest() {

		String a = "Hell.ow";
		String pathParts = a.replace(".", "");
		logger.info("pathParts: " + pathParts);

		// String pathsPartName = "";
		// for (int i = (pathParts.length - 2); i > 0; i--) {
		// pathsPartName += (pathParts[i] + "_");
		// }
		// logger.info("pathsPartName: " + pathsPartName);
	}

}
