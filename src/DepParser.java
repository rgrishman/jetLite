// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import java.util.*;

import tratz.parse.*;
import tratz.parse.types.Arc;
import tratz.parse.types.Sentence;
import tratz.parse.types.Token;
import edu.nyu.jetlite.tipster.*;

/**
 *  interface to a dependency parser (currently the Tratz-Hovy parser).
 */

public class DepParser extends Annotator {

    private static FullSystemWrapper fsw=null;

    /**
     *  load the parse model file from parameter 'DepParser.model.fileName'
     *  of the Jet properties file.
     */

    public DepParser (Properties config) {
	String parseModelFile = config.getProperty("DepParser.model.fileName");
	if (parseModelFile != null) {
	    initWrapper(parseModelFile);
	}
    }

    /**Initialize the Wrapper*/
    private static void initWrapper(String parseModelFile){
	initWrapper(null, null, null, null, null, null, parseModelFile, null);
    }
    /**Initialize the Wrapper*/
    private static void initWrapper(String prepositionModelFile, String nounCompoundModelFile,
				    String possessivesModelFile, String srlArgsModelFile, 
				    String srlPredicatesModelFile, String posModelFile, 
				    String parseModelFile, String wnDir){
	if (fsw==null){
	    try{
		fsw=new FullSystemWrapper(prepositionModelFile, 
					  nounCompoundModelFile, 
					  possessivesModelFile, 
					  srlArgsModelFile, srlPredicatesModelFile,
					  posModelFile, parseModelFile, wnDir);
	    }
	    catch(Exception ex){
		System.out.println(ex);
	    }
	}
    }

    public static boolean isInitialized () {
	return fsw != null;
    }
	
    public Document annotate (Document doc, Span span) {
	parseSentence (doc, span);
	return doc;
    }

    /**
     *  parse all the sentences in Document 'doc', returning a
     *  SyntacticRelationSet containing all the dependency relations.
     */

    public static Document parseDocument (Document doc) {
	Vector<Annotation> sentences = doc.annotationsOfType("sentence");
	if (sentences == null || sentences.size() == 0) {
	    System.out.println ("DepParser:  no sentences");
	    return null;
	}
	if (fsw == null) {
	    System.out.println ("DepParser:  no model loaded");
	    return null;
	}
	for (Annotation sentence : sentences) {
	    Span span = sentence.span();
	    parseSentence (doc, span);
	}
	return doc;
    }

    static String[] SPECIAL_TOKEN = new String[] {"enamex", "numex", "timex", "timex2", "term"};

    /**
     *  generate the dependency parse for a sentence, adding its arcs to
     *  'relations'.
     */

    public static void parseSentence (Document doc, Span span) {
	if (fsw == null) {
	    System.out.println ("DepParser:  no model loaded");
	    return;
	}
	List<Token> tokens = new ArrayList<Token>();
	List<Mention> annotations = new ArrayList<Mention>();
	annotations.add(null); // don't use 0th entry
	int tokenNum = 0;
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	while (posn < span.end()) {
	    tokenNum++;
	    Mention tokenAnnotation = doc.tokenAt(posn);
	    if (tokenAnnotation == null)
		return;
	    // String pos = (String) tokenAnnotation.get("pos");
	    String pos = (String) ((edu.nyu.jetlite.Token) tokenAnnotation).getPos();
	    for (String s : SPECIAL_TOKEN) {
		Vector<Annotation> va = doc.annotationsAt(posn, s);
		if (va != null && va.size() > 0) {
		    tokenAnnotation = (Mention) va.get(0);
		    // treat all enamex's as proper nouns
		    pos = "NNP";
		    break;
		}
	    }
	    String tokenText = doc.normalizedText(tokenAnnotation).replaceAll(" ", "_");
	    tokens.add (new Token(tokenText, pos, tokenNum));
	    annotations.add(tokenAnnotation);
	    if (posn >= tokenAnnotation.end()) {
		break;
	    }
	    posn = tokenAnnotation.end();
	}
	Sentence sent = new Sentence(tokens);
	// parse sentence
	Arc[] arcs = fsw.process(sent, tokens.size() > 0 && tokens.get(0).getPos() == null,
				 true, true, true, true, true).getParse().getHeadArcs();

	// get dependencies
	for (Arc arc : arcs) {
	    if (arc == null) continue;
	    if (arc.getDependency().equalsIgnoreCase("ROOT")) continue;
	    Token head=arc.getHead();
	    Mention headAnnotation = annotations.get(head.getIndex());
	    Token dep=arc.getChild();
	    Mention depAnnotation = annotations.get(dep.getIndex());
	    String type=arc.getDependency();
	    if (headAnnotation.getDependents() == null) {
		headAnnotation.setDependents(new ArrayList<Mention>());
		headAnnotation.setDepRelations(new ArrayList<String>());
	    }
	    headAnnotation.getDependents().add(depAnnotation);
	    headAnnotation.getDepRelations().add(type);
	    // reverse dependency links --  not needed at present
	    // depAnnotation.put("governor", headAnnotation);
	    // depAnnotation.put("govRelation", type);
	}
    }

}
