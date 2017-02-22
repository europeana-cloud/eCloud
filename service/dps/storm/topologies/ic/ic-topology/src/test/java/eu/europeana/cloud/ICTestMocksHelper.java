package eu.europeana.cloud;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.europeana.cloud.cassandra.CassandraConnectionProvider;
import eu.europeana.cloud.cassandra.CassandraConnectionProviderSingleton;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.mcs.driver.RepresentationIterator;
import eu.europeana.cloud.service.dps.service.zoo.ZookeeperKillService;
import eu.europeana.cloud.service.dps.storm.topologies.ic.topology.api.ImageConverterServiceImpl;
import eu.europeana.cloud.service.dps.storm.utils.CassandraSubTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTaskInfoDAO;
import org.apache.storm.Config;
import org.apache.storm.testing.MkClusterParam;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * @author krystian.
 */
public class ICTestMocksHelper {
    protected FileServiceClient fileServiceClient;
    protected ImageConverterServiceImpl imageConverterService;
    protected DataSetServiceClient dataSetClient;
    protected RecordServiceClient recordServiceClient;
    protected CassandraTaskInfoDAO taskInfoDAO;
    protected CassandraSubTaskInfoDAO subTaskInfoDAO;
    protected RepresentationIterator representationIterator;

    private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private static Gson gson = new GsonBuilder().create();

    static void prettyPrintJSON(List printIn, String input) {
        String json = prettyGson.toJson(printIn);
        System.out.println("=============================" + input + "=============================\n" + json);
    }

    static String parse(List input) {
        return gson.toJson(input);
    }

    static MkClusterParam prepareMKClusterParm() {
        MkClusterParam mkClusterParam = new MkClusterParam();
        int SUPERVISORS = 4;
        mkClusterParam.setSupervisors(SUPERVISORS);
        Config daemonConf = new Config();
        daemonConf.put(Config.STORM_LOCAL_MODE_ZMQ, false);
        mkClusterParam.setDaemonConf(daemonConf);
        return mkClusterParam;
    }

    protected void mockZookeeperKS() throws Exception {
        ZookeeperKillService zookeeperKillService = Mockito.mock(ZookeeperKillService.class);
        when(zookeeperKillService.hasKillFlag(anyString(), anyLong())).thenReturn(false);
        PowerMockito.whenNew(ZookeeperKillService.class).withAnyArguments().thenReturn(zookeeperKillService);

    }

    protected void mockRecordSC() throws Exception {
        recordServiceClient = Mockito.mock(RecordServiceClient.class);
        PowerMockito.whenNew(RecordServiceClient.class).withAnyArguments().thenReturn(recordServiceClient);

    }

    protected void mockFileSC() throws Exception {
        fileServiceClient = Mockito.mock(FileServiceClient.class);
        PowerMockito.whenNew(FileServiceClient.class).withAnyArguments().thenReturn(fileServiceClient);

    }

    protected void mockDatSetClient() throws Exception {
        dataSetClient = Mockito.mock(DataSetServiceClient.class);
        PowerMockito.whenNew(DataSetServiceClient.class).withAnyArguments().thenReturn(dataSetClient);

    }

    protected void mockImageCS() throws Exception {
        imageConverterService = Mockito.mock(ImageConverterServiceImpl.class);
        PowerMockito.whenNew(ImageConverterServiceImpl.class).withAnyArguments().thenReturn(imageConverterService);
    }


    protected void mockRepresentationIterator() throws Exception {
        representationIterator = Mockito.mock(RepresentationIterator.class);
        PowerMockito.whenNew(RepresentationIterator.class).withAnyArguments().thenReturn(representationIterator);
    }


    protected void mockDPSDAO() throws Exception {
        taskInfoDAO = Mockito.mock(CassandraTaskInfoDAO.class);
        PowerMockito.mockStatic(CassandraTaskInfoDAO.class);
        when(CassandraTaskInfoDAO.getInstance(isA(CassandraConnectionProvider.class))).thenReturn(taskInfoDAO);
        subTaskInfoDAO = Mockito.mock(CassandraSubTaskInfoDAO.class);
        PowerMockito.mockStatic(CassandraSubTaskInfoDAO.class);
        when(CassandraSubTaskInfoDAO.getInstance(isA(CassandraConnectionProvider.class))).thenReturn(subTaskInfoDAO);
        PowerMockito.mockStatic(CassandraConnectionProviderSingleton.class);
        when(CassandraConnectionProviderSingleton.getCassandraConnectionProvider(anyString(), anyInt(), anyString(), anyString(), anyString())).thenReturn(Mockito.mock(CassandraConnectionProvider.class));
    }


}
