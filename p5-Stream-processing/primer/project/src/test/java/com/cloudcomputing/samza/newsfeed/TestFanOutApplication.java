package com.cloudcomputing.samza.newsfeed;

import java.time.Duration;
import java.util.*;

import org.apache.samza.serializers.NoOpSerde;
import org.apache.samza.test.framework.TestRunner;
import org.apache.samza.test.framework.system.descriptors.InMemoryInputDescriptor;
import org.apache.samza.test.framework.system.descriptors.InMemoryOutputDescriptor;
import org.apache.samza.test.framework.system.descriptors.InMemorySystemDescriptor;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import com.cloudcomputing.samza.newsfeed.FanOutApplication;

public class TestFanOutApplication {
    @Test
    public void testFanOutApplication() throws Exception {
        Map<String, String> confMap = new HashMap<>();
        confMap.put("stores.social-graph.factory", "org.apache.samza.storage.kv.RocksDbKeyValueStorageEngineFactory");
        confMap.put("stores.social-graph.key.serde", "string");
        confMap.put("stores.social-graph.msg.serde", "string");
        confMap.put("stores.user-timeline.factory", "org.apache.samza.storage.kv.RocksDbKeyValueStorageEngineFactory");
        confMap.put("stores.user-timeline.key.serde", "string");
        confMap.put("stores.user-timeline.msg.serde", "json");
        confMap.put("serializers.registry.json.class", "org.apache.samza.serializers.JsonSerdeFactory");
        confMap.put("serializers.registry.string.class", "org.apache.samza.serializers.StringSerdeFactory");


        Map<String, Object> majd_follow_planck = new HashMap<>();
        majd_follow_planck.put("follower", "Majd");
        majd_follow_planck.put("followee", "Planck");
        majd_follow_planck.put("event", "follow");
        majd_follow_planck.put("time", NewsfeedConfig.currentDateTime());

        Map<String, Object> planck_send_msg = new HashMap<>();
        planck_send_msg.put("sender", "Planck");
        planck_send_msg.put("text", "I am working on test in Samza.");
        planck_send_msg.put("time", NewsfeedConfig.currentDateTime());
        planck_send_msg.put("event", "postMessage");

        // This data should be pre-defined, and it will be stored in in-memory stream.
        List<Map<String, Object>> followData = new ArrayList<>();
        followData.add(majd_follow_planck);

        List<Map<String, Object>> postData = new ArrayList<>();
        postData.add(planck_send_msg);

        InMemorySystemDescriptor isd = new InMemorySystemDescriptor("kafka");

        InMemoryInputDescriptor followEvents = isd.getInputDescriptor("newsfeed-follows", new NoOpSerde<>());
        InMemoryInputDescriptor postEvents = isd.getInputDescriptor("newsfeed-messages", new NoOpSerde<>());

        InMemoryOutputDescriptor deliverEvents = isd.getOutputDescriptor("newsfeed-deliveries", new NoOpSerde<>());

        TestRunner.of(new FanOutApplication())
                .addInputStream(followEvents, followData)
                .addInputStream(postEvents, postData)
                .addOutputStream(deliverEvents, 1)
                .addConfig(confMap)
                .addConfig("deploy.test", "true")
                .run(Duration.ofSeconds(5));

        Assert.assertEquals(1, TestRunner.consumeStream(deliverEvents, Duration.ofSeconds(10)).get(0).size());

        ListIterator<Object> resultIter = TestRunner.consumeStream(deliverEvents, Duration.ofSeconds(10)).get(0).listIterator();
        Map<String, Object> resultMap = (Map<String, Object>)resultIter.next();

        Assert.assertTrue(resultMap.get("sender").toString().equals("Planck") && resultMap.get("recipient").toString().equals("Majd"));
    }
}