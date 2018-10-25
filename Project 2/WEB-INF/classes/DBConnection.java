//********************************************************************************
//         File: DBConnection.java
//       Author: Joe Schick
//         Date: 11/26/03
//     Computer: PCs and Suns
//      Purpose: This abstract class can be extended to facilitate the creation 
//               of any database connection.
//
//********************************************************************************

import java.sql.*;

public abstract class DBConnection
{
  public DBConnection()
    {
    }
  public abstract Connection getConnection(String userName, String password) 
  			                                    throws SQLException;
}
