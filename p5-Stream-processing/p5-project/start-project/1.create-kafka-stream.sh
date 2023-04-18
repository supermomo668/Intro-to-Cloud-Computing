#	1. Create a 2 topic called test-topic with 5 partition and 1 replication factor.
kafka-topics.sh --zookeeper localhost:2181 --create --topic driver-locations --partitions 5 --replication-factor 1
kafka-topics.sh --zookeeper localhost:2181 --create --topic events --partitions 5 --replication-factor 1
