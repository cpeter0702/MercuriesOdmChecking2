package com.firsttech.insurance.odmchecking.service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firsttech.insurance.odmchecking.domain.Policy;
import com.firsttech.insurance.odmchecking.service.utils.DateUtil;
import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;

@Service
public class VersionComparingService {

	private final static Logger logger = LoggerFactory.getLogger(VersionComparingService.class);
	
	@Autowired
	private Environment environment;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public List<String> getRptHeader (String startDate, String endDate) {
		List<String> rptList = new ArrayList<>();
		rptList.add("ODM Comparing Test Start from " + startDate + " to " + endDate);
		rptList.add("Test Date Time: " + DateUtil.formatDateToString("yyyy-MM-dd hh:mm:ss", new Date()));
		rptList.add("");
		rptList.add("PolicyNo, Status, Diff");
		rptList.add("===========================, ===========================, ==========================");
		return rptList;
	}
	
	private List<String> getRptBody (String startDate, String endDate) {
		List<String> reportBody = new ArrayList<>();
		List<String> nbList = this.calODM("nb", startDate, endDate);
		List<String> taList = this.calODM("ta", startDate, endDate);
//		List<String> etsList = this.calODM("ets", startDate, endDate);
		
		reportBody.addAll(nbList);
		reportBody.addAll(taList);
//		reportBody.addAll(etsList);
		return reportBody;
	}
	
	private List<String> getRptFooter(List<String> reportBody) {
		List<String> rptList = new ArrayList<>();
		rptList.add("===========================, ===========================, ==========================");
		int iPass = 0;
		int iFail = 0;
		int iError = 0;
		
		for (String eachRecord : reportBody) {
			if (eachRecord.contains("PASS")) {
				iPass += 1;
			} else if (eachRecord.contains("FAIL")) {
				iFail += 1;
			} else if (eachRecord.contains("ERROR")) {
				iError += 1;
			} else {
				if (eachRecord.contains(",")) 
					logger.info("下面紀錄無法統計: " + eachRecord);
			}
		}
		
		rptList.add("ODM Testing Result PASS: " + iPass + ", FAIL: " + iFail + ", ERROR: " + iError);
		return rptList;
	}

	public boolean doComparing (String startDate, String endDate) {
		List<String> reportList = new ArrayList<>();
		reportList.addAll(this.getRptHeader(startDate, endDate));
		reportList.addAll(this.getRptBody(startDate, endDate));
		reportList.addAll(this.getRptFooter(reportList));
		
		String rptOutputPath = environment.getProperty("output.path") + "\\ODM9_testing_report_" + startDate + ".csv";
		logger.info("匯出報告路徑: {}", rptOutputPath);
		boolean isSuccess = FileUtil.writeToFile(reportList, rptOutputPath);
		logger.info("比對報告產生結果: " + (isSuccess ? "SUCCESSFUL" : "FAIL"));
		return isSuccess;
	}
	
	private List<String> calODM (String target, String startDate, String endDate) {
		logger.info("---------------------------------------------------------");
		logger.info("開始比對目標: {} ", target);
		HttpUtil httpUtil = new HttpUtil();
		
		// 取得 要測試 target 對應的 ODM url
		Map<String, String> urlMap = this.getOdmUrlMap(target);
		String odm8CheckUrl = urlMap.get("odm8CheckUrl");
		String odm9CheckUrl = urlMap.get("odm9CheckUrl");
		logger.info("取得舊版連結 (odm8CheckUrl): {}", odm8CheckUrl);
		logger.info("取得新版連結 (odm9CheckUrl): {}", odm9CheckUrl);
		
		// request header
		Map<String, String> headerMap = new HashMap<>();
    	headerMap.put("Accept", "application/json");
    	headerMap.put("Content-type", "application/json");
        
        // DB 取得驗測案例
        String sql = this.getQuerySQL(target, startDate, endDate);
        logger.info("SQL: {}", sql);
		List<Policy> policyList = this.getCaseInDatasFromDB(target, sql);
		logger.info("DB 取出資料總比數為: {}" + policyList.size());
		List<String> nodeCode8 = null;
		List<String> nodeCode9 = null;
		List<String> reportList = new ArrayList<>();
		StringBuilder eachSb = null;
		
		
        for (Policy policy : policyList) {
        	eachSb = new StringBuilder();
        	nodeCode8 = null;
        	nodeCode9 = null;
        	// ODM firing : origin
        	try {
        		HttpResponse originResponse = httpUtil.httpRequestPost(odm8CheckUrl, policy.getCase_in(), headerMap);
				int statusCode = originResponse.getStatusLine().getStatusCode();
				String bodyContent = EntityUtils.toString(originResponse.getEntity(), "UTF-8");
				
				if (policy.getPolicy_no().equals("157900794578")) {
					logger.info("origin bodyContent: {}", bodyContent);
				}
				
	        	if (statusCode >= 200 && statusCode < 300) {
	        		JsonNode originJsonNode = mapper.readTree(bodyContent);
	        		nodeCode8 = originJsonNode.path("outParam").path("resultItem").findValuesAsText("noteCode");
				} else {
					logger.info("origin FAIL policyNo: {}, status code: {}, return body: {}", policy.getPolicy_no(), statusCode,  bodyContent);
				}
        	
        	} catch (KeyManagementException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (KeyStoreException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        	// ODM firing : new 
        	try {
        		HttpResponse newResponse = httpUtil.httpRequestPost(odm9CheckUrl, policy.getCase_in(), headerMap);
	        	int statusCode = newResponse.getStatusLine().getStatusCode();
	        	String bodyContent = EntityUtils.toString(newResponse.getEntity(), "UTF-8");
	        	
	        	if (policy.getPolicy_no().equals("157900794578")) {
					logger.info("new bodyContent: {}", bodyContent);
				}
	        	
	        	if (statusCode >= 200 && statusCode < 300) {
	        		logger.info("new policyNo: {}, status code: {}", policy.getPolicy_no(), statusCode);
	        		JsonNode newJsonNode = mapper.readTree(bodyContent);
	        		nodeCode9 = newJsonNode.path("outParam").path("resultItem").findValuesAsText("noteCode");
				} else {
					logger.info("new FAIL policyNo: {}, status code: {}, return body: {}", policy.getPolicy_no(), statusCode,  bodyContent);
				}
			} catch (KeyManagementException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (KeyStoreException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        	String status = "";
        	String diff = "";
        	
        	if (nodeCode8.isEmpty() || nodeCode8 == null) {
        		status = "ERROR";
        		diff = "Origin 發生錯誤";
			} else if (nodeCode9.isEmpty() || nodeCode9 == null) {
				status = "ERROR";
				diff = "new 發生錯誤";
			} else {
				if (this.isEqual(nodeCode8, nodeCode9)) {
					status = "PASS";
					diff ="NoteCode is same.";
				} else {
					status = "FAIL";
					diff = this.getDiffCodes(nodeCode8, nodeCode9);
				}
			}
			
			eachSb.append(policy.getPolicy_no()).append(", ")
				  .append(status).append(", ")
				  .append(diff);
			
			reportList.add(eachSb.toString());
        	
        }
		return reportList;
	}
	
	private boolean isEqual(List<String> nodeCode1, List<String> nodeCode2) {
		if (nodeCode1.equals(nodeCode2))
			return true;
		return false;
	}
	
	private String getDiffCodes(List<String> nodeCode1, List<String> nodeCode2) {
        
		StringBuilder sb = new StringBuilder();
		for (String code1 : nodeCode1) {
			boolean isDuplicated = false;
			for (String code2 : nodeCode2) {
				if (code1.equals(code2)) {
					isDuplicated = true;
					break;
				}
			}
			
			if (isDuplicated == false) {
				sb.append("[少] " + code1).append("; ");
			}
		}
		

		for (String code2 : nodeCode2) {
			boolean isDuplicated = false;
			for (String code1 : nodeCode1) {
				if (code2.equals(code1)) {
					isDuplicated = true;
					break;
				}
			}
			
			if (isDuplicated == false) {
				sb.append("[多] " + code2).append("; ");
			}
		}
		
        return sb.toString();
    }
	
	private Map<String, String> getOdmUrlMap (String target) {
		Map<String, String> map = new HashMap<>();
		
		// 取得當下 IP
		String infoFilePath = environment.getProperty("current.ip.info");
		logger.info("infoFilePath: ", infoFilePath);
		Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String currentIP = infoMap.get("local.ip");
		logger.info("取得當下IP: " + currentIP);
		// SIT
		if (currentIP.startsWith("172.16.16")) {
			if (target.equals("nb")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.sit.nb.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.sit.nb.new"));
			} else if (target.equals("ta")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.sit.ta.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.sit.ta.new"));
			} else if (target.equals("ets")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.sit.ets.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.sit.ets.new"));
			} else {
				logger.info("沒有找到 SIT 環境 ODM 對應 URL");
			}
		// UAT
		} else if (currentIP.startsWith("172.16.18")) {
			if (target.equals("nb")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.uat.nb.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.uat.nb.new"));
			} else if (target.equals("ta")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.uat.ta.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.uat.ta.new"));
			} else if (target.equals("ets")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.uat.ets.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.uat.ets.new"));
			} else {
				logger.info("沒有找到 UAT 環境 ODM 對應 URL");
			}
		// PROD1
		} else if (currentIP.equals("172.16.9.92")) {
			if (target.equals("nb")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.prod1.nb.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.prod1.nb.new"));
			} else if (target.equals("ta")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.prod1.ta.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.prod1.ta.new"));
			} else if (target.equals("ets")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.prod1.ets.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.prod1.ets.new"));
			} else {
				logger.info("沒有找到 SIT 環境 ODM 對應 URL");
			}
		} else if (currentIP.equals("172.16.9.93")) {
			if (target.equals("nb")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.prod2.nb.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.prod2.nb.new"));
			} else if (target.equals("ta")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.prod2.ta.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.prod2.ta.new"));
			} else if (target.equals("ets")) {
				map.put("odm8CheckUrl", environment.getProperty("odm.prod2.ets.origin"));
				map.put("odm9CheckUrl", environment.getProperty("odm.prod2.ets.new"));
			} else {
				logger.info("沒有找到 SIT 環境 ODM 對應 URL");
			}
		} else {
			logger.info("沒有找到本機IP資訊無法對應到正確的 ODM URL");
		}
		
		return map;
	}
	
	
	private String getQuerySQL(String target, String startDate, String endDate) {
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
		String todayStr = LocalDate.now().format(f);
		String startDateStr = startDate == null ?
				LocalDate.parse(todayStr, f).format(f).toString() :
				LocalDate.parse(startDate, f).format(f).toString() + " 00:00:00";	
		String endDateStr = endDate == null ?
				LocalDate.parse(startDateStr, f).plusDays(1).format(f).toString() :
				LocalDate.parse(endDate, f).format(f).toString() + " 23:59:59";
		
		return "SELECT policy_no, keep_date_time, " + target + "_json_in " + "FROM SITODMDB.dbo." + target + "_case_in"
				+ " WHERE keep_date_time BETWEEN '" + startDateStr + "' AND '" + endDateStr + "' ORDER BY keep_date_time ";
	}
	
	private List<Policy> getCaseInDatasFromDB(String target, String sql) {
        String url = environment.getProperty("db.sit.url");
        String username = environment.getProperty("db.sit.username");
        String password = environment.getProperty("db.sit.password");
        
        logger.info("DB connection info: url: {}", url);

        List<Policy> list = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
        	// 建立連線
        	Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(url, username, password);
            stmt = conn.createStatement();		// 建立 Statement 物件
            rs = stmt.executeQuery(sql);		// 執行查詢
            Policy policy = null;
            
            // 處理查詢結果
            while (rs.next()) {
            	policy = new Policy(
            			rs.getDate("keep_date_time"), 
            			rs.getString("policy_no"), 
            			rs.getString(target + "_json_in"));
            	list.add(policy);
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
				rs.close();
				stmt.close();
	            conn.close();
			} catch (Exception e) {
				logger.debug("資源關閉...");
			}
            
        }
        
        return list;
	}
}
