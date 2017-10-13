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
import opennlp.maxent.*;
import opennlp.maxent.io.*;
import opennlp.model.*;

/**
  *  A named entity tagger trained on the CoNLL English data.
  */

public class NEtagger extends Annotator {

    String modelFileName;
    
    GISModel model;

    String[] columns = {"token", null, null, "NEtype"};

    public NEtagger (Properties config) throws IOException {
	modelFileName = config.getProperty("NEtagger.model.fileName");
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
	MaxEnt.buildModel(modelFileName, 1);
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
        Datum d = new Datum();
	int nTokens = words.length;
	String prior = (i > 0) ? words[i-1].toLowerCase() : "^";
	String current = words[i].toLowerCase();
	String next = (i >= nTokens -1) ? "$" : words[i+1].toLowerCase();

	d.addFV ("p", prior + ":" + priorTag);
	d.addFV ("c", current + ":" + priorTag);
	d.addFV ("n", next + ":" + priorTag);
	d.addFV ("w[i]", words[i]);
	return d;
	}


    public void tagDocument (Document doc, Span span) {
	if (model == null)
	    model = MaxEnt.loadModel(modelFileName, "NEtagger");
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
	    String prediction = model.getBestOutcome(model.eval(context.toArray()));
	    response[i] = prediction;
	    priorTag = prediction;
	}
	BIO.tag (doc, spans, response);
    }

    public void  evaluate (String conllFileName) throws IOException {
	model = MaxEnt.loadModel(modelFileName, "NEtagger");
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
		String prediction = model.getBestOutcome(model.eval(context.toArray()));
		response[i] = prediction;
		key[i] = s.get("NEtype", i);
		priorTag = prediction;
	    }
	    BIO.score (response, key);
	}
	BIO.reportScore();
    }
}
