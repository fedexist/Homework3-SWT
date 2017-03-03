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
}
