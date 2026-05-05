import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Database {

    private static final String URI  = "mongodb://localhost:27017";
    private static final String NAME = "textdoc";

    private static MongoClient   client;
    private static MongoDatabase database;

    public static void initialize() {
        try {
            client   = MongoClients.create(URI);
            database = client.getDatabase(NAME);
            System.out.println(" MongoDB connected: " + NAME);
        } catch (Exception e) {
            System.err.println(" MongoDB connection failed");
            e.printStackTrace();
        }
    }

    public static MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    public static void close() {
        if (client != null) {
            client.close();
            System.out.println("MongoDB connection closed");
        }
    }
}