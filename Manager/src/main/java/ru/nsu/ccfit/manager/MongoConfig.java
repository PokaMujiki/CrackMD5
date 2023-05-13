package ru.nsu.ccfit.manager;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {
    private static final String CONNECTION_STRING = "mongodb://mongodb1:27017,mongodb2:27017,mongodb3:27017";
    private static final String DATABASE_NAME = "crack-md5-manager";
    private static final String TASKS_COLLECTION_NAME = "tasks";
    private static final String ACTIVE_REQUESTS_COLLECTION_NAME = "active_requests";

    @Bean
    public MongoDatabase database() {
        MongoClient mongoClient = MongoClients.create(CONNECTION_STRING);
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    @Bean
    public List<String> collectionNames(MongoDatabase database) {
        return database.listCollectionNames().into(new ArrayList<>());
    }

    @Bean
    public MongoCollection<Document> tasksCollection(List<String> collectionNames, MongoDatabase database) {
        if (!collectionNames.contains(TASKS_COLLECTION_NAME)) {
            database.createCollection(TASKS_COLLECTION_NAME);
        }
        return database.getCollection(TASKS_COLLECTION_NAME);
    }

    @Bean
    public MongoCollection<Document> activeRequestsCollection(List<String> collectionNames, MongoDatabase database) {
        if (!collectionNames.contains(ACTIVE_REQUESTS_COLLECTION_NAME)) {
            database.createCollection(ACTIVE_REQUESTS_COLLECTION_NAME);
        }
        return database.getCollection(ACTIVE_REQUESTS_COLLECTION_NAME);
    }
}