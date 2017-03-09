import java.util.ArrayList;
import java.util.HashMap;

public class Pizzeria {
    public String name;
    public boolean baseCeliaci;
    public HashMap< String, ArrayList<String>> pizzeDellaCasa;

    @Override
    public String toString() {
        return "Pizzeria{\n" +
                "name='" + name + '\'' +
                ",\nbaseCeliaci=" + baseCeliaci +
                ",\npizzeDellaCasa=" + pizzeDellaCasa +
                "\n}";
    }

    public Pizzeria(String name, boolean baseCeliaci) {
        this.name = name;
        this.baseCeliaci = baseCeliaci;
        pizzeDellaCasa = new HashMap<>();
    }

    public ArrayList<String> pizzeDellaCasaForDBPedia()
    {
        ArrayList<String> temp = new ArrayList<>();
        for (String pizzaName : pizzeDellaCasa.keySet())
            temp.add(pizzaName.replace(" ", "_"));
        return temp;
    }

    public ArrayList<String> pizzeDellaCasaForPizzaOwl()
    {
        ArrayList<String> temp = new ArrayList<>();
        for (String pizzaName:pizzeDellaCasa.keySet())
            temp.add(pizzaName.replace(" ", ""));
        return temp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pizzeria pizzeria = (Pizzeria) o;

        return name.equals(pizzeria.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
