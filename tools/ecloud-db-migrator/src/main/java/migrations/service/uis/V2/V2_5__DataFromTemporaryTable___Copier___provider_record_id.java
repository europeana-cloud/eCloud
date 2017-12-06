package migrations.service.uis.V2;

import com.contrastsecurity.cassandra.migration.api.JavaMigration;
import com.datastax.driver.core.*;
import eu.europeana.cloud.common.utils.*;
import eu.europeana.cloud.service.commons.utils.BucketSize;
import eu.europeana.cloud.service.commons.utils.BucketsHandler;

import java.util.UUID;

/**
 * @author Tarek.
 */
public class V2_5__DataFromTemporaryTable___Copier___provider_record_id implements JavaMigration {

    private static final String SOURCE_TABLE = "provider_record_id_copy";
    private static final String TARGET_TABLE = "provider_record_id";
    private static final String PROVIDER_RECORD_ID_BUCKETS_TABLE = "provider_record_id_buckets";

    PreparedStatement selectFromTemporaryStatement;
    PreparedStatement insertToProviderRecordIdStatement;


    private void initStatements(Session session) {

        final String selectFromTemporary = "SELECT "
                + " provider_id, record_id, cloud_id "
                + " FROM " + SOURCE_TABLE + " ;\n";

        final String insertToProviderRecordId = "INSERT INTO "
                + TARGET_TABLE + " (provider_id,bucket_id, record_id, cloud_id) "
                + "VALUES (?,?,?,?);\n";

        selectFromTemporaryStatement = session.prepare(selectFromTemporary);
        selectFromTemporaryStatement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        insertToProviderRecordIdStatement = session.prepare(insertToProviderRecordId);
        insertToProviderRecordIdStatement.setConsistencyLevel(ConsistencyLevel.QUORUM);
    }

    @Override
    public void migrate(Session session) {
        BucketsHandler bucketsHandler = new BucketsHandler(session);
        initStatements(session);
        BoundStatement boundStatement = selectFromTemporaryStatement.bind();
        boundStatement.setFetchSize(100);
        ResultSet rs = session.execute(boundStatement);
        for (Row providerRecordIdRow : rs) {
            Bucket bucket = bucketsHandler.getCurrentBucket(PROVIDER_RECORD_ID_BUCKETS_TABLE, providerRecordIdRow.getString("provider_id"));
            if (bucket == null || bucket.getRowsCount() >= BucketSize.PROVIDER_RECORD_ID_TABLE) {
                bucket = new Bucket(providerRecordIdRow.getString("provider_id"), new com.eaio.uuid.UUID().toString(), 0);
            }
            bucketsHandler.increaseBucketCount(PROVIDER_RECORD_ID_BUCKETS_TABLE, bucket);
            insertToProviderRecordIdTable(session, bucket.getBucketId(), providerRecordIdRow);
        }
    }


    private void insertToProviderRecordIdTable(Session session, String bucketId, Row providerRecordIdRow) {
        BoundStatement boundStatement = insertToProviderRecordIdStatement.bind(
                providerRecordIdRow.getString("provider_id"),
                UUID.fromString(bucketId),
                providerRecordIdRow.getString("record_id"),
                providerRecordIdRow.getString("cloud_id")
        );
        session.execute(boundStatement);
    }
}