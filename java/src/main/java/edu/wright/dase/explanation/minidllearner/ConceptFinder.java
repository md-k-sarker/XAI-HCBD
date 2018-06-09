package edu.wright.dase.explanation.minidllearner;


import org.dase.util.Monitor;
import org.dase.util.SharedDataHolder;
import org.dase.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("Duplicates")
public class ConceptFinder {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public final OWLOntology ontology;
    public final OWLReasoner reasoner;
    public final PrintStream out;
    public final Monitor monitor;
    // private final static String ontoPath = "data/TestOntoTest.owl";
    // private final static String ontoPath2 =
    // "data/sumo_aligned_with_bathroom_livingroom_3_instances_rnr_manual.owl";

//    public ConceptFinder(){
//        this(null,null,null,null);
//    }

    /**
     * Constructor
     */
    public ConceptFinder(PrintStream _printStream, Monitor _monitor) {
        this.reasoner = SharedDataHolder.owlReasoner;
        this.ontology = SharedDataHolder.owlOntology;
        this.out = _printStream;
        this.monitor = _monitor;
    }


    /**
     * @param posIndivs
     * @param negIndivs
     * @param tolerance
     * @param imgContains
     * @formatter:off findconcepts method find the concepts of the individuals which is the r filler of individuals appeared in the images.
     * For example:
     * image1 contains indiv1, indivs2.
     * indivs1 type claz1.
     * indivs2 type claz2.
     * and
     * image1 is positive instance.
     * then it will come up with the result:
     * claz1 and claz2.
     * <p>
     * TODO: Things to consider:
     * 1. Image may have very different type of objects recognized.
     * Need to find the optimal set of objects.
     * 2. No need to find upper classes of the objects if 100% coverage if found.
     * 3. Currently not finding the common super classes of the types.
     * Need to find least common super class of the individuals.
     * For example:
     * image1 is positive instance.
     * image1 contains indiv1, indivs2.
     * indivs1 type claz1.
     * indivs2 type claz2.
     * claz1 subClassOf claz3
     * claz2 subClassOf claz3
     * claz3 subClassOf claz4
     * <p>
     * then it must come up with the result:
     * claz3. as claz3 is the least common superclass of the individuals.
     * <p>
     * // probably I should have a tree structure by myself.
     * @formatter:on
     */
    public void findConcepts(Set<OWLNamedIndividual> posIndivs, Set<OWLNamedIndividual> negIndivs, double tolerance,
                             OWLObjectProperty imgContains) {

        logger.info("Given obj property: " + imgContains.toString());

        HashSet<OWLClass> positiveTypes = new HashSet<>();
        HashSet<OWLClass> negativeTypes = new HashSet<>();


        HashSet<OWLNamedIndividual> indivsAppearedInPosImages = new HashSet<OWLNamedIndividual>();

        HashSet<OWLNamedIndividual> indivsAppearedInNegImages = new HashSet<OWLNamedIndividual>();

        // find the corresponding objects/indivs which appeared in the positive images
        for (OWLNamedIndividual posIndiv : posIndivs) {
            logger.info("Individual " + Utility.getShortName(posIndiv) + " has below objects in his image: ");
            logger.info("rootOntology of the reasoner: " + reasoner.getRootOntology().getOntologyID().toString());
            reasoner.getObjectPropertyValues(posIndiv, imgContains);
//            .getFlattened().forEach(eachIndi -> {
//                indivsAppearedInPosImages.add(eachIndi);
//                logger.info("\tObject: " + Utility.getShortName(eachIndi));
//            });
        }

        // find the corresponding indivs which appeared in the negative images
        for (OWLNamedIndividual negIndiv : negIndivs) {
            logger.info("Individual " + Utility.getShortName(negIndiv) + " has below objects in his image: ");
            reasoner.getObjectPropertyValues(negIndiv, imgContains).getFlattened().forEach(eachIndi -> {
                indivsAppearedInNegImages.add(eachIndi);
                logger.info("\tObject: " + Utility.getShortName(eachIndi));
            });
        }

        /**
         *
         * Optimization thinking:
         */

        // find the type of the indivs which appeard in the images
        // reasoner.instances(ce, direct)
        // reasoner.getInstances(ce, direct)
        // reasoner.getTypes(ind, direct)
        // reasoner.types(ind, direct)
        for (OWLNamedIndividual indi : indivsAppearedInPosImages) {
            // use tolerance measure
            // need to exclude indivs which appear in neg images
            // if(! indivsAppearedInNegImages.contains(indi))
            reasoner.getTypes(indi, true).getFlattened().forEach(eachType -> {
                positiveTypes.add(eachType);
            });
        }

        for (OWLNamedIndividual indi : indivsAppearedInNegImages) {
            reasoner.getTypes(indi, true).getFlattened().forEach(eachType -> {
                negativeTypes.add(eachType);
            });
        }

        // print the output
        logger.info("\nMatched classes are:  ");
        for (OWLClass claz : positiveTypes) {
            logger.info("\t" + Utility.getShortName(claz));
        }

    }

    /**
     * Test the functionality
     *
     * @param args
     * @throws OWLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("javadoc")
    public static void main(String[] args)
            throws OWLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

    }
}
