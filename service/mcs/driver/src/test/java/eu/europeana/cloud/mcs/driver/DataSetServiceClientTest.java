package eu.europeana.cloud.mcs.driver;

import co.freeside.betamax.Betamax;
import co.freeside.betamax.Recorder;
import eu.europeana.cloud.common.exceptions.ProviderDoesNotExistException;
import eu.europeana.cloud.common.model.DataSet;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.mcs.driver.exception.DriverException;
import eu.europeana.cloud.service.mcs.exception.DataSetAlreadyExistsException;
import eu.europeana.cloud.service.mcs.exception.DataSetNotExistsException;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.mcs.exception.ProviderNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Ignore;

public class DataSetServiceClientTest {

    @Rule
    public Recorder recorder = new Recorder();

    //TODO clean
    //this is only needed for recording tests
    private final String baseUrl = "http://localhost:8080/ecloud-service-mcs-rest-0.2-SNAPSHOT";

    @Betamax(tape = "dataSets/getDataSetsChunkSuccess")
    @Test
    public void shouldRetrieveDataSetsFirstChunk()
            throws Exception {
        String providerId = "Provider002";
        //the tape was recorded when the result chunk was 100
        int resultSize = 100;
        String startFrom = "dataset000101";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        ResultSlice<DataSet> result = instance.getDataSetsForProviderChunk(providerId, null);
        assertNotNull(result.getResults());
        assertThat(result.getResults().size(), is(resultSize));
        assertThat(result.getNextSlice(), is(startFrom));
    }

    @Betamax(tape = "dataSets/getDataSetsChunkSecondSuccess")
    @Test
    public void shouldRetrieveDataSetsSecondChunk()
            throws Exception {
        String providerId = "Provider002";
        int resultSize = 100;
        String startFrom = "dataset000101";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        ResultSlice<DataSet> result = instance.getDataSetsForProviderChunk(providerId, startFrom);
        assertNotNull(result.getResults());
        assertThat(result.getResults().size(), is(resultSize));
        assertNull(result.getNextSlice());
    }

    @Betamax(tape = "dataSets/getDataSetsChunkNoProvider")
    @Test
    public void shouldNotThrowProviderNotExistsForDataSetsChunk()
            throws Exception {
        String providerId = "notFoundProviderId";
        String startFrom = null;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        ResultSlice<DataSet> result = instance.getDataSetsForProviderChunk(providerId, startFrom);
        assertNotNull(result.getResults());
        assertThat(result.getResults().size(), is(0));
    }

    @Betamax(tape = "dataSets/getDataSetsSuccess")
    @Test
    public void shouldReturnAllDataSets()
            throws Exception {
        String providerId = "Provider002";
        int resultSize = 200;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        List<DataSet> result = instance.getDataSetsForProvider(providerId);
        assertNotNull(result);
        assertThat(result.size(), is(resultSize));
    }

    @Betamax(tape = "dataSets/getDataSetsNoProvider")
    @Test
    public void shouldNotThrowProviderNotExistsForDataSetsAll()
            throws Exception {
        String providerId = "notFoundProviderId";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        List<DataSet> result = instance.getDataSetsForProvider(providerId);
        assertNotNull(result);
        assertThat(result.size(), is(0));
    }

    //to test it you can turn off Cassandra
    @Betamax(tape = "dataSets/getDataSetsChunkInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForGetDataSetsChunk()
            throws Exception {
        String providerId = "Provider001";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.getDataSetsForProviderChunk(providerId, null);
    }

    @Betamax(tape = "dataSets/getDataSetsInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForGetDataSets()
            throws Exception {
        String providerId = "Provider001";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.getDataSetsForProviderChunk(providerId, null);
    }

    @Betamax(tape = "dataSets/createDataSetSuccess")
    @Test
    public void shouldSuccessfullyCreateDataSet()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000008";
        String description = "description01";

        String expectedLocation = baseUrl + "/data-providers/" + providerId + "/data-sets/" + dataSetId;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        URI result = instance.createDataSet(providerId, dataSetId, description);
        assertThat(result.toString(), is(expectedLocation));
    }

    @Betamax(tape = "dataSets/createDataSetConflict")
    @Test(expected = DataSetAlreadyExistsException.class)
    public void shouldThrowDataSetAlreadyExists()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000002";
        String description = "description";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.createDataSet(providerId, dataSetId, description);
    }

    @Betamax(tape = "dataSets/createDataSetProviderNotFound")
    @Test(expected = ProviderNotExistsException.class)
    public void shouldThrowProviderNotExists()
            throws Exception {
        String providerId = "notFoundProviderId";
        String dataSetId = "dataSetId";
        String description = "description";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.createDataSet(providerId, dataSetId, description);
    }

    @Betamax(tape = "dataSets/createDataSetInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForCreateDataSet()
            throws Exception {
        String providerId = "providerId";
        String dataSetId = "dataSetId";
        String description = "description";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.createDataSet(providerId, dataSetId, description);
    }

    @Betamax(tape = "dataSets/getRepresentationsChunkSuccess")
    @Test
    public void shouldRetrieveRepresentationsFirstChunk()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000002";
        //the tape was recorded when the result chunk was 100
        int resultSize = 100;
        String startFrom = "G5DFUSCILJFVGQSEJYFHGY3IMVWWCMI=";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        ResultSlice<Representation> result = instance.getDataSetRepresentationsChunk(providerId, dataSetId, null);
        assertNotNull(result.getResults());
        assertThat(result.getResults().size(), is(resultSize));
        assertThat(result.getNextSlice(), is(startFrom));
    }

    @Betamax(tape = "dataSets/getRepresentationsChunkSecondSuccess")
    @Test
    public void shouldRetrieveRepresentationsSecondChunk()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000002";
        int resultSize = 100;
        String startFrom = "G5DFUSCILJFVGQSEJYFHGY3IMVWWCMI=";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        ResultSlice<Representation> result = instance.getDataSetRepresentationsChunk(providerId, dataSetId, startFrom);
        assertNotNull(result.getResults());
        assertThat(result.getResults().size(), is(resultSize));
        assertNull(result.getNextSlice());
    }

    @Betamax(tape = "dataSets/getRepresentationsChunkDataSetNotExists")
    @Test(expected = DataSetNotExistsException.class)
    public void shouldThrowDataSetNotExistsForRepresentationsChunk()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000042";
        String startFrom = "G5DFUSCILJFVGQSEJYFHGY3IMVWWCMI=";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.getDataSetRepresentationsChunk(providerId, dataSetId, startFrom);
    }

    @Betamax(tape = "dataSets/getRepresentationsSuccess")
    @Test
    public void shouldReturnAllRepresentations()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000002";
        int resultSize = 200;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        List<Representation> result = instance.getDataSetRepresentations(providerId, dataSetId);
        assertNotNull(result);
        assertThat(result.size(), is(resultSize));
    }

    @Betamax(tape = "dataSets/getRepresentationsDataSetNotExists")
    @Test(expected = DataSetNotExistsException.class)
    public void shouldThrowDataSetNotExistsForRepresentationsAll()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000042";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.getDataSetRepresentations(providerId, dataSetId);
    }

    @Betamax(tape = "dataSets/getRepresentationsChunkInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForGetRepresentationsChunk()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000002";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.getDataSetRepresentationsChunk(providerId, dataSetId, null);
    }

    @Betamax(tape = "dataSets/getRepresentationsInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForGetRepresentations()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000002";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.getDataSetRepresentations(providerId, dataSetId);
    }

    //we cannot mock system state change in Betamax
    //because it will not record two different answers for the same request 
    @Betamax(tape = "dataSets/updateDescriptionSuccess")
    @Test
    public void ShouldUpdateDescriptionOfDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String description = "TEST1";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);

        instance.updateDescriptionOfDataSet(providerId, dataSetId, description);
        List<DataSet> dataSets = instance.getDataSetsForProvider(providerId);

        for (DataSet dataSet : dataSets) {
            if (dataSetId.equals(dataSet.getId())) {

                assertThat(dataSet.getDescription(), is(description));
            }

        }

    }

    @Betamax(tape = "dataSets/updateDescriptionEmptySuccess")
    @Test
    public void ShouldUpdateDescriptionOfDataSetToEmpty()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String description = "";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);

        instance.updateDescriptionOfDataSet(providerId, dataSetId, description);
        List<DataSet> dataSets = instance.getDataSetsForProvider(providerId);

        for (DataSet dataSet : dataSets) {
            if (dataSetId.equals(dataSet.getId())) {

                assertThat(dataSet.getDescription(), is(description));
            }

        }

    }

    @Betamax(tape = "dataSets/updateDescriptionNullSuccess")
    @Test
    public void ShouldUpdateDescriptionOfDataSetToNull()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String description = null;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);

        instance.updateDescriptionOfDataSet(providerId, dataSetId, description);
        List<DataSet> dataSets = instance.getDataSetsForProvider(providerId);

        for (DataSet dataSet : dataSets) {
            if (dataSetId.equals(dataSet.getId())) {

                assertNull(dataSet.getDescription());
            }

        }

    }

    @Betamax(tape = "dataSets/updateDescriptionDataSetNotExists")
    @Test(expected = DataSetNotExistsException.class)
    public void shouldThrowDataSetNotExistsForUpdateDescription()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "noSuchDataset";
        String description = "TEST4";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.updateDescriptionOfDataSet(providerId, dataSetId, description);
    }

    @Betamax(tape = "dataSets/updateDescriptionInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForUpdateDescription()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000001";
        String description = "TEST3";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.updateDescriptionOfDataSet(providerId, dataSetId, description);
    }

    @Betamax(tape = "dataSets/deleteDataSetSuccess")
    @Test
    public void shouldDeleteDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000033";
        DataSet dataSet = new DataSet();
        dataSet.setProviderId(providerId);
        dataSet.setId(dataSetId);
        dataSet.setDescription(null);

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.deleteDataSet(providerId, dataSetId);

        List<DataSet> dataSets = instance.getDataSetsForProvider(providerId);

        assertFalse(dataSets.contains(dataSet));
    }

    //TODO test this when fixed: https://jira.man.poznan.pl/jira/browse/ECL-141
    @Ignore
    //@Betamax(tape = "dataSets/deleteDataSetDataSetNotExists")
    @Test(expected = DataSetNotExistsException.class)
    public void shouldThrowDataSetNotExistsForDeleteDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000033";
        DataSet dataSet = new DataSet();
        dataSet.setProviderId(providerId);
        dataSet.setId(dataSetId);
        dataSet.setDescription(null);

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.deleteDataSet(providerId, dataSetId);
    }

    @Betamax(tape = "dataSets/deleteDataSetInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForDeleteDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000033";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.deleteDataSet(providerId, dataSetId);

    }

    private void assertOneCorrectRepresentationVersion(DataSetServiceClient instance, String providerId, String dataSetId, String schemaId, String versionId) throws MCSException {
        List<Representation> result = instance.getDataSetRepresentations(providerId, dataSetId);

        int found = 0;
        for (Representation r : result) {
            if (r.getSchema().equals(schemaId)) {
                assertThat(r.getVersion(), is(versionId));
                found++;
            }
        }
        assertThat(found, is(1));
    }

    @Betamax(tape = "dataSets/assignRepresentationNoVersionSuccess")
    @Test
    public void shouldAssignRepresentationToDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000008";
        String cloudId = "1DZ6HTS415W";
        String schemaId = "schema66";
        //this is the last persistent version
        String versionId = "b95fcda0-994a-11e3-bfe1-1c6f653f6012";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.assignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId, null);

        assertOneCorrectRepresentationVersion(instance, providerId, dataSetId, schemaId, versionId);
    }

    //should not complain about assigning the same representation version again
    //this test does not have sense using Betamax
    //but I wrote it just in case
    @Betamax(tape = "dataSets/assignTheSameRepresentationSuccess")
    @Test
    public void shouldAssignTheSameRepresentationToDataSet()
            throws Exception {

        shouldAssignRepresentationToDataSet();
        shouldAssignRepresentationToDataSet();
    }

    @Betamax(tape = "dataSets/assignRepresentationVersionSuccess")
    @Test
    public void shouldAssignRepresentationVersionToDataSet()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000066";
        String cloudId = "1DZ6HTS415W";
        String schemaId = "schema77";
        String versionId1 = "49398390-9a3f-11e3-9690-1c6f653f6012";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);

        instance.assignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId, versionId1);
        assertOneCorrectRepresentationVersion(instance, providerId, dataSetId, schemaId, versionId1);

    }

    //this test does not have sense using Betamax
    //but I wrote it just in case
    @Betamax(tape = "dataSets/assignRepresentationOtherVersionSecondSuccess")
    @Test
    public void shouldOverrideAssignedRepresentationVersion()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000066";
        String cloudId = "1DZ6HTS415W";
        String schemaId = "schema77";
        String versionId2 = "97dd0b70-9a3f-11e3-9690-1c6f653f6012";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);

        instance.assignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId, versionId2);
        assertOneCorrectRepresentationVersion(instance, providerId, dataSetId, schemaId, versionId2);

    }

    @Betamax(tape = "dataSets/assignRepresentationInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForAssingRepresentationToDataSet()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000015";
        String cloudId = "1DZ6HTS415W";
        String schemaId = "schema66";
        String versionId = "b929f090-994a-11e3-bfe1-1c6f653f6012";

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.assignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId, versionId);

    }

    @Betamax(tape = "dataSets/assignRepresentationRepresentationNotExists")
    @Test(expected = RepresentationNotExistsException.class)
    public void shouldThrowRepresentationNotExistsForAssingRepresentationToDataSet()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "dataset000016";
        String cloudId = "1DZ6HTS415W";
        String schemaId = "noSuchSchema";
        String versionId = null;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.assignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId, versionId);

    }

    @Betamax(tape = "dataSets/assignRepresentationDataSetNotExists")
    @Test(expected = DataSetNotExistsException.class)
    public void shouldThrowDataSetNotExistsForAssingRepresentationToDataSet()
            throws Exception {
        String providerId = "Provider001";
        String dataSetId = "noSuchDataSet";
        String cloudId = "1DZ6HTS415W";
        String schemaId = "schema66";
        String versionId = null;

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.assignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId, versionId);

    }

    //
    @Betamax(tape = "dataSets/unassignRepresentationSuccess")
    @Test
    public void shouldUnassignRepresentationFromDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String cloudId = ""; //TODO EXISTING ASSIGNED
        String schemaId = ""; //TODO EXISTING ASSIGNED

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.unassignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId);

    }

    //should not complain about unassigning not assigned representation
    @Betamax(tape = "dataSets/unassignNotAssignedRepresentationSuccess")
    @Test
    public void shouldUnassignNotAssignedRepresentationFromDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String cloudId = ""; //TODO EXISTING NOT ASSIGNED
        String schemaId = ""; //TODO EXISTING NOT ASSIGNED

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.unassignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId);

    }

    //should not complain about unassigning non-existing representation
    @Betamax(tape = "dataSets/unassignNonExistingRepresentationSuccess")
    @Test
    public void shouldUnassignNonExistingRepresentationFromDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String cloudId = ""; //TODO EXISTING (I think)
        String schemaId = ""; //TODO NON EXISTING

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.unassignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId);

    }

    @Betamax(tape = "dataSets/unassignRepresentationInternalServerError")
    @Test(expected = DriverException.class)
    public void shouldThrowDriverExceptionForUnassingRepresentationFromDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "dataset000002";
        String cloudId = ""; //TODO EXISTING
        String schemaId = ""; //TODO EXISTING

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.unassignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId);

    }

    @Betamax(tape = "dataSets/unassignRepresentationDataSetNotExists")
    @Test(expected = DataSetNotExistsException.class)
    public void shouldThrowDataSetNotExistsForUnassingRepresentationFromDataSet()
            throws Exception {
        String providerId = "Provider002";
        String dataSetId = "noSuchDataSet";
        String cloudId = "noSuchCloudId"; //TODO EXISTING
        String schemaId = "noSuchSchema"; //TODO EXISTING

        DataSetServiceClient instance = new DataSetServiceClient(baseUrl);
        instance.unassignRepresentationToDataSet(providerId, dataSetId, cloudId, schemaId);

    }

}
