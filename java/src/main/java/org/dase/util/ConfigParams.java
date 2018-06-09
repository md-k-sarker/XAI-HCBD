package org.dase.util;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

public final class ConfigParams {

    private static String configFileName = "config.properties";
    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Properties prop;
    private static InputStream input;

    // properties needed
    public static String confFilePath;
    public static String ontoPath;
    public static String outputResultPath;
    public static String posIndiPath;
    public static String negIndiPath;
    public static String namespace;
    public static Double tolerance;
    public static int combinationK1;
    public static double combinationThreshold;
    public static boolean batch;
    public static String batchOutputResult;
    public static String batchConfFilePath;

    // used in createontologyfromade20k
    public static final String ontologyIRI = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu/";

    static {
        prop = new Properties();

        input = ConfigParams.class.getClassLoader().getResourceAsStream(configFileName);
        if (input == null) {
            System.out.print("Error reading config file");
            System.exit(-1);
        }
        try {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Printing config file before parsing: ");
        // print property values
        prop.forEach((k, v) -> {
            logger.info(k + ": " + v);
        });

        batch = Boolean.valueOf(prop.getProperty("batch"));
        if(batch){
            batchConfFilePath=prop.getProperty("path.batchConfFilePath");
            batchOutputResult=prop.getProperty("path.batchOutputResult");
            // ontoPath will be determined at runtime
        }else{
            confFilePath = prop.getProperty("path.confFilePath");
            ontoPath = Utility.readOntologyPathConf(confFilePath);
            String[] inpPaths = confFilePath.split(File.separator);
            String name = inpPaths[inpPaths.length - 1].replace(".conf", "_expl_by_ecii.txt");
            outputResultPath = prop.getProperty("path.outputResult") + "" + name;
        }

        posIndiPath = prop.getProperty("path.posImages");
        negIndiPath = prop.getProperty("path.negImages");
        namespace = prop.getProperty("namespace");
        tolerance = Double.valueOf(prop.getProperty("tolerance"));
        combinationK1 = Integer.valueOf(prop.getProperty("combinationK1"));
        combinationThreshold = Double.valueOf(prop.getProperty("combinationThreshold"));

        logger.info("Config properties: ");
        printConfigProperties();
    }

    private static void printConfigProperties(){
        logger.info("\tconfFilePath: "+ confFilePath);
        logger.info("\tontoPath: "+ ontoPath);
        logger.info("\toutputResultPath: "+ outputResultPath);
        logger.info("\tnamespace: "+ namespace);
        logger.info("\ttolerance: "+ tolerance);
        logger.info("\tcombinationK1: "+ combinationK1);
        logger.info("\tcombinationThreshold: "+ combinationThreshold);

    }

    // private constructor, no instantiation
    private ConfigParams() {

    }

}
