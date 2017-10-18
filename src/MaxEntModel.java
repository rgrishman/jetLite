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

public abstract class MaxEntModel {

    /**
     *  Build a maximum entropy model from the training data on file 'events',
     *  discarding features which occur fewer than 'cutoff' times..
     */

    public abstract void train (String trainingFileName, int cutoff) throws IOException;

    /**
     *  Build a maximum entropy model from the training data on file 'events'.
     */

    public abstract void train (String trainingFileName) throws IOException;

    /**
      *  Save the model to disk.
      */

    public abstract void saveModel () throws IOException;

    /**
     *  Retrieve the max ent model.
     */

    public abstract void loadModel ();

    /**
      *  Return the most probable label given 'features'.
      */
    
    public abstract String getBestOutcome (String[] features);

    /**
      *  Return true if the model is in main memory.
      */
    public abstract boolean isLoaded ();
}
