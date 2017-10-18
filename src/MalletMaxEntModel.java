package edu.nyu.jetlite;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.classify.*;

/**
  *  An implementation of the abstract class MaxEntModel using Mallet ver. 2
  */

public class MalletMaxEntModel extends MaxEntModel {

    Pipe pipe;
    cc.mallet.classify.MaxEnt me;
    String modelFileName;
    String task;
    boolean loaded;

    public MalletMaxEntModel (String fileName, String task)  {
	modelFileName = fileName;
	this.task = task;
    }

    public void train (String trainingFileName) throws IOException {
	pipe = buildPipe();
	InstanceList training = new InstanceList(pipe);
	String regex = "(\\S+)\\s+(.+)";
	int data = 2;
	int target = 1;
	training.addThruPipe(new LineIterator(new FileReader(trainingFileName), regex, data, target, -1));
	MaxEntTrainer trainer = new MaxEntTrainer();
	trainer.setL1Weight(1.0);
	// trainer.setGaussianPriorVariance(1.0);
	trainer.setNumIterations(100);
	me = trainer.train(training);
	saveModel();
	loaded = true;
    }

    public void train (String fn, int cutoff) throws IOException {
	train(fn);
    }

    public void saveModel () throws IOException {
	ObjectOutputStream oos =
	    new ObjectOutputStream(new FileOutputStream (modelFileName));
	oos.writeObject (me);
	oos.close();
    }

    static public SerialPipes buildPipe () {
	ArrayList pipeList = new ArrayList();
	pipeList.add(new Input2CharSequence("UTF-8"));
	String tokenPattern = "\\S+";
	pipeList.add(new CharSequence2TokenSequence(tokenPattern));
	pipeList.add(new TokenSequence2FeatureSequence());
	// Do the same thing for the "target" field: 
	//  convert a class label string to a Label object,
	//  which has an index in a Label alphabet.
	pipeList.add(new Target2Label());
	// Now convert the sequence of features to a sparse vector,
	//  mapping feature IDs to counts.
	pipeList.add(new FeatureSequence2FeatureVector());
	// Print out the features and the label
	// pipeList.add(new PrintInputAndTarget());
	return new SerialPipes(pipeList);
    }

    public String getBestOutcome (String[] context) {
	String s = "";
	for (String c : context) s += " " + c;
	s = s.trim();
	Instance in = new Instance(s, null, null, null);
	if (pipe == null) pipe = buildPipe();
	in = pipe.instanceFrom(in);
	Classification klass = me.classify(in);
	Labeling labeling = klass.getLabeling();
	return labeling.getLabelAtRank(0).toString(); //  + ":" + labeling.getValueAtRank(0);
    }

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
	    ObjectInputStream ois =
		            new ObjectInputStream (new FileInputStream (modelFileName));
	    me = (cc.mallet.classify.MaxEnt) ois.readObject();
	    ois.close();
	    loaded = true;
	} catch (Exception e) {
	    System.out.print("Unable to load  model " + modelFileName + " due to exception: ");
	    System.out.println(e);
	}
    }

     public boolean isLoaded () {
	 return loaded;
     }

}
