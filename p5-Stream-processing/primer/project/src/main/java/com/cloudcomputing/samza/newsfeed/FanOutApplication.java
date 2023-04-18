package com.cloudcomputing.samza.newsfeed;

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

public class FanOutApplication implements TaskApplication{
    // Replace this ip address using your own master node internal ip.
    private static final List<String> KAFKA_CONSUMER_ZK_CONNECT = ImmutableList.of("54.210.218.197:2181");
    // Replace this ip address using your own master & slave node internal ip.
    private static final List<String> KAFKA_PRODUCER_BOOTSTRAP_SERVERS = ImmutableList.of("54.210.218.197:9092","54.87.154.234:9092","52.91.171.10:9092");
    private static final Map<String, String> KAFKA_DEFAULT_STREAM_CONFIGS = ImmutableMap.of("replication.factor", "1");

    @Override
    public void describe(TaskApplicationDescriptor taskApplicationDescriptor){
        KafkaSystemDescriptor kafkaSystemDescriptor =
                new KafkaSystemDescriptor("kafka").withConsumerZkConnect(KAFKA_CONSUMER_ZK_CONNECT)
                        .withProducerBootstrapServers(KAFKA_PRODUCER_BOOTSTRAP_SERVERS)
                        .withDefaultStreamConfigs(KAFKA_DEFAULT_STREAM_CONFIGS);

        // Input descriptor for the driver-locations topic.
        KafkaInputDescriptor followInputDescriptor =
                kafkaSystemDescriptor.getInputDescriptor("newsfeed-follows", new JsonSerde<>());

        KafkaInputDescriptor messageInputDescriptor =
                kafkaSystemDescriptor.getInputDescriptor("newsfeed-messages", new JsonSerde<>());

        KafkaOutputDescriptor kafkaOutputDescriptor =
                kafkaSystemDescriptor.getOutputDescriptor("newsfeed-deliveries", new JsonSerde<>());

        taskApplicationDescriptor.withDefaultSystem(kafkaSystemDescriptor);

        taskApplicationDescriptor.withInputStream(followInputDescriptor);
        taskApplicationDescriptor.withInputStream(messageInputDescriptor);
        taskApplicationDescriptor.withOutputStream(kafkaOutputDescriptor);

        taskApplicationDescriptor.withTaskFactory((StreamTaskFactory) () -> new FanOutTask());
    }
}
