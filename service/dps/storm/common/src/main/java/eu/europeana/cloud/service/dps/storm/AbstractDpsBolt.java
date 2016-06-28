package eu.europeana.cloud.service.dps.storm;

import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import eu.europeana.cloud.common.model.dps.States;
import eu.europeana.cloud.common.model.dps.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import eu.europeana.cloud.service.dps.TaskExecutionKillService;
import eu.europeana.cloud.service.dps.service.zoo.ZookeeperKillService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Abstract class for all Storm bolts used in Europeana Cloud.
 *
 * @author Pavel Kefurt <Pavel.Kefurt@gmail.com>
 */
public abstract class AbstractDpsBolt extends BaseRichBolt {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDpsBolt.class);

    public static final String NOTIFICATION_STREAM_NAME = "NotificationStream";

    protected Tuple inputTuple;

    protected Map stormConfig;
    protected TopologyContext topologyContext;
    protected OutputCollector outputCollector;
    protected TaskExecutionKillService killService;
    protected String topologyName;

    public abstract void execute(StormTaskTuple t);

    public abstract void prepare();

    @Override
    public void execute(Tuple tuple) {
        LOGGER.debug("Received tuple :" + tuple.toString());
        inputTuple = tuple;

        StormTaskTuple t = null;
        try {
            t = StormTaskTuple.fromStormTuple(tuple);
            if (killService.hasKillFlag(topologyName, t.getTaskId())) {
                LOGGER.info("Task {} going to be killed.", t.getTaskId());
                emitKillNotification(t.getTaskId(), t.getFileUrl(), "", "");
                return;
            }
            LOGGER.debug("Mapped to StormTaskTuple :" + t.toStormTuple().toString());
            execute(t);
        } catch (Exception e) {
            LOGGER.error("AbstractDpsBolt error: {} \nStackTrace: \n{}", e.getMessage(), e.getStackTrace());
            if (t != null) {
                StringWriter stack = new StringWriter();
                e.printStackTrace(new PrintWriter(stack));
                emitErrorNotification(t.getTaskId(), t.getFileUrl(), e.getMessage(), stack.toString());
            }
        } finally {
            outputCollector.ack(tuple);
        }
    }

    @Override
    public void prepare(Map stormConfig, TopologyContext tc, OutputCollector oc) {
        this.stormConfig = stormConfig;
        this.topologyContext = tc;
        this.outputCollector = oc;

        List<String> zooServers = (List<String>) stormConfig.get(Config.STORM_ZOOKEEPER_SERVERS);
        String zooPort = String.valueOf(stormConfig.get(Config.STORM_ZOOKEEPER_PORT));

        this.topologyName = (String) stormConfig.get(Config.TOPOLOGY_NAME);

        //String connectString = String.join(":"+zooPort+",", zooServers);    //Java 8
        String connectString = StringUtils.join(zooServers, ":" + zooPort + ",");    //Java 7

        this.killService = new ZookeeperKillService(connectString);

        prepare();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        //default stream
        declarer.declare(StormTaskTuple.getFields());

        //notifications
        declarer.declareStream(NOTIFICATION_STREAM_NAME, NotificationTuple.getFields());
    }

    /**
     * Emit {@link NotificationTuple} with error notification to {@link #NOTIFICATION_STREAM_NAME}.
     * Only one notification call per resource per task.
     *
     * @param taskId                 task ID
     * @param resource               affected resource (e.g. file URL)
     * @param message                short text
     * @param additionalInformations the rest of informations (e.g. stack trace)
     */
    protected void emitDropNotification(long taskId, String resource, String message, String additionalInformations) {
        NotificationTuple nt = NotificationTuple.prepareNotification(taskId,
                resource, States.DROPPED, message, additionalInformations);
        outputCollector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }

    /**
     * Emit {@link NotificationTuple} with error notification to {@link #NOTIFICATION_STREAM_NAME}.
     * Only one notification call per resource per task.
     *
     * @param taskId                 task ID
     * @param resource               affected resource (e.g. file URL)
     * @param message                short text
     * @param additionalInformations the rest of informations (e.g. stack trace)
     */
    protected void emitErrorNotification(long taskId, String resource, String message, String additionalInformations) {
        NotificationTuple nt = NotificationTuple.prepareNotification(taskId,
                resource, States.ERROR, message, additionalInformations);
        outputCollector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }

    /**
     * Emit {@link NotificationTuple} with kill notification to {@link #NOTIFICATION_STREAM_NAME}.
     * Only one notification call per resource per task.
     *
     * @param taskId                 task ID
     * @param resource               affected resource (e.g. file URL)
     * @param message                short text
     * @param additionalInformations the rest of informations
     */
    protected void emitKillNotification(long taskId, String resource, String message, String additionalInformations) {
        NotificationTuple nt = NotificationTuple.prepareNotification(taskId,
                resource, States.KILLED, message, additionalInformations);
        outputCollector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }


    protected void endTask(long taskId,String info, TaskState state, Date finishTime) {
        NotificationTuple nt = NotificationTuple.prepareEndTask(taskId, info, state, finishTime);
        outputCollector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }

    protected void updateTask(long taskId,String info, TaskState state, Date startTime) {
        NotificationTuple nt = NotificationTuple.prepareUpdateTask(taskId, info, state, startTime);
        outputCollector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }


    protected void logAndEmitError(StormTaskTuple t, String message) {
        LOGGER.error(message);
        emitErrorNotification(t.getTaskId(), t.getFileUrl(), message, t.getParameters().toString());
    }

    protected void logAndEmitError(StormTaskTuple t, String message, Exception e) {
        LOGGER.error(message, e);
        StringWriter stack = new StringWriter();
        e.printStackTrace(new PrintWriter(stack));
        logAndEmitError(t, message + e.getMessage());
    }

    protected void emitSuccess(StormTaskTuple t) {
        outputCollector.emit(inputTuple, t.toStormTuple());
    }


}
