import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
		{
			omsHistory = OMS_Collection.readOMS_Collection("/Users/brown303/workspace/eclipse/OSM-HBASE/data/cab.20150720.his");
			OpenSmMonitorService oms = omsHistory.getNewestOMS();
			RT_Table RoutingTable = RT_Table.buildRT_Table(oms.getFabric());
			LinkedHashMap<String, IB_Link> ibLinks = oms.getFabric().getIB_Links();
			
			//RT_Table.writeRT_Table("/Users/brown303/workspace/eclipse/OSM-HBASE/data/route.table", RoutingTable);
			
			
			StringBuffer buff = new StringBuffer();
			buff.append("Links: \n");
			
			for(Map.Entry<String, IB_Link> entry: ibLinks.entrySet()){
		        String rn = entry.getKey();
		        IB_Link ln = entry.getValue();
		        
		        OSM_Port port1 = ln.getEndpoint1();
		        OSM_Port port2 = ln.getEndpoint2();
		        
		        IB_Guid nguid1 = port1.getNodeGuid();
		        IB_Guid nguid2 = port2.getNodeGuid();
		        
		        int pn1 =  port1.getPortNumber();
		        int pn2 =  port2.getPortNumber();
		        
		        String ntyp1 = port1.getNodeType().getAbrevName();
		        String ntyp2 = port2.getNodeType().getAbrevName();
		        
		        int lid1 = port1.getAddress().getLocalId();
		        int lid2 = port2.getAddress().getLocalId();
		        
		        buff.append(nguid1 + ":" + nguid2 + ":" + pn1  + ":" + pn2  + ":" + ntyp1 + ":" + ntyp2 + ":" + lid1 + ":" + lid2 + "\n");
			}
			File file = new File("/Users/brown303/workspace/eclipse/OSM-HBASE/data/links.table");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(buff.toString());
			bw.close();
			
		
		}
		catch (Exception e)
		{
			System.err.println("Couldn't open the file");
			e.printStackTrace();
		}
		System.out.println("Done");

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
