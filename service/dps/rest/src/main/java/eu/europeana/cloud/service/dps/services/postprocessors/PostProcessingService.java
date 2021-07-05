package eu.europeana.cloud.service.dps.services.postprocessors;

import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.exception.TaskInfoDoesNotExistException;
import eu.europeana.cloud.service.dps.storm.dao.CassandraTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.dao.TasksByStateDAO;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static eu.europeana.cloud.common.model.dps.TaskState.IN_POST_PROCESSING;
import static eu.europeana.cloud.common.model.dps.TaskState.READY_FOR_POST_PROCESSING;

public class PostProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostProcessingService.class);

    public static final String SCHEDULE_CRON_RULE = "15,45 * * * * *";

    public static final String MESSAGE_SUCCESSFULLY_POST_PROCESSED = "Successfully post processed task with id={}";
    public static final String MESSAGE_FAILED_POST_PROCESSED = "Could not post process task with id={}";

    private CassandraTaskInfoDAO taskInfoDAO;

    private TasksByStateDAO tasksByStateDAO;

    private PostProcessorFactory postProcessorFactory;

    @Inject
    public PostProcessingService(PostProcessorFactory postProcessorFactory,
                                 CassandraTaskInfoDAO taskInfoDAO,
                                 TasksByStateDAO tasksByStateDAO) {

        this.postProcessorFactory = postProcessorFactory;

        this.taskInfoDAO = taskInfoDAO;
        this.tasksByStateDAO = tasksByStateDAO;
        LOGGER.info("Created post processing service");
    }

    @Scheduled(cron = SCHEDULE_CRON_RULE)
    public void execute() {
        findTask(Arrays.asList(IN_POST_PROCESSING, READY_FOR_POST_PROCESSING)).ifPresent(this::postProcess);
    }

    public void postProcess(TaskInfo taskInfo) {
        try {
            var dpsTask = loadTask(taskInfo.getId());
            postProcessorFactory.getPostProcessor(taskInfo).execute(dpsTask);
            LOGGER.info(MESSAGE_SUCCESSFULLY_POST_PROCESSED, taskInfo.getId());
        } catch (IOException | TaskInfoDoesNotExistException e) {
            LOGGER.error(MESSAGE_FAILED_POST_PROCESSED, taskInfo.getId(), e);
        }
    }

    private Optional<TaskInfo> findTask(List<TaskState> state) {
        LOGGER.info("Finding tasks in {} state...", state);
        Optional<TaskInfo> result = tasksByStateDAO.findTaskByState(state);

        if (result.isPresent()) {
            LOGGER.info("Found task to post process with id= {}", result.get());
        } else {
            LOGGER.info("There are no tasks in {} state on this machine.", state);
        }

        return result;
    }

    ///!!! TODO Wyjaśnic parametr PluginParameterKeys.HARVEST_DATE
/*
    private DpsTask loadTask(long taskId) throws IOException, TaskInfoDoesNotExistException {
        var taskInfo = taskInfoDAO.findById(taskId).orElseThrow(TaskInfoDoesNotExistException::new);
        var dpsTask = new ObjectMapper().readValue(taskInfo.getTaskDefinition(), DpsTask.class);
        dpsTask.addParameter(PluginParameterKeys.HARVEST_DATE, DateHelper.getISODateString(taskInfo.getSentDate()));
        return dpsTask;
    }
*/

    private DpsTask loadTask(long taskId) throws IOException, TaskInfoDoesNotExistException {
        var taskInfo = taskInfoDAO.findById(taskId).orElseThrow(TaskInfoDoesNotExistException::new);
        return new ObjectMapper().readValue(taskInfo.getTaskDefinition(), DpsTask.class);
    }

}
