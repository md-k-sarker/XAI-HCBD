package edu.wright.dase.util;

import edu.wright.dase.datastructure.Trees;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.HashMap;
import java.util.HashSet;

public class SharedDataHolder {

    public static HashMap<String, OWLClassExpression> atomicCLasses = new HashMap<>();

    /**
     * allAtomicClassesToConsider is the all named/atomic classes to consider.
     */
    public static HashSet<OWLClassExpression> allAtomicClassesToConsider = new HashSet<>();

    public static HashSet<OWLObjectProperty> objectPropertiesToConsider = new HashSet<>();

    public static HashMap<OWLClass, OWLClassExpression> conjunctedClasses = new HashMap<>();

    public static HashMap<OWLClass, OWLClassExpression> rFillerClass = new HashMap<>();

    public static HashMap<OWLClass, OWLClassExpression> conjunctedClassesAfterRFiller = new HashMap<>();

    /* All classes without the atomic classes. */
    public static HashMap<OWLClass, OWLClassExpression> allCreatedClasses = new HashMap<>();

    public static HashMap<OWLClassExpression,HashMap<String,Double>> solutionClasses = new HashMap<>();

    public static long complexClassCounter = 0;

    public static long totalClassesInOntology = 0;

    public static HashSet<OWLClass> disjunctedClasses = new HashSet<>();

    public static HashSet<OWLClass> negatedClasses = new HashSet<>();

    public static OWLClass owlThing = new OWLManager().get().getOWLDataFactory().getOWLThing();
    public static OWLClass owlNothing = new OWLManager().get().getOWLDataFactory().getOWLNothing();

    public static HashSet<OWLNamedIndividual> posIndivs = new HashSet<>();
    public static HashSet<OWLNamedIndividual> negIndivs = new HashSet<>();

    public static HashSet<OWLNamedIndividual> objectsInPosIndivs = new HashSet<>();
    public static HashSet<OWLNamedIndividual> objectsInNegIndivs = new HashSet<>();

    public static OWLOntology owlOntology;
    public static OWLOntologyManager owlOntologyManager;
    public static OWLDataFactory owlDataFactory;
    public static OWLReasoner owlReasoner;

    //public static OWLConceptHierarchy owlConceptHierarchy;
    public static Trees<OWLClassExpression> owlClassExpressionTrees;

}
