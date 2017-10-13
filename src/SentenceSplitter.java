// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import java.util.*;
import edu.nyu.jetlite.tipster.*;

/**
 *  A trivial sentence splitter which splits the document text at periods.
 *  The hard work should already have been done by the Tokenizer in identifying
 *  abbreviations and making the period of abbreviations part of the
 *  abbreviation token.
 */

public class SentenceSplitter extends Annotator {

  public SentenceSplitter (Properties config) {
  }

  /**
   *  Add Sentence annotations to Span span of Document doc.
   */

  public Document annotate (Document doc, Span span) {
      int posn = span.start();
      int end = span.end();
      posn = doc.skipWhitespace(posn, span.end());
      int sentenceStart = posn;
      while (posn < span.end()) {
	  Token tok = doc.tokenAt(posn);
	  if (tok == null) {
	      return doc; // error
	  }
	  String tokenText = doc.normalizedText(tok);
	  if (tokenText.equals(".")) {
	      doc.addAnnotation( new Sentence (new Span(sentenceStart, tok.end())));
	      sentenceStart = tok.end();
	  }
	  posn = tok.end();
      }
      //  should check for a partial sentence at end
      return doc;
  }

}
