package org.dase.datastructure;

import java.util.NoSuchElementException;

/**
 * Doubly linkedList
 * 
 * @author sarker
 *
 * @param <E>
 */
public class QueueAsLinkedList<E> implements DynamicSet<E> {

	// need a head pointer to point the queue elements
	private Node<E> first;
	private Node<E> last;
	private int size;

	public QueueAsLinkedList() {
		size = 0;
	}

	@Override
	public void push(E data) {
		final Node<E> newNode = new Node<>(null, data, null);
		addToLast(newNode);
	}

	/**
	 * 
	 */
	private void addToLast(final Node<E> newNode) {
		if (first == null && last == null) {
			// currently the list is empty
			first = newNode;
			last = newNode;
		} else {
			last.next = newNode;
			newNode.previous = last;
			last = newNode;
		}
		size++;
	}

	/**
	 * 
	 */
	private Node<E> deleteFromFirst() {
		if (first == null && last == null)
			throw new NoSuchElementException();

		Node<E> d = first;
		if (first != null && first.next == null) {
			// if 1 items
			// delete the single item
			first = null;
			last = null;
		} else if (first != null && first.next != null) {
			// if more than 1 items

			first = first.next;
		}
		size--;
		return d;
	}

	/**
	 * pick and delete
	 */
	@Override
	public E pop() {
		Node<E> d = deleteFromFirst();
		return d.data;
	}

	/**
	 * Only pick, no delete
	 */
	@Override
	public E peek() {
		if (first == null)
			throw new NoSuchElementException();
		return first.data;
	}

	@Override
	public int searchIndex(E item) {

		return searchIndex(item, true);
	}

	public int searchIndex(E item, boolean fromBegining) {
		int index = -1;
		if (item == null) {
			if (fromBegining) {
				index = 0;
				for (Node<E> x = first; x != null; x = x.next) {
					if (x.data == null)
						return index;
					index++;
				}
			} else {
				index = size - 1;
				for (Node<E> x = last; x != null; x = x.previous) {
					if (x.data == null)
						return index;
					index--;
				}
			}
		} else {
			if (fromBegining) {
				index = 0;
				for (Node<E> x = first; x != null; x = x.next) {
					if (item.equals(x.data))
						return index;
					index++;
				}
			} else {
				index = size - 1;
				for (Node<E> x = last; x != null; x = x.previous) {
					if (item.equals(x.data))
						return index;
					index--;
				}
			}
		}
		if (index == size)
			return -1;

		return index;
	}

	/**
	 * return true if empty
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return size == 0 ? true : false;
	}

	/**
	 * Internal class to represent a node in the linked list
	 * 
	 * @author sarker
	 *
	 */
	private static class Node<E> {
		// forward pointer and backward pointer
		Node<E> next;
		Node<E> previous;
		E data;

		public Node(Node<E> prev, E data, Node<E> next) {
			this.data = data;
			this.next = next;
			this.previous = prev;
		}
	}

	public static void main(String[] args) {
		Integer[] ob = new Integer[2];
		ob[0] = 1;
		ob[1] = null;

		for (Object o : ob) {
			System.out.println("Object: " + o);
		}
	}

}
