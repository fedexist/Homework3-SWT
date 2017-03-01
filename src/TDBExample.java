import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.tdb.store.DatasetGraphTDB;
import org.apache.jena.tdb.sys.TDBInternal;
import org.apache.jena.util.FileManager;

import java.io.InputStream;

public class TDBExample {

    private static String TDBDatasetPath = "./res/tdb-storage/TDBExample/";

    public static void main(String[] args) {

        FileManager fm = FileManager.get();


        Dataset dataset = TDBFactory.createDataset(TDBDatasetPath);
        //DatasetGraphTDB dsg = TDBInternal.getBaseDatasetGraphTDB(dataset.asDatasetGraph());



        Model modelContacts = ModelFactory.createDefaultModel();
        Model modelPizzas = ModelFactory.createDefaultModel();


        //Apertura dei dataset creati nell'homework 2
        TDBLoader.loadModel(modelContacts, "./res/Edoardo/contacts.nt");
        TDBLoader.loadModel(modelPizzas, "./res/tomato-mozzarella.nt");

        //Caricamento nel TDB storage dei dataset in input
        dataset.addNamedModel("Contacts", modelContacts);

        dataset.addNamedModel("Pizzas", modelPizzas);


        dataset.begin(ReadWrite.READ) ;
        String qs1 = "SELECT * WHERE {?s ?p ?o } " ;

        try(QueryExecution qExec = QueryExecutionFactory.create(qs1, dataset.getNamedModel("Contacts"))) {
            ResultSet rs = qExec.execSelect() ;
            ResultSetFormatter.out(rs) ;
        }

    }


}