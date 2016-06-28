package eu.europeana.cloud.service.dps.storm.utils;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import eu.europeana.cloud.cassandra.CassandraConnectionProvider;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.exception.TaskInfoDoesNotExistException;
import eu.europeana.cloud.service.dps.service.cassandra.CassandraTablesAndColumnsNames;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The {@link eu.europeana.cloud.common.model.dps.TaskInfo} DAO
 *
 * @author akrystian
 */
public class CassandraTaskInfoDAO extends CassandraDAO {
    private PreparedStatement taskSearchStatement;
    private PreparedStatement taskInsertStatement;
    private PreparedStatement notificationResourcesCountStatement;
    private CassandraSubTaskInfoDAO cassandraSubTaskInfoDAO;
    private PreparedStatement updateTask;
    private PreparedStatement endTask;


    /**
     * @param dbService The service exposing the connection and session
     */
    public CassandraTaskInfoDAO(CassandraConnectionProvider dbService) {
        super(dbService);
        cassandraSubTaskInfoDAO = new CassandraSubTaskInfoDAO(dbService);
    }

    @Override
    void prepareStatements() {

        notificationResourcesCountStatement = dbService.getSession().prepare("SELECT count(*) FROM " + CassandraTablesAndColumnsNames.NOTIFICATIONS_TABLE + " WHERE " + CassandraTablesAndColumnsNames.NOTIFICATION_TASK_ID + " = ?");
        taskSearchStatement = dbService.getSession().prepare(
                "SELECT * FROM " + CassandraTablesAndColumnsNames.BASIC_INFO_TABLE + " WHERE " + CassandraTablesAndColumnsNames.BASIC_TASK_ID + " = ?");
        taskSearchStatement.setConsistencyLevel(dbService.getConsistencyLevel());
        updateTask = dbService.getSession().prepare("UPDATE " + CassandraTablesAndColumnsNames.BASIC_INFO_TABLE + " SET " + CassandraTablesAndColumnsNames.STATE + " = ? , " + CassandraTablesAndColumnsNames.START_TIME + " = ? , " + CassandraTablesAndColumnsNames.INFO + " =? WHERE " + CassandraTablesAndColumnsNames.BASIC_TASK_ID + " = ?");
        updateTask.setConsistencyLevel(dbService.getConsistencyLevel());
        endTask = dbService.getSession().prepare("UPDATE " + CassandraTablesAndColumnsNames.BASIC_INFO_TABLE + " SET " + CassandraTablesAndColumnsNames.STATE + " = ? , " + CassandraTablesAndColumnsNames.FINISH_TIME + " = ? , " + CassandraTablesAndColumnsNames.INFO + " =? WHERE " + CassandraTablesAndColumnsNames.BASIC_TASK_ID + " = ?");
        endTask.setConsistencyLevel(dbService.getConsistencyLevel());
        taskInsertStatement = dbService.getSession().prepare("INSERT INTO " + CassandraTablesAndColumnsNames.BASIC_INFO_TABLE +
                "(" + CassandraTablesAndColumnsNames.BASIC_TASK_ID + ","
                + CassandraTablesAndColumnsNames.BASIC_TOPOLOGY_NAME + ","
                + CassandraTablesAndColumnsNames.BASIC_EXPECTED_SIZE + ","
                + CassandraTablesAndColumnsNames.STATE + ","
                + CassandraTablesAndColumnsNames.INFO + ","
                + CassandraTablesAndColumnsNames.SENT_TIME + ","
                + CassandraTablesAndColumnsNames.START_TIME + ","
                + CassandraTablesAndColumnsNames.FINISH_TIME +
                ") VALUES (?,?,?,?,?,?,?,?)");
        taskInsertStatement.setConsistencyLevel(dbService.getConsistencyLevel());
    }

    public List<TaskInfo> searchById(long taskId)
            throws NoHostAvailableException, QueryExecutionException, TaskInfoDoesNotExistException {
        ResultSet rs = dbService.getSession().execute(taskSearchStatement.bind(taskId));
        if (!rs.iterator().hasNext()) {
            throw new TaskInfoDoesNotExistException();
        }
        List<TaskInfo> result = new ArrayList<>();
        for (Row row : rs.all()) {
            TaskInfo task = new TaskInfo(row.getLong(CassandraTablesAndColumnsNames.BASIC_TASK_ID), row.getString(CassandraTablesAndColumnsNames.BASIC_TOPOLOGY_NAME), TaskState.valueOf(row.getString(CassandraTablesAndColumnsNames.STATE)), row.getString(CassandraTablesAndColumnsNames.INFO), row.getDate(CassandraTablesAndColumnsNames.SENT_TIME), row.getDate(CassandraTablesAndColumnsNames.START_TIME), row.getDate(CassandraTablesAndColumnsNames.FINISH_TIME));
            task.setContainsElements(row.getInt(CassandraTablesAndColumnsNames.BASIC_EXPECTED_SIZE));
            result.add(task);
        }
        return result;
    }


    public void insert(long taskId, String topologyName, int expectedSize, String state, String info, Date sentTime, Date startTime, Date finishTime)
            throws NoHostAvailableException, QueryExecutionException {
        dbService.getSession().execute(taskInsertStatement.bind(taskId, topologyName, expectedSize, state, info, sentTime, startTime, finishTime));
    }

    public void updateTask(long taskId, String info, String state, Date startDate)
            throws NoHostAvailableException, QueryExecutionException {
        dbService.getSession().execute(updateTask.bind(state, startDate, info, taskId));
    }

    public void endTask(long taskId, String info, String state, Date finishDate)
            throws NoHostAvailableException, QueryExecutionException {
        dbService.getSession().execute(endTask.bind(state, finishDate, info, taskId));
    }

    public void insert(long taskId, String topologyName, int expectedSize, String state, String info, Date sentTime)
            throws NoHostAvailableException, QueryExecutionException {
        insert(taskId, topologyName, expectedSize, state, info, sentTime, null, null);
    }

    public List<TaskInfo> searchByIdWithSubtasks(long taskId)
            throws NoHostAvailableException, QueryExecutionException, TaskInfoDoesNotExistException {
        List<TaskInfo> result = searchById(taskId);
        for (TaskInfo taskInfo : result) {
            List<SubTaskInfo> subTasks = cassandraSubTaskInfoDAO.searchById(taskId);
            for (SubTaskInfo subTask : subTasks) {
                taskInfo.addSubtask(subTask);
            }
        }
        return result;
    }

    public long getProcessedCount(long taskId) throws TaskInfoDoesNotExistException {
        ResultSet rs = dbService.getSession().execute(notificationResourcesCountStatement.bind(taskId));
        if (!rs.iterator().hasNext()) {
            throw new TaskInfoDoesNotExistException();
        }
        Row row = rs.one();
        return row.getLong("count");

    }
}
