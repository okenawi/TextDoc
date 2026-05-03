import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OperationRepository {

    private static MongoCollection<Document> collection() {
        return Database.getCollection("operations");
    }


    // ── SAVE ONE OPERATION ───────────────────────────────────
    // called by server every time it receives an INSERT or DELETE
    // this is the op log we discussed
    public static void saveOperation(String documentId, String type,
                                     String siteId,     int    clock,
                                     String value,      String afterSiteId,
                                     int    afterClock, boolean isBold,
                                     boolean isItalic) {
        Document op = new Document()
                .append("documentId",  documentId)
                .append("type",        type)
                .append("siteId",      siteId)
                .append("clock",       clock)
                .append("value",       value)        // null for DELETE
                .append("afterSiteId", afterSiteId)  // null for first char
                .append("afterClock",  afterClock)
                .append("isBold",      isBold)
                .append("isItalic",    isItalic)
                .append("timestamp",   LocalDateTime.now().toString());

        collection().insertOne(op);
    }


    // ── LOAD ALL OPERATIONS FOR A DOCUMENT ───────────────────
    // called when a new user opens a document
    // returns list of JSON strings
    // server sends each one to the new user's handleRemoteOperation()
    public static List<String> getOperations(String documentId) {
        List<String> ops = new ArrayList<>();

        collection()
                .find(Filters.eq("documentId", documentId))
                .sort(Sorts.ascending("timestamp"))
                // sort by timestamp → replay in correct order ✅
                .forEach(doc -> {

                    String afterSiteId = doc.getString("afterSiteId");
                    String afterSiteIdJson = (afterSiteId == null)
                            ? "null"
                            : "\"" + afterSiteId + "\"";

                    // rebuild as JSON in the exact format
                    // Main.java's handleRemoteOperation() already knows
                    String json = String.format(
                            "{\"type\":\"%s\",\"siteId\":\"%s\",\"clock\":%d," +
                                    "\"value\":\"%s\",\"afterSiteId\":%s,\"afterClock\":%d}",
                            doc.getString("type"),
                            doc.getString("siteId"),
                            doc.getInteger("clock"),
                            doc.getString("value") != null ? doc.getString("value") : "",
                            afterSiteIdJson,
                            doc.getInteger("afterClock")
                    );

                    ops.add(json);
                });

        return ops;
        // Main.java already handles this format ✅
        // no changes needed to CRDT or UI
    }


    // ── DELETE ALL OPS FOR A DOCUMENT ────────────────────────
    // called when document is deleted
    public static void deleteAllForDocument(String documentId) {
        collection().deleteMany(Filters.eq("documentId", documentId));
    }
}