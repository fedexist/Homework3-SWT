import biweekly.Biweekly;
import biweekly.ICalendar;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SmartAgent {

    private String delegateID;

    //Raw dataset
    private Dataset ContactsProfileAgenda;

    //Needed Info, generated at construction from ContactsProfileAgenda
    private ArrayList<String> contacts;


    private ArrayList< Pair<Property, RDFNode> > personalPreferences;
    private HashMap<String, ArrayList< Pair<Property, RDFNode> > > contactsPreferences; // <contactID, Preferences>
    private HashMap<String, ICalendar> agenda; // <meetingID, ICalObject>

    HashMap<Resource, OWLsameAs> equivalentResources = new HashMap<>();

    private ArrayList<Pizzeria> pizzerias = new ArrayList<>();
    private HashMap<Pizzeria, ArrayList< Pair< String, ArrayList< String > > > > ContactPizzerie = new HashMap<>();


    private String findPizzasIngredients =
            "PREFIX dbo: <http://dbpedia.org/ontology/>" +
            "PREFIX dbr: <http://dbpedia.org/resource/>" +
            "SELECT ?pizza ?ingredient WHERE {" +
            " ?pizza dbo:type dbr:Pizza" +
            " . ?pizza dbo:Ingredient ?pizza" +
            "}";


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
           contacts.add(currentSubject.toString());

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
            //if(!participant.equals(this)){
                contactsPreferences.put(participant.getPersonalID(), participant.getPersonalPreferences());
            //}
        }

        //Insert own preferences
        //contactsPreferences.put(delegateID, personalPreferences);
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

    private Set<String> pizzasLikedByContact(String contact){

        Set<String> Pizzas = new HashSet<>(1);

        for( Pair<Property, RDFNode> preference : contactsPreferences.get(contact) ){
            
        }

        return Pizzas;
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

        //Find equivalent objects with owl:sameAs property
        OntModel pizzaOntology = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        pizzaOntology.read(FileManager.get().open("./res/mypizza.owl"), "");
        StmtIterator stmtIterator = pizzaOntology.listStatements();
        Property sameAs = pizzaOntology.getAnnotationProperty("http://www.w3.org/2002/07/owl#sameAs");

        while(stmtIterator.hasNext()){

            Statement currentStatement = stmtIterator.nextStatement();

            if(currentStatement.getPredicate().equals(sameAs)){

                OWLsameAs currentEquivalentClass = equivalentResources.get(currentStatement.getSubject());
                if( currentEquivalentClass == null){
                    currentEquivalentClass = new OWLsameAs(currentStatement.getSubject());
                    equivalentResources.put(currentStatement.getSubject(), currentEquivalentClass);
                }
                currentEquivalentClass.add(currentStatement.getObject());

            }

        }
        for(Pizzeria pizzeria : pizzerias){
            Set<String> pizzas = new HashSet<>(pizzeria.pizzeDellaCasa);
            for(String contact : contacts) {
                Set<String> intersection = new HashSet<>(pizzas); // use the copy constructor
                intersection.retainAll( pizzasLikedByContact(contact) );

                if(!intersection.isEmpty()){
                    ArrayList<String> containedPizzas = new ArrayList<>(intersection);

                    ArrayList< Pair <String, ArrayList<String>> > currentPizzeria = ContactPizzerie.get(pizzeria);
                    if(currentPizzeria == null)
                        currentPizzeria = new ArrayList<>();
                    currentPizzeria.add(new Pair<>(contact, containedPizzas));
                }
            }
        }

        //Scorre il db delle pizzerie

        //Per ogni pizzeria controlla le proprie preferenze e quelle dei contatti

        //Ritorna un Ical con l'evento programmato e i contatti partecipanti
        return null;
    }

}
