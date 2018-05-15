package org.dase.explanation.dllearner;


import com.google.common.base.Optional;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Took it from protege merger in github 
 */
public class OntologyMerger {

    private static final Logger logger = LoggerFactory.getLogger(OntologyMerger.class);

    private OWLOntologyManager owlOntologyManager;

    private Set<OWLOntology> ontologies;

    private OWLOntology targetOntology;


    public OntologyMerger(OWLOntologyManager owlOntologyManager, Set<OWLOntology> ontologies, OWLOntology targetOntology) {
        this.ontologies = new HashSet<>(ontologies);
        this.owlOntologyManager = owlOntologyManager;
        this.targetOntology = targetOntology;
    }


    public void mergeOntologies() {
        List<OWLOntologyChange> changes = new ArrayList<>();
        for (OWLOntology ont : ontologies) {
            if (!ont.equals(targetOntology)){

                // move the axioms
                for (OWLAxiom ax : ont.getAxioms()) {
                    changes.add(new AddAxiom(targetOntology, ax));
                }

                // move ontology annotations
                for (OWLAnnotation annot : ont.getAnnotations()){
                    changes.add(new AddOntologyAnnotation(targetOntology, annot));
                }

                if (!targetOntology.getOntologyID().isAnonymous()){
                    // move ontology imports
                    for (OWLImportsDeclaration decl : ont.getImportsDeclarations()){
                    	if (ontologies.contains(ont.getOWLOntologyManager().getImportedOntology(decl))) {
                    		continue;
                    	}
                        Optional<IRI> defaultDocumentIRI = targetOntology.getOntologyID().getDefaultDocumentIRI();
                        if (defaultDocumentIRI.isPresent() && !decl.getIRI().equals(defaultDocumentIRI.get())){
                            changes.add(new AddImport(targetOntology, decl));
                        }
                        else{
                            logger.warn("Merge: ignoring import declaration for ontology " + targetOntology.getOntologyID() +
                                        " (would result in target ontology importing itself).");
                        }
                    }
                }
            }
        }
        try {
            owlOntologyManager.applyChanges(changes);
        }
        catch (OWLOntologyChangeException e) {
            // ToDo
        }
    }
}