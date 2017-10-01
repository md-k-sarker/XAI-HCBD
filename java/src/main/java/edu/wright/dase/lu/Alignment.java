package edu.wright.dase.lu;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.wcohen.ss.Levenstein;

public class Alignment {
	public static void main(String[] args) throws Exception {
		alignment();
	}

	public static void alignment() throws Exception {
		Set<OWLClass> aClasses = new HashSet<>();
		Set<OWLClass> bClasses = new HashSet<>();
		Set<OWLClass> cClasses = new HashSet<>();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		File fileA = new File("./data/hcbdwsu.owl");
		OWLOntology ontA = manager.loadOntologyFromOntologyDocument(fileA);

		System.out.println("Loaded ontology: " + ontA);

		File fileB = new File("./data/SUMO.owl");
		OWLOntology ontB = manager.loadOntologyFromOntologyDocument(fileB);

		System.out.println("Loaded ontology: " + ontB);

		File fileC = new File("./data/WordNet.owl");
		OWLOntology ontC = manager.loadOntologyFromOntologyDocument(fileC);

		System.out.println("Loaded ontology: " + ontC);

		aClasses = ontA.getClassesInSignature();
		bClasses = ontB.getClassesInSignature();
		cClasses = ontC.getClassesInSignature();

		System.out.println(aClasses.size());
		System.out.println(bClasses.size());
		System.out.println(cClasses.size());

		// Double threshold = 0.86;
		// int i = 0;
		// for (OWLClass c: aClasses) {
		// String labelA = getString(c);
		// labelA = stringTokenize(labelA, false);
		//
		// for (OWLClass c1: bClasses) {
		// String labelB = getString(c1);
		// labelB = stringTokenize(labelB, false);
		//
		// double confidence = computeConfidence(labelA, labelB);
		//
		// if (confidence >= threshold){
		// i++;
		// System.out.println(labelA + " - " + labelB + " = " + confidence);
		// }
		// }
		// }
		// System.out.println(i);
	}

	public static String getString(OWLClass e) {

		String label = e.getIRI().toString();

		if (label.contains("#")) {
			label = label.substring(label.indexOf('#') + 1);
			return label;
		}

		if (label.contains("/")) {
			label = label.substring(label.lastIndexOf('/') + 1);
			return label;
		}

		return label;
	}

	public static String stringTokenize(String s, boolean lowercase) throws Exception {
		String result = "";

		ArrayList<String> tokens = tokenize(s, lowercase);

		for (String token : tokens) {

			result += token + " ";
		}

		return result.trim();
	}

	public static ArrayList<String> tokenize(String s, boolean lowercase) {
		if (s == null) {
			return null;
		}

		ArrayList<String> strings = new ArrayList<String>();

		String current = "";
		Character prevC = 'x';

		for (Character c : s.toCharArray()) {
			if ((Character.isLowerCase(prevC) && Character.isUpperCase(c)) ||
			// c == '_' || c == '-' || c == ' ' || c == '/' || c == '\\' || c ==
			// '>' || c == '\n' || c == '\'') {
					c == '_' || c == '-' || c == ',' || c == ';' || c == ':' || c == '(' || c == ')' || c == '?'
					|| c == '!' || c == '.' || c == ' ' || c == '<' || c == '>') {
				current = current.trim();

				if (current.length() > 0) {
					if (lowercase)
						strings.add(current);
					else
						strings.add(current.toLowerCase());
				}

				current = "";
			}

			// if (c != '_' && c != '-' && c != '/' && c != '\\' && c != '>' &&
			// c != '\'') {
			if (c != '_' && c != '-' && c != ',' && c != ';' && c != ':' && c != '(' && c != ')' && c != '?' && c != '!'
					&& c != '.' && c != ' ' && c != '<' && c != '>') {
				current += c;
				prevC = c;
			}
		}

		current = current.trim();

		if (current.length() > 0) {
			// this check is to handle the id numbers in YAGO
			if (!(current.length() > 4 && Character.isDigit(current.charAt(0))
					&& Character.isDigit(current.charAt(current.length() - 1)))) {
				strings.add(current.toLowerCase());
			}
		}

		return strings;
	}

	public static double computeConfidence(String labelA, String labelB) {
		double confidenceValue = 1 - (Math.abs(new Levenstein().score(labelA, labelB))
				/ (double) Math.max(labelA.length(), labelB.length()));

		return confidenceValue;
	}

	public static void doAlign() {
//		 FileOutputStream os1 = new
//		 FileOutputStream("./logging/anatomy_logging.txt");
//		 PrintWriter output = new PrintWriter(new
//		 OutputStreamWriter(os1,"UTF-8"), true);
//		
//		 ArticleAlignment aligner = new ArticleAlignment("anatomy");
//		
//		 File f1 = new File("./data/anatomy/mouse.owl");
//		 File f2 = new File("./data/anatomy/human.owl");
//		
//		 String s1 = f1.toString();
//		 String s2 = f2.toString();
//		 String name1 = s1.substring(s1.lastIndexOf("/") +
//		 1,s1.lastIndexOf("."));
//		 String name2 = s2.substring(s2.lastIndexOf("/") +
//		 1,s2.lastIndexOf("."));
//		
//		 System.out.println("\tAligning " + name1 + " and " + name2);
//		
//		 aligner.articleAlign(f1.toURI(), f2.toURI(), name1, name2, output);
//		
//		 output.close();
	}
}
