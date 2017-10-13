package edu.nyu.jetlite;

import java.util.*;

/**
  *  Bundles together information from a CoNLL-format file
  *  for one sentence.
  */

public class SentenceFromStream {

    Map<String, List<String>> map = new HashMap<String, List<String>>();
    String[] columns;
    int size = 0;

    /**
      *  Used by SentenceStream to initialize a new sentence.
      */

    public SentenceFromStream (String[] columns) {
	this.columns = columns;
	for (String column : columns) {
	    if (column != null) {
		map.put(column, new ArrayList<String>());
	    }
	}
    }

    /**
      *  Used by SentenceStream to extend the sentence by one word.
      */

    public void add (String[] value) {
	int i = 0;
	for (String column : columns) {
	    if (column != null) {
		map.get(column).add(value[i]);
	    }
	    i++;
	}
	size++;
    }

    /**
      *  returns the value of the column labeled 'column' for the <i>i-th</i>
      *  word of the sentence.
      */

    public String get (String column, int i) {
	return map.get(column).get(i);
    }

    /**
      *  Returns the size (number of words) of the sentence.
      */

    public int size () {
	return size;
    }
	 
}

