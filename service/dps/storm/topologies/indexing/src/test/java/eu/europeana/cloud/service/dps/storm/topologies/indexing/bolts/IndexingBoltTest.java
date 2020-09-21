package eu.europeana.cloud.service.dps.storm.topologies.indexing.bolts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.metis.indexing.DataSetCleanerParameters;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.dps.storm.topologies.indexing.bolts.IndexingBolt.IndexerPoolWrapper;
import eu.europeana.indexing.IndexerPool;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IndexingBoltTest {

    @Mock(name = "outputCollector")
    private OutputCollector outputCollector;

    @Mock(name = "indexerPoolWrapper")
    private IndexerPoolWrapper indexerPoolWrapper;

    @Mock
    private IndexerPool indexerPool;

    @Mock
    private Properties indexingProperties;

    @InjectMocks
    private IndexingBolt indexingBolt = new IndexingBolt(indexingProperties);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Captor
    ArgumentCaptor<Values> captor = ArgumentCaptor.forClass(Values.class);

    @Test
    public void shouldIndexFileForPreviewEnv() throws Exception {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        String targetIndexingEnv = "PREVIEW";
        StormTaskTuple tuple = mockStormTupleFor(targetIndexingEnv);
        mockIndexerFactoryFor(null);
        //when
        indexingBolt.execute(anchorTuple, tuple);
        //then
        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(Tuple.class), captor.capture());
        Values capturedValues = captor.getValue();
        assertEquals(8, capturedValues.size());
        assertEquals("sampleResourceUrl", capturedValues.get(2));
        Map<String, String> parameters = (Map<String, String>) capturedValues.get(4);
        assertEquals(5, parameters.size());
    }

    @Test
    public void shouldIndexFilePublishEnv() throws Exception {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        String targetIndexingEnv = "PUBLISH";
        StormTaskTuple tuple = mockStormTupleFor(targetIndexingEnv);
        mockIndexerFactoryFor(null);
        //when
        indexingBolt.execute(anchorTuple, tuple);
        //then
        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(Tuple.class), captor.capture());


        Values capturedValues = captor.getValue();
        assertEquals(8, capturedValues.size());
        assertEquals("sampleResourceUrl", capturedValues.get(2));
        Map<String, String> parameters = (Map<String, String>) capturedValues.get(4);
        assertEquals(5, parameters.size());
    }


    @Test
    public void shouldEmitErrorNotificationForIndexerConfiguration() throws IndexingException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = mockStormTupleFor("PREVIEW");
        mockIndexerFactoryFor(IndexerRelatedIndexingException.class);
        //when
        indexingBolt.execute(anchorTuple, tuple);
        //then
        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(String.class), any(Tuple.class), captor.capture());
        Values capturedValues = captor.getValue();
        Map val = (Map) capturedValues.get(2);

        assertEquals("sampleResourceUrl", val.get("resource"));
        Assert.assertTrue(val.get("additionalInfo").toString().contains("Error while indexing"));
    }

    @Test
    public void shouldEmitErrorNotificationForIndexing() throws IndexingException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = mockStormTupleFor("PUBLISH");
        mockIndexerFactoryFor(IndexerRelatedIndexingException.class);
        //when
        indexingBolt.execute(anchorTuple, tuple);
        //then
        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(String.class), any(Tuple.class), captor.capture());
        Values capturedValues = captor.getValue();
        Map val = (Map) capturedValues.get(2);

        assertEquals("sampleResourceUrl", val.get("resource"));
        Assert.assertTrue(val.get("additionalInfo").toString().contains("Error while indexing"));

    }

    @Test
    public void shouldThrowExceptionWhenDateIsUnParsable() throws IndexingException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = mockStormTupleFor("PREVIEW");
        tuple.getParameters().remove(PluginParameterKeys.METIS_RECORD_DATE);
        tuple.addParameter(PluginParameterKeys.METIS_RECORD_DATE, "UN_PARSABLE_DATE");
        //when
        indexingBolt.execute(anchorTuple, tuple);

        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(String.class), any(Tuple.class), captor.capture());
        Values capturedValues = captor.getValue();
        Map val = (Map) capturedValues.get(2);

        assertEquals("sampleResourceUrl", val.get("resource"));
        Assert.assertTrue(val.get("info_text").toString().contains("Could not parse RECORD_DATE parameter"));
        Assert.assertTrue(val.get("state").toString().equals("ERROR"));
    }

    @Test
    public void shouldThrowExceptionForUnknownEnv() throws IndexingException {
        //given
        Tuple anchorTuple = mock(TupleImpl.class);
        StormTaskTuple tuple = mockStormTupleFor("UNKNOWN_ENVIRONMENT");
        mockIndexerFactoryFor(RuntimeException.class);
        //when
        indexingBolt.execute(anchorTuple, tuple);

        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(String.class), any(Tuple.class), captor.capture());
        Values capturedValues = captor.getValue();
        Map val = (Map) capturedValues.get(2);

        assertEquals("sampleResourceUrl", val.get("resource"));
        Assert.assertTrue(val.get("state").toString().equals("ERROR"));
    }

    private StormTaskTuple mockStormTupleFor(final String targetDatabase) {
        //
        return new StormTaskTuple(
                1,
                "taskName",
                "sampleResourceUrl",
                new byte[]{'a', 'b', 'c'},
                new HashMap<String, String>() {
                    {
                        put(PluginParameterKeys.METIS_TARGET_INDEXING_DATABASE, targetDatabase);
                        DateFormat dateFormat = new SimpleDateFormat(IndexingBolt.DATE_FORMAT, Locale.US);
                        put(PluginParameterKeys.METIS_RECORD_DATE, dateFormat.format(new Date()));
                        put(PluginParameterKeys.MESSAGE_PROCESSING_START_TIME_IN_MS, "0");
                    }
                }, new Revision());
    }

    private void mockIndexerFactoryFor(Class clazz) throws IndexingException {
        when(indexerPoolWrapper.getIndexerPool(Mockito.anyString(), Mockito.anyString())).thenReturn(indexerPool);
        if (clazz != null) {
            doThrow(clazz).when(indexerPool).index(Mockito.anyString(), Mockito.any(Date.class), Mockito.anyBoolean(), Mockito.anyList(), Mockito.anyBoolean());
        }
    }
}
