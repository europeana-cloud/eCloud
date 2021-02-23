package eu.europeana.cloud.service.dps.storm.utils;

import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.generated.StormTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TopologySubmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologySubmitter.class);

    public static final String START_TOPOLOGY_ON_LOCAL_CLUSTER_PROPERTY = "startTopologyOnLocalCluster";
    private static final boolean START_ON_LOCAL_CLUSTER = Boolean.getBoolean(START_TOPOLOGY_ON_LOCAL_CLUSTER_PROPERTY);

    public static void submitTopology(String name, Map stormConf, StormTopology topology)
            throws AlreadyAliveException, InvalidTopologyException, AuthorizationException {
        if (START_ON_LOCAL_CLUSTER) {
            LOGGER.warn("Cause \"{}\" property is set true, topology is started in LocalCluster!!!\nThis could be use for tests only!!!\n", START_TOPOLOGY_ON_LOCAL_CLUSTER_PROPERTY);
            new LocalCluster().submitTopology(name, stormConf, topology);
        } else {
            StormSubmitter.submitTopology(name, stormConf, topology);
        }
    }

}
