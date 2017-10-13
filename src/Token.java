package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.List;

public class Token extends Mention {

    public Token (Span s) {
	span = s;
	type = "token";
    }

    private String pos;

    public void setPos (String pos) {this.pos = pos;}

    public String getPos () {return pos;}

    public String toString() {
	return super.toString()
	+ Annotation.feat("pos", pos); 
    }
}
