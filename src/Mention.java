package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.util.List;

public class Mention extends Annotation {

    public Mention (Span s) {
	span = s;
	type = "mention";
    }

    public Mention() {
    }

    private Entity mentionOf;

    public void setMentionOf (Entity mentionOf) {this.mentionOf = mentionOf;}

    public Entity getMentionOf () {return mentionOf;}

    private List<Mention> dependents;

    public void setDependents (List<Mention> dependents) {this.dependents = dependents;}

    public List<Mention> getDependents () {return dependents;}

    private List<String> depRelations;

    public void setDepRelations (List<String> depRelations) {this.depRelations = depRelations;}

    public List<String> getDepRelations () {return depRelations;}

    public String toString() {
	return super.toString()
	    + Annotation.feat("mentionOf", mentionOf)
	    + Annotation.feat("dependents", dependents)
	    + Annotation.feat("depRelations", depRelations);
    }
}
