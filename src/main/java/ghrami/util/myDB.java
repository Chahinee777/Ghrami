package ghrami.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class myDB {

        private static myDB instance ;
        private Connection conn ;
        private String URL = "jdbc:mysql://localhost:3306/ghrami_db" ;
        private String USER= "root"  ;
        private String PASSWORD = "";
        private myDB(){
            try {
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("connected");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static myDB getInstance() {
            if (instance == null) instance = new myDB();
            return instance;
        }

        public Connection getConnection() {
            return conn;
        }
    }





