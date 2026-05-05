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
                .append("value",       value)
                .append("afterSiteId", afterSiteId)
                .append("afterClock",  afterClock)
                .append("isBold",      isBold)
                .append("isItalic",    isItalic)
                .append("timestamp",   LocalDateTime.now().toString());

        collection().insertOne(op);
    }



    public static List<String> getOperations(String documentId) {
        List<String> ops = new ArrayList<>();

        collection()
                .find(Filters.eq("documentId", documentId))
                .sort(Sorts.ascending("timestamp"))
                .forEach(doc -> {

                    String afterSiteId = doc.getString("afterSiteId");
                    String afterSiteIdJson = (afterSiteId == null)
                            ? "null"
                            : "\"" + afterSiteId + "\"";


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

    }



    public static void deleteAllForDocument(String documentId) {
        collection().deleteMany(Filters.eq("documentId", documentId));
    }
}