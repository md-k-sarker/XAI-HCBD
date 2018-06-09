package edu.wright.dase.datastructure;

import java.util.ArrayList;

public class Trees<E> implements TreeInterface<E> {

	private TreesNode<E> root;
	private TreesNode<ArrayList<E>> leafs;

	// no. of nodes in the tree
	int size;

	/**
	 * Constructor
	 */
	private Trees() {
		// can not allow a tree without a root
	}

	public Trees(E rootData) {
		if (rootData == null)
			throw new NullPointerException();
		TreesNode<E> newNode = new TreesNode<E>(rootData);
		this.root = newNode;
		size = 1;

	}

	/**
	 * Constructor
	 * 
	 * @param rootNode
	 */
	public Trees(TreesNode<E> rootNode) {
		if (rootNode == null)
			throw new NullPointerException();
		this.root = rootNode;
		size = 1;
	}


    public void addToDefaultRoot(E data) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (this.root == null) {
            throw new NullPointerException();
        }

        TreesNode<E> newNode = new TreesNode<>(data);

        addToDefaultRoot(newNode);

    }


    /**
	 * Add node to default root of the tree
	 * 
	 * @param node
	 */
	public void addToDefaultRoot(TreesNode<E> node) {
		if (node == null) {
			throw new NullPointerException();
		}
		if (this.root == null) {
			throw new NullPointerException();
		}
		node.parent = this.root;

		if (this.root.childrens != null) {
			this.root.childrens.add(node);
			size++;
		} else {
			this.root.childrens = new ArrayList<>();
			this.root.childrens.add(node);
			size++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see wright.dase.ds.TreeInterface#addChild(wright.dase.ds.TreesNode,
	 * java.lang.Object)
	 */
	@Override
	public void addChild(TreesNode<E> parent, E data) {
		TreesNode<E> newNode = new TreesNode<E>(parent, data, null);

		addChild(parent, newNode);
	}

	public void addChild(TreesNode<E> parent, TreesNode<E> node) {
		if (node == null) {
			throw new NullPointerException();
		}
		if (parent == null) {
			throw new NullPointerException();
		}

		// if parent does not exist in the graph ?? how to check it?

		// if parent exists in the graph
		node.parent = parent;
		if (parent.childrens != null) {
			parent.childrens.add(node);
		} else {
			parent.childrens = new ArrayList<TreesNode<E>>();
			parent.childrens.add(node);
		}
		size++;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see wright.dase.ds.TreeInterface#deleteNode(wright.dase.ds.TreesNode)
	 */
	@Override
	public boolean deleteNode(TreesNode<E> node) {
		if (node == null)
			return false;

		// detect parent
		if (node.equals(root)) {
			System.out.println("Deleting root. Tree will not be accessible");
			this.root = null;
			size = 0;
		} else {
			// this is intermediate or leaf node
			TreesNode<E> immediateParent = getParent(node);
			ArrayList<TreesNode<E>> childrens = getChildrens(node);
			if (childrens != null) {
				// detect childrens
				immediateParent.childrens = childrens;
				for (TreesNode<E> children : childrens) {
					children.parent = immediateParent;
				}
			}
			// else this is leaf node
			// add childrens to parent
			size--;
		}
		return false;
	}

	/**
	 * Traverse using queue
	 * 
	 * @param datas
	 */
	private void preOderTraverse(ArrayList<E> datas) {
		if (datas == null)
			return;

		QueueAsLinkedList<TreesNode<E>> queue = new QueueAsLinkedList<>();
		queue.push(this.root);

		while (!queue.isEmpty()) {

			TreesNode<E> currentNode = queue.pop();
			datas.add(currentNode.getData());
			if (currentNode.childrens != null) {
				for (TreesNode<E> node : getChildrens(currentNode)) {
					queue.push(node);
				}
			}
		}
	}

	/**
	 * User recursion/system stack for this.
	 * 
	 * @param datas
	 */
	private void postOrderTraverse(ArrayList<E> datas, TreesNode<E> node) {
		if (datas == null)
			return;
		if (isLeaf(node)) {
			datas.add(node.getData());
			return;
		}
		if (node.childrens != null) {
			for (TreesNode<E> child : getChildrens(node)) {
				postOrderTraverse(datas, child);
			}
		}
		datas.add(node.getData());
	}

	/**
	 * User recursion/system stack for this.
	 * 
	 * invariant: if node.childrens.length >=1 then if processing a single children
	 * node have to process.
	 * 
	 * @param datas
	 */
	private void inOrderTraverse(ArrayList<E> datas, TreesNode<E> node) {

		if (isLeaf(node)) {
			datas.add(node.getData());
			return;
		}

		if (node.childrens != null) {
			if (getChildrens(node).size() >= 1) {
				inOrderTraverse(datas, getChildrens(node).get(0));
				datas.add(node.getData());
				for (int i = 1; i < getChildrens(node).size(); i++) {
					inOrderTraverse(datas, getChildrens(node).get(i));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see wright.dase.ds.TreeInterface#traverseData(wright.dase.ds.TreeInterface.
	 * TraverseDirection)
	 */
	@Override
	public ArrayList<E> traverseData(TraverseDirection direction) {
		if (this.root == null)
			return null;

		ArrayList<E> datas = new ArrayList<E>();
		if (TraverseDirection.PRE_ORDER == direction) {
			// root -> childrens
			preOderTraverse(datas);
		} else if (TraverseDirection.IN_ORDER == direction) {
			// left most children -> root/more children -> right most children
			inOrderTraverse(datas, this.root);
		} else if (TraverseDirection.POST_ORDER == direction) {
			// childrens -> root
			postOrderTraverse(datas, this.root);
		}
		return datas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see wright.dase.ds.TreeInterface#traverseNode(wright.dase.ds.TreeInterface.
	 * TraverseDirection)
	 */
	@Override
	public ArrayList<TreesNode<E>> traverseNode(TraverseDirection direction) {
		return null;
	}

    /**
     *
     * @param node
     * @param data
     * @return
     */
	private TreesNode<E> searchUsingDFS(TreesNode<E> node, E data) {
		TreesNode<E> resultNode = null;
		if (node.getData().equals(data)) {
			resultNode = node;
			return resultNode;
		}

		if (node.childrens != null) {
			for (TreesNode<E> kid : node.childrens) {
				resultNode = searchUsingDFS(kid, data);
			}
		}
		return resultNode;
	}

    /**
     *
     * @param root
     * @param data
     * @return
     */
	private TreesNode<E> searchUsingBFS(TreesNode<E> root, E data) {
		if (root.getData().equals(data))
			return root;

		QueueAsLinkedList<TreesNode<E>> queue = new QueueAsLinkedList<>();
		queue.push(root);

		while (!queue.isEmpty()) {
			TreesNode<E> node = queue.pop();
			if (node.getData().equals(data))
				return node;

			if (node.childrens != null) {
				for (TreesNode<E> kid : node.childrens) {
					if (kid.getData().equals(data))
						return kid;
					queue.push(kid);
				}
			}
		}

		return null;
	}


    /**
     * Default searching technique is DFS.
     * @param data
     * @return the treesNode.
     */
	public TreesNode<E> searchNode(E data){
	   return searchNode(data, SearchTechnique.DFS);
    }

	@Override
	public TreesNode<E> searchNode(E data, SearchTechnique technique) {
		if (data == null || this.root == null)
			return null;
		if (technique ==null || technique == SearchTechnique.BFS)
			return searchUsingBFS(this.root, data);
		else if (technique == SearchTechnique.DFS) {
			return searchUsingDFS(this.root, data);
		}
		return null;
	}

    /**
     *  Get list of childrens.
     * @param node
     * @return
     */
	public ArrayList<TreesNode<E>> getChildrens(TreesNode<E> node) {
		return node != null ? node.childrens : null;
	}

    /**
     *
     * @param node
     * @return
     */
	public TreesNode<E> getParent(TreesNode<E> node) {
		return node != null ? node.parent : null;
	}

	/**
	 * Find root from any node. Go from any root to upper nodes to find root
	 * 
	 * @param node
	 * @return
	 */
	public TreesNode<E> getRoot(TreesNode<E> node) {
		return node.parent == null ? node : getRoot(node);
	}

    public TreesNode<E> getRoot() {
        return root;
    }

    public boolean isRoot(TreesNode<E> node) {
		return node.parent == null ? true : false;
	}

	public boolean isLeaf(TreesNode<E> node) {
		return node.childrens == null ? true : false;
	}

}
