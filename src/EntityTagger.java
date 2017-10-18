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
 *  Assigns semantic type information to entities.  Each entity is tagged with
 *  one of tihe ACE types (person, organization, location, GPE, facility,
 *  vehicle, or weapon) or "other".
 *  <p>
 *  In training the classifier, we do not have an explicit list of all entities,
 *  only those assigned an ACE type.  We approximate this list by using the
 *  output of the pipeline through the coref stage.
 */

public class EntityTagger extends Annotator {

    String modelFileName;

    MaxEntModel model;

    public EntityTagger (Properties config) throws IOException {
	modelFileName = config.getProperty("EntityTagger.model.fileName");
	model = new MalletMaxEntModel(modelFileName, "EntityTagger");
    }

    /**
     *  Command-line-callable method for training and evaluating an entity tagger.
     *  <p>
     *  Takes 4 arguments:  training  test  documents  model <br>
     *  where  <br>
     *  training = file containing list of training documents  <br>
     *  test = file containing list of test documents  <br>
     *  documents = directory containing document files <br>
     *  model = file containing max ent model
     */

    public static void main (String[] args) throws IOException {
	if (args.length != 4) {
	    System.out.println ("Error, 4 arguments required:");
	    System.out.println ("   listOfTrainingDocs listOfTestDocs documentDirectory modelFileName");
	    System.exit(1);
	}
	String trainDocListFileName = args[0];
	String testDocListFileName = args[1];
	String docDir = args[2];
	String mfn = args[3];
	Properties p = new Properties();
	p.setProperty("EntityTagger.model.fileName", mfn);
	EntityTagger etagger = new EntityTagger(p);
	etagger.trainTagger(docDir, trainDocListFileName);
	etagger.evaluate(docDir, testDocListFileName);
    }

    /**
     *  Train the entity type tagger.
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
	doc.setText(eraseXML(doc.text()));
	String apfFileName = docFileName.replace("sgm" , "apf.xml");
	AceDocument aceDoc = new AceDocument(docFileName, apfFileName);
	// --- split and pos tag
	Properties config = new Properties();
	config.setProperty("POStagger.model.fileName", "POSmodel");
	config.setProperty("NEtagger.model.fileName", "NEmodel");
	config.setProperty("annotators", "token sentence pos name");
	doc = Hub.processDocument(doc, config);
	// 
	findEntityMentions (aceDoc);
	// loop over tokens 
	Span span = Hub.getTEXTspan(doc);
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	while (posn < span.end()) {
	    Annotation tokenAnnotation = doc.tokenAt(posn);
	    if (tokenAnnotation == null)
		return;
	    String tokenText = doc.normalizedText(tokenAnnotation);
	    Datum d = entityFeatures(tokenText);
            AceEntityMention mention = mentionMap.get(posn);
	    String type = (mention == null) ? "other" : mention.entity.type;
	    d.setOutcome(type);
	    eventWriter.println(d);
	    posn = tokenAnnotation.end();
	}
    }

    Map<Integer, AceEntityMention> mentionMap;

    void findEntityMentions (AceDocument aceDoc) {
	mentionMap = new HashMap<Integer, AceEntityMention>();
	ArrayList entities = aceDoc.entities;
	for (int i=0; i<entities.size(); i++) {
	    AceEntity entity = (AceEntity) entities.get(i);
	    String type = entity.type;
	    String subtype = entity.subtype;
	    ArrayList mentions = entity.mentions;
	    for (int j=0; j<mentions.size(); j++) {
		AceEntityMention mention = (AceEntityMention) mentions.get(j);
		mentionMap.put(mention.jetHead.start(), mention);
	    }
	}
    }

    Datum entityFeatures (String word) {
	Datum d = new Datum(model);
	d.addF(word);
	return d;
    }

    /**
     *  Removes all XML tags from a String.
     *  <p>
     *  In computing character offsets within a Document, Jet counts all characters.
     *  ACE does not count characters in XML tags.  To make the offsets compatible, we
     *  delete all XML tags from ACE training documents using eraseXML
     *
     *  @param  fileTextWithXML  the original ocument text
     *
     *  @return  the text with all XML tags removed
     */

    static String eraseXML (String fileTextWithXML) {
	boolean inTag = false;
	int length = fileTextWithXML.length();
	StringBuffer fileText = new StringBuffer();
	for (int i=0; i<length; i++) {
	    char c = fileTextWithXML.charAt(i);
	    if(c == '<') inTag = true;
	    if (!inTag) fileText.append(c);
	    if(c == '>') inTag = false;
	}
	return fileText.toString();
    }
    
    int correctEntities;
    int responseEntities;
    int keyEntities;

    /**
     *  Evaluate the performance of the entity tagger.
     *
     *  @param  docdir               the directory containing the test documents
     *  @param  testDocListFileName  the file containing a list of the test documents, one per line
     */

    void evaluate (String docDir, String testDocListFileName) throws IOException {
	correctEntities = 0;
	responseEntities = 0;
	keyEntities = 0;
	BufferedReader docListReader = new BufferedReader (new FileReader (testDocListFileName));
	String line; 
	while ((line = docListReader.readLine()) != null)
	    evaluateOnDocument (docDir + "/" + line.trim());
	float recall = 100.0f * correctEntities / keyEntities;
	float precision = 100.0f * correctEntities / responseEntities;
	System.out.println ("correct: " + correctEntities + "   response: " + responseEntities
		+ "   key: " + keyEntities);
	System.out.println ("precision: " + precision + "   recall: " + recall);
    }

    void evaluateOnDocument (String docFileName) throws IOException {
	File docFile = new File(docFileName);
	Document doc = new Document(docFile);
	doc.setText(eraseXML(doc.text()));
	String apfFileName = docFileName.replace("sgm" , "apf.xml");
	AceDocument aceDoc = new AceDocument(docFileName, apfFileName);
	// --- split and pos tag
	Properties config = new Properties();
	config.setProperty("POStagger.model.fileName", "POSmodel");
	config.setProperty("NEtagger.model.fileName", "NEmodel");
	config.setProperty("annotators", "token sentence pos name");
	doc = Hub.processDocument(doc, config);
	// 
	findEntityMentions (aceDoc);
	// loop over tokens 
	Span span = Hub.getTEXTspan(doc);
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	while (posn < span.end()) {
	    Annotation tokenAnnotation = doc.tokenAt(posn);
	    if (tokenAnnotation == null)
		return;
	    String tokenText = doc.normalizedText(tokenAnnotation);
	    Datum d = entityFeatures(tokenText);
	    AceEntityMention mention = mentionMap.get(posn);
	    String type = (mention == null) ? "other" : mention.entity.type;
	    String prediction = model.getBestOutcome(d.toArray());
	    if (prediction.equals(type) && !prediction.equals("other"))
		correctEntities++;
	    if ( !prediction.equals("other"))
		responseEntities ++;
	    if ( !type.equals("other"))
		keyEntities++;
	    posn = tokenAnnotation.end();
	}
    }

    public Document annotate (Document doc, Span span) {
	if (!model.isLoaded())
	    model.loadModel();
	Vector<Annotation> entities = doc.annotationsOfType("entity");
	if (entities == null)
	    return doc;
	for (Annotation entity : entities) {
	    String tokenText = doc.normalizedText(entity);
	    Datum d = entityFeatures(tokenText);
	    String prediction = model.getBestOutcome(d.toArray());
	    ((Entity) entity).setSemType(prediction);
	}
	return doc;
    }
}
