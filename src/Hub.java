// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import java.util.*;
import java.io.*;
import edu.nyu.jetlite.tipster.*;

public class Hub {

    static Properties config = new Properties();

    public static void main (String[] args) throws IOException {

	config.load(new FileReader("props"));

	String documentFileName = args[0];
	File documentFile = new File(documentFileName);
	Document doc = new Document(documentFile);
	    // "A title\n<TEXT>\nJohn, a noted lawyer, died. He was one of the best lawyers in New York.  </TEXT>");
	processDocument (doc, config);
	System.out.println(doc);
    }

    public static Document processDocument (Document doc, Properties config) throws IOException {

	Tokenizer tok = new Tokenizer(props(config, "Tokenizer"));
	SentenceSplitter splitter = new SentenceSplitter(props(config, "Splitter"));
	POStagger post = new POStagger(props(config, "POStagger"));
	NEtagger names = new NEtagger(props(config, "NEtagger"));
	DepParser parser = new DepParser(props(config, "DepParser"));
	Coref coref = new Coref(props(config, "Coref"));
	EntityTagger etagger = new EntityTagger(props(config, "EntityTagger"));
	RelationTagger rtagger = new RelationTagger(props(config, "RelationTagger"));
	EventTagger vtagger = new EventTagger(props(config, "EventTagger"));
    
	Span span = getTEXTspan(doc);

	String annotatorProp = config.getProperty("annotators");
	if (annotatorProp == null)
	    System.out.println("annotators not specified");
	String[] annotators = annotatorProp.split(" ");
	for (String annotator : annotators) {
	    if (annotator.equals("token"))
		doc = tok.annotate(doc, span);
	    else if (annotator.equals("sentence"))
		doc = splitter.annotate(doc, span);
	    else if (annotator.equals("pos"))
		doc = post.annotate(doc, span);
	    else if (annotator.equals("name"))
		doc = names.annotate(doc, span);
	    else if (annotator.equals("parse"))
		doc = parser.annotate(doc, span);
	    else if (annotator.equals("coref"))
		doc = coref.annotate(doc, span);
	    else if (annotator.equals("entity"))
		doc = etagger.annotate(doc, span);
	    else if (annotator.equals("relation"))
		doc = rtagger.annotate(doc, span);
	    else if (annotator.equals("event"))
		doc = vtagger.annotate(doc, span);
	    else System.out.println("Unknown annotator " + annotator);
	}
    
	return doc;
    }

    /**
     *  Given a Properties table, returns a (eneraly) smaller table
     *  containing only those entries starting with 'prefix'.
     */

    static Properties props (Properties config, String prefix) {
	Properties result = new Properties();
	for (String p : config.stringPropertyNames()) {
	    if (p.startsWith(prefix + "."))
		result.setProperty(p, config.getProperty(p));
	}
	return result;
    }

    public static Span getTEXTspan (Document doc) {
	doc.annotateWithTag("TEXT");
	Vector<Annotation> text = doc.annotationsOfType("TEXT");
	if (text != null)
	    return text.get(0).span();
	else
	    return doc.fullSpan();
    }
}
