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

    // call this ONCE when server starts
    // keeps one connection open for the whole session
    public static void initialize() {
        try {
            client   = MongoClients.create(URI);
            database = client.getDatabase(NAME);
            System.out.println("✅ MongoDB connected: " + NAME);
        } catch (Exception e) {
            System.err.println("❌ MongoDB connection failed");
            e.printStackTrace();
        }
    }

    // each repository calls this to get its collection
    public static MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
        // if collection doesn't exist MongoDB creates it automatically ✅
    }

    // call this when server shuts down
    public static void close() {
        if (client != null) {
            client.close();
            System.out.println("MongoDB connection closed");
        }
    }
}