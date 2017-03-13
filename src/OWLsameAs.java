import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;

public class OWLsameAs {

    private ArrayList<Resource> sameAsOwlClass;

    public OWLsameAs(Resource subject){

        sameAsOwlClass = new ArrayList<>();
        sameAsOwlClass.add(subject);

    }


    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if(o instanceof OWLsameAs){

            OWLsameAs toCompare = (OWLsameAs) o;

            ArrayList<Resource> tmp = new ArrayList<>(toCompare.sameAsOwlClass);
            tmp.retainAll(sameAsOwlClass);
            return !tmp.isEmpty();
        }

        return false;
    }

    public void add(RDFNode resource){

        if(!sameAsOwlClass.contains(resource.asResource()))
            sameAsOwlClass.add(resource.asResource());

    }

    public ArrayList<String> equivalentLocals(){

        ArrayList<String> local = new ArrayList<>();

        for(Resource rs : sameAsOwlClass)
            local.add(rs.getLocalName());

        return local;
    }

    @Override
    public String toString() {
        return "OWLsameAs{" +
                "sameAsOwlClass= " + sameAsOwlClass +
                '}';
    }

    @Override
    public int hashCode() {
        return sameAsOwlClass.hashCode();
    }
}
