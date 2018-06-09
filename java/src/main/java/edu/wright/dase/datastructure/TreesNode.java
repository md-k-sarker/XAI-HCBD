package edu.wright.dase.datastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

public class TreesNode<E> {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TreesNode<E> parent;
    public ArrayList<TreesNode<E>> childrens;
    public TreesNode<E> leftSibling;
    public TreesNode<E> rightSibling;
    public E data;
    public boolean shouldExpand;
    public double score;

    public void setScore(double score) {
        if(score < 0 || score > 1) {
            logger.error("Fatal ERROR||||||||. Score can not be more than 1 or less than 0.");
        }else{
            this.score = score;
        }

    }

    public void setAlreadyVisited(boolean alreadyVisited) {
        this.alreadyVisited = alreadyVisited;
    }

    /**
     * alreadyvisited will be evaluated by the corresponding class expressions.
     */
    public boolean alreadyVisited;

    // restricting empty constructor
    private TreesNode() {

    }

    public TreesNode(E data) {
        this(null, data, null);
    }

    public TreesNode(TreesNode<E> parent, E data, ArrayList<TreesNode<E>> childrens) {
        this(parent,data,childrens,true,-1);

//        if (data == null) {
//            throw new NullPointerException();
//        }
//
//        this.parent = parent;
//        this.data = data;
//        this.childrens = childrens;
    }

    public TreesNode(TreesNode<E> parent, E data, ArrayList<TreesNode<E>> childrens, boolean shouldExpand, double score) {
        if (data == null) {
            throw new NullPointerException();
        }
        this.parent = parent;
        this.data = data;
        this.childrens = childrens;
        this.shouldExpand = shouldExpand;
        this.score = score;
    }

    public E getData() {
        return this.data;
    }

}
