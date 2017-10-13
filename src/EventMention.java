// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.List;

public class EventMention extends Annotation {

    public EventMention (Span s) {
	span = s;
	type = "eventMention";
    }

    private String semType;

    public void setSemType (String semType) {this.semType = semType;}

    public String getSemType () {return semType;}

    public String toString () {
	return super.toString()
	    + Annotation.feat("semType", semType);
    }

}
