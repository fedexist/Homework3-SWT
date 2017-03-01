import biweekly.ICalendar;
import biweekly.component.VEvent;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class TakeAPizzaTogether {

    static ArrayList<String> users = new ArrayList<String>();
    static ArrayList<SmartAgent> smartAgents = new ArrayList<SmartAgent>();

    static ArrayList<ICalendar> usersCalendars = new ArrayList<ICalendar>();
    static HashMap<String, Dataset> usersDatasets = new HashMap<String, Dataset>(); // <userID, Dataset>

    static String datasetURI = "https://www.smartcontacts.com/ontology";
    static String NS = datasetURI + "#";

    public static void main(String[] args) {

        //Valori di test
        users.add("Edoardo");
        users.add("Federico");

        //Agenda TDB
        //Inserisce nel dataset dell'utente un modello con nome "Agenda" a cui si può fare riferimento
        for(String user : users){

            Model agenda = ModelFactory.createDefaultModel();
            Dataset dataset = usersDatasets.get(user);
            dataset.addNamedModel("Agenda", agenda);

        }

        for(String user : users){

            ICalendar ical = new ICalendar();
            ical.setUid(UUID.randomUUID().toString());

                VEvent event = new VEvent();
                event.setDescription("Test Event for user " + user);
                event.setUid(UUID.randomUUID().toString());

            ical.addEvent(event);

            usersCalendars.add(ical);

            Dataset currentDataset = usersDatasets.get(user);
            Model agenda = currentDataset.getNamedModel("Agenda");

            Resource subject = agenda.createResource(user);
            Property predicate = agenda.createProperty(NS, "hasCalendar");
            Resource object = agenda.createResource(ical.getUid().toString());
            //Aggiunta tripla del calendar nel TDB
            agenda.add(subject, predicate, object);

            try(PrintWriter writer = new PrintWriter("./res/" + user + "/" + ical.getUid().toString() + ".ics", "UTF-8")){

                ical.write(writer);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }

}
