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
 *  A part-of-speech tagger trained on the Penn Treebank in CoNLL format.  Adds
 *  a 'pos' feature to all Token annotations.
 */

public class POStagger extends Annotator {

    String modelFileName;
    
    GISModel model;

    String[] columns = {"token", "pos"};

    public POStagger (Properties config) throws IOException {
	modelFileName = config.getProperty("POStagger.model.fileName");
    }

    /**
     * Add part-of-speech information in the form of 'pos' features to all
     *  Token annotations of Document doc.
     */

    public Document annotate (Document doc, Span span) {
	tagDocument(doc, span);
	return doc;
    }

    /**
     *  A command-line-callable method for training and
     *  evaluating a part-of-speech tagger.
     *  <p>
     *  Takes 3 arguments:  training  test  model <br>
     *  where  <br>
     *  training = file containing training data in CoNLL format  <br>
     *  test = file containing test data in CoNLL format  <br>
     *  model = file to contain max ent model
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
	p.setProperty("POStagger.model.fileName", modelFN);
	POStagger tagger = new POStagger(p);
	tagger.trainTagger (trainingCorpus);
	tagger.evaluate (testCorpus);
    }

    /**
     *  Train the POS tagger using file conllFileName.
     */

    public void trainTagger (String conllFileName) throws IOException {
	SentenceStream ss = new SentenceStream(new File(conllFileName), columns, "\t");
	PrintWriter eventWriter = new PrintWriter (new FileWriter ("events"));
	SentenceFromStream s;
	while ((s = ss.read()) != null) {
	    trainOnSentence(s, eventWriter);
	}
	eventWriter.close();
	MaxEnt.buildModel(modelFileName);
    }

    /**
     *  Train the tagger from sentence 's' of the training corpus.  For each
     *  token in the sentence, it writes one line to the 'events' file
     *  including the features (context vector) and the correct tag.
     */

    private void trainOnSentence (SentenceFromStream s, PrintWriter eventWriter) {
	int nTokens = s.size();
	String[] words = new String[nTokens];
	for (int i=0; i < nTokens; i++)
	    words[i] = s.get("token", i);
	String priorTag = "^";
	for (int i=0; i < nTokens; i++) {
	    Datum context = POSfeatures (i, words, priorTag);
	    context.setOutcome(s.get("pos", i));
	    eventWriter.println(context);
	    priorTag = s.get( "pos", i);
	}
    }

    /**
     *  Defines the features to be used by the POS classifier.
     */

    Datum POSfeatures (int i, String[] words, String priorTag) {
        Datum d = new Datum();
	int nTokens = words.length;
	String prior = (i > 0) ? words[i-1].toLowerCase() : "^";
	String current = words[i].toLowerCase();
	String next = (i >= nTokens -1) ? "$" : words[i+1].toLowerCase();

	// d.addFV ("pt", priorTag);
	d.addFV ("p", prior + ":" + priorTag);
	d.addFV ("c", current + ":" + priorTag);
	d.addFV ("n", next + ":" + priorTag);
	d.addFV ("w", words[i]);
	// d.addFV ("firstchar", current.substring(0, 1));
	if (current.length() > 2)
	d.addFV ("lastchar", current.substring(current.length() - 2));
	d.addFV ("cap", Character.isUpperCase(words[i].charAt(0)) ? "y" : "n");
	return d;
	}


    public void tagDocument (Document doc, Span span) {
	if (model == null)
	    model = MaxEnt.loadModel(modelFileName, "POStagger");
	Vector<Annotation> sentences = doc.annotationsOfType("sentence");
	for (Annotation sentence : sentences) {
	    tagSentence (doc, sentence);
	}
    }

    public void tagSentence (Document doc, Annotation sentence) {
	int posn = sentence.start();
	 // collect tokens list
	List<Token> tokens = new ArrayList<Token>();
	Token token;
	while ((token = doc.tokenAt(posn)) != null) {
	    tokens.add(token);
	    posn = token.end();
	    if (posn >= sentence.end()) break;
	}
	int nTokens = tokens.size();
	String[] words = new String[nTokens];
	for (int i=0; i < nTokens; i++)
	    words[i] = doc.text(tokens.get(i)).trim();
	String priorTag = "^";
	for (int i=0; i < nTokens; i++) {
	    Datum context = POSfeatures(i, words, priorTag);
	    String prediction = model.getBestOutcome(model.eval(context.toArray()));
	    tokens.get(i).setPos(prediction);
	    priorTag = prediction;
	}
    }

    /**
     *  Evaluates the accuracy of the tagger using the test corpus 'conllFileName.
     */

    public void  evaluate (String conllFileName) throws IOException {
	model = MaxEnt.loadModel(modelFileName, "POStagger");
	int tags = 0;
	int correct = 0;
	SentenceStream ss = new SentenceStream(new File(conllFileName), columns, "\t");
	SentenceFromStream s;
	while ((s = ss.read()) != null) {
	    int nTokens = s.size();
	    String[] words = new String[nTokens];
	    for (int i=0; i < nTokens; i++)
		words[i] = s.get("token", i);
	    String priorTag = "^";
	    for (int i=0; i < nTokens; i++) {
		tags++;
		Datum context = POSfeatures (i, words, priorTag);
		String prediction = model.getBestOutcome(model.eval(context.toArray()));
		if (s.get("pos", i).equals(prediction))
		    correct++;
		priorTag = prediction;
	    }
	}
	float accuracy = ((float) correct) / tags;
	System.out.println("Tags " + tags + "   correct " + correct + "   accuracy " + accuracy);
    }
}
