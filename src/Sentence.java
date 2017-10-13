package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.List;

public class Sentence extends Annotation {

    public Sentence (Span s) {
	span = s;
	type = "sentence";
    }

}
