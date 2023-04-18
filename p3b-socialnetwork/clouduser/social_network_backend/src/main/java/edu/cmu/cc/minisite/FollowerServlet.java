package edu.cmu.cc.minisite;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.tagplugins.jstl.core.Catch;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
//
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
/**
 * Task 2:
 * Implement your logic to retrieve the followers of this user.
 * You need to send back the Name and Profile Image URL of his/her Followers.
 *
 * You should sort the followers alphabetically in ascending order by Name.
 */
public class FollowerServlet extends HttpServlet {

    /**
     * The Neo4j driver.
     */
    private final Driver driver;

    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include
     * the credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables.
     * export NEO4J_HOST=...
     * export NEO4J_NAME=...
     * export NEO4J_PWD=...
     */
    private static final String NEO4J_HOST = System.getenv("NEO4J_HOST");
    /**
     * Neo4J username.
     */
    private static final String NEO4J_NAME = System.getenv("NEO4J_NAME");
    /**
     * Neo4J Password.
     */
    private static final String NEO4J_PWD = System.getenv("NEO4J_PWD");

    /**
     * Initialize the connection.
     */
    public FollowerServlet() {
        driver = getDriver();
    }

    /**
     * Constructor for mocking the class behaviour
     * @param driver    Mocked driver object
     */
    FollowerServlet(Driver driver) {
        this.driver = driver;
    }

    private Driver getDriver() {
        return GraphDatabase.driver(
                "bolt://" + NEO4J_HOST + ":7687",
                AuthTokens.basic(NEO4J_NAME, NEO4J_PWD));
    }


    /**
     * Method to get the user UD from the request,
     * and print the response.
     * @param request  the request object that is passed to the servlet
     * @param response the response object that the servlet
     *                 uses to return the headers to the client
     * @throws IOException      if an input or output error occurs
     * @throws ServletException if the request for the HEAD
     *                          could not be handled
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getParameter("id");
        JsonObject result = new JsonObject();
        result.add("followers", getFollowers(id));
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(result.toString());
        writer.close();
    }


    /**
     * Return the name and profile image url of followers, sorted
     * lexicographically in ascending order by userName.
     * Input: id(string)
     * Output: [{name, url}...]
     */
    public JsonArray getFollowers(String id) {  
        JsonArray followers = new JsonArray();
        // TODO: To be implemented
        final Driver driver = GraphDatabase.driver("bolt://20.121.35.83:7687", 
            AuthTokens.basic(NEO4J_NAME, NEO4J_PWD));
            
        String neo4j_query = String.join(
            System.getProperty("line.separator"),
            "MATCH (follower:User)-[r:FOLLOWS]->(followee:User)",
            "WHERE followee.username='"+id+"'",
            "RETURN follower.username as name, follower.url as url",
            "ORDER BY name ASC;");
                
        //neo4j_query = "MATCH (follower:User)-[r:FOLLOWS]->(followee:User) WHERE followee.username='"+id+"' RETURN follower.username as name, follower.url as url ORDER BY follower ASC;";
        return make_query(neo4j_query);
        //"followers":[{"profile":"https://farm9.staticflickr.com/8007/29550375982_ce265dbf12_m.jpg","name":"DaveDroll"},{"profile":"https://farm8.staticflickr.com/7548/15784200337_fe06762b92_m.jpg","name":"jstolfi"}]
    }
    /**
     * Return Json result to neo4j query
     * Input: (string) neo4j_query
     * Output: (JsonArray) [{name, url}...]
     */
    public JsonArray query_fellowee(String id){
        final Driver driver = GraphDatabase.driver("bolt://20.121.35.83:7687", 
            AuthTokens.basic(NEO4J_NAME, NEO4J_PWD));
        String neo4j_followee_query = String.join(   // Query to get FOLLOWEE
            System.getProperty("line.separator"),
            "MATCH (follower:User)-[r:FOLLOWS]->(followee:User) ",
            "WHERE follower.username='"+id+"' ",
            "RETURN DISTINCT followee.username as name, followee.url as url ",
            "ORDER BY name ASC;");
        //MATCH (follower:User)-[r:FOLLOWS]->(followee:User) WHERE follower.username='zylo_' RETURN DISTINCT followee.username as name, followee.url as url ORDER BY name ASC;
        return make_query(neo4j_followee_query);
    }
    
    public JsonArray make_query(String neo4j_query){
        String profile_image_url;
        String name;
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        try (Session session = driver.session()) {
            StatementResult rs = session.run(neo4j_query);
            while (rs.hasNext()) {
                user = new JsonObject();
                Record record = rs.next();
                profile_image_url = record.get("url").asString();
                name = record.get("name").asString();
                //System.out.println("QUERY RESULT:\n"+name+":"+profile_image_url);
                user.addProperty("profile", profile_image_url);
                user.addProperty("name", name);
                users.add(user);
            }
        }
        // [{name, url}...]
        return users;
    }
}


