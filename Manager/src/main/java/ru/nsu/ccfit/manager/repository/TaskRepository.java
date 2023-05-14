package ru.nsu.ccfit.manager.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.nsu.ccfit.manager.models.Task;
import ru.nsu.ccfit.manager.models.TaskStatus;

@Repository
public class TaskRepository {
    private static final String REQUEST_ID_FIELD_NAME = "request_id";
    private static final String TASK_STATUS_FIELD_NAME = "task_status";
    private static final String TASK_DATA_FIELD_NAME = "task_data";
    private static final String TASK_PARTS_FIELD_NAME = "task_parts";
    private final MongoCollection<Document> tasksCollection;

    @Autowired
    public TaskRepository(MongoCollection<Document> tasksCollection) {
        this.tasksCollection = tasksCollection;
    }

    public void insert(String requestId, Task task) {
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

    public Task get(String requestId) {
        var item = tasksCollection.find(Filters.eq(REQUEST_ID_FIELD_NAME, requestId)).first();

        if (item == null) {
            return null;
        }

        Task task = new Task(TaskStatus.valueOf(item.getString(TASK_STATUS_FIELD_NAME)));
        task.getData().addAll(item.getList(TASK_DATA_FIELD_NAME, String.class));
        task.getFinishedParts().addAll(item.getList(TASK_PARTS_FIELD_NAME, Integer.class));

        return task;
    }
}
