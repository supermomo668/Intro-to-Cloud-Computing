package edu.cmu.cc.minisite;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.zookeeper.server.quorum.Follower;
// ADDITIONAL
// Task 3
//BSON
import org.bson.Document;
import org.bson.conversions.Bson;
//Mongodb
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;
//
//import org.bson.*;
import com.google.gson.JsonArray;
import com.mongodb.client.model.Projections;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
//import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCursor;

// SQL imports
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * In this task you will populate a user's timeline.
 * This task helps you understand the concept of fan-out. 
 * Practice writing complex fan-out queries that span multiple databases.
 *
 * Task 4 (1):
 * Get the name and profile of the user as you did in Task 3
 * Put them as fields in the result JSON object
 *
 * Task 4 (2);
 * Get the follower name and profiles as you did in Task 4
 * Put them in the result JSON object as one array
 *
 * Task 4 (3):
 * From the user's followees, get the 30 most popular comments
 * and put them in the result JSON object as one JSON array.
 * (Remember to find their parent and grandparent)
 *
 * The posts should be sorted:
 * First by ups in descending order.
 * Break tie by the timestamp in descending order.
 */
public class TimelineServlet extends HttpServlet {

    /**
     * Your initialization code goes here.
     */
    
    private static final String MONGO_HOST = System.getenv("MONGO_HOST");
    /**
     * MongoDB server URL.
     */
    private static final String URL = "mongodb://" + MONGO_HOST + ":27017";
    /**
     * Database name.
     */
    private static final String DB_NAME = "reddit_db";
    /**
     * Collection name.
     */
    private static final String COLLECTION_NAME = "posts";
    /**
     * MongoDB connection.
     */
    private static MongoCollection<Document> collection;
    
    private static final String NEO4J_HOST = System.getenv("NEO4J_HOST");
    /**
     * Neo4J username.
     */
    private static final String NEO4J_NAME = System.getenv("NEO4J_NAME");
    /**
     * Neo4J Password.
     */
    private static final String NEO4J_PWD = System.getenv("NEO4J_PWD");
    public TimelineServlet() {
        Objects.requireNonNull(MONGO_HOST);
        MongoClientURI connectionString = new MongoClientURI(URL);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }
    /**
     * Initialize the connection.
     */
    /**
     * Don't modify this method.
     *
     * @param request  the request object that is passed to the servlet
     * @param response the response object that the servlet
     *                 uses to return the headers to the client
     * @throws IOException      if an input or output error occurs
     * @throws ServletException if the request for the HEAD
     *                          could not be handled
     */
    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {

        // DON'T modify this method.
        String id = request.getParameter("id");
        String result = getTimeline(id);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print(result);
        writer.close();
    }

    /**
     * Method to get given user's timeline.
     *
     * @param id user id
     * @return timeline of this user
     */
    private String getTimeline(String id) {
        JsonObject result = new JsonObject();
        System.out.print("\nQuery timeline of ID:"+id+'\n');
        // TODO: implement this method
        /*
         * Task 4 (1):
        * Get the name and profile of the user as you did in Task 3
        * Put them as fields in the result JSON object
        */ 
        
        try{      
            ProfileServlet profile_servlet = new ProfileServlet();
            result = profile_servlet.query_profile(id);    
            if (result.get("name").getAsString()=="Unauthorized"){
                System.out.println("No such user or query error");
                return "";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        } 
        /*
        * Task 4 (2);
        * Get the follower name and profiles as you did in Task 4
        * Put them in the result JSON object as one array
        */
        //Driver followers = follower_servlet.getFollowers(id);
                
        FollowerServlet follower_servlet = new FollowerServlet();
        JsonArray followee_info = follower_servlet.query_fellowee(id);   // [{name, url}...]
        JsonArray follower_info = follower_servlet.getFollowers(id);   // [{name, url}...]
        result.add("followers", follower_info); // Add follower information
        // Get Followee name as list for Mongo query
        String[] fellowee_names = new String[followee_info.size()]; 
        //Iterating JSON array  
        for (int i=0;i<followee_info.size();i++){   
             // add name to the list
            fellowee_names[i] = followee_info.get(i).getAsJsonObject().get("name").getAsString();
        }   
        System.out.println("Fellowee names:"); System.out.println(Arrays.asList(fellowee_names));
        //
        
        // Prepare Mongo DB comment query to get top 30 followees by popularity (ups)
        Bson projection = Projections.fields(Projections.excludeId());
        MongoCursor<Document> comment_cursor1 = collection.find(in("uid", Arrays.asList(fellowee_names)))
                .sort(descending("ups","timestamp")).projection(projection).limit(30).iterator();
                
         // [{ "_id" : ObjectId("622471accb09d9c81d00fc5e"), "uid" : "zylo_", "downs" : 0, "parent_id" : "t1_cra3fh7", "ups" : 2, "subreddit" : "funny", "content" : "I think OP is projecting some self-hatred onto other people with this post.", "cid" : "t1_craa1td", "timestamp" : "1431715643" }   
        HomepageServlet homepage_servlet = new HomepageServlet();   // Initiate query
        JsonArray followee_comment_arr = homepage_servlet.query_comments(comment_cursor1);
        JsonArray followee_comment_arr_ = new JsonArray();
        for (int com = 0; com < followee_comment_arr.size(); com++) {  /// 30 posts
            JsonObject comment =  followee_comment_arr.get(com).getAsJsonObject();
            System.out.println("a COMMENT:"); System.out.println(comment);;                
            // Get parent ID
            String pid = comment.get("parent_id").getAsString();
            // Nested query to get parent & grandparent comments
            MongoCursor<Document> parent_comment_cursor = collection.find(eq("cid", pid))
                .sort(descending("ups","timestamp")).projection(projection).iterator(); 
            // to add to "parent" field  
            JsonArray parent_comment_arr = homepage_servlet.query_comments(parent_comment_cursor);
            
            if (parent_comment_arr.size()!=0){  // add parent post if exists
                System.out.println("Adding PARENT");
                JsonObject parent_comments= parent_comment_arr.get(0).getAsJsonObject();  
                String grand_parent_cid = parent_comments.get("parent_id").getAsString();
                comment.add("parent", parent_comments);   /// Append grandparent result to comment
                // Query for grandparent
                MongoCursor<Document> grandparent_comment_cursor = collection.find(eq("cid", grand_parent_cid))
                    .sort(descending("ups","timestamp")).projection(projection).iterator(); 
                JsonArray grandparent_comment_arr = homepage_servlet.query_comments(grandparent_comment_cursor);
                    
                if (grandparent_comment_arr.size()!=0){  // add grandparent post 
                    System.out.println("Adding GRANDPARENT");
                    JsonObject grandparent_comments= grandparent_comment_arr.get(0).getAsJsonObject();      
                    comment.add("grand_parent", grandparent_comments);   /// Append parent result to comment
                }
            }
            // Add to result 
            System.out.println("....................ADD COMMENT array to Followee...............");
            // add comment objecct to fellowee user profile (up to 30)
            followee_comment_arr_.add(comment);  
        }
        // Add "Grand comment" to each fellowee
        result.add("comment",  followee_comment_arr_);
        
        return result.toString();
    }
    /*
    Create followee query as according to ID
    */
}

