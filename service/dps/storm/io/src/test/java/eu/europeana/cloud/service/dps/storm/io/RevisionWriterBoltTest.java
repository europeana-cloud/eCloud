package eu.europeana.cloud.service.dps.storm.io;

import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.mcs.driver.RevisionServiceClient;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.storm.task.OutputCollector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;

public class RevisionWriterBoltTest {

    @Mock(name = "outputCollector")
    private OutputCollector outputCollector;

    @Mock(name = "revisionsClient")
    private RevisionServiceClient revisionServiceClient;

    @InjectMocks
    private RevisionWriterBolt revisionWriterBolt = new RevisionWriterBolt("http://sample.ecloud.com/");

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void nothingShouldBeAddedForEmptyRevisionsList() throws MCSException, URISyntaxException, MalformedURLException {
        RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
        testMock.execute(new StormTaskTuple());

        Mockito.verify(revisionServiceClient, Mockito.times(0)).addRevision(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), any(Revision.class),anyString(),anyString());
        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(List.class));
    }

    @Test
    public void methodForAddingRevisionsShouldBeExecuted() throws MalformedURLException, MCSException {
        RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
        testMock.execute(prepareTuple());
        Mockito.verify(revisionServiceClient, Mockito.times(1)).addRevision(any(), any(), any(), any(Revision.class),anyString(),any());
        Mockito.verify(outputCollector, Mockito.times(1)).emit(any(List.class));
    }

    @Test
    public void malformedUrlExceptionShouldBeHandled() throws MalformedURLException, MCSException {
        RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
        testMock.execute(prepareTupleWithMalformedURL());
        Mockito.verify(revisionServiceClient, Mockito.times(0)).addRevision(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), any(Revision.class),anyString(),anyString());
        Mockito.verify(outputCollector, Mockito.times(1)).emit(Mockito.eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), any(List.class));
    }

    @Test
    public void mcsExceptionShouldBeHandledWithRetries() throws MalformedURLException, MCSException {
        Mockito.when(revisionServiceClient.addRevision(any(), any(), any(), any(Revision.class),anyString(),any())).thenThrow(MCSException.class);
        RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
        testMock.execute(prepareTuple());
        Mockito.verify(revisionServiceClient, Mockito.times(4)).addRevision(any(), any(), any(), any(Revision.class),anyString(),any());
        Mockito.verify(outputCollector, Mockito.times(1)).emit(Mockito.eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), any(List.class));

    }

    private StormTaskTuple prepareTuple() {
        StormTaskTuple tuple = new StormTaskTuple(123L, "sampleTaskName", "http://inputFileUrl", null, new HashMap(), new Revision());
        tuple.addParameter(PluginParameterKeys.OUTPUT_URL, "http://sampleFileUrl");

        return tuple;
    }

    private StormTaskTuple prepareTupleWithMalformedURL() {
        StormTaskTuple tuple = new StormTaskTuple(123L, "sampleTaskName", "http://inputFileUrl", null, new HashMap(), new Revision());
        tuple.addParameter(PluginParameterKeys.OUTPUT_URL, "malformedURL");
        return tuple;
    }
}
