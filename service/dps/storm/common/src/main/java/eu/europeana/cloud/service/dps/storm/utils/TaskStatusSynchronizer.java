package eu.europeana.cloud.service.dps.storm.utils;

import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.storm.dao.CassandraTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.dao.TasksByStateDAO;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskStatusSynchronizer {
    private final TaskStatusUpdater taskStatusUpdater;
    private final CassandraTaskInfoDAO taskInfoDAO;

    private final TasksByStateDAO tasksByStateDAO;

    public TaskStatusSynchronizer(CassandraTaskInfoDAO taskInfoDAO, TasksByStateDAO tasksByStateDAO,
                                  TaskStatusUpdater taskStatusUpdater) {
        this.taskInfoDAO = taskInfoDAO;
        this.tasksByStateDAO = tasksByStateDAO;
        this.taskStatusUpdater = taskStatusUpdater;
    }

    public void synchronizeTasksByTaskStateFromBasicInfo(String topologyName, Collection<String> availableTopics) {
        List<TaskInfo> tasksFromTaskByTaskStateTableList = tasksByStateDAO.findTasksByStateAndTopology(
                Arrays.asList(TaskState.PROCESSING_BY_REST_APPLICATION, TaskState.QUEUED), topologyName);
        Map<Long, TaskInfo> tasksFromTaskByTaskStateTableMap = tasksFromTaskByTaskStateTableList.stream().filter(info -> availableTopics.contains(info.getTopicName()))
                .collect(Collectors.toMap(TaskInfo::getId, Function.identity()));
        List<TaskInfo> tasksFromBasicInfoTable = findByIds(tasksFromTaskByTaskStateTableMap.keySet());
        List<TaskInfo> tasksToCorrect = tasksFromBasicInfoTable.stream().filter(this::isFinished).collect(Collectors.toList());
        for (TaskInfo task : tasksToCorrect) {
            taskStatusUpdater.updateTask(topologyName, task.getId(), tasksFromTaskByTaskStateTableMap.get(task.getId()).getState().toString(), task.getState().toString());
        }
    }

    private List<TaskInfo> findByIds(Collection<Long> taskIds) {
        return taskIds.stream().map(taskInfoDAO::findById).flatMap(Optional::stream).collect(Collectors.toList());
    }

    private boolean isFinished(TaskInfo info) {
        return info.getState() == TaskState.DROPPED || info.getState() == TaskState.PROCESSED;
    }

}
