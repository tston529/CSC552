package com.test;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.URL;
import java.text.DecimalFormat;
import javax.servlet.*;
import javax.servlet.http.*;

public class HealthDBAccess extends HttpServlet {

    private static final String USERNAME = "spiegel";
    private static final String PASSWORD = "legeips";

    private static final String popQuery =
       "SELECT sName, sPop, sCap FROM States WHERE sPop BETWEEN ? AND ? "+
                            " ORDER BY sName";
    private static Statement stmnt;
    private static Connection con;

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
    public void createStatesTable(PrintWriter out)
    {  
        int ctr=0;
        //State[] sList=new State[51]; 

        URL TheFile=null;
        try {   // Set up a URL to the file
           TheFile=new URL("http://acad.kutztown.edu/~spiegel/cis521/Examples/StatesDB/states.txt");
        }
        catch (Exception e) {
           out.println("URL Setup failed...");
        }
        InputStream s=null;
        try { // Hook up to the file on the server
           s=TheFile.openStream();
        }
        catch (Exception e)  {
           out.println("!! Stream open failed !!");
        }
        try {

          BufferedReader DataInput=new BufferedReader(new InputStreamReader(s));
          // Read the file and print the States that were read
        /*
          int ctr=0;
          System.out.println("The List in File Order...");
          State.printHeader();
        */
            while ((sList[ctr++]=State.readStateFromFile(DataInput,out))!=null);
        } catch (Exception e) {
          out.println("Error Reading States File:"+e+"<BR>");
        }

        // Set up JDBC stuff
        Statement stmnt;
        try {
          ArrayList<HealthData> csvList = readCsv("healthdata.csv");
          Connection con = new OracleConnection().getConnection(USERNAME, PASSWORD);
          stmnt=con.createStatement();
          // Need to use unique names (Area messed things up!)
          String createString="CREATE TABLE Health (sId VARCHAR(64), sName VARCHAR(64), "+
                    "sLocation VARCHAR(32), sYear NUMBER, sVar VARCHAR(128), "+
                                            "sValue NUMBER, dDesc VARCHAR2(128))";
          stmnt.executeUpdate(createString);
          for (int idx=0;idx<csvList.length();idx++) {
            // Form the string representing the insertion statement for this state
            // Note use of ' to delimit strings in the SQL statement
            String StateSQLString = ("INSERT INTO Health VALUES('" +csvList[idx].name
              + "'," + csvList[idx].population + "," + csvList[idx].area + "," +
                             csvList[idx].year + "," + csvList[idx].order + ",'" +
                                                        csvList[idx].capital + "')");
        out.println(sList[idx].name);
            stmnt.executeUpdate(StateSQLString);
          } // for
          // We can remove the table (use when table needs to go)
          // stmnt.executeUpdate("DROP TABLE States");
          stmnt.close();
        }
        catch (Exception e) {
          out.println("Database table create error:"+e+"<BR>");
        }
    
    }

    /**
     * Imports a CSV file and generates a list containing HealthData objects,
     * each containing the data for one resulting row.
     * @param filename the name of the file to read
     * @return the list of HealthData objects
     * @throws IOException
     */
    public List<HealthData> readCsv(String filename) throws IOException {
        List<HealthData> hdList = new ArrayList<>();
        try{
            BufferedReader br = new BufferedReader(new FileReader(filename));
            br.readLine(); //Skip first line (the header info)

            String line;
            String[] splitData;
            while ((line = br.readLine()) != null) {
                splitData = line.split(",");
                hdList.add(new HealthData(splitData[0], splitData[1], splitData[2], Integer.valueOf(splitData[3]), splitData[4], Float.valueOf(splitData[5])));
            }
        } catch (FileNotFoundException e) { }
        return hdList;
    }
}