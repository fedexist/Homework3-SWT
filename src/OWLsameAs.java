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
        if (o == null || getClass() != o.getClass()) return false;

        OWLsameAs owLsameAs = (OWLsameAs) o;

        return owlClass.equals(owLsameAs.owlClass) || sameAsOwlClass.contains(owLsameAs.owlClass);
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
}
