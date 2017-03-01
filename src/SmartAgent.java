import biweekly.ICalendar;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.HashMap;

public class SmartAgent {

    private String delegateID;

    //Raw dataset
    private Dataset ContactsProfileAgenda;

    //Needed Info, generated at construction from ContactsProfileAgenda
    private ArrayList<String> contacts;


    private ArrayList<Pair<Property, Resource>> personalPreferences;
    private HashMap<String, Pair<Property, Resource> > contactsPreferences; // <contactID, Preferences>
    private HashMap<String, ICalendar> agenda; // <meetingID, ICalObject>

    public SmartAgent(String ID, Dataset dataset) {

        delegateID = ID;
        ContactsProfileAgenda = dataset;

        generatePersonalInfo();

    }

    private void generatePersonalInfo() {

        //Contacts: from dataset
        Model model = ContactsProfileAgenda.getDefaultModel();
        StmtIterator stmtIt = model.listStatements(null, RDF.type, FOAF.Person);

        for( ; stmtIt.hasNext(); ){

            Statement currentstmt = stmtIt.next();
            contacts.add(currentstmt.getSubject().toString());

        }


        //Personal preferences: from dataset

        //Contacts Preferences: Contacts -> Contacts' smart agents -> Contacts' Preferences

    }

    public String getPersonalID() {
        return delegateID;
    }
    public ArrayList<Pair<Property, Resource>> getPersonalPreferences() {
        return personalPreferences;
    }

    public ICalendar createOrganisedEvent(){

        return null;
    }

}
