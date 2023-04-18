#	1. Create a new topic called test-topic with 1 partition and 1 replication factor.
kafka-topics.sh --zookeeper localhost:2181 --create --topic test-topic --partitions 1 --replication-factor 1
#	2. Create a Kafka producer that will send the message to the topic you just created.
kafka-console-producer.sh --broker-list localhost:9092 --topic test-topic
#	3. In another console, create a Kafka consumer that will listen to messages sent to the topic.
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning

# check the Kafka streams that are created by the application by 
kafka-topics.sh --zookeeper localhost:2181 --list
# to inspect data in each stream
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic <name of your topic> --from-beginning 