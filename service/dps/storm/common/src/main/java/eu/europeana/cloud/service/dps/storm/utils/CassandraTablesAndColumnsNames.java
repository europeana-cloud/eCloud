package eu.europeana.cloud.service.dps.storm.utils;

public class CassandraTablesAndColumnsNames {
    //------- TABLES -------
    public static final String BASIC_INFO_TABLE = "basic_info";
    public static final String NOTIFICATIONS_TABLE = "notifications";
    public static final String ERROR_NOTIFICATIONS_TABLE = "error_notifications";
    public static final String ERROR_COUNTERS_TABLE = "error_counters";
    public static final String GENERAL_STATISTICS_TABLE = "general_statistics";
    public static final String NODE_STATISTICS_TABLE = "node_statistics";
    public static final String ATTRIBUTE_STATISTICS_TABLE = "attribute_statistics";
    public static final String STATISTICS_REPORTS_TABLE = "statistics_reports";
    public static final String TASKS_BY_STATE_TABLE = "tasks_by_task_state";
    public static final String PROCESSED_RECORDS_TABLE = "processed_records";
    public static final String HARVESTED_RECORD_TABLE = "harvested_records";

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
    public static final String RETRY_COUNT = "retry_count";
    public static final String ERRORS = "errors";
    public static final String TASK_INFORMATIONS = "task_informations";

    //------- NOTIFICATION -------
    public static final String NOTIFICATION_TASK_ID = "task_id";
    public static final String NOTIFICATION_BUCKET_NUMBER = "bucket_number";
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
    public static final String ERROR_NOTIFICATION_ADDITIONAL_INFORMATIONS = "additional_informations";


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

    //--------- STATISTICS REPORTS ------------
    public static final String STATISTICS_REPORTS_TASK_ID = "task_id";
    public static final String STATISTICS_REPORTS_REPORT_DATA = "report_data";

    //-------TASKS_BY_STATE_TABLE----------------
    public static final String TASKS_BY_STATE_STATE_COL_NAME = "state";
    public static final String TASKS_BY_STATE_TOPOLOGY_NAME = "topology_name";
    public static final String TASKS_BY_STATE_TASK_ID_COL_NAME = "task_id";
    public static final String TASKS_BY_STATE_APP_ID_COL_NAME = "application_id";
    public static final String TASKS_BY_STATE_TOPIC_NAME_COL_NAME = "topic_name";
    public static final String TASKS_BY_STATE_START_TIME = "start_time";

    //------- PROCESSED_RECORDS -------
    public static final String PROCESSED_RECORDS_TASK_ID = "task_id";
    public static final String PROCESSED_RECORDS_RECORD_ID = "record_id";
    public static final String PROCESSED_RECORDS_ATTEMPT_NUMBER = "attempt_number";
    public static final String PROCESSED_RECORDS_DST_IDENTIFIER = "dst_identifier";
    public static final String PROCESSED_RECORDS_TOPOLOGY_NAME = "topology_name";
    public static final String PROCESSED_RECORDS_STATE = "state";
    public static final String PROCESSED_RECORDS_START_TIME = "start_time";
    public static final String PROCESSED_RECORDS_INFO_TEXT = "info_text";
    public static final String PROCESSED_RECORDS_ADDITIONAL_INFORMATIONS = "additional_informations";
    public static final String PROCESSED_RECORDS_BUCKET_NUMBER = "bucket_number";
    //------- HARVESTED_RECORD -------
    public static final String HARVESTED_RECORD_METIS_DATASET_ID = "metis_dataset_id";
    public static final String HARVESTED_RECORD_BUCKET_NUMBER = "bucket_number";
    public static final String HARVESTED_RECORD_LOCAL_ID = "record_local_id";
    public static final String HARVESTED_RECORD_LATEST_HARVEST_DATE = "latest_harvest_date";
    public static final String HARVESTED_RECORD_LATEST_HARVEST_MD5 = "latest_harvest_md5";
    public static final String HARVESTED_RECORD_PUBLISHED_HARVEST_DATE = "published_harvest_date";
    public static final String HARVESTED_RECORD_PUBLISHED_HARVEST_MD5 = "published_harvest_md5";
    public static final String HARVESTED_RECORD_PREVIEW_HARVEST_DATE = "preview_harvest_date";
    public static final String HARVESTED_RECORD_PREVIEW_HARVEST_MD5 = "preview_harvest_md5";

    private CassandraTablesAndColumnsNames() {
    }
}
