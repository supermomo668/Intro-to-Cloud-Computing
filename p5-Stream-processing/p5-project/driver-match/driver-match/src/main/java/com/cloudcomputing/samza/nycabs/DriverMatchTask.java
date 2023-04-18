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
    private KeyValueStore<String, Map<String, String>> KVDrivers;
    
    //private KeyValueStore<Integer, String> kv_driverInfo;
    
    private String K_BLOCK_ID = "blockId";
    private String K_DRIVER_ID = "driverId";
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
        // KV store for drivers available 
        KVDrivers = (KeyValueStore<String, Map<String, String>>) context.getTaskContext().getStore("driver-loc");

        //kv_driverInfo = (KeyValueStore<Integer, Object>) context.getTaskContext().getStore("driver-locations");
        // KVDrivers.flush();
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
    
    /*/
    Key design
    */
    private String formKey(Integer blockId, Integer driverId) {
        return blockId.toString() + ":" + driverId.toString();
    }

    private Integer retrieveDriverID(String KVkey) {
        String driverID = KVkey.split(":")[1];
        return Integer.valueOf(driverID);
    }

    private Map<String, String> FindDriverInfoFromKV(Integer blockId, Integer driverId) {
        Map<String, String> driver = KVDrivers.get(formKey(blockId, driverId));
        if (driver == null) {
            return new LinkedHashMap<>();
        }
        else {
            return driver;
        }
    }
    
    /* Process driver location stream 
     Input driver location stream map message
    */
    private void streamLocation(Map<String, Object> driverLoc_entry) {
        // Get driver inforatmion
        int blockID = (int) driverLoc_entry.get(K_BLOCK_ID);
        Integer driverID = (int) driverLoc_entry.get(K_DRIVER_ID);
        Double latitude = (Double) driverLoc_entry.get(K_LATITUDE);
        Double longitude = (Double) driverLoc_entry.get(K_LONGITUDE);
        
        // retrieve KV entry for driver
        String driverIDKey = driverID.toString();
        Map<String, String> driverInfo = FindDriverInfoFromKV(blockID, driverID);
        
        // update the driver locations 
        driverInfo.put(K_LATITUDE, latitude.toString());
        driverInfo.put(K_LATITUDE, longitude.toString());
        String newKey = formKey(blockID, driverID);
        // put driver information back into KV store
        // Map<String, String> driver_key = retrieve_driverInfo(driver_id);
        KVDrivers.put(newKey, driverInfo);
    }
    
    /* process event stream with 11 key-value pairs */
    private void streamEvent(Map<String, Object> entry, MessageCollector collector) {
        // Get information for type and block in event stream
        String type = (String) entry.get(K_TYPE);
        // location of driver
        Double rideLatitude = (Double) entry.get(K_LATITUDE);
        Double rideLongitude = (Double) entry.get(K_LONGITUDE);
        Integer blockID = (Integer) entry.get(K_BLOCK_ID);
        // Get driver ID from store
        // Processing logic 
        if (type.equals("RIDE_REQUEST")) {
            
            // request specific information
            Integer clientID = (Integer) entry.get(K_CLIENT_ID);
            String genderPref = (String) entry.get(K_GENDER_PREF);
        
            // search for drivers
            String bestEntryKey = "";
            String bestDriverID = "";
            Double bestDriverScore = 0.0;
            
            // create iterator for the driver store
            KeyValueIterator<String, Map<String, String>> driverIter = 
                KVDrivers.range(blockID.toString() + ":", blockID.toString() + ";");
                
            while(driverIter.hasNext()) {
                /// iterate drivers in KV store (available drivers)
                // get driver info
                Entry<String, Map<String, String>> record = driverIter.next();
                Map<String, String> driverEntry= record.getValue();
                // Score init
                Double genderScore = 0.0;
                // get specific driver information
                String gender = driverEntry.get(K_GENDER);
                String status = driverEntry.get(K_STATUS);
                String entryKey = record.getKey();
                
                if (status.equals("AVAILABLE")) {
                    // compute score here
                    System.out.println("printing gender over here "+ gender);
                    System.out.println(retrieveDriverID(entryKey));
                    System.out.println("\n\n\n\n\n-------------------------------------------");
                    genderPref = (String) entry.get(K_GENDER_PREF);
                    Double rating = Double.valueOf(driverEntry.get(K_RATING));
                    Double salary = Double.valueOf(driverEntry.get(K_SALARY));
                    double driverLongtitude = Double.valueOf(driverEntry.get(K_LONGITUDE));
                    double driverLatitude = Double.valueOf(driverEntry.get(K_LONGITUDE));
                    double distX = Math.pow(driverLatitude - rideLatitude, 2) ;
                    double distY = Math.pow(driverLongtitude - rideLongitude, 2);
                    double distance = Math.sqrt(distX + distY); 
                    
                    if (gender.equals(genderPref) || gender.equals("N")) {
                        genderScore = 1.0;
                    }
                    // Compute score //distance:  1 * e ^ (-1 * client_driver_distance)
                    Double distScore = 1.0 * Math.pow(Math.E, -1.0 * distance);
                    double matchScore = 0.4 * distScore + 0.1 * genderScore + 
                                         0.3 * (rating/5.0) + 0.2 * (1 - (salary/100.0));
                    // update best driver
                    if (matchScore > bestDriverScore){
                        bestDriverScore = matchScore;
                        bestEntryKey = entryKey;
                        bestDriverID = retrieveDriverID(bestEntryKey).toString();
                    }
                }
            }
            driverIter.close();
            // Update Store // delete free driver
            KVDrivers.delete(bestEntryKey);
            
            System.out.println("DEBUG \n");
            System.out.println(blockID);
            System.out.println(clientID);
            // System.out.println(gender);
            System.out.println("GENDER PREF");
            System.out.println(genderPref);
            // System.out.println(driverID);
            System.out.println(bestDriverID);
            System.out.println("\n\n\n\n\n-------------------------------------------");
            // save result entry
            Map<String, String> output =  new HashMap<String, String>();
            output.put(K_DRIVER_ID, bestDriverID.toString());
            output.put(K_CLIENT_ID, clientID.toString());
            // send the output to the stream
            collector.send(new OutgoingMessageEnvelope(DriverMatchConfig.MATCH_STREAM, output));
            
            
        } else { // non-request, update information (leaving_block, entering_block, ride_complete, status)
            
            Integer driverID = (Integer) entry.get(K_DRIVER_ID);
            
            if (type.equals("RIDE_COMPLETE") || type.equals("ENTERING_BLOCK")) {
                // obtain information for driver from store (for updating)
                Map<String, String> driverInfo = FindDriverInfoFromKV(blockID, driverID);
                
                // Retrieve more driver's information
                String gender = (String) entry.get(K_GENDER);
                Double rating = (Double) entry.get(K_RATING);
                Integer salary = (Integer) entry.get(K_SALARY);
                
                // update neccessity information (location)
                driverInfo.put(K_LONGITUDE, rideLongitude.toString());
                driverInfo.put(K_LATITUDE, rideLatitude.toString());
                driverInfo.put(K_GENDER, gender);
                driverInfo.put(K_SALARY, salary.toString());
                
                if (type.equals("RIDE_COMPLETE")) {
                    Double userRating = (Double) entry.get(K_USER_RATING); 
                    // UPDATE SCORE
                    Double newRating = (rating + userRating) / 2.0; 
                    driverInfo.put(K_STATUS, "AVAILABLE");   // drivier became available
                    driverInfo.put(K_RATING, newRating.toString());  // Perform average 
                    
                } else if (type.equals("ENTERING_BLOCK")) { 
                    // ENTERING BLOCK information
                    String status = (String) entry.get(K_STATUS);   
                    driverInfo.put(K_STATUS, status);
                    driverInfo.put(K_RATING, rating.toString());
                }
                // Add info back into driver entry
                KVDrivers.put(formKey(blockID, driverID), driverInfo);
            } else { 
                // LEAVING_BLOCK : // remove the driver 
                KVDrivers.delete(driverID.toString());
            }
        }
    }
}
