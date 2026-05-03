import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;

public class Database {

    public static void main(String[] args) {

        String uri = "mongodb://localhost:27017";

        // Connect to MongoDB
        try (MongoClient client = MongoClients.create(uri)) {

            // Get database
            MongoDatabase database = client.getDatabase("myDatabase");

            // Get collection
            MongoCollection<Document> usersCollection = database.getCollection("users");

            // Create document
            Document user = new Document()
                    .append("name", "seif alaaaa")
                    .append("age", 21)
                    .append("university", "Cairo University");

            // Insert document
            usersCollection.insertOne(user);

            System.out.println("✅ User inserted successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error occurred:");
            e.printStackTrace();
        }
    }
}