import java.util.ArrayList;

public class Pizzeria {
    private String name;
    private boolean baseCeliaci;
    public ArrayList<String> pizzeDellaCasa;

    public Pizzeria(String name, boolean baseCeliaci) {
        this.name = name;
        this.baseCeliaci = baseCeliaci;
        pizzeDellaCasa = new ArrayList<>();
    }

    public ArrayList<String> pizzeDellaCasaForDBPedia()
    {
        ArrayList<String> temp = new ArrayList<>();
        for (String pizzaName:pizzeDellaCasa)
            temp.add(pizzaName.replace(" ", "_"));
        return temp;
    }

    public ArrayList<String> pizzeDellaCasaForPizzaOwl()
    {
        ArrayList<String> temp = new ArrayList<>();
        for (String pizzaName:pizzeDellaCasa)
            temp.add(pizzaName.replace(" ", ""));
        return temp;
    }
}
