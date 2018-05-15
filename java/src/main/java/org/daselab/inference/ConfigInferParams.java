package org.daselab.inference;

import java.util.*;
import java.io.*;

public final class ConfigInferParams {

	private static String configFileName = "config.infer.properties";

	private static Properties prop;
	private static InputStream input;

	// properties needed
	public static String ontoPath;
	public static String outputOntoPath;
	public static String namespace;

	static {
		prop = new Properties();

		input = ConfigInferParams.class.getClassLoader().getResourceAsStream(configFileName);
		if (input == null) {
			System.out.print("Error reading config file");
			System.exit(-1);
		}
		try {
			prop.load(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("Printing config properties: ");
		// print proeprty values
		prop.forEach((k, v) -> {
			System.out.println(k + ": " + v);
		});

		ontoPath = prop.getProperty("path.inputOntology");
		outputOntoPath = prop.getProperty("path.outputOntology");
		namespace = prop.getProperty("namespace");

	}

	// private constructor, no instantiation
	private ConfigInferParams() {

	}

}
