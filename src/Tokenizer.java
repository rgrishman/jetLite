// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.*;

/**
 *  A simple tokenizer, adding Token annotations to a document.  Treats as a
 *  single token each sequence of one or more letters,  each  sequence of one
 *  or more digits, and each individual special chaacter.  
 *
 *  Two special cases are handled:  abbreviation-ending periods are made part of
 *  the abbreviation token, so the sentence splitter does not treat the period as
 *  ending a sentence.  In addition, contractions are split following
 *  Penn Tree Bank conventions.
 */

public class  Tokenizer extends Annotator {

    /**
     *  abbreviations which never end a sentence.
     */

    static HashSet<String> abbreviations = new HashSet<String>();

    static {// titles
	abbreviations.add("Adm.");
	abbreviations.add("Brig.");
	abbreviations.add("Capt.");
	abbreviations.add("Cmdr.");
	abbreviations.add("Col.");
	abbreviations.add("Dr.");
	abbreviations.add("Gen.");
	abbreviations.add("Gov.");
	abbreviations.add("Lt.");
	abbreviations.add("Maj.");
	abbreviations.add("Messrs.");
	abbreviations.add("Mr.");
	abbreviations.add("Mrs.");
	abbreviations.add("Ms.");
	abbreviations.add("Prof.");
	abbreviations.add("Rep.");
	abbreviations.add("Reps.");
	abbreviations.add("Rev.");
	abbreviations.add("Sen.");
	abbreviations.add("Sens.");
	abbreviations.add("Sgt.");
	abbreviations.add("Sr.");
	abbreviations.add("St.");

	// abbreviated first names
	abbreviations.add("Alex.");
	abbreviations.add("Benj.");
	abbreviations.add("Chas.");

	// other abbreviations
	abbreviations.add("a.k.a.");
	abbreviations.add("c.f.");
	abbreviations.add("i.e.");
	abbreviations.add("vs.");
	abbreviations.add("v.");
	abbreviations.add("e.g.");
	abbreviations.add("U.S.");
	abbreviations.add("U.N.");
	abbreviations.add("D.C.");
    }

    int tokenStart;

    public Tokenizer (Properties config) {
    }

    public Document annotate (Document doc, Span span) {

	String text = doc.text();
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());

	while (posn < span.end()) {
	    int tokenStart = posn;
	    char c = doc.charAt(posn);
	    int len = abbreviationCheck(text, posn);
	    if (len > 0) {
		posn += len;
	    } else if (!Character.isLetterOrDigit(c)) {
		posn++;
	    } else if (Character.isLetter(c)) {
		posn++;
		while (posn < span.end() && Character.isLetter(doc.charAt(posn)))
		    posn++;
		if (posn + 2 < span.end())
		    posn = contractionCheck(doc, posn);
	    } else {
		posn++;
		while (posn < span.end() && Character.isDigit(doc.charAt(posn)))
		    posn++;
	    }
	    posn = doc.skipWhitespace(posn, span.end());
	    doc.addAnnotation (new Token (new Span(tokenStart, posn)));
	}
	return doc;
    }

    /**
     *  If the text at offset 'posn' is an abbreviation, return its length,
     *  else return -1.
     */

    private int abbreviationCheck (String text, int posn) {
	for (String abbrev : abbreviations)
	    if (text.startsWith(abbrev, posn))
		return abbrev.length();
	return -1;
    }

    /**
     *  If the text at offset 'posn' is the apostrophe of a contraction
     *  split the contraction into two tokens following Penn Tree Bank rules.
     */

    private int contractionCheck (Document doc, int posn) {
	if (doc.charAt(posn) == '\'') {
	    if (doc.charAt(posn + 1) == 's' && !Character.isLetter(doc.charAt(posn + 2))) {
		doc.addAnnotation(new Token (new Span (tokenStart, posn)));
		tokenStart = posn;
		posn = posn + 2;
	    }
	    if (doc.charAt(posn - 1) == 'n' && doc.charAt(posn + 1) == 't' && 
		    Character.isLetter(doc.charAt(posn + 2))) {
		doc.addAnnotation(new Token (new Span (tokenStart, posn - 1)));
		tokenStart = posn - 1;
		posn = posn + 2;
	    }
	}
	return posn;
    }

}
