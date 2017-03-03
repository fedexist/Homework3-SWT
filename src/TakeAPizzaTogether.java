import biweekly.ICalendar;
import biweekly.component.VEvent;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;

import java.io.*;
import java.util.*;

public class TakeAPizzaTogether {

    static ArrayList<String> users = new ArrayList<>();
    static ArrayList<SmartAgent> smartAgents = new ArrayList<>();

    static HashMap<String, ICalendar> usersCalendars = new HashMap<>();
    static HashMap<String, Dataset> userDatasets = new HashMap<>(); // <userID, Dataset>

    private static String TDBDatasetPath = "./res/tdb-storage/";
    static String datasetURI = "https://www.smartcontacts.com/ontology";
    static String NS = datasetURI + "#";

    static ArrayList<Pizzeria> pizzerias = new ArrayList<>();

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
            smartAgents.add(new SmartAgent(user, dataset));
        }

        /*
        for(SmartAgent smartAgent : smartAgents)
            smartAgent.fillContactsPreferences(smartAgents);
        */

        //Scelta organizzatore, casuale
        SmartAgent organizer = smartAgents.get(new Random().nextInt(smartAgents.size()));

        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileReader("./res/PizzaGiuseppe.csv"));
        } catch(Exception e) {
            e.printStackTrace();
        }
        scanner.nextLine();

        boolean cont = true;
        while (cont) {
            String pizzeriaData = scanner.nextLine();
            String[] data = pizzeriaData.split(",");
            if(!data[0].equals(";")) {
                Pizzeria pizzeriaTemp = new Pizzeria(data[0], data[1].equals("yes"));
                for (int i = 2; i < data.length; i++) {
                    pizzeriaTemp.pizzeDellaCasa.add(data[i]);
                    //System.out.println(pizzeriaTemp.pizzeDellaCasa);
                }
                pizzerias.add(pizzeriaTemp);
            }
            else cont = false;
        }
        scanner.close();

        for (Pizzeria pizzeria : pizzerias)
        {
            System.out.println(pizzeria.pizzeDellaCasaForDBPedia());
        }

        for (Pizzeria pizzeria : pizzerias)
        {
            System.out.println(pizzeria.pizzeDellaCasaForPizzaOwl());
        }


        ICalendar pizzata = organizer.createOrganisedEvent(smartAgents);

        /*
        try (PrintWriter writer = new PrintWriter("./" + pizzata.getUid().getValue() + ".ics", "UTF-8")) {

            pizzata.write(writer);

        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
}
