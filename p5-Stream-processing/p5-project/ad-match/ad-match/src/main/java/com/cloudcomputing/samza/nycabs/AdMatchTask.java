package com.cloudcomputing.samza.nycabs;

import com.google.common.io.Resources;
import org.apache.samza.context.Context;
import org.apache.samza.storage.kv.KeyValueStore;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.task.InitableTask;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskCoordinator;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Timer;

/*Consumes the stream of events.*Outputs a stream which handles static file and one stream*and gives a stream of advertisement matches.*/public

class AdMatchTask implements StreamTask, InitableTask {

    /*
     * Define per task state here. (kv stores etc)
     * READ Samza API part in Writeup to understand how to start
     */
    // RIDER INTEREST KEYS
    private String K_USER_ID = "userId";
    private String K_INTEREST = "interest";
    private String K_DURATION = "duration";
    // RIDER STATUS
    private String K_MOOD = "mood";
    private String K_BLOOD = "blood_sugar";
    private String K_STRESS = "stress";
    private String K_ACTIVE = "active";
    
    private KeyValueStore<Integer, Map<String, Object>> userInfo;
    // KEYS OF USERINFO
    private String KV_USER_ID = "userId";
    private String KV_USER_GENDER = "gender";
    private String KV_USER_AGE = "age";
    private String KV_USER_INTEREST = "interest";
    private String KV_USER_TRAVEL ="travel_count";
    private String KV_USER_DEVICE = "device";
    
    private KeyValueStore<String, Map<String, Object>> yelpInfo;
    // KEYS OF STORE INFO
    private String KV_STORE_ID = "storeId";
    private String KV_STORE_NAME = "name";
    private String KV_STORE_REVIEW = "review_count";
    private String KV_STORE_CATEGORIES = "categories";
    private String KV_STORE_RATING ="rating";
    private String KV_STORE_PRICE = "price";
    private String KV_STORE_LATITUDE = "latitude";
    private String KV_STORE_LONGITUDE ="longitude";
    private String KV_STORE_BLOCKID = "blockId";

    private Set<String> lowCalories;

    private Set<String> energyProviders;

    private Set<String> willingTour;

    private Set<String> stressRelease;

    private Set<String> happyChoice;

    private void initSets() {
        lowCalories = new HashSet<>(Arrays.asList("seafood", "vegetarian", "vegan", "sushi"));
        energyProviders = new HashSet<>(Arrays.asList("bakeries", "ramen", "donuts", "burgers",
                "bagels", "pizza", "sandwiches", "icecream",
                "desserts", "bbq", "dimsum", "steak"));
        willingTour = new HashSet<>(Arrays.asList("parks", "museums", "newamerican", "landmarks"));
        stressRelease = new HashSet<>(Arrays.asList("coffee", "bars", "wine_bars", "cocktailbars", "lounges"));
        happyChoice = new HashSet<>(Arrays.asList("italian", "thai", "cuban", "japanese", "mideastern",
                "cajun", "tapas", "breakfast_brunch", "korean", "mediterranean",
                "vietnamese", "indpak", "southern", "latin", "greek", "mexican",
                "asianfusion", "spanish", "chinese"));
    }

    // Get store tag
    private String getTag(String cate) {
        String tag = "";
        if (happyChoice.contains(cate)) {
            tag = "happyChoice";
        } else if (stressRelease.contains(cate)) {
            tag = "stressRelease";
        } else if (willingTour.contains(cate)) {
            tag = "willingTour";
        } else if (energyProviders.contains(cate)) {
            tag = "energyProviders";
        } else if (lowCalories.contains(cate)) {
            tag = "lowCalories";
        } else {
            tag = "others";
        }
        return tag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Context context) throws Exception {
        // Initialize kv store (user , Yelp)
        userInfo = (KeyValueStore<Integer, Map<String, Object>>) context.getTaskContext().getStore("user-info");
        yelpInfo = (KeyValueStore<String, Map<String, Object>>) context.getTaskContext().getStore("yelp-info");

        // userInfo.flush();
        // yelpInfo.flush();
        // Initialize store tags set
        initSets();

        // Initialize static data and save them in kv store
        initialize("UserInfoData.json", "NYCstore.json");
    }

    /*
     * This function will read the static data from resources folder
     * and save data in KV store.
     * <p>
     * This is just an example, feel free to change them.
     */
    public void initialize(String userInfoFile, String businessFile) {
        // start timer 
        Timer timer = new Timer();
        // Read file
        List<String> userInfoRawString = AdMatchConfig.readFile(userInfoFile);
        System.out.println("Reading user info file from " + Resources.getResource(userInfoFile).toString());
        System.out.println("UserInfo raw string size: " + userInfoRawString.size());
        for (String rawString : userInfoRawString) {
            Map<String, Object> mapResult;
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapResult = mapper.readValue(rawString, HashMap.class);
                int userId = (Integer) mapResult.get("userId");
                // User info
                userInfo.put(userId, mapResult);
            } catch (Exception e) {
                System.out.println("Failed at parse user info :" + rawString);
            }
        }

        List<String> businessRawString = AdMatchConfig.readFile(businessFile);

        // ystem.out.println("Reading store info file from " + Resources.getResource(businessFile).toString());
        // System.out.println("Store raw string size: " + businessRawString.size());

        for (String rawString : businessRawString) {
            Map<String, Object> mapResult;
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapResult = mapper.readValue(rawString, HashMap.class);
                String storeId = (String) mapResult.get("storeId");
                String cate = (String) mapResult.get("categories");
                String tag = getTag(cate);
                mapResult.put("tag", tag);
                // Store info
                yelpInfo.put(storeId, mapResult);   
            } catch (Exception e) {
                System.out.println("Failed at parse store info :" + rawString);
            }
        }
    }

    //// * main processing function *////
    @Override
    @SuppressWarnings("unchecked")
    public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) {
        /*
         * All the messsages are partitioned by blockId, which means the messages
         * sharing the same blockId will arrive at the same task, similar to the
         * approach that MapReduce sends all the key value pairs with the same key
         * into the same reducer.
         */
        incomingStream = envelope.getSystemStreamPartition().getStream();

        if (incomingStream.equals(AdMatchConfig.EVENT_STREAM.getStream())) {
            // Handle Event messages
            streamEvent((Map<String, Object>) envelope.getMessage());
        } else if (incomingStream.equals(AdMatchConfig.AD_STREAM.getStream())) {
            //
            streamAd((Map<String, Object>) envelope.getMessage(), collector);
        } else {
            // handle with task 2 logic ?
            DriverMatchTask drivermatchtask = new DriverMatchTask(Context context);
            drivermatchtask.process(envelope, collector, coordinator);
            // or just throw exception
            //throw new IllegalStateException("Unexpected input stream: " + envelope.getSystemStreamPartition());
        }

    }

    /**
     * Process event stream and produce ranked information
     */
    public void streamEvent(Map<String, Object> event, MessageCollector collector) {
        // Get information for type and block in event stream
        // Input fields:
        // {"userId":4783,"interest":"sushi","duration":3172236,"type":"RIDER_INTEREST"}
        // {"userId":4783,"mood":6,"blood_sugar":3,"stress":2,"active":1,"type":"RIDER_STATUS"}
        /*
        // RIDER INTEREST KEYS
            private String K_INTEREST = "interest";
            private String K_DURATION = "duration";
            // RIDER STATUS
            private String K_MOOD = "mood";
            private String K_BLOOD = "blood_sugar";
            private String K_STRESS = "stress";
            private String K_ACTIVE = "active";
        */
        // Event type
        String type = (String) event.get(K_TYPE);  
        // User ID
        String userID = (String) event.getKey();
        String type = (String) event.get(K_TYPE);
        String type = (String) event.get(K_TYPE);
        
        // get store ID from
        Integer blockID = (Integer) entry.get(K_BLOCK_ID);
        // Processing logic
        if (type.equals("RIDER_INTEREST") || type.equals("RIDER_STATUS")) { // If it is leaving, it is removed
            String userID = (String) event.get(userId);
            // send the output to the stream
            // obtain information for driver from store (for updating)  
            Map<String, String> userInfoEntry = userInfo.get(userID);
            
            if (type.equals("RIDER_INTEREST")) {
                // request event information
                Double duration = (Double) entry.get(K_DURATION);
                String interest = (String) entry.get(K_INTEREST);
                // User information for
                userinfo = (String) userInfo.get(userID);
                // search for best business
                String bestStoreID;
                Double bestStoreScore = 0.0;
                Double currentScore = 0.0;
                // create iterator for the businesses
                KeyValueIterator<String, Map<String, String>> yelpIter = 
                    yelpInfo.range(blockID.toString() + ":", blockID.toString() + ";");
                
                 while(yelpIter.hasNext()) {
                     /* KEYS OF STORE INFO
                        private String KV_STORE_NAME = "name";
                        private String KV_STORE_REVIEW = "review_count";
                        private String KV_STORE_CATEGORIES = "categories";
                        private String KV_STORE_RATING ="rating";
                        private String KV_STORE_PRICE = "price";
                        private String KV_STORE_LATITUDE = "latitude";
                        private String KV_STORE_LONGITUDE ="longitude";
                        private String KV_STORE_BLOCKID = "blockId";
                    */
                    // iterate drivers in KV store (available drivers)
                    // get driver info
                    Entry<String, Map<String, Object>> record = yelpIter.next();
                    Map<String, Object> yelpEntry= record.getValue();
                    
                    // Get store information
                    String StoreID = (String) yelpEntry.get(KV_STORE_ID);
                    // Initial score
                    Double review = (Double) yelpEntry.get(KV_STORE_REVIEW);
                    Double rating = (Double) yelpEntry.get(KV_STORE_RATING);
                    currentScore = review * rating;
                    
                    // 2nd score update
                    String storeCategory = (String) yelpEntry.get(KV_STORE_CATEGORIES);
                    if storeCategory.equalsIgnoreCase(interest){
                        currentScore += 10;
                    }
                    // 3rd score update
                    String deviceType = 
                    // Leader store
                    if (currentScore > bestStoreScore){
                        // Update best store information
                        bestStoreScore = currentScore;
                        bestStoreID = StoreID; 
                    }
                    // get specific driver information
                    String gender = yelpEntry.get(KV_STORE_CATEGORIES);
                    String status = yelpEntry.get(K_STATUS);
                    
                yelpIter.close();
                // save result entry (DRIVER ID TO STORE ID)
                Map<String, String> output =  new HashMap<String, String>();
                output.put(K_, bestDriverID);
                output.put(K_CLIENT_ID, clientID);
                // send result to stream
                collector.send(new OutgoingMessageEnvelope(DriverMatchConfig.AD_STREAM, output));
                
            } else if (type.equals("RIDER_STATUS") { 
                // Retrieve user's event information
                String mood = (String) event.get(K_MOOD);
                Double blood = (Double) entry.get(K_BLOOD);
                Double stress = (Double) entry.get(K_STRESS);
                Double active = (Double) entry.get(K_ACTIVE);
                
                // UPDATE RIDER INFORMATION
                userInfoEntry.put(K_MOOD, mood);
                userInfoEntry.put(K_BLOOD, blood);
                userInfoEntry.put(K_STRESS, stress);
                userInfoEntry.put(K_ACTIVE, active);
            }

        } else {
            // Use task 2 logic
            // Retrieve the driver's information
            Integer block_id = (Integer) event.get(K_BLOCK_ID);
            Integer driver_id = (Integer) event.get(K_DRIVER_ID);
            // location of driver
            Double ride_latitude = (Double) event.get(K_LATITUDE);
            Double ride_longitude = (Double) event.get(K_LONGITUDE);
            
        }
    }

    /* Stream Ads */
    public void streamAd(Map<String, Object> event, MessageCollector collector) {
        // Get information for type and block in event stream
        String type = (String) event.get(K_TYPE);
        // Retrieve the the rider's food pref
        String interest = (String) event.get(K_INTEREST);
        Integer driver_id = (Integer) event.get(K_DURATION);

        // categorize the food interest
        String tag = getTag(interest);

    }

}
