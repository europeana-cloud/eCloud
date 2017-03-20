package eu.europeana.cloud.service.dps.storm.io;

import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.RevisionServiceClient;
import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.anyString;

@RunWith(PowerMockRunner.class)
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

		Mockito.verify(testMock, Mockito.times(0)).addDefinedRevisions(Mockito.any(StormTaskTuple.class));
		Mockito.verify(outputCollector, Mockito.times(1)).emit(Mockito.any(Tuple.class), Mockito.any(List.class));
	}

	@Test
	public void methodForAddingRevisionsShouldBeExecuted() throws MalformedURLException, MCSException {
		RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
		testMock.execute(prepareTuple());
		Mockito.verify(testMock, Mockito.times(1)).addDefinedRevisions(Mockito.any(StormTaskTuple.class));
		Mockito.verify(outputCollector, Mockito.times(1)).emit(Mockito.any(Tuple.class), Mockito.any(List.class));
	}

	@Test
	public void malformedUrlExceptionShouldBeHandled() throws MalformedURLException, MCSException {
		RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
		testMock.execute(prepareTupleWithMalformedURL());
		Mockito.verify(testMock, Mockito.times(1)).addDefinedRevisions(Mockito.any(StormTaskTuple.class));
		Mockito.verify(outputCollector, Mockito.times(1)).emit(Mockito.eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), Mockito.any(List.class));
	}

	@Test
	public void mcsExceptionShouldBeHandled() throws MalformedURLException, MCSException {
		Mockito.when(revisionServiceClient.addRevision(anyString(), anyString(), anyString(), Mockito.any(Revision.class))).thenThrow(MCSException.class);
		RevisionWriterBolt testMock = Mockito.spy(revisionWriterBolt);
		testMock.execute(prepareTuple());
		Mockito.verify(testMock, Mockito.times(1)).addDefinedRevisions(Mockito.any(StormTaskTuple.class));
		Mockito.verify(outputCollector, Mockito.times(1)).emit(Mockito.eq(AbstractDpsBolt.NOTIFICATION_STREAM_NAME), Mockito.any(List.class));
	}

	private StormTaskTuple prepareTuple() {
		StormTaskTuple tuple = new StormTaskTuple(123L, "sampleTaskName", "http://sampleFileUrl", null, new HashMap(), Arrays.asList(new Revision()));
		return tuple;
	}

	private StormTaskTuple prepareTupleWithMalformedURL() {
		StormTaskTuple tuple = new StormTaskTuple(123L, "sampleTaskName", "malformedURL", null, new HashMap(), Arrays.asList(new Revision()));
		return tuple;
	}
}