import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.URL;
import java.text.DecimalFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.SQLException;
import java.sql.Statement;
//import javax.servlet.annotation.WebServlet;

//@WebServlet("/HealthDBAccess")
public class HealthDBAccess extends HttpServlet {

    private final String apiBase = "https://odn.data.socrata.com/resource/csbu-um39.json?$$app_token=3sOTYDZnPDKhhnzKX0VLJIAe7&";

    private static Statement stmnt;
    private static Connection con;

    private static final String USERNAME = "tston529";
    private static final String PASSWORD = "bA2a4wrE";

    private static final boolean StateWide = true;
    private static final boolean PerCounty = false;
    private static boolean regionCheck = false;
    private static String state = "AL";

    private static String emptyMsg = "";
    private static String tableTitle = "";
    private static ArrayList<String> errorMsgs = new ArrayList<>();

    private static String queryType = "PERCENTAGE";



    // Need to open DB connection here. In doPost, it would close at end of fn
    public void init(ServletConfig config)  throws ServletException
    {
        super.init(config);
        // Set up JDBC stuff
        try { // Get a connection
            con = new OracleConnection().getConnection(USERNAME,PASSWORD);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Looks at the user's selections, builds a query around them.
     * @param   req The POST data sent through the form
     * @return the newly-built query
     */
    private String parseQuery(HttpServletRequest req)
    {
        int where = 0;
        StringBuilder queryString = new StringBuilder(apiBase);

        if(req!=null) {

            regionCheck = req.getParameter("regionSize").equals("state");

            String stateSelect = req.getParameter("state");
            String topicSelect = req.getParameter("topic");

            /* if(!stateSelect.equals(null) && !"".equals(stateSelect) && !"(State)".equals(stateSelect)) {
                queryString.append("$where=name%20like%20%27%25%2C%20");
                queryString.append(stateSelect);
                queryString.append("%25%27");
                state = stateSelect;
                where++;
            } */

            
            if(!stateSelect.equals(null) && !"".equals(topicSelect) && !"(Variable Topic)".equals(topicSelect)) {
                    // For population queries
                if("2011_population_estimate".equals(topicSelect)) {
                    queryType = "POPULATION";
                    if(regionCheck == StateWide) {
                        String selectedState = StatesData.getAbbrToStateMap().get(stateSelect);
                        if(where==0)
                            queryString.append("$where=variable%20=%20%27_2011_population_estimate_value%27%20AND%20name%20like%20%27%25");
                        else
                            queryString.append("%20AND%20variable%20=%20%27_2011_population_estimate_value%27%20AND%20name%20like%20%27%25");
                        queryString.append(selectedState);
                        queryString.append("%25%27");
                        
                    } else if(regionCheck == PerCounty) {
                        if(where==0)
                            queryString.append("$where=variable%20=%20%27_2011_population_estimate_value%27%20AND%20name%20like%20%27%25%2C%20");
                        else
                            queryString.append("%20AND%20variable%20=%20%27_2011_population_estimate_value%27%20AND%20name%20like%20%27%25%2C%20");
                        queryString.append(stateSelect);
                        queryString.append("%25%27");
                    }
                    
                } else { // For anything but a population query
                    queryType = "PERCENTAGE";
                    if(!stateSelect.equals(null) && !"".equals(stateSelect) && !"(State)".equals(stateSelect)) {
                        queryString.append("$where=name%20like%20%27%25%2C%20");
                        queryString.append(stateSelect);
                        queryString.append("%25%27");
                        state = stateSelect;
                        where++;
                    }
                    if(where==0)
                        queryString.append("$where=variable%20like%20%27%25");
                    else
                        queryString.append("%20AND%20(variable%20like%20%27%25");
                    queryString.append(topicSelect);
                    queryString.append("_numerator%25%27%20OR%20variable%20like%20%27%25");
                    queryString.append(topicSelect);
                    queryString.append("_denominator%25%27)");
                    //errorMsgs.add("Whooho");
                }

                where++;
            }

        }

        if(where>0) {
            queryString.append("%20AND%20");
        } else {
            queryString.append("$where=");
        }
        queryString.append("value>=0"); // Prevent grabbing data with empty cells

        return queryString.toString();
    }

    /**
     * Modified version of method by Dr.&nbsp;Spiegel; returns the result of
     * a visit to a desired URL in plain text, parses returned JSON, saves to
     * a *.CSV file, and imports the final data from this file.
     * @param query the full URL to the desired data
     */
    private boolean readUrl(PrintWriter out, String query)
    {
        URL TheFile=null;
        try { // Set up a URL to the file
            TheFile=new URL(query);
        }
        catch (Exception e) {
            errorMsgs.add("URL Setup failed...");
            e.printStackTrace();
        }
        InputStream s=null;
        try { // Hook up to the file on the server
            s=TheFile.openStream();
        }
        catch (Exception e)  {
            e.printStackTrace();
            errorMsgs.add("!! Stream open failed !!");
        }
        BufferedReader Inf=null;
        try {
            Inf=new BufferedReader(new InputStreamReader(s));
        }
        catch (Exception e){
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        int next;
        try {
            next = Inf.read();
            while (next >= 0) {
                sb.append((char) next);
                next = Inf.read();
            }
        } catch(IOException e){
          errorMsgs.add("Failed to parse stream object <br>");
        }


        // Set up JDBC stuff
        Statement stmnt;
        try {
            ArrayList<HealthData> healthList = parse(out, sb.toString());

            Connection con = new OracleConnection().getConnection(USERNAME, PASSWORD);
            stmnt=con.createStatement();

            String createString="CREATE TABLE Health (sId VARCHAR(64), sLocation VARCHAR(64), sLocType VARCHAR(32), sYear NUMBER, sDesc VARCHAR(128), sValue NUMBER)";
            stmnt.executeUpdate(createString);

            errorMsgs.add("<div style='color:#228B22;'>Created Database.</div><br>");

            for (int idx=0;idx<healthList.size();idx++) {
                String HealthSQLString = ("INSERT INTO Health VALUES('" 
                        + healthList.get(idx).getId() + "', '"         // string
                        + healthList.get(idx).getLocation() + "', '"   // string
                        + healthList.get(idx).getType() + "',"         // string
                        + healthList.get(idx).getYear() + ", '"        // number
                        + healthList.get(idx).getDescription() + "',"  // string
                        + healthList.get(idx).getRawValue() + ")");       // number;
                stmnt.executeUpdate(HealthSQLString);
            } 

            stmnt.close();
            return true;
        }
        catch (Exception e) {
          errorMsgs.add("<div class='error'>Failed to create database:"+e+"</div><BR>");
          return false;
        }


    }

    /**
     * Imports a String of JSON data and breaks it down into its
     * component parts, creating a list of HealthCare objects
     * each representing one line of the results.
     * @param jsonLine the line of JSON data returned from the query call
     * @return a list of HealthData objects each containing one "table row" of the parsed JSON data
     */
    public ArrayList<HealthData> parse(PrintWriter out, String jsonLine)
    {
        ArrayList<HealthData> list = new ArrayList<>();

        int i = 0;
        int j = 0;
        try{
            JsonParser parser = new JsonParser();
            JsonElement tradeElement = parser.parse(jsonLine);
            JsonArray jarray = tradeElement.getAsJsonArray();

            j = jarray.size();
            for (i = 0; i < jarray.size(); i++) {
                JsonObject jsonobject = jarray.get(i).getAsJsonObject();
                String id = jsonobject.get("id").getAsString();
                String location = jsonobject.get("name").getAsString().replace(",", "");
                String type = jsonobject.get("type").getAsString();
                String year = jsonobject.get("year").getAsString();
                String description = jsonobject.get("variable").getAsString();
                String value = jsonobject.get("value").getAsString();
                list.add(HealthDataFactory.getHealthData(id, location, type, Integer.valueOf(year), description, Float.valueOf(value)));
            }
            return list;
        } catch (Exception e) {
            out.println("<div style='color:#8B0000;'>Couldn't parse json. Failed on element " + i + " of " + j + "</div><br>");
        }
      return list;
    }


    // Output name, cap, & pop of states returned from a query
    public static ArrayList<HealthData> outputQueryResults(ResultSet data,PrintWriter out) throws java.sql.SQLException
    {
        //ResultSetMetaData rsmd = data.getMetaData();

        ArrayList<HealthData> healthRow = new ArrayList<>();

        HashMap<String, Float> countyStats = new HashMap<String, Float>();
        ArrayList<HealthData> countyStatsList = new ArrayList<>();
        
        String tableDesc = "";
        while (data.next()) {

            try {
                HealthData row = HealthDataFactory.getHealthData(data.getString(1), data.getString(2), data.getString(3), Integer.valueOf(data.getString(4)), data.getString(5), Float.valueOf(data.getString(6)));
                
                countyStatsList.add(row);

                if("".equals(tableDesc))
                    tableDesc = data.getString(5);

            } catch(Exception e) {
                errorMsgs.add("<br><div class='error'>Damn, botched it</div><br>");
                break;
            }
        }

        // Slicing and prettying the important parts from the queried description.
        //   "access_to_exercise_numerator" -> "access to exercise"
        ArrayList<String> descParts = new ArrayList<>(Arrays.asList(tableDesc.split("_"))); 
        String descriptionSubstring = "";
        for(int i = 0; i < descParts.size()-1; i++) {
            descriptionSubstring += descParts.get(i);
            descriptionSubstring += " ";
        }
        tableTitle = String.format("<div Align=Center><h3>Results for '%s' in %s</h3></div><br>", descriptionSubstring, state);
        if(countyStatsList.size() == 0) {
            emptyMsg = "<br><div class='neutral' Align=Center>No data exists for that state-variable combo, unfortunately. Try another.</div><br>";
        } else {
            emptyMsg = "";
            // stateData is used as a dummy object to get the formatting for 
            //   the value's data right and to prove that my objects work :P
            HealthData stateData = HealthDataFactory.getHealthData(countyStatsList.get(0).getId(), state, "State", Integer.valueOf(countyStatsList.get(0).getYear()), descriptionSubstring, new Float(0.1));
            
            float sumNum = 0;
            float sumDen = 0;

            // Using len to assert that no hiccups happen regarding the
            // size of the array immediately resizing the array. 
            int len = countyStatsList.size();

            if("PERCENTAGE".equals(queryType)) {
                // Count exists because not all states have -BOTH- a numerator and a 
                //   denominator with their data. To get a proper average, I only
                //   tally the counties which passed my inspection down yonder,
                //   ergo they have both a numerator and a denominator.
                int count = 0;
                try{
                    
                    while(len > 2) {
                        float num = 0;
                        float denom = 0;
                        String loc = countyStatsList.get(0).getLocation();
                        if(countyStatsList.get(0).getDescription().indexOf("numerator")!=-1) {
                            num = countyStatsList.get(0).getRawValue();
                            sumNum+=num;
                        }
                        if(countyStatsList.get(0).getDescription().indexOf("denominator")!=-1) {
                            denom = countyStatsList.get(0).getRawValue();
                            sumDen+=denom;
                        }
                        for(int i = 1; i < len; i++){
                            if(countyStatsList.get(i).getLocation().equals(loc)) {
                                if(countyStatsList.get(i).getDescription().indexOf("numerator")!=-1) {
                                    num = countyStatsList.get(i).getRawValue();
                                    sumNum+=num;
                                }
                                else if(countyStatsList.get(i).getDescription().indexOf("denominator")!=-1) {
                                    denom = countyStatsList.get(i).getRawValue();
                                    sumDen+=denom;
                                }
                                
                                countyStatsList.remove(i);
                                
                                float result = num/denom;
                                countyStats.put(loc, result);
                                len--;
                                count++;
                                break;
                            }
                        }
                        countyStatsList.remove(0);
                    }
                } catch(Exception e){}

                if(sumDen>0) {
                    stateData.setValue( new Float(sumNum/sumDen/count) );
                }
                if(regionCheck == PerCounty){
                    Set set = countyStats.entrySet();
                    Iterator i = set.iterator();

                    while(i.hasNext()) {
                        Map.Entry<String, Float> me = (Map.Entry<String, Float>)i.next();
                        healthRow.add(HealthDataFactory.getHealthData(me.getKey(), me.getValue()));
                    }
                }
                else if(regionCheck == StateWide) {
                    healthRow.add(HealthDataFactory.getHealthData(state, stateData.getRawValue()));
                }
                
            } else {
                return countyStatsList;
            }
            
        } 
        return healthRow;
    }

    public void dropStatesTable(PrintWriter out, String table)
    {
        Statement stmnt;
        try {
            stmnt=con.createStatement();
            // We can remove the table (use when table needs to go)
            stmnt.executeUpdate("DROP TABLE " + table);
            stmnt.close();
            out.println("Health Table Dropped<br>");
        }
        catch (Exception e) {
            out.println("Error removing table:"+e+"<BR>");
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
      doPost(req,res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {   //Get the response's PrintWriter to return text to the client.
        PrintWriter toClient = res.getWriter();
        // No matter what, set the "content type" header of the response
        res.setContentType("text/html");
        errorMsgs = new ArrayList<>();
        String table = "Health";
        if(req==null || !"".equals(req.getParameter("state"))){
            try {
                Connection con = new OracleConnection().getConnection(USERNAME,PASSWORD);
                // Create a statement to access database
                stmnt=con.createStatement();
                String newQuery = parseQuery(req);

                // test for the table exists
                boolean created = false;

                ArrayList<HealthData> healthRow = null;
                

                try {
                    dropStatesTable(toClient, table);

                    created = readUrl(toClient, newQuery);

                    PreparedStatement qStmnt=con.prepareStatement(String.format("SELECT * FROM %s", table));
                    ResultSet r = qStmnt.executeQuery();

                    healthRow = outputQueryResults(r, toClient);
                    
                    //
                } catch(Exception e) {  // no table (likely)
                    errorMsgs.add(String.format("<div class='error'> - SQL error: %s</div><br> -",e));
                    try {   // make the table
                    if(!created)
                        readUrl(toClient, newQuery);
                    }
                    catch (Exception ee) {     
                        errorMsgs.add(String.format("<div class='error'>Unspecified error creating table:, %s</div><br>",ee));
                    }    
                }
                req.setAttribute("healthRow", healthRow);
                req.setAttribute("errorMsgs", errorMsgs);
                req.setAttribute("emptyMsg", emptyMsg);
                req.setAttribute("tableTitle", tableTitle);
                // req.getRequestDispatcher("/index.jsp").forward(req, res);
        // Close the statement
        stmnt.close();
            } catch (Exception e) {
                toClient.println(stmnt+"Unspecified error:"+e+" 2 <BR>");
            }
        }
        req.getRequestDispatcher("/index.jsp").forward(req, res);
        
    }

}
