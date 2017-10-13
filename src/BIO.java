// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;

/**
 *  Methods to support BIO tagging.
 */

public class BIO {

    private static int correct = 0;
    private static int incorrect = 0;
    private static int keyGroupCount = 0;
    private static int responseGroupCount = 0;
    private static int correctGroupCount = 0;

    /**
     *  Initalize scoring of BIO-tagged data.
     */

    public static void resetScore () {
	correct = 0;
	incorrect = 0;
	keyGroupCount = 0;
	responseGroupCount = 0;
	correctGroupCount = 0;
    }

    /**
     *  Increment counts of correct and incorrect tags based on a comparison of
     *  response and key.
     */

    public static void score (String[] response, String[] key) {
	int len = response.length;
	int keyStart = -1;
	int responseStart = -1;
	String keyGroupType = "";
	String responseGroupType = "";
	for (int i=0; i < len; i++) {
	    String responseTag = response[i].intern();
	    String responsePrefix = responseTag.substring(0, 1).intern();
	    String responseType = (responseTag.length() < 2) ? "" : responseTag.substring(2).intern();
	    String keyTag = key[i].intern();
	    String keyPrefix = keyTag.substring(0, 1).intern();
	    String keyType = (keyTag.length() < 2) ? "" : keyTag.substring(2).intern();
	    if (responseTag == keyTag)
		correct++;
	    else
		incorrect++;
	    //  the previous token ends a group if
	    //    we are in a group AND
	    //    the current tag is O OR the current tag is a B tag
	    //    the current tag is an I tag with a different type from the current group
	    boolean responseEnd =  responseStart>=0 && (responseTag=="O" || responsePrefix=="B" || (responsePrefix=="I" && responseType!=responseGroupType));
	    // the current token begins a group if
	    //   the previous token was not in a group or ended a group AND
	    //   the current tag is an I or B tag
	    boolean responseBegin = (responseStart<0 || responseEnd) && (responsePrefix=="B" || responsePrefix=="I");
	    boolean keyEnd =  keyStart>=0 && (keyTag=="O" || keyPrefix=="B" || (keyPrefix=="I" && keyType!=keyGroupType));
	    boolean keyBegin = (keyStart<0 || keyEnd) && (keyPrefix=="B" || keyPrefix=="I");
	    if (responseEnd)
		responseGroupCount = responseGroupCount + 1;
	    if (keyEnd)
		keyGroupCount = keyGroupCount + 1;
	    if (responseEnd & keyEnd & responseStart == keyStart & responseGroupType == keyGroupType)
		correctGroupCount = correctGroupCount + 1;
	    if (responseBegin) {
		responseStart = i;
		responseGroupType = responseType;
	    }
	    else if (responseEnd)
		responseStart = -1;
	    if (keyBegin) {
		keyStart = i;
		keyGroupType = keyType;
	    }
	    else if (keyEnd)
		keyStart = -1;
	}
    }

    /**
     *  Write to standard output a report of tagger performance.
     */

    public static void reportScore () {
	System.out.println ( correct + " out of " + (correct + incorrect) + " tags correct \n");
	double accuracy = 100.0 * correct / (correct + incorrect);
	System.out.printf ( "  accuracy: %5.2f \n\n",  accuracy);
	System.out.println ( keyGroupCount + " groups in key\n");
	System.out.println ( responseGroupCount + " groups in response\n");
	System.out.println ( correctGroupCount + " correct groups\n");
	double precision = 100.0 * correctGroupCount / responseGroupCount;
	double recall = 100.0 * correctGroupCount / keyGroupCount;
	double F = 2 * precision  * recall / (precision + recall);
	System.out.printf ( "  precision: %5.2f", precision);
	System.out.printf ( "  recall:    %5.2f",  recall);
	System.out.printf ( "  F1:        %5.2f \n",  F);
    }

    /**
     *  Generate enamex annotations based on token-level BIO tags.
     *
     *  @param  doc       Document to be annotated
     *  @param  spans     span of i-th token
     *  @param  response  BIO tag of i-th token
     */    

    public static void tag (Document doc, Span[] spans, String[] response) {
	int len = response.length;
	int responseStart = -1;
	String responseGroupType = "";
	for (int i=0; i < len; i++) {
	    String responseTag = response[i].intern();
	    String responsePrefix = responseTag.substring(0, 1).intern();
	    String responseType = (responseTag.length() < 2) ? "" : responseTag.substring(2).intern();
	    //  the previous token ends a group if
	    //    we are in a group AND
	    //    the current tag is O OR the current tag is a B tag
	    //    the current tag is an I tag with a different type from the current group
	    boolean responseEnd =  responseStart>=0 && (responseTag=="O" || responsePrefix=="B" || (responsePrefix=="I" && responseType!=responseGroupType));
	    // the current token begins a group if
	    //   the previous token was not in a group or ended a group AND
	    //   the current tag is an I or B tag
	    boolean responseBegin = (responseStart<0 || responseEnd) && (responsePrefix=="B" || responsePrefix=="I");
	    if (responseEnd) {
		Enamex t = new Enamex (new Span (spans[responseStart].start(), spans[i-1].end()));
		doc.addAnnotation(t);
		t.setNameType(responseGroupType);
	    }
	    if (responseBegin) {
		responseStart = i;
		responseGroupType = responseType;
	    }
	    else if (responseEnd)
		responseStart = -1;
	}
    }

}
