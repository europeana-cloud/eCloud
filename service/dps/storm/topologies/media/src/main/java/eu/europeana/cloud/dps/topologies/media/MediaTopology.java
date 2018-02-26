package eu.europeana.cloud.dps.topologies.media;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.grouping.ShuffleGrouping;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.shade.org.yaml.snakeyaml.Yaml;
import org.apache.storm.shade.org.yaml.snakeyaml.constructor.SafeConstructor;
import org.apache.storm.topology.IRichSpout;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europeana.cloud.dps.topologies.media.support.DummySpout;
import eu.europeana.cloud.dps.topologies.media.support.StatsInitTupleData;
import eu.europeana.cloud.dps.topologies.media.support.StatsTupleData;
import eu.europeana.cloud.dps.topologies.media.support.Util;
import eu.europeana.cloud.service.dps.storm.topologies.properties.TopologyPropertyKeys;

public class MediaTopology {
	
	public static final String SOURCE_FIELD = "source";
	
	private static final Logger logger = LoggerFactory.getLogger(MediaTopology.class);
	
	private static Config conf;
	
	public static void main(String[] args)
			throws AlreadyAliveException, InvalidTopologyException, AuthorizationException {
		
		loadConfig();
		
		final boolean isTest = args.length > 0;
		
		TopologyBuilder builder = new TopologyBuilder();
		String topologyName = (String) conf.get(TopologyPropertyKeys.TOPOLOGY_NAME);
		
		IRichSpout baseSpout = isTest ? new DummySpout() : new KafkaSpout(Util.getKafkaSpoutConfig(conf));
		builder.setSpout("source", new DataSetReaderSpout(baseSpout), 1);
		
		builder.setBolt("downloadBolt", new DownloadBolt(),
				(int) conf.get("MEDIATOPOLOGY_PARALLEL_HINT_DOWNLOAD"))
				.fieldsGrouping("source", new Fields(SOURCE_FIELD));
		builder.setBolt("processingBolt", new ProcessingBolt(),
				(int) conf.get("MEDIATOPOLOGY_PARALLEL_HINT_PROCESSING"))
				.customGrouping("downloadBolt", new ShuffleGrouping());
		
		builder.setBolt("statsBolt", new StatsBolt(), 1)
				.shuffleGrouping("source", StatsInitTupleData.STREAM_ID)
				.shuffleGrouping("downloadBolt", StatsTupleData.STREAM_ID)
				.shuffleGrouping("processingBolt", StatsTupleData.STREAM_ID);
		
		if (isTest) {
			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology(topologyName, conf, builder.createTopology());
			Utils.sleep(600000);
			cluster.killTopology(topologyName);
			cluster.shutdown();
		} else {
			StormSubmitter.submitTopology(topologyName, conf, builder.createTopology());
		}
		
	}
	
	private static void loadConfig() {
		conf = new Config();
		Yaml yamlConf = new Yaml(new SafeConstructor());
		String configFileName = "media-topology-config.yaml";
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName)) {
			conf.putAll((Map) yamlConf.load(is));
		} catch (IOException e) {
			throw new RuntimeException("Built in config could not be loaded: " + configFileName, e);
		}
		try (InputStream is = new FileInputStream(configFileName)) {
			conf.putAll((Map) yamlConf.load(is));
		} catch (IOException e) {
			logger.warn("Could not load custom config file, using defaults");
			logger.debug("Custom config load problem", e);
		}
	}
}
