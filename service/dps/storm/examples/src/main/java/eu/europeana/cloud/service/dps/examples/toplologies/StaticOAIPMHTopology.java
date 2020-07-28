package eu.europeana.cloud.service.dps.examples.toplologies;

import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.OAIPHMHarvestingTopology;
import eu.europeana.cloud.service.dps.storm.utils.TopologyHelper;
import org.apache.storm.LocalCluster;
import org.apache.storm.utils.Utils;

import static eu.europeana.cloud.service.dps.storm.utils.TopologiesNames.OAI_TOPOLOGY;

//<Environment name="dps/topology/availableTopics" type="java.lang.String" value="oai_topology:oai_topology_1,oai_topology_2,oai_topology_3;another_topology:topic1,topic2,topic3"/>

public class StaticOAIPMHTopology {

    public static void main(String[] args) {
        OAIPHMHarvestingTopology oaiphmHarvestingTopology
//                = new  OAIPHMHarvestingTopology("oai-topology-config.properties", null);
                = new  OAIPHMHarvestingTopology("oai-topology-config.properties", "/home/arek/prj/europeana-ws/europeana-cloud/service/dps/storm/examples/src/main/resources/oai-topology-config.local.properties");

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology(OAI_TOPOLOGY, TopologyHelper.buildConfig(OAIPHMHarvestingTopology.getProperties(), true),
                oaiphmHarvestingTopology.buildTopology());

        Utils.sleep((long)(1000*60*1000)); //1000 minutes
        cluster.killTopology(OAI_TOPOLOGY);
        cluster.shutdown();
    }

}
