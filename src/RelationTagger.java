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
import opennlp.maxent.*;
import opennlp.maxent.io.*;
import opennlp.model.*;

/**
 *  A relation tagger trained on the ACE 2005 data.
 */

public class RelationTagger extends Annotator {

    // the file containing the MaxEnt model
    String modelFileName;

    // the MaxEnt model
    GISModel model;

    /**
     *  Create a new RelationTagger.
     *
     *  @param  config  A jet property file.  Property RelationTagger.model.fileName
     *                 specifies the file to contain the model.
     */

    public RelationTagger (Properties config) throws IOException {
	modelFileName = config.getProperty("RelationTagger.model.fileName");
    }

    /**
     *  Command-line-callable method for training and evaluating a relation tagger.
     *  <p>
     *  Takes 4 arguments:  training  test  documents  model <br>
     *  where  <br>
     *  training = file containing list of training documents  <br>
     *  test = file containing list of test documents  <br>
     *  documents = directory containing document files  <br>
     *  model = file to contain max ent model
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
	String modelFN = args[3];
	Properties p = new Properties();
	p.setProperty("RelationTagger.model.fileName", modelFN);
	RelationTagger rtagger = new RelationTagger(p);
 	rtagger.trainTagger(docDir, trainDocListFileName);
	rtagger.evaluate(docDir, testDocListFileName);
    }

    /**
     *  Train the relation tagger.
     *
     *  @param  docDir           directory containing document files
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
	MaxEnt.buildModel(modelFileName, 3);
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
	// --- apply tokenizer and sentence segmenter
	Properties config = new Properties();
	config.setProperty("annotators", "token sentence");
	doc = Hub.processDocument(doc, config);
	// ---	
	findEntityMentions (aceDoc);
	findRelationMentions (aceDoc);
	// collect all pairs of nearby mentions
	List<AceEntityMention[]> pairs = findMentionPairs (doc);
	// iterate over pairs of adjacent mentions, record candidates for ACE relations
	for (AceEntityMention[] pair : pairs)
	    addTrainingInstance (doc, pair[0], pair[1], eventWriter);
	// were any positive instances not captured?
	// reportLeftovers ();
    }

    static Set<AceEntityMention> mentionSet;

    /**
     *  Puts all mentions of all AceEntities in 'aceDoc' into 'mentionSet'.
     */

    static void findEntityMentions (AceDocument aceDoc) {
	mentionSet = new HashSet<AceEntityMention>();
	ArrayList<AceEntity> entities = aceDoc.entities;
	for (AceEntity entity : entities) {
	    String type = entity.type;
	    String subtype = entity.subtype;
	    ArrayList<AceEntityMention>  mentions = entity.mentions;
	    for (AceEntityMention mention : mentions) {
		mentionSet.add (mention);
	    }
	}
    }

    static List<AceRelationMention> relMentionList;

    /**
     *  Puts all AceRelationMentions in 'aceDoc' on relMentionList.
     */

    private static void findRelationMentions (AceDocument aceDoc) {
	relMentionList = new ArrayList<AceRelationMention>();
	ArrayList relations = aceDoc.relations;
	for (int i=0; i<relations.size(); i++) {
	    AceRelation relation = (AceRelation) relations.get(i);
	    String relationClass = relation.relClass;
	    relMentionList.addAll(relation.mentions);
	}
    }

    private static final int mentionWindow = 4;

    /**
     *  returns the set of all pairs of mentions separated by at most mentionWindow mentions
     */

    static List<AceEntityMention[]> findMentionPairs (Document doc) {
	List<AceEntityMention[]> pairs = new ArrayList<AceEntityMention[]> ();
	if (mentionSet.isEmpty()) return pairs;
	ArrayList mentionList = new ArrayList(mentionSet);
	Collections.sort(mentionList);
	for (int i=0; i<mentionList.size()-1; i++) {
	    for (int j=1; j<=mentionWindow && i+j<mentionList.size(); j++) {
		AceEntityMention m1 = (AceEntityMention) mentionList.get(i);
		AceEntityMention m2 = (AceEntityMention) mentionList.get(i+j);
		// if two mentions co-refer, they can't be in a relation
		// if (!canBeRelated(m1, m2)) continue;
		// if two mentions are not in the same sentence, they can't be in a relation
		if (!inSameSentence(m1.jetHead.start(), m2.jetHead.start(), doc)) continue;
		pairs.add(new AceEntityMention[] {m1, m2});
	    }
	}
	return pairs;
    }

    /**
     *  Returns true if character offsets <code>s1</code> and <code>s2</code>
     *  fall wihin the same sentence in Document doc.
     */

    static boolean inSameSentence (int s1, int s2, Document doc) {
	Vector<Annotation> sentences = doc.annotationsOfType("sentence");
	if (sentences == null) {
	    System.out.println("no sentence annotations");
	    return false;
	}
	for (Annotation sentence : sentences) 
	    if (within(s1,sentence.span()))
		return within(s2, sentence.span());
	return false;
    }

    private static boolean within (int i, Span s) {
	return (i >= s.start()) && (i <= s.end());}

    /**
     *  Check whether there is a relation between m1 and m2 in the training corpus;
     *  If so, write the feature vector with the relation type (or, in the absence of a 
     *  relation, the outcome "other")).
     */

    private static void addTrainingInstance (Document doc, AceEntityMention m1, AceEntityMention m2,
	    PrintWriter eventWriter) {
	// generate features
	Datum d = relationFeatures(doc, m1, m2);
	// retrieve tag from APF document
	String outcome = "other";
loop:
	for (AceRelationMention mention : relMentionList) {
	    if (mention.arg1 == m1 && mention.arg2 == m2) {
		outcome = mention.relation.type + ":" + mention.relation.subtype;
		relMentionList.remove(mention);
		break loop;
	    } else if (mention.arg1 == m2 && mention.arg2 == m1) {
		outcome = mention.relation.type + ":" + mention.relation.subtype + "-1";
		relMentionList.remove(mention);
		break loop;
	    }
	}
	d.setOutcome(outcome);
	eventWriter.println(d);
    }

    /**
     *  Features for relation tagging:  the types and identities of the arguments
     *  and the number of words between the arguments.
     *  <p>
     *  There are two slightly different feature functions.  The first is used
     *  in training the tagger.  In training, we rely on 'perfect entity mentions'
     *  from the hand-tagged APF files.  The second is used in applying the
     *  tagger as part of a pipeline to process new text.  In that case we
     *  use entity mentions generated by prior stages in the pipeline
     */

    static Datum relationFeatures (Document doc, AceEntityMention m1, AceEntityMention m2) {
	Datum d = new Datum();
	d.addFV ("arg1", m1.headText.replace(" ", "_").replace("\n", "_"));
	d.addFV ("arg2", m2.headText.replace(" ", "_").replace("\n", "_"));
	d.addFV ("type1", m1.entity.type);
	d.addFV ("type2", m2.entity.type);
	d.addFV ("types", m1.entity.type + "-" + m2.entity.type);
	int x = m1.jetHead.end();
	int wordsBetween = 0;
	String phraseBetween = "";
	while (x < m2.jetHead.start()) {
	    Token token = doc.tokenAt(x);
	    if (token == null) break;
	    String tokenText = doc.normalizedText(token);
	    wordsBetween++;
	    if (phraseBetween == "")
		phraseBetween = tokenText;
	    else
		phraseBetween += "_" + tokenText;
	    // d.addF(doc.normalizedText(token));
	    x = token.end();
	}
	d.addFV ("WordsBetween", Integer.toString(wordsBetween));
	// d.addFV ("phraseBetween", phraseBetween);
	return d;
    }

    /**
     *  Features for relation tagging:  the types and identities of the arguments
     *  and the number of words between the arguments.
     */

    static Datum relationFeatures (Document doc, Mention m1, Mention m2) {
	Datum d = new Datum();
	d.addFV ("arg1", doc.normalizedText(m1));
	d.addFV ("arg2", doc.normalizedText(m2));
	String type1 = m1.getMentionOf().getSemType();
	String type2 = m2.getMentionOf().getSemType();
	d.addFV ("type1", type1);
	d.addFV ("type2", type2);
	d.addFV ("types", type1 + "-" + type2);
	int x = m1.end();
	int wordsBetween = 0;
	String phraseBetween = "";
	while (x < m2.start()) {
	    Annotation token = doc.tokenAt(x);
	    if (token == null) break;
	    String tokenText = doc.normalizedText(token);
	    wordsBetween++;
	    if (phraseBetween == "")
		phraseBetween = tokenText;
	    else
		phraseBetween += "_" + tokenText;
	    // d.addF(doc.normalizedText(token));
	    x = token.end();
	}
	d.addFV ("WordsBetween", Integer.toString(wordsBetween));
	// d.addFV ("phraseBetween", phraseBetween);
	return d;
    }

    static int correctRelations = 0;
    static int responseRelations = 0;
    static int keyRelations = 0;

    /**
     *  Evaluate the relation model just built and print the scores.
     *
     *  @param  docDir               directory containing test documents
     *  @param  testDocListFileName  file containing a list of test documents,
     *                               one per line
     */

    void evaluate (String docDir, String testDocListFileName) throws IOException {
	correctRelations = 0;
	responseRelations = 0;
	keyRelations = 0;
	model = MaxEnt.loadModel(modelFileName, "RelationTagger");
	BufferedReader docListReader = new BufferedReader (new FileReader (testDocListFileName));
	String line;
	while ((line = docListReader.readLine()) != null)
	    evaluateOnDocument (docDir + "/" + line.trim());
	float recall = 100.0f * correctRelations / keyRelations;
	float precision = 100.0f * correctRelations / responseRelations;
	System.out.println ("correct: " + correctRelations + "   response: " + responseRelations
		+ "   key: " + keyRelations);
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
	// --- apply tokenizer and sentence segmenter
	Properties config = new Properties();
	config.setProperty("annotators", "token sentence");
	doc = Hub.processDocument(doc, config);
	// ---
	findEntityMentions (aceDoc);
	findRelationMentions (aceDoc);
	// collect all pairs of nearby mentions
	List<AceEntityMention[]> pairs = findMentionPairs (doc);
	// iterate over pairs of adjacent mentions, record candidates for ACE relations
	for (AceEntityMention[] pair : pairs)
	    evaluateOnPair (doc, pair[0], pair[1]);
    }
											      
    /**
     *  Evaluate the relation tagger with respect to a specific pair of entity mentions.
     */ 

    void evaluateOnPair (Document doc, AceEntityMention m1, AceEntityMention m2) {
	// generate features and predict relation
	Datum d = relationFeatures(doc, m1, m2);
	String prediction = model.getBestOutcome(model.eval(d.toArray()));
	// determine from ACE key whether there is a relation
	String outcome = "other";
loop:
	for (AceRelationMention mention : relMentionList) {
	    if (mention.arg1 == m1 && mention.arg2 == m2) {
		outcome = mention.relation.type + ":" + mention.relation.subtype;
		relMentionList.remove(mention);
		break loop;
	    } else if (mention.arg1 == m2 && mention.arg2 == m1) {
		outcome = mention.relation.type + ":" + mention.relation.subtype + "-1";
		relMentionList.remove(mention);
		break loop;
	    }
	}
	if (prediction.equals(outcome) && !prediction.equals("other"))
	    correctRelations++;
	if ( !prediction.equals("other"))
	    responseRelations ++;
	if ( !outcome.equals("other"))
	    keyRelations++;
    }

    /**
     *  Annotate a document with RelationMention annotations.  
     */

    public Document annotate (Document doc, Span span) {
	// load model if not previously loaded.
	if (model == null)
	    model = MaxEnt.loadModel(modelFileName, "RelationTagger");
	List<Mention> mentionList = Coref.gatherMentions(doc, span);
	// iterate over all pairs of entity mentions appearing in the same sentence
	for (int i=0; i<mentionList.size()-1; i++) {
	    for (int j=1; j<=mentionWindow && i+j<mentionList.size(); j++) {
		Mention m1 = mentionList.get(i);
		Mention m2 = mentionList.get(i+j);
		// if two mentions are not in the same sentence, they can't be in a relation
		if (!inSameSentence(m1.start(), m2.start(), doc)) continue;
		// compte the features for this mentin pair and then use the
		// Maxent model to predict the relation, if any
		Datum d = relationFeatures (doc, m1, m2);
		String prediction = model.getBestOutcome(model.eval(d.toArray()));
		// if model predicts a relation, add a RelationMention annotation
		if ( !prediction.equals("other")) {
		    Span relSpan;
		    if (m1.start() < m2.start())
			relSpan = new Span (m1.start(), m2.end());
		    else
			relSpan = new Span (m2.start(), m1.end());
		    RelationMention rm = new RelationMention(relSpan);
		    doc.addAnnotation(rm);
		    rm.setSemType(prediction);
		    System.out.println("* Found relation " + doc.normalizedText(relSpan));
		    System.out.println("  arg1= " + doc.normalizedText(m1) +
			    " type = " + prediction + " arg2 = " + doc.normalizedText(m2));
		}
	    }
	}
	return doc;
    }

}
