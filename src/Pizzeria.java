import java.util.ArrayList;

/**
 * Created by Enrico on 03/03/2017.
 */
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
