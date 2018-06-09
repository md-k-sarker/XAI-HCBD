package org.dase.datastructure;

public interface DynamicSet<E> {
	
	/**
	 * 
	 * @param data
	 */
	public void push(E data);

	/**
	 * peek and delete
	 */
	public E pop();
	
	/**
	 * Only peek, don't delete
	 */
	public E peek();
	
	/**
	 *  If not found will return null
	 * @param item
	 * @return
	 */
	public int searchIndex(E item);

}
