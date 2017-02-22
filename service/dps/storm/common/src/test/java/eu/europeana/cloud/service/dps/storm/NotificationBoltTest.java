package eu.europeana.cloud.service.dps.storm;


import eu.europeana.cloud.cassandra.CassandraConnectionProviderSingleton;

import eu.europeana.cloud.common.model.dps.States;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.service.cassandra.CassandraReportService;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTestBase;
import org.apache.storm.Config;
import org.apache.storm.task.GeneralTopologyContext;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;




import static org.hamcrest.Matchers.*;

public class NotificationBoltTest extends CassandraTestBase {

    private OutputCollector collector;
    private NotificationBolt testedBolt;
    private CassandraTaskInfoDAO taskInfoDAO;


    @Before
    public void setUp() throws Exception {
        collector = Mockito.mock(OutputCollector.class);
        testedBolt = new NotificationBolt(HOST, PORT, KEYSPACE, "", "");
        NotificationBolt.clearCache();

        Map<String, Object> boltConfig = new HashMap<>();
        boltConfig.put(Config.STORM_ZOOKEEPER_SERVERS, Arrays.asList("", ""));
        boltConfig.put(Config.STORM_ZOOKEEPER_PORT, "");
        boltConfig.put(Config.TOPOLOGY_NAME, "");
        testedBolt.prepare(boltConfig, null, collector);
        taskInfoDAO = CassandraTaskInfoDAO.getInstance(CassandraConnectionProviderSingleton.getCassandraConnectionProvider(HOST, PORT, KEYSPACE, "", ""));

    }

    @Test
    public void testUpdateBasicInfoStateWithStartDateAndInfo() throws Exception {
        //given
        long taskId = 1;
        int containsElements = 1;
        int expectedSize = 1;
        String topologyName = null;
        TaskState taskState = TaskState.CURRENTLY_PROCESSING;
        String taskInfo = "";
        Date startTime = new Date();
        TaskInfo expectedTaskInfo = createTaskInfo(taskId, containsElements, topologyName, taskState, taskInfo, null, startTime, null);
        taskInfoDAO.insert(taskId, topologyName, expectedSize, containsElements, taskState.toString(), taskInfo, null, startTime, null);
        final Tuple tuple = createTestTuple(NotificationTuple.prepareUpdateTask(taskId, taskInfo, taskState, startTime));
        //when
        testedBolt.execute(tuple);
        //then
        TaskInfo result = taskInfoDAO.searchById(taskId);
        assertThat(result, notNullValue());
        assertThat(result, is(expectedTaskInfo));
    }

    @Test
    public void testUpdateBasicInfoStateWithFinishDateAndInfo() throws Exception {
        //given
        long taskId = 1;
        int containsElements = 1;
        int expectedSize = 1;
        String topologyName = null;
        TaskState taskState = TaskState.CURRENTLY_PROCESSING;
        String taskInfo = "";
        Date finishDate = new Date();
        TaskInfo expectedTaskInfo = createTaskInfo(taskId, containsElements, topologyName, taskState, taskInfo, null, null, finishDate);
        taskInfoDAO.insert(taskId, topologyName, expectedSize, containsElements, taskState.toString(), taskInfo, null, null, finishDate);
        final Tuple tuple = createTestTuple(NotificationTuple.prepareEndTask(taskId, taskInfo, taskState, finishDate));
        //when
        testedBolt.execute(tuple);
        //then
        TaskInfo result = taskInfoDAO.searchById(taskId);
        assertThat(result, notNullValue());
        assertThat(result, is(expectedTaskInfo));
    }


    @Test
    public void testSuccessfulNotificationFor101Tuples() throws Exception {
//given

        CassandraReportService cassandraReportService = new CassandraReportService(HOST, PORT, KEYSPACE, "", "");
        long taskId = 1;
        int expectedSize = 101;
        String topologyName = null;
        TaskState taskState = TaskState.CURRENTLY_PROCESSING;
        String taskInfo = "";
        taskInfoDAO.insert(taskId, topologyName, expectedSize, 0, taskState.toString(), taskInfo, null, null, null);
        String resource = "resource";
        States state = States.SUCCESS;
        String text = "text";
        String additionalInformation = "additionalInformation";
        String resultResource = "";
        final Tuple setUpTuple = createTestTuple(NotificationTuple.prepareUpdateTask(taskId, taskInfo, taskState, null));
        testedBolt.execute(setUpTuple);
        final Tuple tuple = createTestTuple(NotificationTuple.prepareNotification(taskId, resource, state, text, additionalInformation, resultResource));
        String beforeExecute = cassandraReportService.getTaskProgress(String.valueOf(taskId));
        testedBolt.execute(tuple);

        for (int i = 0; i < 99; i++) {
            testedBolt.execute(tuple);
        }

        String afterOneHundredExecutions = cassandraReportService.getTaskProgress(String.valueOf(taskId));
        testedBolt.execute(tuple);
        String afterAllExecutions = cassandraReportService.getTaskProgress(String.valueOf(taskId));

        assertThat(beforeExecute, allOf(
                containsString("\"processed\":0,"),
                containsString("\"state\":\"CURRENTLY_PROCESSING\"")));
        assertThat(afterOneHundredExecutions, allOf(
                containsString("\"processed\":100,"),
                containsString("\"state\":\"CURRENTLY_PROCESSING\"")));
        assertThat(afterAllExecutions, allOf(
                containsString("\"processed\":101,"),
                containsString("\"state\":\"PROCESSED\"")));
    }


    private TaskInfo createTaskInfo(long taskId, int containElement, String topologyName, TaskState state, String info, Date sentTime, Date startTime, Date finishTime) {
        TaskInfo expectedTaskInfo = new TaskInfo(taskId, topologyName, state, info, sentTime, startTime, finishTime);
        expectedTaskInfo.setContainsElements(containElement);
        return expectedTaskInfo;
    }


    private Tuple createTestTuple(NotificationTuple tuple) {
        Values testValue = tuple.toStormTuple();
        TopologyBuilder builder = new TopologyBuilder();
        @SuppressWarnings("unchecked")
        GeneralTopologyContext topologyContext = new GeneralTopologyContext(builder.createTopology(), new Config(), new HashMap(), new HashMap(), new HashMap(), "") {
            @Override
            public Fields getComponentOutputFields(String componentId, String streamId) {
                return NotificationTuple.getFields();
            }
        };
        return new TupleImpl(topologyContext, testValue, 1, "");
    }
}