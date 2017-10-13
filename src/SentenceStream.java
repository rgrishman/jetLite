// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool


package edu.nyu.jetlite;

import java.io.*;
import java.util.*;

/**
 *  Treats a file in CoNLL format as a series of sentences.
 */

public class SentenceStream {

    BufferedReader reader;
    String[] columns;
    String separator;

    /**
      *  Creates a new reader for a CoNLL-format file.
      *  @param  conllFile  the file to be read
      *  @param  columns    an array whose dimension is equal to the number of columns
      *                     in the CoNLL file, and whosse <i>i-th</i> element is a label
      *                     for the <i>i-th</i> column of the file, or <i>null</i> if
      *                     that column is to be ignored
      *  @param  separator  the separator between fields in the conll file.
      */

    public SentenceStream (File conllFile, String[] columns, String separator) throws IOException {
	reader = new BufferedReader (new FileReader (conllFile));
	this.columns = columns;
	this.separator = separator;
    }

    /**
     *  Reads the next sentence from the stream.
     */

    public SentenceFromStream read() throws IOException {
	int nColumns = columns.length;
	SentenceFromStream s = new SentenceFromStream(columns);
	String line;
	while ((line = reader.readLine()) != null) {
	    if (line.trim().equals(""))
		return s;
	    String[] field = line.trim().split(separator);
	    if (field.length != nColumns) {
		System.out.println("bad line " + line);
		continue;
	    }
	    s.add(field);
	}
	return null;
    }
}
