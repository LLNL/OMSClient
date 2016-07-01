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

//import gov.llnl.lc.infiniband.opensm.plugin.data.OSM_SysInfo;
//import gov.llnl.lc.infiniband.opensm.plugin.net.OsmClientApi;
//import gov.llnl.lc.infiniband.opensm.plugin.net.OsmServiceManager;
//import gov.llnl.lc.infiniband.opensm.plugin.net.OsmSession;
import gov.llnl.lc.infiniband.core.IB_Link;
import gov.llnl.lc.infiniband.opensm.plugin.data.*;

import net.minidev.json.JSONObject;

public class OSMHBase {
	
	private static BufferedWriter countersBW, linksBW, routesBW, hbaseDumpBW;
	private static boolean writingHeaders = false;
	
	private static FilenameFilter fnameFilter = new FilenameFilter() {
    	public boolean accept(File dir, String name) {
    		return name.startsWith("cab") && name.endsWith(".his");
    	}
	};
	private static File hisDir;
	private static File outDir = new File(".");
	private static enum outFormat {json, delimited};
	
	public static void main(String[] args) throws Exception
	{
			if (args.length > 0){
				if (args[0].equals("help")){
					showUsage();
				}
				if (args[0].equals("parseHis")){
					processOMSHistory(args);
				}
				
			} else{
				System.out.println("No arguments found.");
				showUsage();
				System.exit(1);
			}
			
	}
	
	private static void showUsage(){
		System.out.println("-- ibperfp : IB Performance Processor --");
		System.out.println(" ibperfp <operation> [<args>...] [json/del]");
		System.out.println("");
		System.out.println("Usage:");
		System.out.println("# ibperfp help                            - Shows this help/usage message.");
		System.out.println("# ibperfp parseHis /path/to/his/dir       - Extract data from OMS '.his' files located in a given path.");
		System.out.println("# ibperfp parseHis /path/to/hisFile       - Extract data from a single '.his'");
		System.out.println("# ibperfp parseHis <path> json            - Writes data in JSON format.");
		System.out.println("# ibperfp parseHis <path> del             - Writes data in delimited format.");
	}
	
	private static void processOMSHistory(String[] args){
		
		File path;
		File[] hisFiles = null;
		outFormat outType = null;
		
		if (args.length > 1){
			path = new File(args[1]);
			
			if(path.exists()){
				if(path.isDirectory()){
					hisDir = path;
				} 
				else if(path.isFile()){
					hisFiles = new File[1];
					hisFiles[0] = path;
				}
				else{
					System.err.println("ERROR: Cannot find path to history file(s) at: " + args[1]);
					showUsage();
					System.exit(1);
				}
			}
			outType = outFormat.json;
			
		} else{
			System.err.println("ERROR: No path argument given.");
			showUsage();
			System.exit(1);
		}
		
		if (args.length > 2){
			if (args[2].equals("del")){
				outType = outFormat.delimited;
			} else if(! args[2].equals("json")){
				System.err.println("ERROR: Invalid file format '" + args[2] + "' given. Using JSON.");
			}
		}
		
		OMS_Collection omsHistory = null;
		OpenSmMonitorService oms = null;
		OSM_Fabric fabric = null;
		
		long currentTime = System.currentTimeMillis() / 1000;
		long timestamp = 0;
		
		int i;
		
		try{
			if (outType == outFormat.delimited){
				File counterFile = new File(outDir, "/counters." + currentTime + ".txt");
				File routesFile = new File(outDir, "/routes." + currentTime + ".txt");
				File linksFile = new File(outDir, "/links." + currentTime + ".txt");
				if (!counterFile.exists()) { counterFile.createNewFile(); }
				if (!routesFile.exists()) { routesFile.createNewFile(); }
				if (!linksFile.exists()) { linksFile.createNewFile(); }
				
				countersBW = new BufferedWriter(new FileWriter(counterFile));
				routesBW = new BufferedWriter(new FileWriter(routesFile));
				linksBW = new BufferedWriter(new FileWriter(linksFile));
			} else{
				File hbaseDumpFile = new File(outDir, "/hbaseDump." + currentTime + ".txt");
				if (!hbaseDumpFile.exists()) { hbaseDumpFile.createNewFile(); }
				
				hbaseDumpBW = new BufferedWriter(new FileWriter(hbaseDumpFile));
			}
			
			
			
			if (hisFiles == null){
				hisFiles = hisDir.listFiles(fnameFilter);
			}
			for (File hisFile : hisFiles){
				System.out.println("Processing history file: " + hisFile.getPath());
			
				omsHistory = OMS_Collection.readOMS_Collection(hisFile.getPath());
				
				for (i = 0; i < omsHistory.getSize(); i++){
					oms = omsHistory.getOMS(i);
					fabric = oms.getFabric();
					
					timestamp = oms.getTimeStamp().getTimeInSeconds();
					System.out.println(".snapshot: " + oms.getTimeStamp().toString() + ".");
					
					if (outType == outFormat.json){
						writeLinksJSON(fabric, timestamp);
						writePortCountersJSON(fabric.getOSM_Ports(), timestamp);
						writePortForwardingTableJSON(RT_Table.buildRT_Table(fabric), timestamp);
					} else{
						writeLinksDelimited(fabric.getIB_Links(), timestamp);
						writePortCountersDelimited(fabric.getOSM_Ports(), timestamp);
						writePortForwardingTableDelimited(RT_Table.buildRT_Table(fabric), timestamp);
					}
					
				}
			}
			
			if (outType == outFormat.delimited){
				countersBW.flush();
				countersBW.close();
				
				routesBW.flush();
				routesBW.close();
				
				linksBW.flush();
				linksBW.close();
			}
			if (outType == outFormat.json){
				hbaseDumpBW.flush();
				hbaseDumpBW.close();
			}
		}
		catch (Exception e)
		{
			System.err.println("Couldn't open the file");
			e.printStackTrace();
		}
		System.out.println("- Complete");
	}
	
	private static void writePortCountersDelimited(LinkedHashMap<String, OSM_Port> ports, long timestamp){
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
				jsonPort.put("recordType", "counter");
				jsonPort.put("ts", timestamp);
				jsonPort.put("nguid", nguid);
				jsonPort.put("portNum", portNum);
				
				jsonPort.put("r_data", port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_data) * 4);
				jsonPort.put("r_err", port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_err));
				jsonPort.put("r_sr_err", port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_rem_phys_err));
				jsonPort.put("r_phys_err", port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_rem_phys_err));
				jsonPort.put("r_con_err", port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_constraint_err));
				
				jsonPort.put("xmit_data", port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_data) * 4);
				jsonPort.put("xmit_discards", port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_discards));
				jsonPort.put("xmit_wait", port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_wait));
				jsonPort.put("xmit_con_err", port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_constraint_err));
	
				jsonPort.put("mul_r_pkts", port.pfmPort.getCounter(PFM_Port.PortCounterName.multicast_rcv_pkts));
				jsonPort.put("mul_xmit_pkts", port.pfmPort.getCounter(PFM_Port.PortCounterName.multicast_xmit_pkts));
				jsonPort.put("uni_recv_pkts", port.pfmPort.getCounter(PFM_Port.PortCounterName.unicast_rcv_pkts));
				jsonPort.put("uni_xmit_pkts", port.pfmPort.getCounter(PFM_Port.PortCounterName.unicast_xmit_pkts));
				
				jsonPort.put("sym_err_cnt", port.pfmPort.getCounter(PFM_Port.PortCounterName.symbol_err_cnt));
				jsonPort.put("vl15_drop", port.pfmPort.getCounter(PFM_Port.PortCounterName.vl15_dropped));
				jsonPort.put("buff_overun", port.pfmPort.getCounter(PFM_Port.PortCounterName.buffer_overrun));
				jsonPort.put("l_down", port.pfmPort.getCounter(PFM_Port.PortCounterName.link_downed));
				jsonPort.put("l_err_recov", port.pfmPort.getCounter(PFM_Port.PortCounterName.link_err_recover));
				jsonPort.put("l_integrity", port.pfmPort.getCounter(PFM_Port.PortCounterName.link_integrity));
				
				hbaseDumpBW.write(jsonPort.toString());
				hbaseDumpBW.newLine();
				
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
	
	private static void writePortForwardingTableDelimited(RT_Table RoutingTable, long timestamp){
		
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
						jsonPort.put("recordType", "route");
						jsonPort.put("ts", timestamp);
						jsonPort.put("nguid", nguid);
						jsonPort.put("portNum", portNum);
						
						routes = new StringBuffer();
						for (Map.Entry<String,Integer> item: port.getLidGuidMap().entrySet()){
							routeLid = item.getValue();
							routes.append(routeLid + ":");
							
						}
						
						jsonPort.put("routes", routes.toString());
						hbaseDumpBW.write(jsonPort.toString()); hbaseDumpBW.newLine();
					}
				}
			}catch (Exception e){
				System.out.println("ERROR: Unable to write routes to file.");
			}
			System.out.println("- Wrote routes to file.");
		}
	
	private static void writeLinksDelimited(LinkedHashMap<String, IB_Link> ibLinks, long timestamp){
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
	
	private static void writeLinksJSON(OSM_Fabric fabric, long timestamp){
		OSM_Port port1, port2;
		String nguid1, nguid2;
		Integer portNum1, portNum2;
		String nodeType1, nodeType2, desc1, desc2, conn1, conn2;
		int lid1, lid2;
		JSONObject jsonPort1 = null, jsonPort2 = null;
		
		LinkedHashMap<String, IB_Link> ibLinks;
		
		for(Map.Entry<String, IB_Link> entry: fabric.getIB_Links().entrySet()){
	        IB_Link ln = entry.getValue();
	        
	        port1 = ln.getEndpoint1();
	        port2 = ln.getEndpoint2();
	        
	        nguid1 = port1.getNodeGuid().toColonString().replace(":", "");
	        nguid2 = port2.getNodeGuid().toColonString().replace(":", "");
	        
	        desc1 = fabric.getOSM_Node(port1.getNodeGuid()).sbnNode.description;
	        desc2 = fabric.getOSM_Node(port2.getNodeGuid()).sbnNode.description;
	        
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
	        	jsonPort1.put("recordType", "link");
				jsonPort1.put("ts", timestamp);
				jsonPort1.put("nguid", nguid1);
				jsonPort1.put("ndesc", desc1);
				jsonPort1.put("portNum", portNum1);
				jsonPort1.put("type", nodeType1);
				jsonPort1.put("lid", lid1);
				jsonPort1.put("conn", conn1);
				jsonPort1.put("speed", port1.getSpeedString());
				jsonPort1.put("state", port1.getStateString());
				jsonPort1.put("width", port1.getWidthString());
				jsonPort1.put("rate", port1.getRateString());
				jsonPort1.put("pguid", port1.sbnPort.port_guid);
				
				jsonPort2 = new JSONObject();
				jsonPort2.put("recordType", "link");
				jsonPort2.put("ts", timestamp);
				jsonPort2.put("nguid", nguid2);
				jsonPort2.put("ndesc", desc2);
				jsonPort2.put("portNum", portNum2);
				jsonPort2.put("type", nodeType2);
				jsonPort2.put("lid", lid2);
				jsonPort2.put("conn", conn2);
				jsonPort2.put("speed", port2.getSpeedString());
				jsonPort2.put("state", port2.getStateString());
				jsonPort2.put("width", port2.getWidthString());
				jsonPort2.put("rate", port2.getRateString());
				jsonPort2.put("pguid", port2.sbnPort.port_guid);
				
				hbaseDumpBW.write(jsonPort1.toString());  hbaseDumpBW.newLine();
				hbaseDumpBW.write(jsonPort2.toString());  hbaseDumpBW.newLine();
	        }catch (Exception e){
				System.out.println("ERROR: Unable to write links to file.");
			}
		}
		System.out.println("- Wrote links file.");

	}
}
