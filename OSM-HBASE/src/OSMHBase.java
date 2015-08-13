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

import gov.llnl.lc.infiniband.opensm.plugin.data.OSM_SysInfo;
import gov.llnl.lc.infiniband.opensm.plugin.net.OsmClientApi;
import gov.llnl.lc.infiniband.opensm.plugin.net.OsmServiceManager;
import gov.llnl.lc.infiniband.opensm.plugin.net.OsmSession;
import gov.llnl.lc.infiniband.core.IB_Guid;
import gov.llnl.lc.infiniband.core.IB_Link;
import gov.llnl.lc.infiniband.opensm.plugin.data.*;

public class OSMHBase {
	public static void main(String[] args) throws Exception
	{

		OMS_Collection omsHistory = null;
		try
		{	int i;
			long timestamp;
			omsHistory = OMS_Collection.readOMS_Collection("/Users/brown303/workspace/llnl/networks/oms/cab.20150730.his");
			OpenSmMonitorService oms = omsHistory.getNewestOMS();
			OSM_Fabric fabric = oms.getFabric();
			
			timestamp = oms.getTimeStamp().getTimeInSeconds();
			
			//writePortForwardingTable(RT_Table.buildRT_Table(fabric), timestamp);
			//writeLinks(fabric.getIB_Links(), timestamp);
			
			writePortCounters(fabric.getOSM_Ports(), timestamp);
			
			//for (i = 50; i < omsHistory.getSize(); i++){
				//oms = omsHistory.getOMS(i);
				//System.out.println(i + " - " + oms.getTimeStamp().toString() + " - " + oms.getTimeStamp().getTimeInSeconds());
			//}
			
		}
		catch (Exception e)
		{
			System.err.println("Couldn't open the file");
			e.printStackTrace();
		}
		System.out.println("- Complete");
	}
	
	private static void writePortCounters(LinkedHashMap<String, OSM_Port> ports, long timestamp){
		int i;
		
		OSM_Port port;
		String portId;
		String nguid;
		int portNum;
		long recvData;
		long xmitData;
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("# Record format: \"<timestamp>:<nguid>:<pn>:<recvData>:<xmitData>\"\n");
		buffer.append("----Port Counters BEGIN----\n");
		
		i = 0;
		for (Map.Entry<String, OSM_Port> entry: ports.entrySet()){
			portId = entry.getKey();
			port = entry.getValue();
			
			nguid = portId.substring(0, 19).replace(":", "");
			portNum = Integer.parseInt(portId.substring(20));
			
			recvData = port.pfmPort.getCounter(PFM_Port.PortCounterName.rcv_data);
			xmitData = port.pfmPort.getCounter(PFM_Port.PortCounterName.xmit_data);
			
			//buffer.append(timestamp + ":" + nguid + ":" + portNum + ":" + recvData + ":" + xmitData + "\n"); 
			
			if (i == 10){
				//System.out.println(buffer.toString());
			}
			i++;
		}
				
		//System.out.println("Done: " + i);
	}
	
	private static void writePortForwardingTable(RT_Table RoutingTable, long timestamp){
		
		RT_Node node;
		String nguid;
		RT_Port port;
		int portNum;
		int routeLid;
		StringBuffer buffer = new StringBuffer();
	
		buffer.append("# Record format: \"<ExitPort>:<LID>\"\n");
		buffer.append("----Forwarding Table BEGIN----\n");
		
		for (Map.Entry<String, RT_Node> nEntry: RoutingTable.getSwitchGuidMap().entrySet()){
			node  = nEntry.getValue();
			nguid = node.getGuid().toColonString().replace(":", "");
			
			buffer.append("\nSwitch: 0x" + nguid + "\n");
			
			for (Map.Entry<String,RT_Port> pEntry: node.getPortRouteMap().entrySet()){
				port = pEntry.getValue();
				portNum = port.getPortNumber();
				
				for (Map.Entry<String,Integer> item: port.getLidGuidMap().entrySet()){
					routeLid = item.getValue();
					buffer.append(portNum + ":" + routeLid + "\n");
				}
			}
		}
		
		buffer.append("\n----END----");
		
		try{
			File file = new File("/Users/brown303/workspace/eclipse/OSM-HBASE/data/routes-table." + timestamp + ".txt");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(buffer.toString());
			bw.close();
			System.out.println("- Wrote routes file.");
			
		}catch (Exception e){
			System.out.println("ERROR: Unable to write routes to file.");
		}
	}
	
	private static void writeLinks(LinkedHashMap<String, IB_Link> ibLinks, long timestamp){
		OSM_Port port1, port2;
		IB_Guid nguid1, nguid2;
		Integer portNum1, portNum2;
		String nodeType1, nodeType2;
		Integer lid1, lid2;
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("# Record format: \"nguid1:nguid2:pn1:pn2:ntype1:ntype2:lid1:lid2\"\n");
		buffer.append("----BEGIN----\n\n");
		
		for(Map.Entry<String, IB_Link> entry: ibLinks.entrySet()){
	        IB_Link ln = entry.getValue();
	        
	        port1 = ln.getEndpoint1();
	        port2 = ln.getEndpoint2();
	        
	        nguid1 = port1.getNodeGuid();
	        nguid2 = port2.getNodeGuid();
	        
	        portNum1 =  port1.getPortNumber();
	        portNum2 =  port2.getPortNumber();
	        
	        nodeType1 = port1.getNodeType().getAbrevName();
	        nodeType2 = port2.getNodeType().getAbrevName();
	        
	        lid1 = port1.getAddress().getLocalId();
	        lid2 = port2.getAddress().getLocalId();
	        
	        buffer.append(nguid1 + ":" + nguid2 + ":" + portNum1  + ":" + portNum2  + ":" + nodeType1 + ":" + nodeType2 + ":" + lid1 + ":" + lid2 + "\n");
		}

		buffer.append("\n----END----");
		
		try{
			File file = new File("/Users/brown303/workspace/eclipse/OSM-HBASE/data/links-table." + timestamp + ".txt");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(buffer.toString());
			bw.close();
			System.out.println("- Wrote links file.");
			
		}catch (Exception e){
			System.out.println("ERROR: Unable to write links to file.");
		}
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
