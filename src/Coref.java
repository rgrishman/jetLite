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
 *  A skeleton of an in-document coreference analyzer.  Inludes only two
 *  forms of coreference:  syntactic structures (apposition and copula)
 *  and shared names.
 */

public class Coref extends Annotator {

    boolean trace;

    public Coref (Properties config) {
	trace = config.getProperty("Coref.trace") != null;
    }

    public Document annotate (Document doc, Span span) {
	createEntities (doc, span);
	syntacticCoref (doc, span);
	nameCoref (doc, span);
	return doc;
    }

    static List<String> npHeads = new ArrayList<String>();
    static{npHeads.add("NN");}
    static{npHeads.add("NNS");}
    static{npHeads.add("PRP");}
    static{npHeads.add("PRP$");}

    /**
     *  Collects a list of all mentions in Document 'doc'.  This includes
     *  all Enamexs and all Tokens which are not shadowed by an Enamex.
     */

    static public List<Mention> gatherMentions (Document doc, Span span) {
	List<Mention> mentions = new ArrayList<Mention>();
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	while (posn < span.end()) {
	    List<Enamex> v = (List) doc.annotationsAt(posn, "enamex");
	    if (v != null && !v.isEmpty()) {
		Enamex a = (Enamex) v.get(0);
		mentions.add(a);
		posn = a.end();
	    } else {
		Token a = (Token) doc.tokenAt(posn);
		if (a == null)
		    break;
		String pos =  a.getPos();
		if (npHeads.contains(pos))
		    mentions.add(a);
		posn = a.end();
	    }
	}
	return mentions;
    }

    /**
     *  As the first step in reference resolution, creates for every mention
     *  an entity containing that mention.
     */

    public Document createEntities (Document doc, Span span) {
	for (Mention mention : gatherMentions(doc, span)) {
	    List<Mention> m = new ArrayList<Mention>();
	    m.add(mention);
	    Entity e = new Entity(mention.span());
	    e.setMentions(m);
	    doc.addAnnotation(e);
	    mention.setMentionOf(e);
	}
	return doc;
    }

    /**
     *  Identifies instances of syntactic coreference, due to apposition
     *  and copula constructions.
     */

    public Document syntacticCoref (Document doc, Span span) {
	List<Entity> entities = (List) doc.annotationsOfType("entity");
	if (entities != null)
	for (Entity entity : entities) {
	    List<Mention> mentions = entity.getMentions();
	    if (mentions.size() == 0)
		continue;
	    Mention mention = mentions.get(0);
	    List<String> depRelations = mention.getDepRelations();
	    if (depRelations == null)
		continue;
	    int index = depRelations.indexOf("appos");
	    if (index >= 0) {
		Mention mention2 = mention.getDependents().get(index);
		Entity entity2 = mention2.getMentionOf();
		isCoref (doc, entity, entity2);
	    }
	    int indexNsubj = depRelations.indexOf("nsubj");
	    int indexCop = depRelations.indexOf("Cop");
	    if (indexNsubj >= 0 && indexCop >= 0) {
		Mention mention1 = mention.getDependents().get(indexNsubj);
		Entity entity1 = mention1.getMentionOf();
		Mention mention2 = mention.getDependents().get(indexCop);
		Entity entity2 = mention2.getMentionOf();
		isCoref(doc, entity1, entity2);
	    }
	}
	return doc;
    }

    /**
     *  Marks entity1 and entity2 as coeferential by moving all the
     *  mentions of entity2 to be mentions of entity1.
     */

    public void isCoref (Document doc, Entity entity1, Entity entity2) {
	List<Mention> mentions1 = entity1.getMentions();
	List<Mention> mentions2 = entity2.getMentions();
	if (trace) System.out.println(
	       doc.normalizedText(mentions1.get(0)) + " and " +	
	       doc.normalizedText(mentions2.get(0)) + " marked corefertial");
	mentions1.addAll(mentions2);
	for (Mention m : mentions2)
	    m.setMentionOf(entity1);
	mentions2.clear();
	doc.removeAnnotation(entity2);
    }

    /**
     *  Implements simple name coreference:  if there are two entities
     *  both have names, and the second name is a subset of the first
     *  name, mark the entities as coreferential.
     */

    public void nameCoref (Document doc, Span span) {
	List<Entity> entities = (List) doc.annotationsOfType("entity");
	for (Entity entity1 : entities) {
	    List<String> name1 = nameOf(entity1);
	    if (name1 == null) continue;
	    for (Entity entity2 : entities) {
		List<String> name2 = nameOf(entity2);
		if (name2 == null) continue;
		if // name1.contains(name2))
		    (Collections.indexOfSubList(name1, name2) >= 0)
		    isCoref (doc, entity1, entity2);
	    }
	}
    }

    /**
     *  If one of the mentions of 'entity' is a name, returns that
     *  name, else returns null.
     */

    List<String> nameOf (Entity entity) {
	List<Mention> mentions = entity.getMentions();
	if (mentions.isEmpty())
	    return null;
	for (Mention m : mentions) {
	    if (m instanceof Enamex) {
		return ((Enamex) m).getTokens();
	    }
	}
	return null;
    }

}
