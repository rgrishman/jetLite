package edu.nyu.jetlite;

import java.util.*;

/**
 *  a data point, consisting of a set of features and an outcome, intended
 *  as part of the training set for a classifier.
 */

public class Datum {

	ArrayList features;

	String outcome;

	boolean OUTCOME_FIRST = false;

	/**
	 *  create a new Datum.
	 */

	public Datum (MaxEntModel model) {
		features = new ArrayList();
		OUTCOME_FIRST = model instanceof MalletMaxEntModel;
	}

	/**
	 *  add feature <CODE>feature</CODE> to the Datum.
	 */

	public void addF (String feature) {
		features.add(feature);
	}

	/**
	 *  add feature <CODE>feature=value</CODE> to the Datum.
	 */

	public void addFV (String feature, String value) {
		if (value == null)
			features.add(feature);
		else
			features.add(feature + "=" + value);
	}

	/**
	 *  set the <CODE>outcome</CODE> for this set of features.
	 */

	public void setOutcome (String outcome) {
		this.outcome = outcome;
	}

	/**
	 *  returns the Datum as a sequence of space-separated features, with the
	 *  outcome at  one end.  For the Mallet tagger, the utcome is
	 *  placed at the beginning;  for the OpenNLP tagger it is
	 *  placed at the end.
	 */

	@Override
	public String toString () {
		StringBuffer s = new StringBuffer();
		if (OUTCOME_FIRST) {
		    s.append(outcome);
		    for (int i=0; i<features.size(); i++) {
			s.append(" ");
			s.append((String)features.get(i));
		    }
		} else {
		    for (int i=0; i<features.size(); i++) {
			s.append((String)features.get(i));
			s.append(" ");
		    }
		    s.append(outcome);
		}
		return s.toString();
	}

	/**
	 *  return the Datum as an array of features (with the outcome not included).
	 */

	public String[] toArray () {
		return (String[]) features.toArray(new String[features.size()]);
	}
}

