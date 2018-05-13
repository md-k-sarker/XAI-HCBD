package edu.wright.dase;

import edu.wright.dase.explanation.Explanation;

import java.util.HashSet;
import java.util.Set;

public class Test {

	
	Explanation expl;
	
	private void setup() {
		expl = new Explanation();
	}
	
	static Set<Integer> a = new HashSet<Integer>(); 
	static Set<Integer> b = new HashSet<Integer>(); 
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Explanation.removeNonRelatedConcepts();
		
		a.add(5);
		a.add(10);
		a.add(15);
		
		b = a;
		
		for(Integer i:b) {
			System.out.println(i);
		}
		
		b.add(20);
		System.out.println("\n");
		for(Integer i:b) {
			System.out.println(i);
		}
		System.out.println("\n");
		for(Integer i:a) {
			System.out.println(i);
		}
	}

}
