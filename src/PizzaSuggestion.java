import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.text.ICalReader;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PizzaSuggestion {

    static ArrayList<String> users = new ArrayList<>();
    static HashMap<String, SmartAgent> smartAgents = new HashMap<>();

    static HashMap<String, ICalendar> usersCalendars = new HashMap<>();
    static HashMap<String, Dataset> userDatasets = new HashMap<>(); // <userID, Dataset>

    private static String TDBDatasetPath = "./res/tdb-storage/";
    static String datasetURI = "https://www.smartcontacts.com/ontology";
    static String NS = datasetURI + "#";

    public static void main(String[] args) throws IOException {

        users.add("Ciccio");
        users.add("Edoardo");
        users.add("Enrico");
        users.add("Federico");
        users.add("Giulia");

        for (String user : users) {

            Dataset dataset = TDBFactory.createDataset(TDBDatasetPath + user + "/");
            //Apertura dei dataset creati nell'homework 2
            Model modelContacts = RDFDataMgr.loadModel("./res/" + user + "/contacts.nt");
            Model modelProfile = RDFDataMgr.loadModel("./res/" + user + "/profile.nt");
            //Caricamento nel TDB storage dei dataset in input
            dataset.addNamedModel("Contacts", modelContacts);
            dataset.addNamedModel("Profile", modelProfile);

            userDatasets.put(user, dataset);
            TDB.sync(userDatasets.get(user));
            smartAgents.put(user.toLowerCase(), new SmartAgent(user, dataset));
        }

        InputStream pizzataCalendar = FileManager.get().open("PizzataTraAmici.ics");
        ICalReader reader = new ICalReader(pizzataCalendar);
        ICalendar iCalendar = null;

        try{
            iCalendar = reader.readNext();

        } finally {
            reader.close();
        }

        List<VEvent> pizzataEvents = iCalendar.getEvents();

        for(SmartAgent smartAgent : smartAgents.values())
            smartAgent.settleAdviceFromPizzaEvent(pizzataEvents.get(0));
    }

}
