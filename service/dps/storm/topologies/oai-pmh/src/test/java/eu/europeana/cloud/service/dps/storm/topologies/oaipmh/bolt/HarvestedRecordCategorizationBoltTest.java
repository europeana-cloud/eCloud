package eu.europeana.cloud.service.dps.storm.topologies.oaipmh.bolt;


import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.utils.CategorizationParameters;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.utils.CategorizationResult;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.utils.HarvestedRecordCategorizationService;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HarvestedRecordCategorizationBoltTest {

    @Captor
    ArgumentCaptor<Values> captor = ArgumentCaptor.forClass(Values.class);
    @Mock(name = "harvestedRecordCategorizationService")
    private HarvestedRecordCategorizationService harvestedRecordCategorizationService;
    @Mock(name = "outputCollector")
    private OutputCollector outputCollector;

    @InjectMocks
    private HarvestedRecordCategorizationBolt harvestedRecordCategorizationBolt = new HarvestedRecordCategorizationBolt(null);

    @Before
    public void init() throws IllegalAccessException, MCSException, URISyntaxException {
        MockitoAnnotations.initMocks(this); // initialize all the @Mock objects
    }

    @Test
    public void shouldForwardTupleToNextBoltInCaseOfNonIncrementalProcessing() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = prepareNonIncrementalTuple();
        //when
        harvestedRecordCategorizationBolt.execute(anchorTuple, tuple);
        //then
        verify(outputCollector, never()).emit(eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), any(Tuple.class), anyList());
        verify(outputCollector, times(1)).emit(any(Tuple.class), captor.capture());
        List<Values> allValues = captor.getAllValues();
        allValues.toString();
    }

    @Test
    public void shouldForwardTupleToNextBoltInCaseOfNonExistingIncrementalParameter() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = prepareTupleWithoutIncrementalParameter();
        //when
        harvestedRecordCategorizationBolt.execute(anchorTuple, tuple);
        //then
        verify(outputCollector, never()).emit(eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), any(Tuple.class), anyList());
        verify(outputCollector, times(1)).emit(any(Tuple.class), anyList());
    }

    @Test
    public void shouldCategorizeMessageAsEligibleForProcessing() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = prepareTupleWithoutIncrementalParameter();
        when(harvestedRecordCategorizationService.categorize(any())).thenReturn(
                CategorizationResult
                        .builder()
                        .category(CategorizationResult.Category.ELIGIBLE_FOR_PROCESSING)
                        .build());
        //when
        harvestedRecordCategorizationBolt.execute(anchorTuple, tuple);
        //then
        verify(outputCollector, never()).emit(eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), any(Tuple.class), anyList());
        verify(outputCollector, times(1)).emit(any(Tuple.class), anyList());
    }

    @Test
    public void shouldCategorizeMessageAlreadyProcessed() {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = prepareTupleWithIncrementalParameter();
        when(harvestedRecordCategorizationService.categorize(any())).thenReturn(
                CategorizationResult
                        .builder()
                        .category(CategorizationResult.Category.ALREADY_PROCESSED)
                        .categorizationParameters(
                                CategorizationParameters
                                        .builder()
                                        .recordDateStamp(Instant.now())
                                        .build())
                        .build());
        //when
        harvestedRecordCategorizationBolt.execute(anchorTuple, tuple);
        //then
        verify(outputCollector, times(1)).emit(eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), any(Tuple.class), anyList());
        verify(outputCollector, never()).emit(any(Tuple.class), anyList());
    }


    private StormTaskTuple prepareNonIncrementalTuple() {
        StormTaskTuple tuple = new StormTaskTuple();
        tuple.setTaskId(1);
        tuple.addParameter(PluginParameterKeys.INCREMENTAL_HARVEST, "false");
        tuple.addParameter(PluginParameterKeys.RECORD_DATESTAMP, Instant.now().toString());
        tuple.addParameter(PluginParameterKeys.HARVEST_DATE, Instant.now().toString());
        return tuple;
    }

    private StormTaskTuple prepareTupleWithoutIncrementalParameter() {
        StormTaskTuple tuple = new StormTaskTuple();
        tuple.addParameter(PluginParameterKeys.RECORD_DATESTAMP, Instant.now().toString());
        tuple.addParameter(PluginParameterKeys.HARVEST_DATE, Instant.now().toString());
        return tuple;
    }

    private StormTaskTuple prepareTupleWithIncrementalParameter() {
        StormTaskTuple tuple = new StormTaskTuple();
        tuple.addParameter(PluginParameterKeys.INCREMENTAL_HARVEST, "true");
        tuple.addParameter(PluginParameterKeys.MESSAGE_PROCESSING_START_TIME_IN_MS, "10");
        tuple.addParameter(PluginParameterKeys.RECORD_DATESTAMP, Instant.now().toString());
        tuple.addParameter(PluginParameterKeys.HARVEST_DATE, Instant.now().toString());

        return tuple;
    }
}