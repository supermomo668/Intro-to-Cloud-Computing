package com.cloudcomputing.samza.nycabs.application;

import com.cloudcomputing.samza.nycabs.DriverMatchTask;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.samza.application.TaskApplication;
import org.apache.samza.application.descriptors.TaskApplicationDescriptor;
import org.apache.samza.serializers.JsonSerde;
import org.apache.samza.system.kafka.descriptors.KafkaInputDescriptor;
import org.apache.samza.system.kafka.descriptors.KafkaOutputDescriptor;
import org.apache.samza.system.kafka.descriptors.KafkaSystemDescriptor;
import org.apache.samza.task.StreamTaskFactory;

/*----------------------------------------------------------------
    //Copy the below information for pasting into .properties file and for submitting Task 1!
    //The IP of the master is: 172.31.31.191
    //The IP list of Samza brokers in the cluster is given below for your reference.
    //172.31.22.73:9092,172.31.29.160:9092,172.31.31.191:9092
*/
public class DriverMatchTaskApplication implements TaskApplication {
    // Consider modify this zookeeper address, localhost may not be a good choice.
    // If this task application is executing in slave machine.
    private static final List<String> KAFKA_CONSUMER_ZK_CONNECT = ImmutableList.of("172.31.31.191:2181");

    // Consider modify the bootstrap servers address. This example only cover one
    // address.
    private static final List<String> KAFKA_PRODUCER_BOOTSTRAP_SERVERS = ImmutableList.of("172.31.22.73:9092","172.31.29.160:9092","172.31.31.191:9092");
    private static final Map<String, String> KAFKA_DEFAULT_STREAM_CONFIGS = ImmutableMap.of("replication.factor", "1");

    @Override
    public void describe(TaskApplicationDescriptor taskApplicationDescriptor) {
        // Define a system descriptor for Kafka.
        KafkaSystemDescriptor kafkaSystemDescriptor = new KafkaSystemDescriptor("kafka")
                .withConsumerZkConnect(KAFKA_CONSUMER_ZK_CONNECT)
                .withProducerBootstrapServers(KAFKA_PRODUCER_BOOTSTRAP_SERVERS)
                .withDefaultStreamConfigs(KAFKA_DEFAULT_STREAM_CONFIGS);

        // Hint about streams, please refer to DriverMatchConfig.java
        // We need two input streams "events", "driver-locations", one output stream
        // "match-stream".
        
        // Define your input and output descriptor in here.
        // Reference solution:
            // https://github.com/apache/samza-hello-samza/blob/master/src/main/java/samza/examples/wikipedia/task/application/WikipediaStatsTaskApplication.java
        // Input descriptorS
        KafkaInputDescriptor eventsInputDescriptor =
            kafkaSystemDescriptor.getInputDescriptor("events", new JsonSerde<>());

        KafkaInputDescriptor locInputDescriptor =
            kafkaSystemDescriptor.getInputDescriptor("driver-locations", new JsonSerde<>());
            
        KafkaOutputDescriptor matchOutputDescriptor =
                kafkaSystemDescriptor.getOutputDescriptor("match-stream", new JsonSerde<>());

        // Set the default system descriptor to Kafka, so that it is used for all
        // internal resources, e.g., kafka topic for checkpointing, coordinator stream.
        taskApplicationDescriptor.withDefaultSystem(kafkaSystemDescriptor);

        // Set the input
        taskApplicationDescriptor.withInputStream(eventsInputDescriptor);
        taskApplicationDescriptor.withInputStream(locInputDescriptor);
        // Set the output
        taskApplicationDescriptor.withOutputStream(matchOutputDescriptor);
        
        // Bound you descriptor with your taskApplicationDescriptor in here.
        // Please refer to the same link.
        // Set the task factory
        taskApplicationDescriptor.withTaskFactory((StreamTaskFactory)() -> new DriverMatchTask());
    }
}
