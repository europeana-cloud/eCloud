package eu.europeana.cloud.service.dps.storm.topologies.oaipmh.bolt;

import eu.europeana.cloud.service.dps.OAIPMHHarvestingDetails;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RecordHarvestingBolt}
 */

public class RecordHarvestingBoltTest {
    @Mock
    private OutputCollector outputCollector;

    @Mock
    private OaiHarvester harvester;

    @InjectMocks
    private final RecordHarvestingBolt recordHarvestingBolt = new RecordHarvestingBolt();

    private static InputStream getFileContentAsStream(String name) {
        return RecordHarvestingBoltTest.class.getResourceAsStream(name);
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void harvestingForAllParametersSpecified() throws IOException, HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        OaiRecord oaiRecord = new OaiRecord(new OaiRecordHeader("id", false, Instant.now()), fileContent("/sampleEDMRecord.xml"));
        when(harvester.harvestRecord(any(), anyString())).thenReturn(oaiRecord);
        StormTaskTuple task = taskWithAllNeededParameters();
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifySuccessfulEmit();
        verify(spiedTask).setFileData(Mockito.any(InputStream.class));
    }

    @Test
    public void shouldHarvestRecordInEDMAndExtractIdentifiers() throws IOException, HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        OaiRecord oaiRecord = new OaiRecord(new OaiRecordHeader("id", false, Instant.now()), fileContent("/sampleEDMRecord.xml"));
        when(harvester.harvestRecord(any(), anyString())).thenReturn(oaiRecord);
        StormTaskTuple task = taskWithAllNeededParameters();
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifySuccessfulEmit();

        verify(spiedTask).setFileData(Mockito.any(InputStream.class));
        assertEquals("http://more.locloud.eu/object/DCU/24927017", spiedTask.getParameter(PluginParameterKeys.ADDITIONAL_LOCAL_IDENTIFIER));
        assertEquals("/2020739_Ag_EU_CARARE_2Cultur/object_DCU_24927017", spiedTask.getParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER));
    }

    private Supplier<byte[]> fileContent(String fileName){
        InputStream fileContentAsStream = getFileContentAsStream(fileName);
        return () -> {
            try {
                return fileContentAsStream.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };
    }

    @Test
    public void shouldHarvestRecordInEDMAndNotUseHeaderIdentifierIfParameterIsDifferentThanTrue() throws IOException, HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        OaiRecord oaiRecord = new OaiRecord(new OaiRecordHeader("id", false, Instant.now()), fileContent("/sampleEDMRecord.xml"));
        when(harvester.harvestRecord(any(), anyString())).thenReturn(oaiRecord);

        StormTaskTuple task = taskWithGivenValueOfUseHeaderIdentifiersParameter("blablaba");
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifySuccessfulEmit();

        verify(spiedTask).setFileData(Mockito.any(InputStream.class));
        assertEquals("http://more.locloud.eu/object/DCU/24927017", spiedTask.getParameter(PluginParameterKeys.ADDITIONAL_LOCAL_IDENTIFIER));
        assertEquals("/2020739_Ag_EU_CARARE_2Cultur/object_DCU_24927017", spiedTask.getParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER));
    }

    @Test
    public void shouldHarvestRecordInEDMAndUseHeaderIdentifierIfSpecifiedInTaskParameters() throws IOException, HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        OaiRecord oaiRecord = new OaiRecord(new OaiRecordHeader("id", false, Instant.now()), fileContent("/sampleEDMRecord.xml"));
        when(harvester.harvestRecord(any(), anyString())).thenReturn(oaiRecord);

        StormTaskTuple task = taskWithGivenValueOfUseHeaderIdentifiersParameter("true");
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifySuccessfulEmit();

        verify(spiedTask).setFileData(Mockito.any(InputStream.class));
        assertNull(spiedTask.getParameter(PluginParameterKeys.ADDITIONAL_LOCAL_IDENTIFIER));
        assertEquals("http://data.europeana.eu/item/2064203/o_aj_kk_tei_3", spiedTask.getParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER));
    }

    @Test
    public void shouldHarvestRecordInEDMAndUseHeaderIdentifierAndTrimItIfSpecifiedInTaskParameters() throws IOException, HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        OaiRecord oaiRecord = new OaiRecord(new OaiRecordHeader("id", false, Instant.now()), fileContent("/sampleEDMRecord.xml"));
        when(harvester.harvestRecord(any(), anyString())).thenReturn(oaiRecord);

        StormTaskTuple task = taskWithGivenValueOfUseHeaderIdentifiersAndTrimmingPrefix("true");
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifySuccessfulEmit();

        verify(spiedTask).setFileData(Mockito.any(InputStream.class));
        assertNull(spiedTask.getParameter(PluginParameterKeys.ADDITIONAL_LOCAL_IDENTIFIER));
        assertEquals("/item/2064203/o_aj_kk_tei_3", spiedTask.getParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER));
    }

    @Test
    public void shouldEmitErrorOnHarvestingExceptionWhenCannotExctractEuropeanaIdFromEDM() throws HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        OaiRecord oaiRecord = new OaiRecord(new OaiRecordHeader("id", false, Instant.now()), fileContent("/corruptedEDMRecord.xml"));
        when(harvester.harvestRecord(any(), anyString())).thenReturn(oaiRecord);
        StormTaskTuple task = taskWithAllNeededParameters();
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifyErrorEmit();
    }

    @Test
    public void shouldEmitErrorOnHarvestingException() throws HarvesterException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);

        when(harvester.harvestRecord(any(), anyString())).thenThrow(new HarvesterException("Some!"));
        StormTaskTuple task = taskWithAllNeededParameters();
        StormTaskTuple spiedTask = spy(task);

        //when
        recordHarvestingBolt.execute(anchorTuple, spiedTask);

        //then
        verifyErrorEmit();
    }

    @Test
    public void harvestingForEmptyUrl() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple task = taskWithoutResourceUrl();

        //when
        recordHarvestingBolt.execute(anchorTuple, task);

        //then
        verifyErrorEmit();
    }

    @Test
    public void harvestingForEmptyRecordId() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple task = taskWithoutRecordId();

        //when
        recordHarvestingBolt.execute(anchorTuple, task);

        //then
        verifyErrorEmit();
    }

    @Test
    public void harvestForEmptyPrefix() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple task = taskWithoutPrefix();

        //when
        recordHarvestingBolt.execute(anchorTuple, task);

        //then
        verifyErrorEmit();
    }

    private StormTaskTuple taskWithGivenValueOfUseHeaderIdentifiersAndTrimmingPrefix(String paramValue) {
        StormTaskTuple stormTaskTuple = taskWithGivenValueOfUseHeaderIdentifiersParameter(paramValue);
        stormTaskTuple.addParameter(PluginParameterKeys.MIGRATION_IDENTIFIER_PREFIX, "http://data.europeana.eu");
        return stormTaskTuple;
    }

    private StormTaskTuple taskWithGivenValueOfUseHeaderIdentifiersParameter(String paramValue) {
        StormTaskTuple stormTaskTuple = taskWithAllNeededParameters();
        stormTaskTuple.addParameter(PluginParameterKeys.USE_DEFAULT_IDENTIFIERS, paramValue);
        stormTaskTuple.addParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER, "http://data.europeana.eu/item/2064203/o_aj_kk_tei_3");
        return stormTaskTuple;
    }

    private StormTaskTuple taskWithAllNeededParameters() {
        StormTaskTuple task = new StormTaskTuple();
        OAIPMHHarvestingDetails details = new OAIPMHHarvestingDetails();
        task.setSourceDetails(details);
        task.addParameter(PluginParameterKeys.DPS_TASK_INPUT_DATA, "urlToOAIEndpoint");
        task.addParameter(PluginParameterKeys.MESSAGE_PROCESSING_START_TIME_IN_MS, "0");
        task.addParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER, "oaiIdentifier");
        task.addParameter(PluginParameterKeys.SCHEMA_NAME, "schema");
        task.addParameter(PluginParameterKeys.METIS_DATASET_ID, "2020739_Ag_EU_CARARE_2Culture");
        return task;
    }

    private StormTaskTuple taskWithoutResourceUrl() {
        StormTaskTuple task = new StormTaskTuple();
        OAIPMHHarvestingDetails details = new OAIPMHHarvestingDetails("schema");
        task.addParameter(PluginParameterKeys.MESSAGE_PROCESSING_START_TIME_IN_MS, "0");
        task.setSourceDetails(details);
        return task;
    }

    private StormTaskTuple taskWithoutRecordId() {
        StormTaskTuple task = new StormTaskTuple();
        OAIPMHHarvestingDetails details = new OAIPMHHarvestingDetails();
        task.setSourceDetails(details);
        task.addParameter(PluginParameterKeys.DPS_TASK_INPUT_DATA, "urlToOAIEndpoint");
        task.addParameter(PluginParameterKeys.SCHEMA_NAME, "schema");
        task.addParameter(PluginParameterKeys.MESSAGE_PROCESSING_START_TIME_IN_MS, "0");
        return task;
    }

    private StormTaskTuple taskWithoutPrefix() {
        StormTaskTuple task = new StormTaskTuple();
        OAIPMHHarvestingDetails details = new OAIPMHHarvestingDetails();
        task.addParameter(PluginParameterKeys.DPS_TASK_INPUT_DATA, "urlToOAIEndpoint");
        task.addParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER, "oaiIdentifier");
        task.addParameter(PluginParameterKeys.MESSAGE_PROCESSING_START_TIME_IN_MS, "0");
        task.setSourceDetails(details);
        return task;
    }

    /**
     * Checks if emit to standard stream occured
     */
    private void verifySuccessfulEmit() {
        verify(outputCollector, times(1)).emit(Mockito.any(Tuple.class), Mockito.anyList());
        verify(outputCollector, times(0)).emit(eq("NotificationStream"), Mockito.any(Tuple.class), Mockito.anyList());
    }

    /**
     * Checks if emit to error stream occured
     */
    private void verifyErrorEmit() {

        verify(outputCollector, times(1)).emit(eq("NotificationStream"), Mockito.any(Tuple.class), Mockito.anyList());
        verify(outputCollector, times(0)).emit(Mockito.any(Tuple.class), Mockito.anyList());
    }
}
