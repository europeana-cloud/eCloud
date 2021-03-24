package eu.europeana.cloud.service.dps.storm.utils;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import eu.europeana.cloud.cassandra.CassandraConnectionProvider;

import java.util.Date;
import java.util.Iterator;
import java.util.Optional;

public class HarvestedRecordDAO extends CassandraDAO {

    private static final int OAI_ID_BUCKET_COUNT = 64;
    private static final String DB_COMMUNICATION_FAILURE_MESSAGE = "Database communication failure";
    private static HarvestedRecordDAO instance;
    private PreparedStatement insertHarvestedRecord;
    private PreparedStatement updateIndexingFields;
    private PreparedStatement findRecord;
    private PreparedStatement findAllRecordInDataset;
    private PreparedStatement deleteRecord;

    public static synchronized HarvestedRecordDAO getInstance(CassandraConnectionProvider cassandra) {
        if (instance == null) {
            instance = new HarvestedRecordDAO(cassandra);
        }
        return instance;
    }

    public HarvestedRecordDAO(CassandraConnectionProvider dbService) {
        super(dbService);
    }

    @Override
    void prepareStatements() {
        insertHarvestedRecord = dbService.getSession().prepare("INSERT INTO "
                + CassandraTablesAndColumnsNames.HARVESTED_RECORD_TABLE + "("
                + CassandraTablesAndColumnsNames.HARVESTED_RECORD_PROVIDER_ID
                + "," + CassandraTablesAndColumnsNames.HARVESTED_RECORD_DATASET_ID
                + "," + CassandraTablesAndColumnsNames.HARVESTED_RECORD_BUCKET_NUMBER
                + "," + CassandraTablesAndColumnsNames.HARVESTED_RECORD_OAI_ID
                + "," + CassandraTablesAndColumnsNames.HARVESTED_RECORD_HARVEST_DATE
                + "," + CassandraTablesAndColumnsNames.HARVESTED_RECORD_MD5
                + ") VALUES(?,?,?,?,?,?);"
        );

        insertHarvestedRecord.setConsistencyLevel(dbService.getConsistencyLevel());

        updateIndexingFields = dbService.getSession().prepare("UPDATE "
                + CassandraTablesAndColumnsNames.HARVESTED_RECORD_TABLE
                + " SET " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_INDEXING_DATE + " = ? , "
                + CassandraTablesAndColumnsNames.HARVESTED_RECORD_INDEXED_HARVEST_DATE + " = ? , "
                + CassandraTablesAndColumnsNames.HARVESTED_RECORD_IGNORED + " = false "
                + " WHERE " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_PROVIDER_ID + " = ? "
                + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_DATASET_ID + " = ? "
                + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_BUCKET_NUMBER + " = ? "
                + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_OAI_ID + " = ? "
        );

        updateIndexingFields.setConsistencyLevel(dbService.getConsistencyLevel());

        findRecord = dbService.getSession().prepare(
                "SELECT * FROM " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_TABLE
                        + " WHERE " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_PROVIDER_ID + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_DATASET_ID + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_BUCKET_NUMBER + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_OAI_ID + " = ? "
        );

        findRecord.setConsistencyLevel(dbService.getConsistencyLevel());

        findAllRecordInDataset = dbService.getSession().prepare(
                "SELECT * FROM " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_TABLE
                        + " WHERE " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_PROVIDER_ID + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_DATASET_ID + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_BUCKET_NUMBER + " = ? "
        );

        findAllRecordInDataset.setConsistencyLevel(dbService.getConsistencyLevel());

        deleteRecord = dbService.getSession().prepare(
                "DELETE FROM " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_TABLE
                        + " WHERE " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_PROVIDER_ID + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_DATASET_ID + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_BUCKET_NUMBER + " = ? "
                        + " AND " + CassandraTablesAndColumnsNames.HARVESTED_RECORD_OAI_ID + " = ? "
        );

        deleteRecord.setConsistencyLevel(dbService.getConsistencyLevel());

    }

    public void insertHarvestedRecord(HarvestedRecord record) {
        dbService.getSession().execute(insertHarvestedRecord.bind(record.getProviderId(), record.getDatasetId(),
                oaiIdBucketNo(record.getOaiId()), record.getOaiId(), record.getHarvestDate(), record.getMd5()));
    }

    public void updateIndexingFields(String providerId, String datasetId, String oaiId, Date indexingDate, Date indexedHarvestDate) {
        RetryableMethodExecutor.executeOnDb(DB_COMMUNICATION_FAILURE_MESSAGE,
                () -> dbService.getSession().execute(updateIndexingFields.bind(indexingDate, indexedHarvestDate, providerId, datasetId, oaiIdBucketNo(oaiId), oaiId)));
    }

    public void deleteRecord(String providerId, String datasetId, String oaiId) {
        dbService.getSession().execute(
                deleteRecord.bind(providerId, datasetId, oaiIdBucketNo(oaiId), oaiId));
    }

    public Optional<HarvestedRecord> findRecord(String providerId, String datasetId, String oaiId) {
        return Optional.ofNullable(dbService.getSession().execute(
                findRecord.bind(providerId, datasetId, oaiIdBucketNo(oaiId), oaiId))
                .one()
        ).map(this::readRecord);
    }

    public Iterator<HarvestedRecord> findDatasetRecords(String providerId, String datasetId) {
        return new BucketRecordIterator<>(OAI_ID_BUCKET_COUNT,
                (bucketNumber -> queryBucket(providerId, datasetId, bucketNumber)),
                this::readRecord);
    }

    private Iterator<Row> queryBucket(String providerId, String datasetId, Integer bucketNumber) {
        return dbService.getSession().execute(
                findAllRecordInDataset.bind(providerId, datasetId, bucketNumber))
                .iterator();
    }

    private HarvestedRecord readRecord(Row row) {
        return HarvestedRecord.builder()
                .providerId(row.getString(CassandraTablesAndColumnsNames.HARVESTED_RECORD_PROVIDER_ID))
                .datasetId(row.getString(CassandraTablesAndColumnsNames.HARVESTED_RECORD_DATASET_ID))
                .oaiId(row.getString(CassandraTablesAndColumnsNames.HARVESTED_RECORD_OAI_ID))
                .harvestDate(row.getTimestamp(CassandraTablesAndColumnsNames.HARVESTED_RECORD_HARVEST_DATE))
                .md5(row.getUUID(CassandraTablesAndColumnsNames.HARVESTED_RECORD_MD5))
                .indexingDate(row.getTimestamp(CassandraTablesAndColumnsNames.HARVESTED_RECORD_INDEXING_DATE))
                .indexedHarvestingDate(row.getTimestamp(CassandraTablesAndColumnsNames.HARVESTED_RECORD_INDEXED_HARVEST_DATE))
                .ignored(row.getBool(CassandraTablesAndColumnsNames.HARVESTED_RECORD_IGNORED))
                .build();
    }

    private int oaiIdBucketNo(String oaiId) {
        return BucketUtils.bucketNumber(oaiId, OAI_ID_BUCKET_COUNT);
    }

}
