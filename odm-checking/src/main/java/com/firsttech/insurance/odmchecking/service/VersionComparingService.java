package com.firsttech.insurance.odmchecking.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class VersionComparingService {

	@Autowired
	private Environment environment;
	
	private void connectDB(String sql) {
		// 替換成你的 MySQL 資訊
        String url = environment.getProperty("db.sit.url");
        String username = environment.getProperty("db.sit.username");
        String password = environment.getProperty("db.sit.password");

        Connection conn = null;
        Statement stmt = null;
        try {
        	Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // 建立連線
            conn = DriverManager.getConnection(url, username, password);

            // 建立 Statement 物件
            stmt = conn.createStatement();

            // 執行查詢
            ResultSet rs = stmt.executeQuery(sql);

            // 處理查詢結果
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                // ... 其他欄位
                System.out.println("id = " + id + ", name = " + name);
            }

            // 關閉資源
            rs.close();
            stmt.close();
            conn.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
	}
}
