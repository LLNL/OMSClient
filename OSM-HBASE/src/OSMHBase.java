import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

import gov.llnl.lc.infiniband.opensm.plugin.data.OSM_SysInfo;
import gov.llnl.lc.infiniband.opensm.plugin.net.OsmClientApi;
import gov.llnl.lc.infiniband.opensm.plugin.net.OsmServiceManager;
import gov.llnl.lc.infiniband.opensm.plugin.net.OsmSession;
import gov.llnl.lc.infiniband.core.IB_Guid;
import gov.llnl.lc.infiniband.core.IB_Link;
import gov.llnl.lc.infiniband.opensm.plugin.data.*;

import org.json.JSONException;
import org.json.JSONObject;

public class OSMHBase {
	
	private static BufferedWriter countersBW, linksBW, routesBW;
	private static boolean writingHeaders = false;
	
	private static FilenameFilter fnameFilter = new FilenameFilter() {
    	public boolean accept(File dir, String name) {
    		return name.startsWith("cab.201507") && name.endsWith(".his");
    	}
	};
	private static File hisDir = new File("/Users/brown303/workspace/eclipse/OSM-HBASE/data/history");
	private static File outDir = new File("/Users/brown303/workspace/eclipse/OSM-HBASE/data/processed");
	
	public static void main(String[] args) throws Exception
	{

		OMS_Collection omsHistory = null;
		OpenSmMonitorService oms = null;
		OSM_Fabric fabric = null;
		
		long currentTime = System.currentTimeMillis() / 1000;
		long timestamp = 0;
		
		int i;
		
		try{	
			File counterFile = new File(outDir, "/counters." + currentTime + ".txt");
			File routesFile = new File(outDir, "/routes." + currentTime + ".txt");
			File linksFile = new File(outDir, "/links." + currentTime + ".txt");
			if (!counterFile.exists()) { counterFile.createNewFile(); }
			if (!routesFile.exists()) { routesFile.createNewFile(); }
			if (!linksFile.exists()) { linksFile.createNewFile(); }
			
			countersBW = new BufferedWriter(new FileWriter(counterFile));
			routesBW = new BufferedWriter(new FileWriter(routesFile));
			linksBW = new BufferedWriter(new FileWriter(linksFile));
			
			
			File[] hisFiles = hisDir.listFiles(fnameFilter);
			for (File hisFile : hisFiles){
				System.out.println("Processing history file: " + hisFile.getPath());
			
				omsHistory = OMS_Collection.readOMS_Collection(hisFile.getPath());
				
				for (i = 0; i < omsHistory.getSize(); i++){
					oms = omsHistory.getOMS(i);
					fabric = oms.getFabric();
					
					timestamp = oms.getTimeStamp().getTimeInSeconds();
					System.out.println(".snapshot: " + oms.getTimeStamp().toString() + ".");
					
					//writePortCountersJSON(fabric.getOSM_Ports(), timestamp);
					//writeLinksJSON(fabric.getIB_Links(), timestamp);
					//writePortForwardingTableJSON(RT_Table.buildRT_Table(fabric), timestamp);
					
				}
			}
			//writePortForwardingTableJSON(RT_Table.buildRT_Table(fabric), timestamp);
			//writeLinks(fabric.getIB_Links(), timestamp);
			//writePortCounters(fabric.getOSM_Ports(), timestamp);
			
			countersBW.flush();
			countersBW.close();
			
			routesBW.flush();
			routesBW.close();
			
			linksBW.flush();
			linksBW.close();
			
		}
		catch (Exception e)
		{
			System.err.println("Couldn't open the file");
			e.printStackTrace();
		}
		System.out.println("- Complete");
	}
		
	private static void writePortCounters(LinkedHashMap<String, OSM_Port> ports, long timestamp){
		OSM_Port port;
		String portId;
		String nguid;
		int portNum;
		long recvData, xmitDrop, xmitWait, recvErr, recvSwRE, recvRPhE, xmitData, xmitConE;
		
		if(writingHeaders){
			try {
				countersBW.write("# Record format: \"<timestamp>:<nguid>:<pn>:<xmitData>:<recvData>\""); 
				countersBW.newLine();
				countersBW.write("# ----Port Counters BEGIN----");
				countersBW.newLine();
			
			} catch (Exception e) {
				System.out.println("ERROR: could not write port counter header.");
				
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		for (Map.Entry<String, OSM_Port> entry: ports.entrySet()){
			
			portId = entry.getKey();
			port = entry.getValue();
			
			nguid = portId.substring(0, 19).replace(":", "");
			portNum = Integer.parseInt(portId.substring(20));
			
			if (port.getPfmPort() == null){
				continue;
			}
			timestamp = port.pfmPort.counter_ts;
			recvData = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_data) * 4;
			recvErr = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_err);
			recvSwRE = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_switch_relay_err);
			recvRPhE = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_rem_phys_err);
			xmitData = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_data) * 4;
			xmitDrop = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_discards);
			xmitWait = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_wait);
			xmitConE = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_constraint_err);
			
			try {
				
				countersBW.write(timestamp + ":" + nguid + ":" + portNum + ":" + xmitData + ":" + recvData);
				countersBW.newLine();
				
				//if (i == 2) break;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("ERROR: could not write port counters. ");
				
				e.printStackTrace();
				System.exit(1);
			}
		}
		System.out.println("- Wrote port counters.");
	}
	
	private static void writePortCountersJSON(LinkedHashMap<String, OSM_Port> ports, long timestamp){
		int i;
		
		OSM_Port port;
		String portId;
		String nguid;
		int portNum;
		long recvData, xmitDrop, xmitWait, recvErr, recvSwRE, recvRPhE, xmitData, xmitConE;
		JSONObject jsonPort = null;
		
		i = 0;
		for (Map.Entry<String, OSM_Port> entry: ports.entrySet()){
			
			portId = entry.getKey();
			port = entry.getValue();
			
			nguid = portId.substring(0, 19).replace(":", "");
			portNum = Integer.parseInt(portId.substring(20));
			
			if (port.getPfmPort() == null){
				continue;
			}
			//timestamp = port.pfmPort.counter_ts;
			recvData = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_data) * 4;
			recvErr = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_err);
			recvSwRE = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_switch_relay_err);
			recvRPhE = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_rem_phys_err);
			xmitData = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_data) * 4;
			xmitDrop = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_discards);
			xmitWait = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_wait);
			xmitConE = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_constraint_err);
			
			jsonPort = new JSONObject();
			try {
				jsonPort.put("ts", timestamp);
				jsonPort.put("nguid", nguid);
				jsonPort.put("portNum", portNum);
				
				jsonPort.put("r_data", recvData);
				jsonPort.put("r_err", recvErr);
				jsonPort.put("r_sr_err", recvSwRE);
				jsonPort.put("r_phys_err", recvRPhE);
				
				jsonPort.put("xmit_data", xmitData);
				jsonPort.put("xmit_discards", xmitDrop);
				jsonPort.put("xmit_wait", xmitWait);
				jsonPort.put("xmit_con_err", xmitConE);
				
				countersBW.write(jsonPort.toString());
				countersBW.newLine();
				
				//if (i == 2) break;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("ERROR: could not write port counters. ");
				
				e.printStackTrace();
				System.exit(1);
			}
			i++;
		}
		System.out.println("- Wrote counters file.");
	}
	
	private static void writePortForwardingTable(RT_Table RoutingTable, long timestamp){
		
		RT_Node node;
		String nguid;
		RT_Port port;
		int portNum;
		int routeLid;
		
		if(writingHeaders){
			try {
				routesBW.write("# Record format: \"<ExitPort>:<LID>\"");
				routesBW.newLine();
				routesBW.write("#----Forwarding Table BEGIN----");
				routesBW.newLine();
			
			} catch (Exception e) {
				System.out.println("ERROR: could not write port counter header.");
				
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		try{
			for (Map.Entry<String, RT_Node> nEntry: RoutingTable.getSwitchGuidMap().entrySet()){
				node  = nEntry.getValue();
				nguid = node.getGuid().toColonString().replace(":", "");
				
				routesBW.newLine();
				routesBW.write("Switch: 0x" + nguid); routesBW.newLine();
				
				for (Map.Entry<String,RT_Port> pEntry: node.getPortRouteMap().entrySet()){
					port = pEntry.getValue();
					portNum = port.getPortNumber();
					
					for (Map.Entry<String,Integer> item: port.getLidGuidMap().entrySet()){
						routeLid = item.getValue();
						routesBW.write(portNum + ":" + routeLid); routesBW.newLine();
					}
				}
			}
		}catch (Exception e){
			System.out.println("ERROR: Unable to write routes to file.");
		}
		System.out.println("- Wrote routes file.");
	}
	
	private static void writePortForwardingTableJSON(RT_Table RoutingTable, long timestamp){
			
			RT_Node node;
			String nguid;
			RT_Port port;
			int portNum;
			int routeLid;
			JSONObject jsonPort = null;
			StringBuffer routes;
			
			try{
				for (Map.Entry<String, RT_Node> nEntry: RoutingTable.getSwitchGuidMap().entrySet()){
					node  = nEntry.getValue();
					nguid = node.getGuid().toColonString().replace(":", "");
					
					for (Map.Entry<String,RT_Port> pEntry: node.getPortRouteMap().entrySet()){
						port = pEntry.getValue();
						portNum = port.getPortNumber();
						
						jsonPort = new JSONObject();
						jsonPort.put("ts", timestamp);
						jsonPort.put("nguid", nguid);
						jsonPort.put("portNum", portNum);
						
						routes = new StringBuffer();
						for (Map.Entry<String,Integer> item: port.getLidGuidMap().entrySet()){
							routeLid = item.getValue();
							routes.append(routeLid + ":");
							
						}
						
						jsonPort.put("routes", routes);
						routesBW.write(jsonPort.toString()); routesBW.newLine();
					}
				}
			}catch (Exception e){
				System.out.println("ERROR: Unable to write routes to file.");
			}
			System.out.println("- Wrote routes to file.");
		}
	
	private static void writeLinks(LinkedHashMap<String, IB_Link> ibLinks, long timestamp){
		OSM_Port port1, port2;
		String nguid1, nguid2;
		Integer portNum1, portNum2;
		String nodeType1, nodeType2;
		Integer lid1, lid2;
		
		if(writingHeaders){
			try {
				linksBW.write("# Record format: \"nguid1:nguid2:pn1:pn2:ntype1:ntype2:lid1:lid2\"");
				linksBW.newLine();
				linksBW.write("#----Links BEGIN----");
				linksBW.newLine();
			
			} catch (Exception e) {
				System.out.println("ERROR: could not write port counter header.");
				
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		for(Map.Entry<String, IB_Link> entry: ibLinks.entrySet()){
	        IB_Link ln = entry.getValue();
	        
	        port1 = ln.getEndpoint1();
	        port2 = ln.getEndpoint2();
	        
	        nguid1 = port1.getNodeGuid().toColonString().replace(":", "");
	        nguid2 = port2.getNodeGuid().toColonString().replace(":", "");
	        
	        portNum1 =  port1.getPortNumber();
	        portNum2 =  port2.getPortNumber();
	        
	        nodeType1 = port1.getNodeType().getAbrevName();
	        nodeType2 = port2.getNodeType().getAbrevName();
	        
	        lid1 = port1.getAddress().getLocalId();
	        lid2 = port2.getAddress().getLocalId();
	        
	        try{
		        linksBW.write(nguid1 + ":" + nguid2 + ":" + portNum1  + ":" + portNum2  + ":" + nodeType1 + ":" + nodeType2 + ":" + lid1 + ":" + lid2);
		        linksBW.newLine();
	        }catch (Exception e){
				System.out.println("ERROR: Unable to write links to file.");
			}
		}
		System.out.println("- Wrote links file.");

	}
	
	private static void writeLinksJSON(LinkedHashMap<String, IB_Link> ibLinks, long timestamp){
		OSM_Port port1, port2;
		String nguid1, nguid2;
		Integer portNum1, portNum2;
		String nodeType1, nodeType2, conn1, conn2;
		int lid1, lid2;
		JSONObject jsonPort1 = null, jsonPort2 = null;
		
		for(Map.Entry<String, IB_Link> entry: ibLinks.entrySet()){
	        IB_Link ln = entry.getValue();
	        
	        port1 = ln.getEndpoint1();
	        port2 = ln.getEndpoint2();
	        
	        nguid1 = port1.getNodeGuid().toColonString().replace(":", "");
	        nguid2 = port2.getNodeGuid().toColonString().replace(":", "");
	        
	        portNum1 =  port1.getPortNumber();
	        portNum2 =  port2.getPortNumber();
	        
	        nodeType1 = port1.getNodeType().getAbrevName();
	        nodeType2 = port2.getNodeType().getAbrevName();
	        
	        lid1 = port1.getAddress().getLocalId();
	        lid2 = port2.getAddress().getLocalId();
	        
	        conn1 = nguid2 + ":" + portNum2;
	        conn2 = nguid1 + ":" + portNum1;
	        
	        try{
	        	jsonPort1 = new JSONObject();
				jsonPort1.put("ts", timestamp);
				jsonPort1.put("nguid", nguid1);
				jsonPort1.put("portNum", portNum1);
				jsonPort1.put("type", nodeType1);
				jsonPort1.put("lid", lid1);
				jsonPort1.put("conn", conn1);
				
				jsonPort2 = new JSONObject();
				jsonPort2.put("ts", timestamp);
				jsonPort2.put("nguid", nguid2);
				jsonPort2.put("portNum", portNum2);
				jsonPort2.put("type", nodeType2);
				jsonPort2.put("lid", lid2);
				jsonPort2.put("conn", conn2);
				
				
		        linksBW.write(jsonPort1.toString());  linksBW.newLine();
		        linksBW.write(jsonPort2.toString());  linksBW.newLine();
	        }catch (Exception e){
				System.out.println("ERROR: Unable to write links to file.");
			}
		}
		System.out.println("- Wrote links file.");

	}
	
	public static void WorkConsole(String[] args) throws Exception
	{
		// Get input using IDE
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Enter oun: ");
		String oun = br.readLine();
		System.out.print("Enter otp: ");
		String otp = br.readLine();
		*/
		
		// Get input at console (non-IDE)
		Console console = System.console();
		String oun = console.readLine("Enter username: ");
		String otp = new String(console.readPassword("Enter OTP: "));
		
		// establish a connection, and dump the system info
	    OsmSession ParentSession = null;
	    /* the one and only OsmServiceManager */
	    OsmServiceManager OsmService = OsmServiceManager.getInstance();
	    try
	    {
	      ParentSession = OsmService.openSession("cab664.llnl.gov", "10011", oun, otp);
	    }
	    catch (Exception e)
	    {
	      System.err.println(e.getStackTrace().toString());
	      System.exit(-1);
	    }
	    if (ParentSession != null)
	    {
	      OsmClientApi clientInterface = ParentSession.getClientApi();
	      /* use the api's to get the system information */
	      OSM_SysInfo sysinfo = clientInterface.getOsmSysInfo();
	      System.out.println(sysinfo);
	      /* all done, so close the session(s) */
	      OsmService.closeSession(ParentSession);
	    }
	}
}
