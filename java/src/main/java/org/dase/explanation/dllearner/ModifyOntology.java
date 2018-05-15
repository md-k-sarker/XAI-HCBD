package org.dase.explanation.dllearner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dase.util.Utility;
import org.dllearner.core.ComponentInitException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormatImpl;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;


public class ModifyOntology {

    private static final Logger logger = LoggerFactory.getLogger(ModifyOntology.class);

    protected OWLOntology ontology;
    protected OWLDataFactory datafactory;
    protected OWLOntologyManager ontologyManager;
    protected File ontoOrgnFile;
    protected File ontoModFile;
    protected String defaultOntologyIRIPrefix;
    protected List<OWLOntologyChange> ontologyChanges;

    String ontoOrgnlFileName = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo/SUMO.owl";
    String ontoModFileName = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo/sumo_without_indi.owl";

    /**
     * @return the ontologyChanges
     */
    public List<OWLOntologyChange> getOntologyChanges() {
        return ontologyChanges;
    }

    /**
     * @param ontologyChanges the ontologyChanges to set
     */
    public void setOntologyChanges(List<OWLOntologyChange> ontologyChanges) {
        this.ontologyChanges = ontologyChanges;
    }

    public String getDefaultOntologyIRIPrefix() {
        return defaultOntologyIRIPrefix;
    }

    public void setDefaultOntologyIRIPrefix(String defaultOntologyIRI) {
        this.defaultOntologyIRIPrefix = defaultOntologyIRI;
    }

    /**
     * @return the ontology
     */
    public OWLOntology getOntology() {
        return ontology;
    }

    /**
     * @param ontology the ontology to set
     */
    public void setOntology(OWLOntology ontology) {
        this.ontology = ontology;
    }

    /**
     * @return the ontoOrgnFile
     */
    public File getOntoOrgnFile() {
        return ontoOrgnFile;
    }

    /**
     * @param ontoOrgnFile the ontoOrgnFile to set
     */
    public void setOntoOrgnFile(File ontoOrgnFile) {
        this.ontoOrgnFile = ontoOrgnFile;
    }

    /**
     * @return the ontoModFile
     */
    public File getOntoModFile() {
        return ontoModFile;
    }

    /**
     * @param ontoModFile the ontoModFile to set
     */
    public void setOntoModFile(File ontoModFile) {
        this.ontoModFile = ontoModFile;
    }

    /**
     * @return the datafactory
     */
    public OWLDataFactory getDatafactory() {
        return datafactory;
    }

    /**
     * @param datafactory the datafactory to set
     */
    public void setDatafactory(OWLDataFactory datafactory) {
        this.datafactory = datafactory;
    }

    /**
     * @return the ontologyManager
     */
    public OWLOntologyManager getOntologyManager() {
        return ontologyManager;
    }

    /**
     * @param ontologyManager the ontologyManager to set
     */
    public void setOntologyManager(OWLOntologyManager ontologyManager) {
        this.ontologyManager = ontologyManager;
    }

    /*
     * Constructor
     */
    public ModifyOntology() {

    }

    public void init() {
        // IRI ontoOrgnFileIRI = IRI.create(Constants.ontoOrgnlFileName);
        ontoOrgnFile = new File(ontoOrgnlFileName);

        ontoModFile = new File(ontoModFileName);
        datafactory = OWLManager.getOWLDataFactory();
        ontologyManager = OWLManager.createOWLOntologyManager();

        ontologyChanges = new ArrayList<>();
    }

    /*
     * remove annotation axioms
     */
    public void removeAnnot() throws OWLOntologyStorageException {

        Set<OWLAxiom> axiomsToRemove = new HashSet<OWLAxiom>();

        for (OWLAxiom ax : ontology.getAxioms(Imports.INCLUDED)) {

            if (AxiomType.ANNOTATION_ASSERTION == ax.getAxiomType()) {
                axiomsToRemove.add(ax);
            }
        }

        ChangeApplied ca = ontologyManager.removeAxioms(ontology, axiomsToRemove);

        // ontologyManager.saveOntology(ontology, IRI.create(ontoModFile));
    }

    /**
     * Remove the existing individuals from sumo ontology
     */
    public void removeIndividuals() {
        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(ontology));
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature(Imports.INCLUDED)) {
            if (!individual.getIRI().getShortForm().contains("_Indi_")) {
                entityRemover.visit(individual);
            }
        }
        ontologyManager.applyChanges(entityRemover.getChanges());
    }

    public void attachIndividual() throws OWLOntologyStorageException {

        logger.info("started attachIndividual()");
        for (OWLClass olwClass : ontology.getClassesInSignature(true)) {

            for (int i = 0; i < 3; i++) {
                String str = olwClass.getIRI().getRemainder().get().toString();
                IRI iri = IRI.create(defaultOntologyIRIPrefix, str + "_indi_" + i);

                OWLIndividual indiv = datafactory.getOWLNamedIndividual(iri);
                OWLClassAssertionAxiom ax = datafactory.getOWLClassAssertionAxiom(olwClass, indiv);
                ChangeApplied ca = ontologyManager.addAxiom(ontology, ax);
            }
        }

        logger.info("attachIndividual() finished");
    }

    public void saveOntology() throws OWLOntologyStorageException {

        this.saveOntology(this.ontology, this.ontoModFile);
    }

    protected void saveOntology(OWLOntology ontology, File modifiedFileName) throws OWLOntologyStorageException {
        ontologyManager.saveOntology(ontology, IRI.create(ontoModFile));
    }

    public void loadOntology() throws OWLOntologyCreationException {

        ontology = ontologyManager.loadOntologyFromOntologyDocument(ontoOrgnFile);

        defaultOntologyIRIPrefix = Utility.getOntologyPrefix(ontology);

    }

    public void renameIRIs() {
        logger.info("Renaming started");
        Set<OWLOntology> ontologySet = new HashSet<OWLOntology>();
        ontologySet.add(ontology);
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ontologyManager, ontologySet);

        // rename owlClasses
        for (OWLClass owlClass : ontology.getClassesInSignature(Imports.INCLUDED)) {
            IRI oldIRI = owlClass.getIRI();
            String shortName = owlClass.getIRI().getRemainder().get();
            IRI newIRI = IRI.create(defaultOntologyIRIPrefix, shortName);
            ontologyChanges.addAll(entityRenamer.changeIRI(oldIRI, newIRI));
        }

        // rename object properties
        for (OWLObjectProperty objectProperty : ontology.getObjectPropertiesInSignature(Imports.INCLUDED)) {
            IRI oldIRI = objectProperty.getIRI();
            String shortName = objectProperty.getIRI().getRemainder().get();
            IRI newIRI = IRI.create(defaultOntologyIRIPrefix, shortName);
            ontologyChanges.addAll(entityRenamer.changeIRI(oldIRI, newIRI));
        }

        // rename individuals
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            IRI oldIRI = individual.getIRI();
            String shortName = individual.getIRI().getRemainder().get();
            IRI newIRI = IRI.create(defaultOntologyIRIPrefix, shortName);
            ontologyChanges.addAll(entityRenamer.changeIRI(oldIRI, newIRI));
        }

        // rename annotation properties
        for (OWLAnnotationProperty annotProperty : ontology.getAnnotationPropertiesInSignature()) {
            IRI oldIRI = annotProperty.getIRI();
            String shortName = annotProperty.getIRI().getRemainder().get();
            IRI newIRI = IRI.create(defaultOntologyIRIPrefix, shortName);
            ontologyChanges.addAll(entityRenamer.changeIRI(oldIRI, newIRI));
        }

        logger.info("Renaming finished");
    }

    protected void writeChanges() {

        if (!ontologyChanges.isEmpty()) {
            ChangeApplied ca = ontologyManager.applyChanges(ontologyChanges);
            logger.info("ChangeApplied: " + ca.toString());
            ontologyChanges.clear();
        }
    }

    public static PrefixDocumentFormat getPrefixOWLOntologyFormat(OWLOntology ontology) {
        PrefixDocumentFormat prefixManager = null;
        if (ontology != null) {
            OWLOntologyManager manager = ontology.getOWLOntologyManager();
            OWLDocumentFormat format = manager.getOntologyFormat(ontology);
            if (format != null && format.isPrefixOWLOntologyFormat()) {
                prefixManager = format.asPrefixOWLOntologyFormat();
            }
        }
        if (prefixManager == null) {
            prefixManager = new PrefixDocumentFormatImpl();
        }
        return prefixManager;
    }

    // String positiveIndividualInitial = "OutdoorMuseum_indi";
    String positiveIndividualInitial = "OutdoorWareHouse_Indi_";

//	public void writeforConfigFiles() {
//		StringBuilder sbuilderPositive = new StringBuilder();
//		StringBuilder sbuilderNegative = new StringBuilder();
//		StringBuilder sbuilderPrefixes = new StringBuilder();
//		sbuilderPositive.append("{");
//		sbuilderNegative.append("{");
//		sbuilderPrefixes.append("[");
//
//		boolean firstofPos = true;
//		boolean firstofNeg = true;
//		for (OWLNamedIndividual indiv : ontology.getIndividualsInSignature()) {
//			String name = indiv.getIRI().getRemainder().get();
//
//			if (name.startsWith(positiveIndividualInitial)) {
//				if (firstofPos) {
//					firstofPos = false;
//					sbuilderPositive.append("\"ex:" + name + "\"");
//				} else {
//					sbuilderPositive.append(", " + "\"ex:" + name + "\"");
//				}
//			} else {
//				if (firstofNeg) {
//					firstofNeg = false;
//					sbuilderNegative.append("\"ex:" + name + "\"");
//				} else {
//					sbuilderNegative.append(", " + "\"ex:" + name + "\"");
//				}
//			}
//		}
//
//		// write prefixes
//		boolean firstofPrefix = true;
//		PrefixDocumentFormat prefixManager = getPrefixOWLOntologyFormat(ontology);
//		Map<String, String> prefixNameMapping = prefixManager.getPrefixName2PrefixMap();
//
//		for (String prefix : prefixNameMapping.keySet()) {
//			if (firstofPrefix) {
//				firstofPrefix = false;
//				sbuilderPrefixes.append("(\"ex\"," + "\"" + prefixNameMapping.get(prefix) + "\")");
//			} else {
//				sbuilderPrefixes.append(", (\"ex\"," + "\"" + prefixNameMapping.get(prefix) + "\")");
//			}
//		}
//
//		sbuilderPositive.append("}");
//		sbuilderNegative.append("}");
//		sbuilderPrefixes.append("]");
//
//		try {
//			// write positive
//			PrintWriter writer = new PrintWriter(Constants.posExamplesFilePath);
//			writer.append(sbuilderPositive);
//			writer.close();
//
//			// write negative
//			writer = new PrintWriter(Constants.negExamplesFilePath);
//			writer.append(sbuilderNegative);
//			writer.close();
//
//			// write prefixes
//			writer = new PrintWriter(Constants.prefixFilePath);
//			writer.append(sbuilderPrefixes);
//			writer.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}


    public static void main(String[] args)
            throws IOException, ComponentInitException, OWLOntologyCreationException, OWLOntologyStorageException {

        logger.info("starting...");

        ModifyOntology mdfOntology = new ModifyOntology();

        mdfOntology.init();
        mdfOntology.loadOntology();

        mdfOntology.removeAnnot();

        mdfOntology.removeIndividuals();

        //mdfOntology.attachIndividual();

        //mdfOntology.renameIRIs();
        mdfOntology.writeChanges();
        mdfOntology.saveOntology();

        //mdfOntology.writeforConfigFiles();

        logger.info("finished");
    }

}
