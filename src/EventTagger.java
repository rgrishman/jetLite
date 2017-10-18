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
import edu.nyu.jet.aceJet.*;

/**
 *  Identify ACE events.
 *  <p>
 *  This 'skeleton' tagger does not include finding event arguments.
 */

public class EventTagger extends Annotator {

    // the file containing the MaxEnt model
    String modelFileName;

    // the MaxEnt model
    MaxEntModel model;

    /**
     *  Create a new EventTagger.
     *
     *  @param  config  A jet property file.  Property EventTagger.model.fileName
     *                 specifies the file to contain the model.
     */

    public EventTagger (Properties config) throws IOException {
	modelFileName = config.getProperty("EventTagger.model.fileName");
	model = new MalletMaxEntModel(modelFileName, "EventTagger");
    }

    /**
     *  Command-line-callable method for training and evaluating an event tagger.
     *  <p>
     *  Takes 4 arguments:  training  test  documents  model <br>
     *  where  <br>
     *  training = file containing list of training documents  <br>
     *  test = file containing list of test documents  <br>
     *  documents = directory containing document files <br>
     *  model = file containing max ent model
     */

    public static void main (String[] args) throws IOException {
	String trainDocListFileName = args[0];
	String testDocListFileName = args[1];
	String docDir = args[2];
	String mfn = args[3];
	Properties p = new Properties();
	p.setProperty("EventTagger.model.fileName", mfn);
	EventTagger etagger = new EventTagger(p);
	etagger.trainTagger(docDir, trainDocListFileName);
	etagger.evaluate(docDir, testDocListFileName);
    }

    /**
     *  Train the event tagger.
     *
     *  @param  docDir           directory containing training documents
     *  @param  docListFileName  file containing list of training documents
     */

    public void trainTagger (String docDir, String docListFileName) throws IOException {
	BufferedReader docListReader = new BufferedReader (new FileReader (docListFileName));
	PrintWriter eventWriter = new PrintWriter (new FileWriter ("events"));
	int docCount = 0;
	String line; 
	while ((line = docListReader.readLine()) != null) {
	    learnFromDocument (docDir + "/" + line.trim(), eventWriter);
	    docCount++;
	    if (docCount % 5 == 0) System.out.print(".");
	}
	eventWriter.close();
	model.train("events");
    }

    /**
     *  Acquire training data from one Document in the training corpus.
     *
     *  @param  docFileName  the name of the document file
     *  @param  eventWriter  the Writer onto which the feature vectors extracted from
     *                       the document are to be written
     */

    void learnFromDocument (String docFileName, PrintWriter eventWriter) throws IOException {
	File docFile = new File(docFileName);
	Document doc = new Document(docFile);
	doc.setText(EntityTagger.eraseXML(doc.text()));
	String apfFileName = docFileName.replace("sgm" , "apf.xml");
	AceDocument aceDoc = new AceDocument(docFileName, apfFileName);
	// --- tokenize and split
	Properties config = new Properties();
	config.setProperty("annotators", "token sentence");
	doc = Hub.processDocument(doc, config);
	// ---
	findEventMentions (aceDoc);
	// loop over tokens 
	Span span = Hub.getTEXTspan(doc);
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	while (posn < span.end()) {
	    Annotation tokenAnnotation = doc.tokenAt(posn);
	    if (tokenAnnotation == null)
		return;
	    String tokenText = doc.normalizedText(tokenAnnotation);
	    Datum d = eventFeatures(tokenText);
            String eventType = mentionMap.get(posn);
	    if (eventType == null)
		eventType = "other";
	    d.setOutcome(eventType);
	    eventWriter.println(d);
	    posn = tokenAnnotation.end();
	}
    }

    /**
     *  A map from the position of the event trigger to the type of the event.
     */

    Map<Integer, String> mentionMap;

    void findEventMentions (AceDocument aceDoc) {
	mentionMap = new HashMap<Integer, String>();
	List<AceEvent> events = aceDoc.events;
	for (AceEvent event : events) {
	    String type = event.type;
	    String subtype = event.subtype;
	    List<AceEventMention> mentions = event.mentions;
	    for (AceEventMention mention : mentions) {
		mentionMap.put(mention.anchorJetExtent.start(), subtype);
	    }
	}
    }

    /**
     *  Features for event tagging
     *  <p>
     *  Initially we have only one feature, the trigger word itelf.  This already does
     *  quite well.  Will need to expand to include the event arguments and the
     *  document topic.
     */

    Datum eventFeatures (String word) {
	Datum d = new Datum(model);
	d.addF(word);
	return d;
    }

    int correctEvents;
    int responseEvents;
    int keyEvents;

     /**
      *  Evaluate the event model just built and print the scores.
      *
      *  @param  docDir               directory containing test documents
      *  @param  testDocListFileName  file containing a list of test documents,
      *                               one per line
      */

    void evaluate (String docDir, String testDocListFileName) throws IOException {
	correctEvents = 0;
	responseEvents = 0;
	keyEvents = 0;
	BufferedReader docListReader = new BufferedReader (new FileReader (testDocListFileName));
	String line; 
	while ((line = docListReader.readLine()) != null)
	    evaluateOnDocument (docDir + "/" + line.trim());
	float recall = 100.0f * correctEvents / keyEvents;
	float precision = 100.0f * correctEvents / responseEvents;
	System.out.println ("correct: " + correctEvents + "   response: " + responseEvents
		+ "   key: " + keyEvents);
	float F = 2 * precision  * recall / (precision + recall);
	System.out.printf ( "  precision: %5.2f", precision);
	System.out.printf ( "  recall:    %5.2f",  recall);
	System.out.printf ( "  F1:        %5.2f \n",  F);
    }

      /**
       *  Evaluate the model with respect to dcument 'docFileName' from the test collection.
       */

    void evaluateOnDocument (String docFileName) throws IOException {
	File docFile = new File(docFileName);
	Document doc = new Document(docFile);
	doc.setText(EntityTagger.eraseXML(doc.text()));
	String apfFileName = docFileName.replace("sgm" , "apf.xml");
	AceDocument aceDoc = new AceDocument(docFileName, apfFileName);
	// --- split and pos tag
	Properties config = new Properties();
	config.setProperty("annotators", "token sentence");
	doc = Hub.processDocument(doc, config);
	//  ---
	findEventMentions (aceDoc);
	// loop over tokens 
	Span span = Hub.getTEXTspan(doc);
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	while (posn < span.end()) {
	    Annotation tokenAnnotation = doc.tokenAt(posn);
	    if (tokenAnnotation == null)
		return;
	    String tokenText = doc.normalizedText(tokenAnnotation);
	    Datum d = eventFeatures(tokenText);
	    String type = mentionMap.get(posn);
	    if (type == null)
		type = "other";
	    String prediction = model.getBestOutcome(d.toArray());
	    if (prediction.equals(type) && !prediction.equals("other"))
		correctEvents++;
	    if ( !prediction.equals("other"))
		responseEvents ++;
	    if ( !type.equals("other"))
		keyEvents++;
	    /**
	     *  Annotate a document with EventMention annotations.
	     */

	    posn = tokenAnnotation.end();
	}
    }

    /**
     *  Annotate a document with EventMention annotations.
     */

    public Document annotate (Document doc, Span span) {
	if (!model.isLoaded())
	    model.loadModel();
	Vector<Annotation> tokens = doc.annotationsOfType("token");
	if (tokens == null)
	    return doc;
	for (Annotation token : tokens) {
	    String tokenText = doc.normalizedText(token);
	    Datum d = eventFeatures(tokenText);
	    String prediction = model.getBestOutcome(d.toArray());
	    if ( !prediction.equals("other")) {
		EventMention em = new EventMention(token.span());
		doc.addAnnotation (em);
		em.setSemType(prediction);
		System.out.println ("* Found event " + tokenText + " of type " + prediction);
	    }
	}
	return doc;
    }
}
