package edu.wright.dase.util;

import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Properties;

public final class ConfigParams {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static String configFileName = "config.properties";

    private static Properties prop;
    private static InputStream input;

    // properties needed
    public static String confFilePath;
    public static String ontoPath;
    public static String outputOntPath;
    // this logPath is different from slf4j log file.
    public static String logPath;
    public static String dllearnerResultPath;
    public static String miniDlLearnerResultPath;
    public static String eciiResultesultPath;
    //public static String posIndiPath;
    //public static String negIndiPath;
    public static String namespace;
    public static String newClassNamePrefix;
    public static Long timeOut;
    public static Double tolerance;
    public static boolean debug;
    public static Long K1;
    public static Long K2;
    public static Long K3;
    public static Long K4;
    public static HashSet<OWLNamedIndividual> posIndivs;
    public static HashSet<OWLNamedIndividual> negIndivs;

    /**
     * Application must call init at first
     */
    public static void init() {
        try {
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

            logger.info("\nPrinting config properties: ");
            // print proeprty values
            prop.forEach((k, v) -> {
                logger.info(k + ": " + v);
            });

            confFilePath = prop.getProperty("path.confFilePath");
            ontoPath = prop.getProperty("path.inputOntology");
            outputOntPath = prop.getProperty("path.outputOntology");

            String[] inpPaths = confFilePath.split(File.separator);
            String name = inpPaths[inpPaths.length - 1].replace(".conf", ".txt");
            if(!name.endsWith(".txt")){
                name = name+".txt";
            }
            logPath = prop.getProperty("path.outputLogPath") + "monitor_" + name;
            dllearnerResultPath = prop.getProperty("path.confFilePath") + "__result_dl_" + name;
            miniDlLearnerResultPath = prop.getProperty("path.confFilePath") + "__result_minidl_" + name;
            eciiResultesultPath = prop.get("path.confFilePath") + "__result_ecii_" + name;

            //            posIndiPath = prop.getProperty("path.posImages");
            //            negIndiPath = prop.getProperty("path.negImages");

            // should be changed later according to input ontology namespace
            namespace = prop.getProperty("namespace");
            newClassNamePrefix = prop.getProperty("newClassNamePrefix");
            timeOut = Long.valueOf(prop.getProperty("timeOut"));
            tolerance = Double.valueOf(prop.getProperty("tolerance"));
            debug = Boolean.parseBoolean(prop.getProperty("debug"));

            K1 = Long.parseLong(prop.getProperty("K1"));
            K2 = Long.parseLong(prop.getProperty("K2"));
            K3 = Long.parseLong(prop.getProperty("K3"));
            K4 = Long.parseLong(prop.getProperty("K4"));

        } catch (Exception ex) {
            logger.error("Error reading config file." + "\n" + Utility.getStackTraceAsString(ex));
            logger.info("exiting application");
            System.exit(1);
        }
    }


    // private constructor, no instantiation
    private ConfigParams() {

    }

}
