// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.io.*;
import java.util.*;

/**
  *  A named entity tagger trained on the CoNLL English data.
  */

public class NEtagger extends Annotator {

    String modelFileName;
    
    MaxEntModel model;

    String[] columns = {"token", null, null, "NEtype"};

    public NEtagger (Properties config) throws IOException {
	modelFileName = config.getProperty("NEtagger.model.fileName");
	model = new MalletMaxEntModel(modelFileName, "NEtagger");
    }

    /**
      *  Add annotations for names to the specified document.
      *
      *  @param  doc  the document to be annnotated
      *  @param  span  the portionof thedocument to be annotated
      */

    public Document annotate (Document doc, Span span) {
	tagDocument(doc, span);
	return doc;
    }

    /**
     *  Train and then evaluate the name tagger using the CoNLL data.
     *  invokable from the command line.
     *  <p>
     *  Takes 3 command-line arguments:                <br>
     *  training corps:  training file in CoNLL format <br>
     *  test corpus:  test data in CoNLL format        <br>
     *  modelFileName: file name of MaxEnt model
     */

    public static void main (String[] args) throws IOException {
	if (args.length != 3) {
	    System.out.println ("Error, 3 arguments required:");
	    System.out.println ("         trainingCorpus testCorpus modelFileName");
	    System.exit(1);
	}
	String trainingCorpus = args[0];
	String testCorpus = args[1];
	String modelFN = args[2];
	Properties p = new Properties();
	p.setProperty("NEtagger.model.fileName", modelFN);
	NEtagger tagger = new NEtagger(p);
	tagger.trainTagger (trainingCorpus);
	tagger.evaluate (testCorpus);
    }

    public void trainTagger (String conllFileName) throws IOException {
	SentenceStream ss = new SentenceStream(new File(conllFileName), columns, " ");
	PrintWriter eventWriter = new PrintWriter (new FileWriter ("events"));
	SentenceFromStream s;
	while ((s = ss.read()) != null) {
	    trainOnSentence(s, eventWriter);
	}
	eventWriter.close();
	model.train("events", 2);
    }

    private void trainOnSentence (SentenceFromStream s, PrintWriter eventWriter) {
	int nTokens = s.size();
	String[] words = new String[nTokens];
	for (int i=0; i < nTokens; i++)
	    words[i] = s.get("token", i);
	String priorTag = "^";
	for (int i=0; i < nTokens; i++) {
	    Datum context = NEfeatures (i, words, priorTag);
	    context.setOutcome(s.get("NEtype", i));
	    eventWriter.println(context);
	    priorTag = s.get("NEtype", i);
	}
    }

    /**
     *  Defines the features used by the NE classifier.
     */

    Datum NEfeatures (int i, String[] words, String priorTag) {
        Datum d = new Datum(model);
	int nTokens = words.length;
	String prior = (i > 0) ? words[i-1].toLowerCase() : "^";
	String current = words[i].toLowerCase();
	String next = (i >= nTokens -1) ? "$" : words[i+1].toLowerCase();

	if (i > 0 && Character.isUpperCase(words[i].charAt(0))) d.addF("cap");
	d.addFV ("prior", prior);
	d.addFV ("current", current);
	d.addFV ("next", next);
	d.addFV ("bigram", prior + ":" + current);
	d.addFV ("priorTag", priorTag);
	if (current.length() > 2) d.addFV ("suffix", current.substring(current.length() - 2));
	return d;
	}


    public void tagDocument (Document doc, Span span) {
	if (!model.isLoaded())
	    model.loadModel();
	Vector<Annotation> sentences = doc.annotationsOfType("sentence");
	for (Annotation sentence : sentences) {
	    tagSentence (doc, sentence);
	}
    }

    public void tagSentence (Document doc, Annotation sentence) {
	int posn = sentence.start();
	 // collect tokens list
	List<Annotation> tokens = new ArrayList<Annotation>();
	Annotation token;
	while ((token = doc.tokenAt(posn)) != null) {
	    tokens.add(token);
	    posn = token.end();
	    if (posn >= sentence.end()) break;
	}
	int nTokens = tokens.size();
	String[] words = new String[nTokens];
	Span[] spans = new Span[nTokens];
	for (int i=0; i < nTokens; i++) {
	    words[i] = doc.text(tokens.get(i)).trim();
	    spans[i] = tokens.get(i).span();
	}
	String[] response = new String[nTokens];
	String priorTag = "^";
	for (int i=0; i < nTokens; i++) {
	    Datum context = NEfeatures(i, words, priorTag);
	    String prediction = model.getBestOutcome(context.toArray());
	    response[i] = prediction;
	    priorTag = prediction;
	}
	BIO.tag (doc, spans, response);
    }

    public void  evaluate (String conllFileName) throws IOException {
	BIO.resetScore();
	SentenceStream ss = new SentenceStream(new File(conllFileName), columns, " ");
	SentenceFromStream s;
	while ((s = ss.read()) != null) {
	    int nTokens = s.size();
	    String[] words = new String[nTokens];
	    for (int i=0; i < nTokens; i++)
		words[i] = s.get("token", i);
	    String[] response = new String[nTokens];
	    String[] key = new String[nTokens];
	    String priorTag = "^";
	    for (int i=0; i < nTokens; i++) {
		Datum context = NEfeatures (i, words, priorTag);
		String prediction = model.getBestOutcome(context.toArray());
		response[i] = prediction;
		key[i] = s.get("NEtype", i);
		priorTag = prediction;
	    }
	    BIO.score (response, key);
	}
	BIO.reportScore();
    }
}
