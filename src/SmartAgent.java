import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Attendee;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.tdb.TDB;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SmartAgent {

    private String delegateID;
    private String delegateURI;
    private String delegateEmail;

    //Raw dataset
    private Dataset ContactsProfileAgenda;

    //Needed Info, generated at construction from ContactsProfileAgenda
    private ArrayList<String> contacts;


    private ArrayList< Pair<Property, RDFNode> > personalPreferences;
    private HashMap<String, ArrayList< Pair<Property, RDFNode> > > contactsPreferences; // <contactID, Preferences>
    private HashMap<String, ICalendar> agenda; // <meetingID, ICalObject>
    private HashMap<String, String> menuSuggestions; // <contactID, namePizza>

    private HashMap< Resource, OWLsameAs > equivalentProperties = new HashMap<>();
    private HashMap< Resource, OWLsameAs > equivalentObjects = new HashMap<>();

    private HashMap< Resource, OWLsameAs > ontologyToPizzeria = new HashMap<>();

    private ArrayList<Pizzeria> pizzerias = new ArrayList<>();

    private OntModel pizzaOntology = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
    private HashMap<String, ArrayList<String>> PizzaIngredients = new HashMap<>();

    //Map to link to each pizzeria a list of contacts who like the venue and the list of pizzas said person would eat there.
    private HashMap<Pizzeria, ArrayList< Pair< String, ArrayList< String > > > > ContactPizzerie = new HashMap<>();

    public SmartAgent(String ID, Dataset dataset) {

        delegateID = ID;
        delegateURI = "https://fleanend.github.io/ontology/" + delegateID.toLowerCase();
        delegateEmail = delegateID.toLowerCase() + "@gmail.com";
        ContactsProfileAgenda = dataset;
        contacts = new ArrayList<>();
        personalPreferences = new ArrayList<>();
        contactsPreferences = new HashMap<>();
        menuSuggestions = new HashMap<>();
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

        Scanner scanner = new Scanner(FileManager.get().open("./res/IngredientiPizzePizzerie.csv"));
        scanner.nextLine();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] data = line.split(",");

            //System.out.println(line);

            Pizzeria pizzeriaTemp = new Pizzeria(data[0], data[1].equals("yes"));
            if(!pizzerias.contains(pizzeriaTemp))
                pizzerias.add(pizzeriaTemp);

            pizzerias.get(pizzerias.indexOf(pizzeriaTemp))
                    .pizzeDellaCasa.put(data[2], new ArrayList<>(Arrays.asList(data).subList(3, data.length)));

        }
        scanner.close();
    }

    private void importPizzeria(String pizzeria){

        Scanner scanner = new Scanner(FileManager.get().open("./res/IngredientiPizzePizzerie.csv"));
        scanner.nextLine();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] data = line.split(",");

            //System.out.println(line);
            if(!data[0].equals(pizzeria))
                return;

            Pizzeria pizzeriaTemp = new Pizzeria(data[0], data[1].equals("yes"));
            if(!pizzerias.contains(pizzeriaTemp))
                pizzerias.add(pizzeriaTemp);

            pizzerias.get(pizzerias.indexOf(pizzeriaTemp))
                    .pizzeDellaCasa.put(data[2], new ArrayList<>(Arrays.asList(data).subList(3, data.length)));

        }
        scanner.close();
    }

    private void findSameIngredientsPizzas(){

        for( Pizzeria pizzeria: pizzerias) {
            for(Map.Entry<String, ArrayList<String>> pizzaFromPizzeria : pizzeria.pizzeDellaCasa.entrySet()) {

                for(Map.Entry<String, ArrayList<String>> pizzaFromOntology : PizzaIngredients.entrySet()) {

                    ArrayList <String> ingredientsFromPizzeria = pizzaFromPizzeria.getValue();
                    ArrayList <String> ingredientsFromOntology = pizzaFromOntology.getValue();

                    //System.out.println(ingredientsFromPizzeria);

                    //if the Pizzeria's Pizza has all and only the ingredients from the Ontology's Pizza, they are the same Pizza.
                    if(equalIngredients(ingredientsFromOntology,ingredientsFromPizzeria)) {

                        //System.out.println(pizzaFromPizzeria.getKey() + " equals " + pizzaFromOntology.getKey());

                        Resource pizzaPizzeriaRes = pizzaOntology.createResource("http://www.co-ode.org/ontologies/pizza/pizza.owl/#" + pizzaFromPizzeria.getKey());
                        Resource pizzaOntologyRes = pizzaOntology.createResource("http://www.co-ode.org/ontologies/pizza/pizza.owl/#" + pizzaFromOntology.getKey());

                        if (ontologyToPizzeria.get(pizzaOntologyRes) != null)
                            ontologyToPizzeria.get(pizzaOntologyRes).add(pizzaPizzeriaRes);
                        else {
                            OWLsameAs pizzaEquivalent = new OWLsameAs(pizzaOntologyRes);
                            pizzaEquivalent.add(pizzaPizzeriaRes);
                            ontologyToPizzeria.put(pizzaOntologyRes,pizzaEquivalent);
                        }
                    }
                }
            }
        }
    }

    private boolean equalIngredients(ArrayList<String> ingredientsA, ArrayList<String> ingredientsB) {

        ArrayList<String> ingredientsAPolished = new ArrayList<>();
        ArrayList<String> ingredientsBPolished = new ArrayList<>();



        for(String ingredientFromA : ingredientsA) {
            if(ingredientFromA.equals("-") || ingredientFromA.equals("flour"))
                continue;

            ingredientsAPolished.add(ingredientFromA.toLowerCase().replace("topping","").replace("sauce","").replace(" ",""));
        }
        for(String ingredientFromB : ingredientsB) {
            if(ingredientFromB.equals("-") || ingredientFromB.equals("flour"))
                continue;

            ingredientsBPolished.add(ingredientFromB.toLowerCase().replace("topping","").replace("sauce","").replace(" ",""));
        }


        return ingredientsAPolished.containsAll(ingredientsBPolished);

    }

    private boolean hasCeliacDisease(String contact) {

        ArrayList<String> dislikedIngredients = new ArrayList<>();
        ArrayList<OWLsameAs> properties = new ArrayList<>(equivalentProperties.values());

        for( Pair<Property, RDFNode> preference : contactsPreferences.get(contact) ) {

            OWLsameAs prop = new OWLsameAs(preference.first.asResource());

            if (!properties.contains(prop))
                dislikedIngredients.add(preference.second.asResource().getLocalName());

        }

        return dislikedIngredients.contains("NormalDough");

    }

    //finds a set of pizzas as string liked by the contact as Pizza.Owl urls
    private Set<String> pizzasLikedByContact(String contact){

        Set<String> Pizzas = new HashSet<>(1);
        ArrayList<String> dislikedIngredients = new ArrayList<>();
        ArrayList<OWLsameAs> properties = new ArrayList<>(equivalentProperties.values());

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
                            Pizzas.addAll(entry.getValue().equivalentLocals());
                            break;
                        }
                    }
                else if(ontologyToPizzeria.values().contains(object)){

                    //System.out.println("ontologytopizzeria contains " + object);

                    for(Map.Entry<Resource, OWLsameAs> entry : ontologyToPizzeria.entrySet()){
                        if(entry.getValue().equals(object)){
                            Pizzas.addAll(entry.getValue().equivalentLocals());
                            break;
                        }
                    }

                }

                else Pizzas.add(preference.second.asResource().getLocalName());

            } else dislikedIngredients.add(preference.second.asResource().getLocalName());

        }

        for (Map.Entry<String, ArrayList<String>> entry : PizzaIngredients.entrySet()) {
            boolean ingredientNotLiked = false; //set to 1 to come out of the double loop
            for (String ingredient : entry.getValue()) {
                if(dislikedIngredients.contains(ingredient)){
                    ingredientNotLiked = true;
                    break;
                }
            }
            if (!ingredientNotLiked)
                Pizzas.add(entry.getKey());
        }


        //System.out.println(contact + " likes " + Pizzas);

        return Pizzas;
    }

    public String getPersonalURI() {
        return delegateURI;
    }

    public ArrayList< Pair<Property, RDFNode> > getPersonalPreferences() {
        return personalPreferences;
    }

    public ICalendar createOrganisedEvent(HashMap<String, SmartAgent> participants, Date date){

        //System.out.println(delegateID + " is the organizer.");

        fillContactsPreferences(new ArrayList<>(participants.values()));
        importPizzerias();

        //Find equivalent objects with owl:sameAs property

        pizzaOntology.read(FileManager.get().open("./res/mypizza.owl"), "");
        equivalentResourcesRetrieval();
        findPizzasIngredients();
        findSameIngredientsPizzas();

/*
        for(Map.Entry<Resource, OWLsameAs> entry : ontologyToPizzeria.entrySet()) {
            System.out.print(entry.getValue() + "\n");
        }
*/
        //System.out.println( PizzaIngredients.size());

        //for each pizzeria fills the list of contacts willing to dine there and the set of pizzas they wish to eat
        for(Pizzeria pizzeria : pizzerias){
            Set<String> pizzas = new HashSet<>(pizzeria.pizzeDellaCasaForPizzaOwl());
            for(String contact : contactsPreferences.keySet()) {
                Set<String> intersection = new HashSet<>(pizzas); // use the copy constructor
                intersection.retainAll( pizzasLikedByContact(contact) );

                //If pizzeria lacks celiac food, and contact is celiac, go away
                if (!pizzeria.baseCeliaci && hasCeliacDisease(contact))
                    continue;

                //if the intersection isn't empty, that there's at least a pizza liked by the current contact
                //then I add these pizzas to the appropriate slot in ContactPizzerie
                if(!intersection.isEmpty()){
                    ArrayList<String> containedPizzas = new ArrayList<>(intersection);

                    //System.out.println(contact + " likes " + containedPizzas + " in " + pizzeria.name);

                    ArrayList< Pair <String, ArrayList<String>> > currentPizzeria = ContactPizzerie.get(pizzeria);
                    if(currentPizzeria == null){
                        currentPizzeria = new ArrayList<>();
                        ContactPizzerie.put(pizzeria, currentPizzeria);
                    }

                    currentPizzeria.add(new Pair<>(contact, containedPizzas));
                }
            }
        }

        int bestPizzeriaIndex = 0;
        for (int i = 1; i < pizzerias.size();i++){

            int candPizzeria = ContactPizzerie.get(pizzerias.get(i)).size();
            int bestPizzeria = ContactPizzerie.get(pizzerias.get(bestPizzeriaIndex)).size();
            if (candPizzeria > bestPizzeria)
                bestPizzeriaIndex = i;

        }

        Property mightlike = ResourceFactory.createProperty("http://www.smartcontacts.com/ontology#mightLike");

        ArrayList<OWLsameAs> properties = new ArrayList<>(equivalentProperties.values());
        for (Pair< String, ArrayList< String > > entry: ContactPizzerie.get(pizzerias.get(bestPizzeriaIndex))) {
            String person = entry.first;
            Set<String> pizzaCandidates = new HashSet<>(entry.second);
            //System.out.println(person + " may like " + pizzaCandidates);
            //System.out.println(person + "\nPizza candidates: " + pizzaCandidates + "\n");

            Set<String> likedPizzas = new HashSet<>();
            for (Pair<Property, RDFNode> preference : contactsPreferences.get(person)) {
                OWLsameAs prop = new OWLsameAs(preference.first.asResource());

                if (properties.contains(prop)) {
                    if(ontologyToPizzeria.get(preference.second.asResource()) != null)
                        likedPizzas.addAll( ontologyToPizzeria.get(preference.second.asResource()).equivalentLocals() );
                    else
                        likedPizzas.add(preference.second.asResource().getLocalName());
                }
            }
            //System.out.println("Liked pizzas: " + likedPizzas + "\n");

            pizzaCandidates.retainAll(likedPizzas);
            ArrayList<String> pizzas = new ArrayList<>(pizzaCandidates);

            //System.out.println("Def candidates: " + pizzas + "\n\n");
            if(!pizzas.isEmpty())
                menuSuggestions.put(person, pizzas.get(new Random().nextInt(pizzas.size())));
            else {

                String suggestion = entry.second.get(new Random().nextInt(entry.second.size()));
                menuSuggestions.put(person, suggestion );
                Resource object = ResourceFactory.createResource("http://www.co-ode.org/ontologies/pizza/pizza.owl/#" + suggestion);
                participants.get(person.substring(36, person.length())).updateProfile(mightlike, object);

            }

        }
        //System.out.println("La pizzeria scelta è " + pizzerias.get(bestPizzeriaIndex).name + "\nIl menù che consigliamo è : " + menuSuggestions);

        //Scorre il db delle pizzerie

        //Per ogni pizzeria controlla le proprie preferenze e quelle dei contatti

        //Ritorna un Ical con l'evento programmato e i contatti partecipanti

        ICalendar pizzataTraAmici = new ICalendar();
        pizzataTraAmici.setUid("PizzataTraAmici");

        VEvent event = new VEvent();
        event.setOrganizer(delegateID + "@gmail.com");
        event.setDateStart(date);
        event.setUid(UUID.randomUUID().toString());
        event.setSummary("Pizzata tra amici");
        event.setDescription("Suggerimenti per le ordinazioni: " + menuSuggestions);
        event.setLocation(pizzerias.get(bestPizzeriaIndex).name);
        for(String person : menuSuggestions.keySet()){
            Attendee currentPerson = new Attendee(person, person.substring(36,person.length()) + "@gmail.com");
            event.addAttendee(currentPerson);
        }
        pizzataTraAmici.addEvent(event);

        Resource calendarNode = ResourceFactory.createResource(pizzataTraAmici.getUid().getValue());
        for(String attendee : menuSuggestions.keySet())
            if(participants.get(attendee.substring(36,attendee.length())) != null)
                participants.get(attendee.substring(36,attendee.length())).updateAgenda(calendarNode);

        settleAdviceFromPizzaEvent(pizzataTraAmici.getEvents().get(0));

        return pizzataTraAmici;
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
                if( currentEquivalentClass == null) {
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
                        if (currentNamedPizza.getLocalName().equals("Margherita")) {
                            PizzaIngredients.get(currentNamedPizza.getLocalName())
                                    .add("Baxeicò");
                        }
                    }

                }
            }

            //System.out.println(PizzaIngredients);
        }

        //Select every pizza with corresponding ingredients from dbpedia
        String prologRdf = "PREFIX rdf: <" + RDF.getURI() + ">";
        String NL = System.getProperty("line.separator");

        String queryStr = "PREFIX dbo: <http://dbpedia.org/ontology/>" +
                "PREFIX dbr: <http://dbpedia.org/resource/>" + prologRdf +
                "SELECT ?pizza ?ingredient "+
                "WHERE {" +
                " {" +
                "?pizza dbo:type dbr:Pizza" +
                ". ?pizza dbo:ingredient ?ingredient" +
                "} UNION { ?pizza rdf:type dbo:Food . ?pizza dbo:ingredient ?ingredient . FILTER(regex(str(?pizza),'[Pp][Ii][Zz][Zz][Aa]', 'i')) }" +
                "}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        ((QueryEngineHTTP) qexec).addParam("timeout", "100000");
        ResultSetRewindable rs =  ResultSetFactory.makeRewindable(qexec.execSelect());

        //System.out.println("Result as CSV: ");
        //ResultSetFormatter.outputAsCSV(rs);
        rs.reset();


        for( ; rs.hasNext() ; ) {
            QuerySolution qs = rs.next();
            String pizza = qs.getResource("pizza").getLocalName().toLowerCase().replace("_", "");
            String ingredient = qs.getResource("ingredient").getLocalName().toLowerCase().replace("_", "");
            boolean added = false;
            //a new ingredient is added only if there isn't a similar one already (if contains...) for a pizza with the same name
            for(Map.Entry <String,ArrayList<String> > entry : PizzaIngredients.entrySet()){
                if(entry.getKey().equalsIgnoreCase(pizza)){
                    for(int i = 0; i<entry.getValue().size(); i++){
                        String ingr = entry.getValue().get(i).toLowerCase();
                        if((ingr.contains(ingredient) || ingredient.contains(ingr)) || ingredient.contains(ingr.replace("topping", ""))) {
                            added = true;
                        }
                    }
                    if(!added) {
                        PizzaIngredients.get(entry.getKey()).add(ingredient);
                        added = true;
                    }
                }
            }
            //otherwise a new entry is added
            if(!added) {
                PizzaIngredients.put(pizza, new ArrayList<String>());
                PizzaIngredients.get(pizza).add(ingredient);
            }
        }

        //System.out.println(PizzaIngredients);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SmartAgent that = (SmartAgent) o;

        return delegateID.equals(that.delegateID) || delegateURI.equals(that.delegateURI);
    }

    @Override
    public int hashCode() {
        return delegateID.hashCode();
    }

    public void updateProfile(Property property, RDFNode object){

        Model profile = ContactsProfileAgenda.getNamedModel("Profile");
        Resource subject = ResourceFactory.createResource(delegateURI);
        profile.createStatement(subject, property, object);
        TDB.sync(profile);

    }

    public void updateAgenda(RDFNode calendar){

        Model agenda = ContactsProfileAgenda.getNamedModel("Agenda");
        Resource subject = ResourceFactory.createResource(delegateURI);
        Property hasCalendar = ResourceFactory.createProperty("https://www.smartcontacts.com/ontology#hasCalendar");
        agenda.createStatement(subject, hasCalendar, calendar);
        TDB.sync(agenda);

    }

    public void settleAdviceFromPizzaEvent(VEvent pizzata){

        List<Attendee> attendees = pizzata.getAttendees();

        String pizzeriaStr = pizzata.getLocation().getValue();

        contactsPreferences.put(delegateURI,personalPreferences);

        //System.out.print(pizzeriaStr);

        boolean iAmInvited = false;

        for(Attendee philosopher : attendees){
            //System.out.print(philosopher.getEmail());
            if (delegateEmail.equals(philosopher.getEmail())){
                iAmInvited = true;
            }
        }

        if(!iAmInvited) {
            return;
        }

        importPizzeria(pizzeriaStr);
        pizzaOntology.read(FileManager.get().open("./res/mypizza.owl"), "");
        equivalentResourcesRetrieval();
        findPizzasIngredients();
        findSameIngredientsPizzas();
        //System.out.println(pizzerias);

        //for each pizzeria fills the list of contacts willing to dine there and the set of pizzas they wish to eat
        for(Pizzeria pizzeria : pizzerias){
            Set<String> pizzas = new HashSet<>(pizzeria.pizzeDellaCasaForPizzaOwl());
            for(String contact : contactsPreferences.keySet()) {
                Set<String> intersection = new HashSet<>(pizzas); // use the copy constructor
                intersection.retainAll( pizzasLikedByContact(contact) );

                //If pizzeria lacks celiac food, and contact is celiac, go away
                if (!pizzeria.baseCeliaci && hasCeliacDisease(contact))
                    continue;

                //if the intersection isn't empty, that there's at least a pizza liked by the current contact
                //then I add these pizzas to the appropriate slot in ContactPizzerie
                if(!intersection.isEmpty()){
                    ArrayList<String> containedPizzas = new ArrayList<>(intersection);

                    //System.out.println(contact + " likes " + containedPizzas + " in " + pizzeria.name);

                    ArrayList< Pair <String, ArrayList<String>> > currentPizzeria = ContactPizzerie.get(pizzeria);
                    if(currentPizzeria == null){
                        currentPizzeria = new ArrayList<>();
                        ContactPizzerie.put(pizzeria, currentPizzeria);
                    }

                    currentPizzeria.add(new Pair<>(contact, containedPizzas));
                }
            }
        }



        Property mightlike = ResourceFactory.createProperty("http://www.smartcontacts.com/ontology#mightLike");

        ArrayList<OWLsameAs> properties = new ArrayList<>(equivalentProperties.values());
        for (Pair< String, ArrayList< String > > entry: ContactPizzerie.get(pizzerias.get(0))) {
            String person = entry.first;
            Set<String> pizzaCandidates = new HashSet<>(entry.second);
            //System.out.println(person + " may like " + pizzaCandidates);
            //System.out.println(person + "\nPizza candidates: " + pizzaCandidates + "\n");

            Set<String> likedPizzas = new HashSet<>();
            for (Pair<Property, RDFNode> preference : contactsPreferences.get(person)) {
                OWLsameAs prop = new OWLsameAs(preference.first.asResource());

                if (properties.contains(prop)) {
                    if(ontologyToPizzeria.get(preference.second.asResource()) != null)
                        likedPizzas.addAll( ontologyToPizzeria.get(preference.second.asResource()).equivalentLocals() );
                    else
                        likedPizzas.add(preference.second.asResource().getLocalName());
                }
            }
            //System.out.println("Liked pizzas: " + likedPizzas + "\n");

            pizzaCandidates.retainAll(likedPizzas);
            ArrayList<String> pizzas = new ArrayList<>(pizzaCandidates);

            //System.out.println("Def candidates: " + pizzas + "\n\n");
            if(!pizzas.isEmpty())
                menuSuggestions.put(person, pizzas.get(new Random().nextInt(pizzas.size())));
            else {

                String suggestion = entry.second.get(new Random().nextInt(entry.second.size()));
                menuSuggestions.put(person, suggestion );
                Resource object = ResourceFactory.createResource("http://www.co-ode.org/ontologies/pizza/pizza.owl/#" + suggestion);
                updateProfile(mightlike, object);
            }

        }

    }


}
