import biweekly.ICalendar;
import biweekly.component.VEvent;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;

import java.io.PrintWriter;
import java.util.*;

public class TakeAPizzaTogether {

    static ArrayList<String> users = new ArrayList<>();
    static HashMap<String, SmartAgent> smartAgents = new HashMap<>();

    static HashMap<String, ICalendar> usersCalendars = new HashMap<>();
    static HashMap<String, Dataset> userDatasets = new HashMap<>(); // <userID, Dataset>

    private static String TDBDatasetPath = "./res/tdb-storage/";
    static String datasetURI = "https://www.smartcontacts.com/ontology";
    static String NS = datasetURI + "#";

    public static void main(String[] args) {

        //Valori di test
        users.add("Ciccio");
        users.add("Edoardo");
        users.add("Enrico");
        users.add("Federico");
        users.add("Giulia");

        //Inserisce nel dataset dell'utente tre modelli con nome "Contacts", "Profile" e "Agenda" a cui si può fare riferimento
        for (String user : users) {

            Dataset dataset = TDBFactory.createDataset(TDBDatasetPath + user + "/");
            //Apertura dei dataset creati nell'homework 2
            Model modelContacts = RDFDataMgr.loadModel("./res/" + user + "/contacts.nt");
            Model modelProfile = RDFDataMgr.loadModel("./res/" + user + "/profile.nt");
            //Caricamento nel TDB storage dei dataset in input
            dataset.addNamedModel("Contacts", modelContacts);
            dataset.addNamedModel("Profile", modelProfile);

            ICalendar ical = new ICalendar();
            ical.setUid(user);

            Calendar c1 = GregorianCalendar.getInstance();
            c1.set(2018, Calendar.JANUARY,1);

            VEvent event = new VEvent();
            event.setSummary("Test event");
            event.setDescription("Test Event for user " + user);
            event.setDateStart(c1.getTime());
            event.setUid(UUID.randomUUID().toString());

            ical.addEvent(event);

            usersCalendars.put(user, ical);

            Model agenda = ModelFactory.createDefaultModel();
            Resource subject = agenda.createResource(user);
            Property predicate = agenda.createProperty(NS, "hasCalendar");
            Resource object = agenda.createResource(ical.getUid().getValue());
            //Aggiunta tripla del calendar nel TDB
            agenda.add(subject, predicate, object);

            try (PrintWriter writer = new PrintWriter("./res/" + user + "/" + ical.getUid().getValue() + ".ics", "UTF-8")) {

                ical.write(writer);

            } catch (Exception e) {
                e.printStackTrace();
            }

            dataset.addNamedModel("Agenda", agenda);
            userDatasets.put(user, dataset);
            TDB.sync(userDatasets.get(user));
            smartAgents.put(user.toLowerCase(), new SmartAgent(user, dataset));
        }

        //Scelta organizzatore, casuale
        SmartAgent organizer = new ArrayList<>(smartAgents.values()).get(new Random().nextInt(smartAgents.size()));

        Calendar c1 = GregorianCalendar.getInstance();
        c1.set(2017, Calendar.MAY,5);

        ICalendar pizzata = organizer.createOrganisedEvent(smartAgents, c1.getTime());

        try (PrintWriter writer = new PrintWriter("./" + pizzata.getUid().getValue() + ".ics", "UTF-8")) {

            pizzata.write(writer);

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}
