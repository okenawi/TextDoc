import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentRepository {

    private static MongoCollection<Document> collection() {
        return Database.getCollection("documents");
    }



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


    public static String[] findByCode(String code) {

        Document doc = collection().find(
                Filters.eq("editorCode", code)
        ).first();

        if (doc != null) {
            return new String[]{doc.getString("_id"), "editor"};
        }

        doc = collection().find(
                Filters.eq("viewerCode", code)
        ).first();

        if (doc != null) {
            return new String[]{doc.getString("_id"), "viewer"};
        }

        return null;
    }



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


    public static void renameDocument(String documentId, String newName) {
        collection().updateOne(
                Filters.eq("_id", documentId),
                new Document("$set", new Document("name", newName))
        );
    }



    public static void deleteDocument(String documentId) {
        collection().deleteOne(Filters.eq("_id", documentId));
        OperationRepository.deleteAllForDocument(documentId);
        UserRepository.deleteAllForDocument(documentId);
        System.out.println("Document deleted: " + documentId);
    }



    public static java.util.List<Document> getDocumentsForUser(String ownerId) {
        return collection()
                .find(Filters.eq("ownerId", ownerId))
                .into(new java.util.ArrayList<>());
    }
}