// -*- tab-width: 4 -*-
//Title:        JET
//Version:      1.00
//Copyright:    Copyright (c) 2000
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

package edu.nyu.jetlite.tipster;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * An Annotation assigns a type and a set of features to a portion of a
 * Document.
 */

public class Annotation {

	public String type;

	public Span span;

	public Object hypo;

	public Annotation(String tp, Span sp) {
		type = tp;
		span = sp;
	}

	public Annotation() {
	}

	public String type() {
		return type;
	}

	/**
	 * returns the span (of text) associated with the annotation.
	 */

	public Span span() {
		return span;
	}

	/**
	 * returns the start of the span associated with the annotation.
	 */

	public int start() {
		return span.start();
	}

	/**
	 * returns the end of the span associated with the annotation.
	 */

	public int end() {
		return span.end();
	}

	public void setHypo (Object s) {
	    hypo = s;
	}

	public Object getHypo () {
	    return hypo;
	}

	/**
	 * sorts a list of annotation order by its start position.
	 */

	public static void sortByStartPosition(List<Annotation> annotations) {
		if (annotations == null) {
			return;
		}
		Collections.sort(annotations, StartPositionComparator.instance);
	}

	/**
	 * Comparator for annotation based on its start position.
	 *
	 * @author Akira ODA
	 */
	private static class StartPositionComparator implements Comparator<Annotation> {
		public static StartPositionComparator instance = new StartPositionComparator();

		private StartPositionComparator() {
		}

		public int compare(Annotation a, Annotation b) {
			if (a.start() < b.start()) {
				return -1;
			} else if (a.start() > b.start()) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	public String toString () {
	    return type + span + "ID=" + getID();
	}

    public static String feat (String name, Object value) {
	if (value == null) 
	    return "";
	else if (value instanceof Annotation)
	    return " " + name + "=" + ((Annotation) value).getID();
	else if (value instanceof String)
	    return " " + name + "=" + value;
	else if (value instanceof List) {
	    StringBuffer sb = new StringBuffer();
	    sb.append (" " + name + "=[");
	    List list = (List) value;
	    for (int i = 0; i < list.size(); i++) {
		Object o = list.get(i);
		if (i > 0) sb.append(" ");
		if (o instanceof String)
		    sb.append(o);
		else if (o instanceof Annotation)
		    sb.append(((Annotation) o).getID());
		else sb.append("*error*");
	    }
	    sb.append("]");
	    return sb.toString();
	}
	else
	    return("*error*");
    }

    private String ID;
    public void setID (String id) {ID = id;}
    public String getID () {return ID;}
}
