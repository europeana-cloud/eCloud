package eu.europeana.cloud.service.dps.service.cassandra;

/**
 * @author Pavel Kefurt <Pavel.Kefurt@gmail.com>
 */
public class CassandraTablesAndColumnsNames {
    //------- TABLES -------
    public static final String BASIC_INFO_TABLE = "basic_info";
    public static final String NOTIFICATIONS_TABLE = "notifications";
    public static final String ERROR_NOTIFICATIONS_TABLE = "error_notifications";
    public static final String ERROR_COUNTERS_TABLE = "error_counters";
    public static final String GENERAL_STATISTICS_TABLE = "general_statistics";
    public static final String NODE_STATISTICS_TABLE = "node_statistics";
    public static final String ATTRIBUTE_STATISTICS_TABLE = "attribute_statistics";

    //------- BASIC INFO -------
    public static final String BASIC_TASK_ID = "task_id";
    public static final String BASIC_TOPOLOGY_NAME = "topology_name";
    public static final String BASIC_EXPECTED_SIZE = "expected_size";
    public static final String STATE = "state";
    public static final String INFO = "info";
    public static final String START_TIME = "start_time";
    public static final String FINISH_TIME = "finish_time";
    public static final String SENT_TIME = "sent_time";
    public static final String PROCESSED_FILES_COUNT = "processed_files_count";
    public static final String ERRORS = "errors";


    //------- NOTIFICATION -------
    public static final String NOTIFICATION_TASK_ID = "task_id";
    public static final String NOTIFICATION_RESOURCE_NUM = "resource_num";
    public static final String NOTIFICATION_TOPOLOGY_NAME = "topology_name";
    public static final String NOTIFICATION_RESOURCE = "resource";
    public static final String NOTIFICATION_STATE = "state";
    public static final String NOTIFICATION_INFO_TEXT = "info_text";
    public static final String NOTIFICATION_ADDITIONAL_INFORMATIONS = "additional_informations";
    public static final String NOTIFICATION_RESULT_RESOURCE = "result_resource";


    //-------- ERROR NOTIFICATION ---------
    public static final String ERROR_NOTIFICATION_TASK_ID = "task_id";
    public static final String ERROR_NOTIFICATION_ERROR_TYPE = "error_type";
    public static final String ERROR_NOTIFICATION_ERROR_MESSAGE = "error_message";
    public static final String ERROR_NOTIFICATION_RESOURCE = "resource";


    //-------- ERROR COUNTERS ----------
    public static final String ERROR_COUNTERS_TASK_ID = "task_id";
    public static final String ERROR_COUNTERS_ERROR_TYPE = "error_type";
    public static final String ERROR_COUNTERS_COUNTER = "error_count";


    //-------- GENERAL STATISTICS ----------
    public static final String GENERAL_STATISTICS_TASK_ID = "task_id";
    public static final String GENERAL_STATISTICS_PARENT_XPATH = "parent_xpath";
    public static final String GENERAL_STATISTICS_NODE_XPATH = "node_xpath";
    public static final String GENERAL_STATISTICS_OCCURRENCE = "occurrence";


    //-------- NODE STATISTICS ----------
    public static final String NODE_STATISTICS_TASK_ID = "task_id";
    public static final String NODE_STATISTICS_NODE_XPATH = "node_xpath";
    public static final String NODE_STATISTICS_VALUE = "node_value";
    public static final String NODE_STATISTICS_OCCURRENCE = "occurrence";


    //-------- ATTRIBUTE STATISTICS ----------
    public static final String ATTRIBUTE_STATISTICS_TASK_ID = "task_id";
    public static final String ATTRIBUTE_STATISTICS_NODE_XPATH = "node_xpath";
    public static final String ATTRIBUTE_STATISTICS_NODE_VALUE = "node_value";
    public static final String ATTRIBUTE_STATISTICS_NAME = "attribute_name";
    public static final String ATTRIBUTE_STATISTICS_VALUE = "attribute_value";
    public static final String ATTRIBUTE_STATISTICS_OCCURRENCE = "occurrence";
}
