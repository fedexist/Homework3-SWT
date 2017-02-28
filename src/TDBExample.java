import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.tdb.store.DatasetGraphTDB;
import org.apache.jena.tdb.sys.TDBInternal;
import org.apache.jena.util.FileManager;

import java.io.InputStream;

public class TDBExample {

    private static String TDBDatasetPath = "./res/tdb-storage/TDBExample/";

    public static void main(String[] args) {

        FileManager fm = FileManager.get();
        //Apertura dei dataset creati nell'homework 2
        InputStream in = fm.open("./res/contacts-update.nt");
        InputStream in2 = fm.open("./res/tomato-mozzarella.nt");

        DatasetGraph dataset = TDBFactory.createDatasetGraph(TDBDatasetPath);
        DatasetGraphTDB dsg = TDBInternal.getBaseDatasetGraphTDB(dataset);

        //Caricamento nel TDB storage dei dataset in input
        TDBLoader.load(dsg, in, false);
        TDBLoader.load(dsg, in2, false);


    }


}