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

/**
  *  An implementation of the absract class MaxEntModel using OpenNLP.
  */

public class OpenMaxEntModel extends MaxEntModel {

    GISModel model;
    String modelFileName;
    String task;
    boolean loaded = false;

    final static boolean USE_SMOOTHING = false;
    final static int NUM_ITERATIONS = 25;

    public OpenMaxEntModel (String modelFileName, String task) {
	this.modelFileName = modelFileName;
	this.task = task;
    }

    /**
     *  Build a maximum entropy model from the training data on file 'trainingFileName',
     *  discarding features which occur fewer than 'cutoff' times..
     */

    public void train (String trainingFileName, int cutoff) {
	try {
	    // read events with blank-separated features
	    FileReader datafr = new FileReader(new File(trainingFileName));
	    EventStream es = new BasicEventStream(new PlainTextByLineDataStream(datafr), " ");
	    // train model using NUM_ITERATIONS iterations, including all events (no low-freq cutoff)
	    model = GIS.trainModel(es, NUM_ITERATIONS, cutoff, USE_SMOOTHING, true);
	    loaded = true;
	    saveModel();
	} catch (Exception e) {
	    System.out.print("Unable to create model due to exception: ");
	    System.out.println(e);
	    e.printStackTrace();
	}
    }

    public void saveModel () throws IOException {
	    File outputFile = new File(modelFileName);
	    GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
	    writer.persist();
    }

    /**
     *  Build a maximum entropy model from the training data on file 'trainingFileName'.
     */

    public void train (String trainingFileName) {
	train (trainingFileName, 1);
    }

    /**
     *  Retrieve the max ent model.
     */

    public void loadModel () {
	try {
	    if (modelFileName == null) {
		System.out.println ("No model specified for " + task);
		System.exit(1);
	    }
	    if (!new File(modelFileName).exists()) {
		System.out.println ("Model file " + modelFileName + " for " + task + " does not exist.");
		System.exit(1);
	    }
	    model = (GISModel) new SuffixSensitiveGISModelReader(new File(modelFileName)).getModel();
	    loaded = true;
	} catch (Exception e) {
	    System.out.print("Unable to load  model " + modelFileName + " due to exception: ");
	    System.out.println(e);
	}
    }

    public String getBestOutcome (String[] features) {
	return model.getBestOutcome(model.eval(features));
    }

    public boolean isLoaded () {
	return loaded;
    }
}
