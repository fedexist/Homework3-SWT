import biweekly.ICalendar;
import biweekly.component.VEvent;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class TakeAPizzaTogether {

    static ArrayList<String> users = new ArrayList<String>();
    static ArrayList<SmartAgent> smartAgents = new ArrayList<SmartAgent>();

    static ArrayList<ICalendar> usersCalendars = new ArrayList<ICalendar>();
    static HashMap<String, Dataset> userDatasets = new HashMap<String, Dataset>(); // <userID, Dataset>

    private static String TDBDatasetPath = "./res/tdb-storage/TDBExample/";
    static String datasetURI = "https://www.smartcontacts.com/ontology";
    static String NS = datasetURI + "#";

    public static void main(String[] args) {

        //Valori di test
        users.add("Ciccio");
        users.add("Edoardo");
        users.add("Enrico");
        users.add("Federico");
        users.add("Giulia");

        //Agenda TDB
        //Inserisce nel dataset dell'utente un modello con nome "Agenda" a cui si può fare riferimento
        for (String user : users) {

            Model agenda = ModelFactory.createDefaultModel();
            Model modelContacts = ModelFactory.createDefaultModel();
            Model modelProfile = ModelFactory.createDefaultModel();

            Dataset dataset = TDBFactory.createDataset(TDBDatasetPath + user + "/");
            //Apertura dei dataset creati nell'homework 2
            modelContacts = RDFDataMgr.loadModel("./res/" + user + "/contacts.nt");
            modelProfile = RDFDataMgr.loadModel("./res/" + user + "/profile.nt");
            //Caricamento nel TDB storage dei dataset in input
            dataset.addNamedModel("Contacts", modelContacts);
            dataset.addNamedModel("Profile", modelProfile);
            dataset.addNamedModel("Agenda", agenda);
            userDatasets.put(user, dataset);


        }

        for (String user : users) {

            ICalendar ical = new ICalendar();
            ical.setUid(UUID.randomUUID().toString());

            VEvent event = new VEvent();
            event.setDescription("Test Event for user " + user);
            event.setUid(UUID.randomUUID().toString());

            ical.addEvent(event);

            usersCalendars.add(ical);

            Dataset currentDataset = userDatasets.get(user);
            Model agenda = currentDataset.getNamedModel("Agenda");

            Resource subject = agenda.createResource(user);
            Property predicate = agenda.createProperty(NS, "hasCalendar");
            Resource object = agenda.createResource(ical.getUid().toString());
            //Aggiunta tripla del calendar nel TDB
            agenda.add(subject, predicate, object);

            try (PrintWriter writer = new PrintWriter("./res/" + user + "/" + ical.getUid().toString() + ".ics", "UTF-8")) {

                ical.write(writer);

            } catch (Exception e) {
                e.printStackTrace();
            }


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
