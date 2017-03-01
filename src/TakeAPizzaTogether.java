import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.util.FileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class TakeAPizzaTogether {

    static ArrayList<String> users;
   static ArrayList<SmartAgent> smartAgents;

    private static String TDBDatasetPath = "./res/tdb-storage/TDBExample/";

    public static void main(String[] args) {

        users = new ArrayList<String>();
        //creazione Users
        users.add("Ciccio");
        users.add("Edoardo");
        users.add("Enrico");
        users.add("Federico");
        users.add("Giulia");

        Hashtable<String,Dataset> userDatasets = new Hashtable<String,Dataset>();

        Dataset dataset;
        Model modelContacts = ModelFactory.createDefaultModel();
        Model modelProfile = ModelFactory.createDefaultModel();

        for(String user : users)
        {
            dataset = TDBFactory.createDataset(TDBDatasetPath+user+"/");
            //Apertura dei dataset creati nell'homework 2
            TDBLoader.loadModel(modelContacts, "./res/"+user+"/contacts.nt");
            TDBLoader.loadModel(modelProfile, "./res/"+user+"/profile.nt");
            //Caricamento nel TDB storage dei dataset in input
            dataset.addNamedModel("Contacts", modelContacts);
            dataset.addNamedModel("Profile", modelProfile);
            userDatasets.put(user,dataset);
        }

        /*userDatasets.get("Edoardo").begin(ReadWrite.READ) ;
        String qs1 = "SELECT * WHERE {?s ?p ?o } " ;

        try(QueryExecution qExec = QueryExecutionFactory.create(qs1, userDatasets.get("Edoardo").getNamedModel("Profile"))) {
            ResultSet rs = qExec.execSelect() ;
            ResultSetFormatter.out(rs) ;
        }
        */
    }

}
