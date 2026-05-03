
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentRepository {

    private static MongoCollection<Document> collection() {
        return Database.getCollection("documents");
    }
    // small helper so every method doesn't repeat this line


    // ── CREATE ──────────────────────────────────────────────
    // called when user creates a new document
    // returns the new document's id
    public static String createDocument(String name, String ownerId) {
        String docId      = UUID.randomUUID().toString();
        String editorCode = UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
        String viewerCode = UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        Document doc = new Document()
                .append("_id",        docId)
                .append("name",       name)
                .append("ownerId",    ownerId)
                .append("createdAt",  LocalDateTime.now().toString())
                .append("editorCode", editorCode)
                .append("viewerCode", viewerCode);

        collection().insertOne(doc);
        System.out.println("Document created: " + docId);
        return docId;
    }


    // ── FIND BY SHARING CODE ─────────────────────────────────
    // user enters a code → find which document it belongs to
    // and whether it gives editor or viewer access
    // returns String[]{documentId, "editor"/"viewer"}
    // returns null if code doesn't exist
    public static String[] findByCode(String code) {

        // check editor codes first
        Document doc = collection().find(
                Filters.eq("editorCode", code)
        ).first();

        if (doc != null) {
            return new String[]{doc.getString("_id"), "editor"};
        }

        // check viewer codes
        doc = collection().find(
                Filters.eq("viewerCode", code)
        ).first();

        if (doc != null) {
            return new String[]{doc.getString("_id"), "viewer"};
        }

        return null; // code not found
    }


    // ── GET SHARING CODES ────────────────────────────────────
    // returns the two codes for a document
    // only call this after confirming user is an editor
    // returns String[]{editorCode, viewerCode}
    public static String[] getCodes(String documentId) {
        Document doc = collection().find(
                Filters.eq("_id", documentId)
        ).first();

        if (doc == null) return null;

        return new String[]{
                doc.getString("editorCode"),
                doc.getString("viewerCode")
        };
    }


    // ── RENAME ───────────────────────────────────────────────
    public static void renameDocument(String documentId, String newName) {
        collection().updateOne(
                Filters.eq("_id", documentId),
                new Document("$set", new Document("name", newName))
        );
    }


    // ── DELETE ───────────────────────────────────────────────
    // deletes document AND all its operations AND user records
    public static void deleteDocument(String documentId) {
        collection().deleteOne(Filters.eq("_id", documentId));
        OperationRepository.deleteAllForDocument(documentId);
        UserRepository.deleteAllForDocument(documentId);
        System.out.println("Document deleted: " + documentId);
    }


    // ── GET ALL DOCUMENTS FOR A USER ─────────────────────────
    // for showing the user's file list in the UI
    public static java.util.List<Document> getDocumentsForUser(String ownerId) {
        return collection()
                .find(Filters.eq("ownerId", ownerId))
                .into(new java.util.ArrayList<>());
    }
}