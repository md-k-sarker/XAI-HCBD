/**
 * 
 */
package edu.wright.dase.datastructure;

import java.util.ArrayList;

/**
 * @author sarker
 *
 */
public interface TreeInterface<E> {
	
	public void addChild(TreesNode<E> parent, E data);
	
	public boolean deleteNode(TreesNode<E> node);
	
	public ArrayList<E> traverseData(TraverseDirection direction);
	
	public ArrayList<TreesNode<E>> traverseNode(TraverseDirection direction);
	
	public TreesNode<E> searchNode(E data, SearchTechnique technique);
	
	public enum TraverseDirection{
		IN_ORDER,PRE_ORDER,POST_ORDER;
	}
	
	public enum SearchTechnique{
		DFS,BFS;
	}
}
