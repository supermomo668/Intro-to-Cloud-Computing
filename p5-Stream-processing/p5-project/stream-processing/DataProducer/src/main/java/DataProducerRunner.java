import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.Properties;
import java.io.File;

public class DataProducerRunner {
    
    // connection info
    //Copy the below information for pasting into .properties file and for submitting Task 1!
    //The IP of the master is: 172.31.31.191
    //The IP list of Samza brokers in the cluster is given below for your reference.
    //172.31.22.73:9092,172.31.29.160:9092,172.31.31.191:9092
    
    public static void main(String[] args) throws Exception {
        
        // Add properties to set up the producer
        
        Properties props = new Properties();
         // ec2-3-89-139-119.compute-1.amazonaws.com // 3-89-139-119
        props.put("bootstrap.servers", "ec2-3-89-139-119.compute-1.amazonaws.com:9092");   
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
       
        String homeDir = System.getProperty("user.dir"); // get home directory where tracefile is located
        /// get input file directory
        File tracefile = new File("tracefile");
        String tracePath = tracefile.getAbsolutePath();
        //System.out.println("[DEBUG] Sending data from: "+trace_fp);  // Sending data
        // <driver_id: "json">
        Producer<Integer, String> producer = new KafkaProducer<Integer, String>(props);
        
        // initialized data producer and send data (TASK 2 TODO , CHANGE TRACE FILE)
        DataProducer dataSender = new DataProducer(producer, "tracefile");
        
        dataSender.sendData();
    }
}
