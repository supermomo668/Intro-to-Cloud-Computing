
import java.io.BufferedReader;
import java.io.FileReader;

//import java.io.InputStreamReader;
//import java.io.FileInputStream;

import java.io.IOException;

import org.json.JSONObject;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;


public class DataProducer {
    private Producer<Integer, String> producer;
    private String traceFileName;
    private FileReader traceFileReader;
    private Integer nPartitions;
    // MAY ADD (optinoal) Logger
    
        /*
        Task 1:
            In Task 1, you need to read the content in the tracefile we give to you, 
            create two streams, and feed the messages in the tracefile to different 
            streams based on the value of "type" field in the JSON string.

            Please note that you're working on an ec2 instance, but the streams should
            be sent to your samza cluster. Make sure you can consume the topics on the
            master node of your samza cluster before you make a submission.
        */
    public DataProducer(Producer producer, String traceFileName) {
        this.producer = producer;
        this.nPartitions = 5;
        try {
            this.traceFileReader = new FileReader(traceFileName);    
        } catch (IOException e) {
            // file not found
            e.printStackTrace();  
        }
        // System.out.println("[DEBUG] Initialized DataProducer");
    }          
    /*
    Send data to stream, each stream consist of data from a json object
    json 'type' field: string
    */
    public void sendData() {
        BufferedReader reader = null;
        try {
        // to catch io exceptions
            Integer part;
            String type;
            Integer blockID;
            Integer driverID;
            String l;
            JSONObject driverEntry;   // initialize new object
            ProducerRecord<Integer, String> record;
            // initialize reader
            reader = new BufferedReader(this.traceFileReader);
            while ((l = reader.readLine()) != null) {
                // keep reading the file until the end of the file
                driverEntry =  new JSONObject(l);
                // get type
                type = (String) driverEntry.get("type");    
                // get ID (key)
                blockID = (Integer) driverEntry.get("blockId");
                driverID = (Integer) driverEntry.get("driverId");
                // partition 
                part = blockID % nPartitions;
                // determined by type 
                if (type.equals("DRIVER_LOCATION")) {
                    // this is a driver location stream
                    record = new ProducerRecord<Integer, String>("driver-locations", part, driverID, l);
                } else {
                    // equals to events 
                    record = new ProducerRecord<Integer, String>("events", part, driverID, l);
                }
                this.producer.send(record);
            }
            
            // Close streams
            reader.close();
            this.producer.close();
            
        } catch (IOException e) {
            e.printStackTrace();  // file not found
        }
    }
}
