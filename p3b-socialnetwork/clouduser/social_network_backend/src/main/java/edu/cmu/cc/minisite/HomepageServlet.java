package edu.cmu.cc.minisite;

import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;
import org.bson.conversions.Bson;
//
//import org.bson.*;
import com.google.gson.JsonArray;
import com.mongodb.client.model.Projections;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
//import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCursor;

/**
 * Task 3:
 * Implement your logic to return all the comments authored by this user.
 *
 * You should sort the comments by ups in descending order (from the largest to the smallest one).
 * If there is a tie in the ups, sort the comments in descending order by their timestamp.
 */
public class HomepageServlet extends HttpServlet {

    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include
     * the credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables.
     * export MONGO_HOST=...
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

    /**
     * Initialize the connection.
     */
    public HomepageServlet() {
        Objects.requireNonNull(MONGO_HOST);
        MongoClientURI connectionString = new MongoClientURI(URL);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    /**
     * Implement this method.
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

        JsonObject result = new JsonObject();
        String id = request.getParameter("id");
        
        // TODO: To be implemented
        result = get_user_posts(id);
        //Write responses
        //Create header
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        //Write response
        PrintWriter writer = response.getWriter();
        writer.write(result.toString());
        writer.close();
    }
    /**
     * Implement this method.
     *
     * @param id  the id of the user to be queried
     * @throws IOException      if an input or output error occurs
     *                          could not be handled
     */
    public JsonObject get_user_posts(String id) {
        JsonObject result = new JsonObject();
        
        // Make query for mongodb
        Bson projection = Projections.fields(Projections.excludeId());
        // Create DB cursor
        MongoCursor<Document> cursor = collection.find(eq("uid", id))
          .sort(descending("ups","timestamp")).projection(projection).iterator();
        // Initiate query
        JsonArray comments = query_comments(cursor);
        // Write results to JSON 
        result.add("comments", comments);
        return result;
        // {"comments":[{comment1_json}, {comment2_json}, ...]}
    }
    public JsonArray query_comments(MongoCursor<Document> cursor){
        JsonArray comments = new JsonArray();
        // Iterate cursor and append results to JSON array
        try {
            while (cursor.hasNext()) {
                JsonObject comment = JsonParser.parseString(cursor.next().toJson()).getAsJsonObject();
                comments.add(comment);
            }
        } finally {
            cursor.close();
        }
        return comments;
        // [{ "_id" : ObjectId("622471accb09d9c81d00fc5e"), "uid" : "zylo_", "downs" : 0, "parent_id" : "t1_cra3fh7", "ups" : 2, "subreddit" : "funny", "content" : "I think OP is projecting some self-hatred onto other people with this post.", "cid" : "t1_craa1td", "timestamp" : "1431715643" }, {comment2_json}, ...]
    }
}

