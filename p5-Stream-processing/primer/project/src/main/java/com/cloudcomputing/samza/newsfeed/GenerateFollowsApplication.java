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

public class GenerateFollowsApplication implements TaskApplication {
    // Replace this ip address using your own master node internal ip.
    private static final List<String> KAFKA_CONSUMER_ZK_CONNECT = ImmutableList.of("54.210.218.197:2181");
    // Replace this ip address using your own master & slave node internal ip.
    private static final List<String> KAFKA_PRODUCER_BOOTSTRAP_SERVERS = ImmutableList.of("54.210.218.197:9092","54.87.154.234:9092","52.91.171.10:9092");
    private static final Map<String, String> KAFKA_DEFAULT_STREAM_CONFIGS = ImmutableMap.of("replication.factor", "1");

    @Override
    public void describe(TaskApplicationDescriptor taskApplicationDescriptor) {
        // Define a system descriptor for Kafka.
        KafkaSystemDescriptor kafkaSystemDescriptor =
                new KafkaSystemDescriptor("kafka").withConsumerZkConnect(KAFKA_CONSUMER_ZK_CONNECT)
                        .withProducerBootstrapServers(KAFKA_PRODUCER_BOOTSTRAP_SERVERS)
                        .withDefaultStreamConfigs(KAFKA_DEFAULT_STREAM_CONFIGS);

        KafkaInputDescriptor kafkaInputDescriptor = kafkaSystemDescriptor.getInputDescriptor("input-placeholder", new JsonSerde<>());

        KafkaOutputDescriptor kafkaOutputDescriptor =
                kafkaSystemDescriptor.getOutputDescriptor("newsfeed-follows", new JsonSerde<>());

        taskApplicationDescriptor.withDefaultSystem(kafkaSystemDescriptor);

        taskApplicationDescriptor.withInputStream(kafkaInputDescriptor);

        taskApplicationDescriptor.withOutputStream(kafkaOutputDescriptor);

        taskApplicationDescriptor.withTaskFactory((StreamTaskFactory) () -> new GenerateFollowsTask());
    }
}
