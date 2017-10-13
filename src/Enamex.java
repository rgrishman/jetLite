package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.List;

public class Enamex extends Mention {

    public Enamex (Span s) {
	span = s;
	type = "enamex";
    }

    private String nameType;

    public void setNameType (String nameType) {this.nameType = nameType;}

    public String getNameType () {return nameType;}

    private List<String> tokens;

    public void setTokens (List<String> tokens) {this.tokens = tokens;}

    public List<String>  getTokens () {return tokens;}

    public String toString() {
	return super.toString() 
	    + Annotation.feat("nameType", getNameType())
	    + Annotation.feat("tokens", getTokens());
    }
}
