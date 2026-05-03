import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;

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
                    .append("name", "Omar")
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