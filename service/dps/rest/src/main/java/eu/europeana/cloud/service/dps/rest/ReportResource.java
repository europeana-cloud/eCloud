package eu.europeana.cloud.service.dps.rest;

import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.cloud.service.dps.Constants;
import eu.europeana.cloud.service.dps.TaskExecutionReportService;
import eu.europeana.cloud.service.dps.ValidationStatisticsService;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrObjectDoesNotExistException;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrTopologyDoesNotExistException;
import eu.europeana.cloud.service.dps.service.utils.TopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@Scope("request")
@RequestMapping("/{topologyName}/tasks")
public class ReportResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportResource.class);

    public static final String TASK_PREFIX = "DPS_Task";

    @Autowired
    private TopologyManager topologyManager;

    @Autowired
    private TaskExecutionReportService reportService;

    @Autowired
    private ValidationStatisticsService validationStatisticsService;

    /**
     * Retrieves a detailed report for the specified task.It will return info about
     * the first 100 resources unless you specified the needed chunk by using from&to parameters
     * <p/>
     * <br/><br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding: 6px;'>
     * <strong>Required permissions:</strong>
     * <ul>
     * <li>Authenticated user</li>
     * <li>Read permission for selected task</li>
     * </ul>
     * </div>
     *
     * @param taskId       <strong>REQUIRED</strong> Unique id that identifies the task.
     * @param topologyName <strong>REQUIRED</strong> Name of the topology where the task is submitted.
     * @param from         The starting resource number should be bigger than 0
     * @param to           The ending resource number should be bigger than 0
     * @return Notification messages for the specified task.
     * @summary Retrieve task detailed report
     */
    @GetMapping(path = "{taskId}/reports/details", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasPermission(#taskId,'" + TASK_PREFIX + "', read)")
    public List<SubTaskInfo> getTaskDetailedReport(
            @PathVariable String taskId,
            @PathVariable final String topologyName,
            @RequestParam(defaultValue = "1")  @Min(1) int from,
            @RequestParam(defaultValue = "100") @Min(1) int to)
            throws AccessDeniedOrTopologyDoesNotExistException, AccessDeniedOrObjectDoesNotExistException {
        assertContainTopology(topologyName);
        reportService.checkIfTaskExists(taskId, topologyName);

        return reportService.getDetailedTaskReport(taskId, from, to);
    }


    /**
     * If error param is not specified it retrieves a report of all errors that occurred for the specified task. For each error
     * the number of occurrences is returned otherwise retrieves a report for a specific error that occurred in the specified task.
     * A sample of identifiers is returned as well. The number of identifiers is between 0 and ${maxIdentifiersCount}.
     * <p>
     * <p/>
     * <br/><br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding: 6px;'>
     * <strong>Required permissions:</strong>
     * <ul>
     * <li>Authenticated user</li>
     * <li>Read permission for selected task</li>
     * </ul>
     * </div>
     *
     * @param taskId       <strong>REQUIRED</strong> Unique id that identifies the task.
     * @param topologyName <strong>REQUIRED</strong> Name of the topology where the task is submitted.
     * @param error        Error type.
     * @param idsCount     number of identifiers to retrieve
     * @return Errors that occurred for the specified task.
     * @summary Retrieve task detailed error report
     */
    @GetMapping(path = "{taskId}/reports/errors", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasPermission(#taskId,'" + TASK_PREFIX + "', read)")
    public TaskErrorsInfo getTaskErrorReport(
            @PathVariable String taskId,
            @PathVariable final String topologyName,
            @RequestParam(required = false) String error,
            @RequestParam(defaultValue = "0") int idsCount)
            throws AccessDeniedOrTopologyDoesNotExistException, AccessDeniedOrObjectDoesNotExistException {
        assertContainTopology(topologyName);
        reportService.checkIfTaskExists(taskId, topologyName);

        if (idsCount < 0 || idsCount > Constants.MAXIMUM_ERRORS_THRESHOLD_FOR_ONE_ERROR_TYPE) {
            throw new IllegalArgumentException("Identifiers count parameter should be between 0 and " + Constants.MAXIMUM_ERRORS_THRESHOLD_FOR_ONE_ERROR_TYPE);
        }
        if (error == null || error.equals("null")) {
            return reportService.getGeneralTaskErrorReport(taskId, idsCount);
        }
        return reportService.getSpecificTaskErrorReport(taskId, error, idsCount > 0 ? idsCount : Constants.MAXIMUM_ERRORS_THRESHOLD_FOR_ONE_ERROR_TYPE);
    }


    /**
     * Check if the task has error report
     * <p>
     * <p/>
     * <br/><br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding: 6px;'>
     * <strong>Required permissions:</strong>
     * <ul>
     * <li>Authenticated user</li>
     * <li>Read permission for selected task</li>
     * </ul>
     * </div>
     *
     * @param taskId       <strong>REQUIRED</strong> Unique id that identifies the task.
     * @param topologyName <strong>REQUIRED</strong> Name of the topology where the task is submitted.
     * @return if the error report exists
     * @summary Check if the task has error report
     */
    @RequestMapping(method = { RequestMethod.HEAD }, path = "{taskId}/reports/errors")
    @PreAuthorize("hasPermission(#taskId,'" + TASK_PREFIX + "', read)")
    public Boolean checkIfErrorReportExists(
            @PathVariable String taskId,
            @PathVariable final String topologyName)
            throws AccessDeniedOrTopologyDoesNotExistException, AccessDeniedOrObjectDoesNotExistException {
        assertContainTopology(topologyName);
        reportService.checkIfTaskExists(taskId, topologyName);
        return reportService.checkIfReportExists(taskId);
    }


    /**
     * Retrieves a statistics report for the specified task. Only applicable for tasks executing {link eu.europeana.cloud.service.dps.storm.topologies.validation.topology.ValidationTopology}
     * <p>
     * <p/>
     * <br/><br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding: 6px;'>
     * <strong>Required permissions:</strong>
     * <ul>
     * <li>Authenticated user</li>
     * <li>Read permission for selected task</li>
     * </ul>
     * </div>
     *
     * @param taskId       <strong>REQUIRED</strong> Unique id that identifies the task.
     * @param topologyName <strong>REQUIRED</strong> Name of the topology where the task is submitted.
     * @return Statistics report for the specified task.
     * @summary Retrieve task statistics report
     */
    @GetMapping(path = "{taskId}/statistics", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasPermission(#taskId,'" + TASK_PREFIX + "', read)")
    public StatisticsReport getTaskStatisticsReport(
            @PathVariable String topologyName,
            @PathVariable  String taskId)
            throws AccessDeniedOrTopologyDoesNotExistException, AccessDeniedOrObjectDoesNotExistException {
        assertContainTopology(topologyName);
        reportService.checkIfTaskExists(taskId, topologyName);
        return validationStatisticsService.getTaskStatisticsReport(Long.parseLong(taskId));
    }


    /**
     * Retrieves a list of distinct values and their occurrences for a specific element based on its path}
     * <p>
     * <p/>
     * <br/><br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding: 6px;'>
     * <strong>Required permissions:</strong>
     * <ul>
     * <li>Authenticated user</li>
     * <li>Read permission for selected task</li>
     * </ul>
     * </div>
     *
     * @param taskId       <strong>REQUIRED</strong> Unique id that identifies the task.
     * @param topologyName <strong>REQUIRED</strong> Name of the topology where the task is submitted.
     * @param elementPath  <strong>REQUIRED</strong> Path for specific element.
     * @return List of distinct values and their occurrences.
     */
    @GetMapping(path = "{taskId}/reports/element", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasPermission(#taskId,'" + TASK_PREFIX + "', read)")
    public List<NodeReport> getElementsValues(
            @PathVariable String topologyName,
            @PathVariable  String taskId,
            @NotNull @RequestParam("path") String elementPath)
            throws AccessDeniedOrTopologyDoesNotExistException, AccessDeniedOrObjectDoesNotExistException {
        assertContainTopology(topologyName);
        reportService.checkIfTaskExists(taskId, topologyName);
        return validationStatisticsService.getElementReport(Long.parseLong(taskId), elementPath);
    }

    private void assertContainTopology(String topology) throws AccessDeniedOrTopologyDoesNotExistException {
        if (!topologyManager.containsTopology(topology)) {
            throw new AccessDeniedOrTopologyDoesNotExistException("The topology doesn't exist");
        }
    }
}
