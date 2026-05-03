import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.time.LocalDateTime;

public class UserRepository {

    private static MongoCollection<Document> collection() {
        return Database.getCollection("users");
    }


    // ── ADD USER TO DOCUMENT ─────────────────────────────────
    // called when user joins a document via sharing code
    public static void addUser(String userId,
                               String documentId,
                               String permission) {

        // check if already exists → don't add twice
        Document existing = collection().find(
                Filters.and(
                        Filters.eq("userId",     userId),
                        Filters.eq("documentId", documentId)
                )
        ).first();

        if (existing != null) {
            System.out.println("User already in document: " + userId);
            return;
        }

        Document user = new Document()
                .append("userId",      userId)
                .append("documentId",  documentId)
                .append("permission",  permission)
                .append("joinedAt",    LocalDateTime.now().toString());

        collection().insertOne(user);
    }


    // ── GET PERMISSION ───────────────────────────────────────
    // returns "editor", "viewer", or null if not found
    public static String getPermission(String userId, String documentId) {
        Document user = collection().find(
                Filters.and(
                        Filters.eq("userId",     userId),
                        Filters.eq("documentId", documentId)
                )
        ).first();

        if (user == null) return null;
        return user.getString("permission");
    }


    // ── PERMISSION CHECKS ────────────────────────────────────
    public static boolean isEditor(String userId, String documentId) {
        return "editor".equals(getPermission(userId, documentId));
    }

    public static boolean isViewer(String userId, String documentId) {
        return "viewer".equals(getPermission(userId, documentId));
    }


    // ── DELETE ALL USERS FOR A DOCUMENT ─────────────────────
    // called when document is deleted
    public static void deleteAllForDocument(String documentId) {
        collection().deleteMany(Filters.eq("documentId", documentId));
    }
}