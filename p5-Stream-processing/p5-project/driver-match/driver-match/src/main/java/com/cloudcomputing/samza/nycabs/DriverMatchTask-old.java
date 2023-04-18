package com.cloudcomputing.samza.nycabs;

import org.apache.samza.context.Context;
import org.apache.samza.storage.kv.KeyValueStore;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.task.InitableTask;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskCoordinator;
import org.apache.commons.collections.MapIterator;
// additional imports
import org.apache.samza.config.Config;
import org.apache.samza.storage.kv.Entry;
import org.apache.samza.storage.kv.KeyValueIterator;
import org.apache.samza.task.WindowableTask;

import scala.Int;

import java.util.*;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.inject.spi.Message;

/**
 * Consumes the stream of driver location updates and rider cab requests.
 * Outputs a stream which joins these 2 streams and gives a stream of rider to
 * driver matches.
 */
public class DriverMatchTask implements StreamTask, InitableTask {

    /* Define per task state here. (kv stores etc)
       READ Samza API part in Primer to understand how to start
    */
    private double MAX_MONEY = 100.0;
    private long numMessages = 0;
    private KeyValueStore<String, Map<String, String>> kv_drivers;
    
    //private KeyValueStore<Integer, Object> kv_driver_loc;
    private KeyValueStore<Integer, String> kv_driver_info;
    
    private String K_BLOCK_ID = "blockId";
    private String K_DRIVER_ID = "blockId";
    private String K_TYPE = "type";
    private String K_LATITUDE = "latitude";
    private String K_LONGITUDE = "longitude";
    private String K_CLIENT_ID = "clientId";
    private String K_GENDER = "gender";
    private String K_GENDER_PREF = "gender_preference";
    private String K_RATING = "rating";
    private String K_SALARY = "salary";
    private String K_STATUS = "status";
    private String K_USER_RATING = "user_rating";
    
    @Override
    @SuppressWarnings("unchecked")
    public void init(Context context) throws Exception {
        // Initialize (maybe the kv stores?)
        kv_drivers = (KeyValueStore<String, Map<String, String>>) context.getTaskContext().getStore("driver-locations");
        //kv_driver_loc = (KeyValueStore<Integer, Object>) context.getTaskContext().getStore("driver-locations");
        //kv_driver_info = (KeyValueStore<Integer, Object>) context.getTaskContext().getStore("driver-locations");
        kv_drivers.flush();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) {
        /*
        All the messsages are partitioned by blockId, which means the messages
        sharing the same blockId will arrive at the same task, similar to the
        approach that MapReduce sends all the key value pairs with the same key
        into the same reducer.
        */
        String incomingStream = envelope.getSystemStreamPartition().getStream();

        if (incomingStream.equals(DriverMatchConfig.DRIVER_LOC_STREAM.getStream())) {
	    // Handle Driver Location messages
            streamLocation((Map<String, Object>) envelope.getMessage());
        } else if (incomingStream.equals(DriverMatchConfig.EVENT_STREAM.getStream())) {
	    // Handle Event messages
            streamEvent((Map<String, Object>) envelope.getMessage(), collector);
        } else {
            throw new IllegalStateException("Unexpected input stream: " + envelope.getSystemStreamPartition());
        }
    }
    /* retrieve driver ID based on driver information */
    private Map<String, String> retrieve_driver_info(int driver_id){
        return kv_drivers.get(String.valueOf(driver_id));
    }
    
    /* Process driver location stream 
     Input driver location stream map message
    */
    private void streamLocation(Map<String, Object> driverLoc_entry) {
        // Get driver inforatmion
        int block_id = (int) driverLoc_entry.get(K_BLOCK_ID);
        int driverId = (int) driverLoc_entry.get(K_DRIVER_ID);
        Double latitude = (Double) driverLoc_entry.get(K_LATITUDE);
        Double longitude = (Double) driverLoc_entry.get(K_LONGITUDE);
        // retrieve latest entry
        Map.Entry<String, Object> entry = driverLoc_entry.entrySet().iterator().next();
        String driver_id = entry.getKey();
        Map<String, String> driver_info =  kv_drivers.get(driver_id);
        
        // or use method to retrieve key
        //Map<String, String> driver = retrieve_driver_id(block_id, driverId);
        // update the driver locations 
        driver_info.put(K_LATITUDE, latitude.toString());
        driver_info.put(K_LATITUDE, longitude.toString());
        
        // put driver information back into KV store
        // Map<String, String> driver_key = retrieve_driver_info(driver_id);
        kv_drivers.put(driver_id, driver_info);
    }
    
    /* process event stream with 11 key-value pairs */
    private void streamEvent(Map<String, Object> event, MessageCollector collector){
        // Get information for type and block in event stream
        String type = (String) event.get(K_TYPE);
        // Retrieve the driver's information
        Integer block_id = (Integer) event.get(K_BLOCK_ID);
        Integer driver_id = (Integer) event.get(K_DRIVER_ID);
        // location of driver
        Double ride_latitude = (Double) event.get(K_LATITUDE);
        Double ride_longitude = (Double) event.get(K_LONGITUDE);
        
        // Processing logic 
        if (type.equals("RIDE_REQUEST")) {  // If it is leaving, it is removed
            // Score init
            Double gender_score = 0.0;
            // request specific information
            Integer client_id = (Integer) event.get(K_CLIENT_ID);
            String gender_preference = (String) event.get(K_GENDER_PREF);

            // search for drivers
            Integer best_driver_id=0;
            Double max_driver_score = 0.0;
            
            // create iterator for the driver store
            KeyValueIterator<String, Map<String, String>> driver_iter = kv_drivers.range(block_id.toString() + ":", block_id.toString() + ";");
            while(driver_iter.hasNext()) { /// iterate drivers
                Entry<String, Map<String, String>> driver_record = driver_iter.next();
                Map<String, String> driver_entry= driver_record.getValue();
                // get specific driver information
                String gender = driver_entry.get(K_GENDER);
                String status = driver_entry.get(K_STATUS);
                if (status.equals("AVAILABLE")){
                    // compute score here
                    gender_preference = driver_entry.get(K_GENDER_PREF);
                    Double rating = Double.valueOf(driver_entry.get(K_RATING));
                    Double salary = Double.valueOf(driver_entry.get(K_SALARY));
                    double driver_longtitude = Double.valueOf(driver_entry.get(K_LONGITUDE));
                    double driver_latitude = Double.valueOf(driver_entry.get(K_LONGITUDE));
                    Double distance = Math.sqrt(Math.pow(driver_latitude - ride_latitude, 2) + Math.pow(driver_longtitude - ride_longitude, 2)); 
                    if (gender.equals(gender_preference) || gender.equals("N")){
                        gender_score = 1.0;};
                    // Compute score
                    double match_score = distance * 0.4 + gender_score * 0.1 + 
                                         rating/5 * 0.3 + (1-(salary/100)) * 0.2;
                    // update best driver
                    if (match_score> max_driver_score){
                        best_driver_id = Integer.valueOf(driver_record.getKey());
                        max_driver_score = match_score;
                    }
                }   
            }
            // Update iformation 
            kv_drivers.delete(best_driver_id.toString());   // delete free driver
            Map<String, Integer> output =  new HashMap<String, Integer>();
            output.put(K_DRIVER_ID, Integer.valueOf(best_driver_id));
            output.put(K_CLIENT_ID, client_id);
            // send the output to the stream
            OutgoingMessageEnvelope output_stream = new OutgoingMessageEnvelope(DriverMatchConfig.MATCH_STREAM, output);
            collector.send(output_stream);
            
        } else{ // non-request, update information (leaving_block, entering_block, ride_complete, status)
            if (type.equals("RIDE_COMPLETE") || type.equals("ENTERING_BLOCK")){
                // obtain information for driver 
                // update neccessity information (location)
                Map<String, String> driver_info = kv_drivers.get(driver_id.toString());
                driver_info.put(K_LONGITUDE, ride_longitude.toString());
                driver_info.put(K_LATITUDE, ride_latitude.toString());
                driver_info.put(K_GENDER, event.get(K_GENDER).toString());
                Double rating = (Double) event.get(K_RATING);
                driver_info.put(K_RATING, rating.toString());
                driver_info.put(K_SALARY, event.get(K_SALARY).toString());
                
                if (type.equals("RIDE_COMPLETE")) {
                    // Double user_rating = (Double) event.get(K_USER_RATING);
                    driver_info.put(K_STATUS, event.get(K_STATUS).toString());
                    driver_info.put(K_STATUS, "AVAILABLE");   // drivier became available
                    
                } else{ // ENTERING BLOCK
                    Double user_rating = (Double) event.get(K_USER_RATING);   
                    driver_info.put(K_USER_RATING, String.valueOf((rating+user_rating)/2));  // Perform average
                    
                }
                // Add info back into driver entry
                kv_drivers.put(driver_id.toString(), driver_info);
                if (type.equals("RIDE_COMPLETE")) {
                    compute_scoreboard(block_id, driver_id, collector);
                }
            } else{ // LEAVING_BLOCK
                // remove the driver 
                kv_drivers.delete(driver_id.toString());
            }
        }
    }
    /* Define driver ratings */
    class DriverRatingPair {
        Integer id;
        Double rating;
        DriverRatingPair(Integer id, Double rating) {
            this.id = id;
            this.rating = rating;
        }
    }
    
    /* compute score for driver-client matching with ranked results */
    private void compute_scoreboard(Integer blockId, Integer driverId, MessageCollector collector){
        double distance_score, gender_score, rating_score, salary_score;
        distance_score = 1;
        gender_score = rating_score = salary_score = 1;
        PriorityQueue<DriverRatingPair> total_rank = new PriorityQueue<>(3, new Comparator<DriverRatingPair>() {
            @Override
            public int compare(DriverRatingPair o1, DriverRatingPair o2) {
                if (o1.rating.equals(o2.rating)) {
                    return o2.id.compareTo(o1.id);
                }
                else {
                    return o1.rating.compareTo(o2.rating);
                }
            }
        });
        KeyValueIterator <String, Map<String, String>> driver_iterator= kv_drivers.range(blockId.toString(), blockId.toString());
        // Iterate the iterator
        while (driver_iterator.hasNext()) {
            Entry<String, Map<String, String>> entry = driver_iterator.next();
            // iterate to get the value for the driver 
            Integer driver_id = Integer.valueOf(entry.getKey());;
            if (entry.getValue().get(K_RATING) == null) {
                continue;
            }
            // Find the rating inforamtion for the driver
            Double rating = Double.valueOf(entry.getValue().get(K_RATING));
            total_rank.add(new DriverRatingPair(driver_id, rating));
            while (total_rank.size() > 3) {
                total_rank.poll();
            }
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(K_BLOCK_ID, blockId);
        output.put(K_DRIVER_ID, driverId);

        LinkedList<Integer> ranks = new LinkedList<>();
        while (!total_rank.isEmpty()) {
            ranks.addFirst(total_rank.poll().id);
        }
        output.put("ranks", ranks);
        // Update the scoreboard here
        collector.send(new OutgoingMessageEnvelope(DriverMatchConfig.RANK_STREAM, output));
    }
}
