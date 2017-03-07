import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;

public class OWLsameAs {

    private Resource owlClass;

    private ArrayList<Resource> sameAsOwlClass;

    public OWLsameAs(Resource subject){

        owlClass = subject;
        sameAsOwlClass = new ArrayList<>();

    }


    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if(o instanceof OWLsameAs){

            OWLsameAs toCompare = (OWLsameAs) o;
            if( this.owlClass.equals(toCompare.owlClass)
                ||   this.sameAsOwlClass.contains(toCompare.owlClass)
                || toCompare.sameAsOwlClass.contains(this.owlClass))
                return true;

            ArrayList<Resource> tmp = new ArrayList<>(toCompare.sameAsOwlClass);
            tmp.retainAll(sameAsOwlClass);
            return !tmp.isEmpty();
        }

        return false;
    }

    public void add(RDFNode resource){

        if(!sameAsOwlClass.contains(resource.asResource()) && !owlClass.equals(resource))
            sameAsOwlClass.add(resource.asResource());

    }

    @Override
    public String toString() {
        return "OWLsameAs{" +
                "owlClass=" + owlClass +
                ", sameAsOwlClass=" + sameAsOwlClass +
                '}';
    }

    @Override
    public int hashCode() {
        int result = owlClass.hashCode();
        result = 31 * result + sameAsOwlClass.hashCode();
        return result;
    }
}
