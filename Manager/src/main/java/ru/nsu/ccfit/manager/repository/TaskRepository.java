package ru.nsu.ccfit.manager.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.nsu.ccfit.manager.models.Task;
import ru.nsu.ccfit.manager.models.TaskStatus;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TaskRepository {
    private static final String REQUEST_ID_FIELD_NAME = "request_id";
    private static final String TASK_STATUS_FIELD_NAME = "task_status";
    private static final String TASK_DATA_FIELD_NAME = "task_data";
    private static final String TASK_PARTS_FIELD_NAME = "task_parts";
    private final MongoCollection<Document> tasksCollection;
    private final ConcurrentHashMap<String, Task> tasksInfo = new ConcurrentHashMap<>();

    private void loadFromDb() {
        tasksCollection.find().forEach(document -> {
            String requestId = document.getString(REQUEST_ID_FIELD_NAME);
            TaskStatus taskStatus = TaskStatus.valueOf(document.get(TASK_STATUS_FIELD_NAME, String.class));
            List<String> taskData = document.getList(TASK_DATA_FIELD_NAME, String.class);
            List<Integer> taskParts = document.getList(TASK_PARTS_FIELD_NAME, Integer.class);

            var task = new Task(taskStatus);
            task.getData().addAll(taskData);
            task.getFinishedParts().addAll(taskParts);

            tasksInfo.put(requestId, task);
        });
    }

    @Autowired
    public TaskRepository(MongoCollection<Document> tasksCollection) {
        this.tasksCollection = tasksCollection;
        loadFromDb();
    }

    // only saves to db
    private void save(String requestId, Task task) {
        var item = new Document();
        item.append(REQUEST_ID_FIELD_NAME, requestId);
        item.append(TASK_STATUS_FIELD_NAME, task.getStatus());
        item.append(TASK_DATA_FIELD_NAME, task.getData());
        item.append(TASK_PARTS_FIELD_NAME, task.getFinishedParts());

        tasksCollection.updateOne(
                Filters.eq(REQUEST_ID_FIELD_NAME, requestId),
                new Document("$set", item),
                new UpdateOptions().upsert(true)
        );
    }

    public void insert(String requestId, Task task) {
        tasksInfo.put(requestId, task);
        save(requestId, task);
    }

    public Task get(String requestId) {
        return tasksInfo.get(requestId);
    }
}
