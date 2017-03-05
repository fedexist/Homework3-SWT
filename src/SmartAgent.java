import biweekly.Biweekly;
import biweekly.ICalendar;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class SmartAgent {

    private String delegateID;

    //Raw dataset
    private Dataset ContactsProfileAgenda;

    //Needed Info, generated at construction from ContactsProfileAgenda
    private ArrayList<Resource> contacts;


    private ArrayList< Pair<Property, RDFNode> > personalPreferences;
    private HashMap<String, Pair<Property, RDFNode> > contactsPreferences; // <contactID, Preferences>
    private HashMap<String, ICalendar> agenda; // <meetingID, ICalObject>

    private ArrayList<Pizzeria> pizzerias = new ArrayList<>();


    public SmartAgent(String ID, Dataset dataset) {

        delegateID = ID;
        ContactsProfileAgenda = dataset;
        contacts = new ArrayList<>();
        personalPreferences = new ArrayList<>();
        contactsPreferences = new HashMap<>();
        agenda = new HashMap<>();

        generatePersonalInfo();

    }

    private void generatePersonalInfo() {

        //Contacts: from dataset
        Model model = ContactsProfileAgenda.getNamedModel("Contacts");
        StmtIterator stmtIt = model.listStatements(null, RDF.type, FOAF.Person);

        for( ; stmtIt.hasNext(); ){

           Resource currentSubject = stmtIt.next().getSubject();
           contacts.add(currentSubject);

        }

        //Personal preferences: from dataset
        //Profile contains only personal preferences (otherwise there should be an apt query)
        model = ContactsProfileAgenda.getNamedModel("Profile");
        stmtIt = model.listStatements();

        for( ; stmtIt.hasNext(); ){

            Statement currentstmt = stmtIt.next();
            personalPreferences.add( new Pair<>(currentstmt.getPredicate(), currentstmt.getObject()));

        }

        //Agenda
        model = ContactsProfileAgenda.getNamedModel("Agenda");
        Property meetingID = model.createProperty("https://www.smartcontacts.com/ontology#hasCalendar");
        NodeIterator nodeIt = model.listObjectsOfProperty(meetingID);
        for( ; nodeIt.hasNext(); ){

            RDFNode currentNode = nodeIt.next();
            ICalendar ical = retrieveICal(currentNode.toString());
            if(ical != null)
                agenda.put(currentNode.toString(), retrieveICal(currentNode.toString()));

        }

    }

    private ICalendar retrieveICal(String ICalUID) {

        FileManager fm = FileManager.get();
        InputStream icalFile = fm.open("./res/"+ delegateID + "/" + ICalUID + ".ics");
        ICalendar iCalendar = null;
        try{
            iCalendar = Biweekly.parse(icalFile).first();
        } catch(IOException e){
            e.printStackTrace();
        }

        return iCalendar;
    }

    //Learning contacts preferences
    private void fillContactsPreferences(ArrayList<SmartAgent> contacts) {

        for (SmartAgent participant: contacts) {
            if(!participant.equals(this)){
                ArrayList< Pair<Property, RDFNode> > partecipantPrefs = participant.getPersonalPreferences();
                for (Pair<Property, RDFNode> element: partecipantPrefs)
                    contactsPreferences.put(participant.getPersonalID(), element);
            }
        }
    }

    private void importPizzerias(){

        Scanner scanner = new Scanner(FileManager.get().open("./res/PizzaGiuseppe.csv"));
        scanner.nextLine();

        while (scanner.hasNextLine()) {
            String[] data = scanner.nextLine().split(",");

            Pizzeria pizzeriaTemp = new Pizzeria(data[0], data[1].equals("yes"));
            pizzeriaTemp.pizzeDellaCasa.addAll(Arrays.asList(data).subList(2, data.length));
            pizzerias.add(pizzeriaTemp);

        }
        scanner.close();

    }

    public String getPersonalID() {
        return delegateID;
    }
    public ArrayList< Pair<Property, RDFNode> > getPersonalPreferences() {
        return personalPreferences;
    }

    public ICalendar createOrganisedEvent(ArrayList<SmartAgent> partecipants){

        fillContactsPreferences(partecipants);
        importPizzerias();

        //Scorre il db delle pizzerie

        //Per ogni pizzeria controlla le proprie preferenze e quelle dei contatti

        //Ritorna un Ical con l'evento programmato e i contatti partecipanti
        return null;
    }

}
