package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.List;

public class Entity extends Annotation {

    public Entity (Span s) {
	span = s;
	type = "entity";
    }

    private List<Mention>  mentions;

    public void setMentions (List<Mention> mentions) {this.mentions = mentions;}

    public List<Mention> getMentions () {return mentions;}

    private String semType;

    public void setSemType (String semType) {this.semType = semType;}

    public String getSemType () {return semType;}

    public String toString() {
	return super.toString()
	    + Annotation.feat("mentions", mentions)
	    + Annotation.feat("semType", semType);
    }
}
