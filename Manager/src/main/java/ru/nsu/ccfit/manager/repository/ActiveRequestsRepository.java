package ru.nsu.ccfit.manager.repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveRequestsRepository {
    @Autowired
    private MongoCollection<Document> activeRequestsCollection;

    public static final String REQUEST_PAYLOAD_FIELD_NAME = "xml_request";

    public void save(String xmlRequest) {

        Document item = new Document();
        item.append(REQUEST_PAYLOAD_FIELD_NAME, xmlRequest);

        activeRequestsCollection.insertOne(item);
    }

    public FindIterable<Document> findAll() {
        return activeRequestsCollection.find();
    }

    public void delete(Document request) {
        activeRequestsCollection.deleteOne(request);
    }
}
