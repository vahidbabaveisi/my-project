package com.example.optimisation;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
 

public class Database {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASS = "postgres";

    public static void main(String[] args) {
        List<Map<String, Object>> result = getQuery("SELECT * FROM \"test\"");
        System.out.println(result);
        
    }


    public static void ConnectToDatabase() {
        try {
             
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connected to the database successfully!");
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This function is used to get the query from the database and return it as a list of maps
    public static List<Map<String, Object>> getQuery(String query) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(query);
            var result = new java.util.ArrayList<Map<String, Object>>();
            var metaData = resultSet.getMetaData();
            var columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                var row = new java.util.HashMap<String, Object>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                result.add(row);
            }
            connection.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public static String getDbUrl() {
        return DB_URL;
    }

    public static String getUser() {
        return USER;
    }

    public static String getPass() {
        return PASS;
    }

}
