package eu.europeana.cloud.service.dps.storm.topologies.link.check;

import eu.europeana.cloud.service.dps.storm.AbstractDpsBolt;
import eu.europeana.cloud.service.dps.storm.NotificationBolt;
import eu.europeana.cloud.service.dps.storm.NotificationTuple;
import eu.europeana.cloud.service.dps.storm.StormTupleKeys;
import eu.europeana.cloud.service.dps.storm.io.ParseFileForLinkCheckBolt;
import eu.europeana.cloud.service.dps.storm.spout.ECloudSpout;
import eu.europeana.cloud.service.dps.storm.topologies.properties.PropertyFileLoader;
import eu.europeana.cloud.service.dps.storm.utils.TopologiesNames;
import eu.europeana.cloud.service.dps.storm.utils.TopologyHelper;
import eu.europeana.cloud.service.dps.storm.utils.TopologySubmitter;
import org.apache.storm.Config;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.grouping.ShuffleGrouping;
import org.apache.storm.topology.BoltDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Properties;

import static eu.europeana.cloud.service.dps.storm.AbstractDpsBolt.NOTIFICATION_STREAM_NAME;
import static eu.europeana.cloud.service.dps.storm.topologies.properties.TopologyPropertyKeys.*;
import static eu.europeana.cloud.service.dps.storm.utils.TopologyHelper.*;
import static java.lang.Integer.parseInt;

/**
 * Created by Tarek on 12/14/2018.
 */
public class LinkCheckTopology {
    private static Properties topologyProperties = new Properties();
    private static final String TOPOLOGY_PROPERTIES_FILE = "link-check-topology-config.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckTopology.class);

    public LinkCheckTopology(String defaultPropertyFile, String providedPropertyFile) {
        PropertyFileLoader.loadPropertyFile(defaultPropertyFile, providedPropertyFile, topologyProperties);
    }

    public final StormTopology buildTopology(String ecloudMcsAddress) {
        TopologyBuilder builder = new TopologyBuilder();

        String[] topics = getTopics(topologyProperties);
        int i=0;
        for(String topic: topics){
            i++;
            ECloudSpout eCloudSpout = TopologyHelper.createECloudSpout(TopologiesNames.LINKCHECK_TOPOLOGY, topologyProperties,new String[]{topic} );


            builder.setSpout(SPOUT+i, eCloudSpout, (getAnInt(KAFKA_SPOUT_PARALLEL)))
                    .setNumTasks((getAnInt(KAFKA_SPOUT_NUMBER_OF_TASKS)));

        }

        BoltDeclarer firstBoltDeclaration = builder.setBolt(PARSE_FILE_BOLT, new ParseFileForLinkCheckBolt(ecloudMcsAddress),
                1)
                .setNumTasks(1);

        for(int j=1;j<=topics.length;j++){
            firstBoltDeclaration.customGrouping(SPOUT+j, new ShuffleGrouping());
        }


        builder.setBolt(LINK_CHECK_BOLT, new LinkCheckBolt(),
                (getAnInt(LINK_CHECK_BOLT_PARALLEL)))
                .setNumTasks((getAnInt(LINK_CHECK_BOLT_NUMBER_OF_TASKS)))
                .fieldsGrouping(PARSE_FILE_BOLT, new Fields(StormTupleKeys.INPUT_FILES_TUPLE_KEY));

        BoltDeclarer notificationBoltDeclarer = builder.setBolt(NOTIFICATION_BOLT, new NotificationBolt(topologyProperties.getProperty(CASSANDRA_HOSTS),
                        parseInt(topologyProperties.getProperty(CASSANDRA_PORT)),
                        topologyProperties.getProperty(CASSANDRA_KEYSPACE_NAME),
                        topologyProperties.getProperty(CASSANDRA_USERNAME),
                        topologyProperties.getProperty(CASSANDRA_SECRET_TOKEN)),
                getAnInt(NOTIFICATION_BOLT_PARALLEL));

        notificationBoltDeclarer
                .setNumTasks(
                        (getAnInt(NOTIFICATION_BOLT_NUMBER_OF_TASKS)));
        for(int j=1;j<=topics.length;j++) {
            notificationBoltDeclarer.fieldsGrouping(SPOUT+j, NOTIFICATION_STREAM_NAME,
                    new Fields(NotificationTuple.taskIdFieldName));
        }

        notificationBoltDeclarer.fieldsGrouping(PARSE_FILE_BOLT, AbstractDpsBolt.NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName))
                .fieldsGrouping(LINK_CHECK_BOLT, AbstractDpsBolt.NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName));

        return builder.createTopology();
    }

    private static int getAnInt(String propertyName) {
        return parseInt(topologyProperties.getProperty(propertyName));
    }

    public static void main(String[] args) {
        try {
            LOGGER.info("Assembling '{}'", TopologiesNames.INDEXING_TOPOLOGY);
            if (args.length <= 1) {
                String providedPropertyFile = (args.length == 1 ? args[0] : "");

                LinkCheckTopology linkCheckTopology = new LinkCheckTopology(TOPOLOGY_PROPERTIES_FILE, providedPropertyFile);

                String ecloudMcsAddress = topologyProperties.getProperty(MCS_URL);
                StormTopology stormTopology = linkCheckTopology.buildTopology(ecloudMcsAddress);
                Config config = buildConfig(topologyProperties);
                LOGGER.info("Submitting '{}'...", topologyProperties.getProperty(TOPOLOGY_NAME));
                TopologySubmitter.submitTopology(topologyProperties.getProperty(TOPOLOGY_NAME), config, stormTopology);
            } else {
                LOGGER.error("Invalid number of parameters");
            }
        } catch (Exception e) {
            LOGGER.error("General error while setting up topology", e);
        }
    }
}
