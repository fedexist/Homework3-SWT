import biweekly.Biweekly;
import biweekly.ICalendar;
import org.apache.jena.base.Sys;
import org.apache.jena.ontology.*;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SmartAgent {

    private String delegateID;
    private String delegateURI;

    //Raw dataset
    private Dataset ContactsProfileAgenda;

    //Needed Info, generated at construction from ContactsProfileAgenda
    private ArrayList<String> contacts;


    private ArrayList< Pair<Property, RDFNode> > personalPreferences;
    private HashMap<String, ArrayList< Pair<Property, RDFNode> > > contactsPreferences; // <contactID, Preferences>
    private HashMap<String, ICalendar> agenda; // <meetingID, ICalObject>

    private HashMap< Resource, OWLsameAs > equivalentProperties = new HashMap<>();
    private HashMap< Resource, OWLsameAs > equivalentObjects = new HashMap<>();

    private ArrayList<Pizzeria> pizzerias = new ArrayList<>();

    private OntModel pizzaOntology = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
    private HashMap<String, ArrayList<String>> PizzaIngredients = new HashMap<>();

    //Map to link to each pizzeria a list of contacts who like the venue and the list of pizzas said person would eat there.
    private HashMap<Pizzeria, ArrayList< Pair< String, ArrayList< String > > > > ContactPizzerie = new HashMap<>();


    private String findDBPediaPizzaIngredients =
            "PREFIX dbo: <http://dbpedia.org/ontology/>" +
            "PREFIX dbr: <http://dbpedia.org/resource/>" +
            "SELECT ?pizza ?ingredient WHERE {" +
            " ?pizza dbo:type dbr:Pizza" +
            " . ?pizza dbo:Ingredient ?ingredient" +
            "}";


    public SmartAgent(String ID, Dataset dataset) {

        delegateID = ID;
        delegateURI = "https://fleanend.github.io/ontology/" + delegateID.toLowerCase();
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

        //contactsPreferences contain both contacts and personal preferences
        for (SmartAgent participant: contacts)
            contactsPreferences.put(participant.getPersonalURI(), participant.getPersonalPreferences());
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


    private boolean hasCeliacDisease(String contact)
    {

        ArrayList<String> dislikedIngredients = new ArrayList<>();
        ArrayList<OWLsameAs> properties = new ArrayList<>(equivalentProperties.values());

        for( Pair<Property, RDFNode> preference : contactsPreferences.get(contact) ) {

            OWLsameAs prop = new OWLsameAs(preference.first.asResource());
            OWLsameAs object = new OWLsameAs(preference.second.asResource());

            if (properties.contains(prop)) {

            } else dislikedIngredients.add(preference.second.asResource().getLocalName());
        }

        for (String ingredient : dislikedIngredients)
        {
            if (ingredient.equals("NormalDough"))
            {
                return true;
            }
        }
        return false;
    }



    //finds a set of pizzas as string liked by the contact as Pizza.Owl urls
    private Set<String> pizzasLikedByContact(String contact){

        Set<String> Pizzas = new HashSet<>(1);
        ArrayList<String> dislikedIngredients = new ArrayList<>();
        ArrayList<OWLsameAs> properties = new ArrayList<>(equivalentProperties.values());
        //find properties sameAs

        //find object sameAs

        //for each preference

        for( Pair<Property, RDFNode> preference : contactsPreferences.get(contact) ){

            OWLsameAs prop = new OWLsameAs(preference.first.asResource());
            OWLsameAs object = new OWLsameAs(preference.second.asResource());

            //For simplicity we hypothesise only positive properties can have an owl:sameAs predicate

            //If current property has an owl:sameAs equivalent then
            //its object is checked for an owl:sameAs equivalent
            //if the object has no equivalent, it gets added to the preferences
            //else its main reference gets added
            if(properties.contains( prop )){
                if(equivalentObjects.values().contains(object))
                    for(Map.Entry<Resource, OWLsameAs> entry : equivalentObjects.entrySet()){
                        if(entry.getValue().equals(object)){
                            Pizzas.add(entry.getKey().getLocalName());
                            break;
                        }
                    }

                else Pizzas.add(preference.second.asResource().getLocalName());

            } else dislikedIngredients.add(preference.second.asResource().getLocalName());



            /* if (propertyOrAlias is smartcontacts:lovesPizza){

                    if(objectOrAlias is in DBPedia or Pizza.owl)
                        Pizzas.add(object.AsPizzaOwl())
                }
                else if (propertyOrAlias is smartcontacts:hatesFood or smartcontacts:isAllergic){

                    unlikedIngredients.add(object.AsPizzaOwl())
                }
            */
        }

        for (Map.Entry<String, ArrayList<String>> entry : PizzaIngredients.entrySet()) {
            boolean ingredientNotLiked = false; //set to 1 to come out of the double loop
            for (String ingredient : entry.getValue()) {
                for (String dislikedIngredient : dislikedIngredients) {
                    //System.out.println(unlikedIngredient + " " + ingredient);
                    if (ingredient.equals(dislikedIngredient)) {
                        //System.out.println(unlikedIngredient + " not liked " + pizzaAndIngredients.getKey() + " sucks");
                        ingredientNotLiked = true;
                        break;
                    }

                }
                if (ingredientNotLiked)
                    break;
            }
            if (!ingredientNotLiked)
                Pizzas.add(entry.getKey());

            //it.remove(); // avoids a ConcurrentModificationException
        }


        //System.out.println(contact + " " + dislikedIngredients + " " + Pizzas);

        return Pizzas;
    }

    public String getPersonalURI() {
        return delegateURI;
    }
    public ArrayList< Pair<Property, RDFNode> > getPersonalPreferences() {
        return personalPreferences;
    }

    public ICalendar createOrganisedEvent(ArrayList<SmartAgent> participants){

        fillContactsPreferences(participants);
        importPizzerias();

        //Find equivalent objects with owl:sameAs property

        pizzaOntology.read(FileManager.get().open("./res/mypizza.owl"), "");
        equivalentResourcesRetrieval();
        findPizzasIngredients();

        //System.out.println( PizzaIngredients.size());

        //for each pizzeria fills the list of contacts willing to dine there and the set of pizzas they wish to eat
        for(Pizzeria pizzeria : pizzerias){
            Set<String> pizzas = new HashSet<>(pizzeria.pizzeDellaCasaForPizzaOwl());
            for(String contact : contacts) {
                Set<String> intersection = new HashSet<>(pizzas); // use the copy constructor
                intersection.retainAll( pizzasLikedByContact(contact) );

                //If pizzeria lacks celiac food, and contact is celiac, go away
                if (!pizzeria.baseCeliaci && hasCeliacDisease(contact))
                {
                    continue;
                }
                //if the intersection isn't empty, that is there's at least a pizza liked by the current contact
                //then I add these pizzas to the appropriate slot in ContactPizzerie
                if(!intersection.isEmpty()){
                    ArrayList<String> containedPizzas = new ArrayList<>(intersection);

                    ArrayList< Pair <String, ArrayList<String>> > currentPizzeria = ContactPizzerie.get(pizzeria);
                    if(currentPizzeria == null){
                        currentPizzeria = new ArrayList<>();
                        ContactPizzerie.put(pizzeria, currentPizzeria);
                    }

                    currentPizzeria.add(new Pair<>(contact, containedPizzas));
                }
            }
        }

        int bestPizzeriaIndex = 2;
        for (int i = 0; i < pizzerias.size();i++) {

            int candPizzeria = ContactPizzerie.get(pizzerias.get(i)).size();
            int bestPizzeria = ContactPizzerie.get(pizzerias.get(bestPizzeriaIndex)).size();
            if (candPizzeria > bestPizzeria)
                bestPizzeriaIndex = i;

        }

        System.out.println("La pizzeria scelta è " + pizzerias.get(bestPizzeriaIndex).name);
        //Scorre il db delle pizzerie

        //Per ogni pizzeria controlla le proprie preferenze e quelle dei contatti

        //Ritorna un Ical con l'evento programmato e i contatti partecipanti
        return null;
    }

    // Here are checked all the statements with an owl:sameAs property,
    // ObjectProperties and Classes are separated, as it comes handy for their access
    private void equivalentResourcesRetrieval(){

        StmtIterator stmtIterator = pizzaOntology.listStatements(null, OWL.sameAs, (RDFNode) null);

        while(stmtIterator.hasNext()){

            Statement currentStatement = stmtIterator.nextStatement();

            if(currentStatement.getSubject().canAs(ObjectProperty.class)){

                OWLsameAs currentEquivalentClass = equivalentProperties.get(currentStatement.getSubject().as(ObjectProperty.class));
                if( currentEquivalentClass == null){
                    currentEquivalentClass = new OWLsameAs(currentStatement.getSubject().asResource());
                    equivalentProperties.put(currentStatement.getSubject(), currentEquivalentClass);
                }
                currentEquivalentClass.add(currentStatement.getObject());

            } else {

                OWLsameAs currentEquivalentClass = equivalentObjects.get(currentStatement.getSubject());
                if( currentEquivalentClass == null){
                    currentEquivalentClass = new OWLsameAs(currentStatement.getSubject());
                    equivalentObjects.put(currentStatement.getSubject(), currentEquivalentClass);
                }
                currentEquivalentClass.add(currentStatement.getObject());
            }
        }

    }

    //Here are retrieved all the ingredients for all the possible pizzas in both mypizza.owl and DBPedia
    //Ingredients, in mypizza.owl are restriction on the property hasTopping of the superclasses of each NamedPizza
    //This restriction is retrieved as an AllValuesFromRestriction and its operands are retrieved
    private void findPizzasIngredients(){

        //Ingredients from all the pizzas in pizza.owl, put into PizzaIngredients
        OntClass namedPizza = pizzaOntology.getOntClass("http://www.co-ode.org/ontologies/pizza/pizza.owl#NamedPizza");
        Property hasTopping = pizzaOntology.getOntProperty("http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping");
        Iterator<OntClass> namedPizzaIt = namedPizza.listSubClasses();

        while(namedPizzaIt.hasNext()){

            OntClass currentNamedPizza = namedPizzaIt.next();
            Iterator<OntClass> currentNamedPizzaIt = currentNamedPizza.listSuperClasses();

            PizzaIngredients.put(currentNamedPizza.getLocalName(), new ArrayList<String>());

            while(currentNamedPizzaIt.hasNext()){
                OntClass c = currentNamedPizzaIt.next();
                if(c.isRestriction()){
                    Restriction r = c.asRestriction();

                    if(r.isAllValuesFromRestriction() && r.getOnProperty().equals(hasTopping)){


                        OntClass restrictedClass = (OntClass) r.asAllValuesFromRestriction().getAllValuesFrom();
                        RDFList disj = restrictedClass.asUnionClass().getOperands();

                        for(RDFNode node : disj.asJavaList()){

                            PizzaIngredients.get(currentNamedPizza.getLocalName())
                                    .add(node.asResource().getLocalName());
                        }
                    }

                }
            }
        }
    }

}
