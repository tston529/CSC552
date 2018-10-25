/********************************************************************************
//         File: OracleConnection.java
//       Author: Dr. Spiegel, Adapted from work by Joe Schick & Chris Walck
//         Date: 11/26/03 --> Aug. 11, 2015
//     Computer: PCs and Suns
//      Purpose: This class provides a getConnection method for the creation 
//               and retrieval of an oracle connection. 
//
//********************************************************************************/

 import java.sql.*;
 import java.sql.DriverManager;
 import java.sql.Connection;
 import java.sql.SQLException;

public class OracleConnection extends DBConnection
{
  private static final String DRIVER_TYPE = "thin";
  // private static final String DRIVER_TYPE = "oci8";

 // private OracleDataSource ods;
  private Connection conn;

  public OracleConnection()
   {
   }
  public Connection getConnection(String userName, String password) 
                          				 throws SQLException
   {
     try 
      {
        Class.forName ("oracle.jdbc.driver.OracleDriver");
      } 
     catch (Exception e) 
      {
        System.out.println ("Could not load the driver"); 
      }
      String ConnectString="jdbc:oracle:thin:@csdb.kutztown.edu:1521:orcl";
      conn=DriverManager.getConnection(ConnectString,userName,password);

     return conn;
   }
}
