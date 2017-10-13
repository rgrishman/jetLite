// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

package edu.nyu.jetlite;

import java.io.*;
import opennlp.maxent.*;
import opennlp.maxent.io.*;
import opennlp.model.*;

public class MaxEnt {

    final static boolean USE_SMOOTHING = false;
    final static int NUM_ITERATIONS = 25;

    /**
     *  Build a maximum entropy model from the training data on file 'events',
     *  discarding features which occur fewer than 'cutoff' times..
     */

    public static void buildModel (String modelFileName, int cutoff) {
	try {
	    // read events with blank-separated features
	    FileReader datafr = new FileReader(new File("events"));
	    EventStream es = new BasicEventStream(new PlainTextByLineDataStream(datafr), " ");
	    // train model using NUM_ITERATIONS iterations, including all events (no low-freq cutoff)
	    GISModel model = GIS.trainModel(es, NUM_ITERATIONS, cutoff, USE_SMOOTHING, true);
	    // save model
	    File outputFile = new File(modelFileName);
	    GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
	    writer.persist();
	} catch (Exception e) {
	    System.out.print("Unable to create model due to exception: ");
	    System.out.println(e);
	    e.printStackTrace();
	}
    }

    /**
     *  Build a maximum entropy model from the training data on file 'events'.
     */

    public static void buildModel (String modelFileName) {
	buildModel (modelFileName, 1);
    }

    /**
     *  Retrieve the max ent model.
     */

    public static GISModel loadModel (String modelFileName, String task) {
	try {
	    if (modelFileName == null) {
		System.out.println ("No model specified for " + task);
		System.exit(1);
	    }
	    if (!new File(modelFileName).exists()) {
		System.out.println ("Model file " + modelFileName + " for " + task + " does not exist.");
		System.exit(1);
	    }
	    return (GISModel) new SuffixSensitiveGISModelReader(new File(modelFileName)).getModel();
	} catch (Exception e) {
	    System.out.print("Unable to load  model " + modelFileName + " due to exception: ");
	    System.out.println(e);
	    return null;
	}
    }
}
