
import java.io.IOException;
import java.io.InputStreamReader;

import gov.llnl.lc.infiniband.opensm.plugin.data.PFM_Port;

import java.io.BufferedReader;
import java.io.FileInputStream;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.util.Bytes;
 
import org.apache.hadoop.conf.Configuration;

public class hbaseLoader {
 
   public static void main(String[] args) throws IOException {
	   FileInputStream fstream = new FileInputStream("/Users/brown303/workspace/eclipse/hbaseLoader/data/sample.txt");
	   BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
	   String line;
	   int i = 0;
	   
	   JSONObject jsonPort;
	   
	   try{
		    while ((line = br.readLine()) != null) {
		    	i++;
		    	jsonPort = (JSONObject) JSONValue.parse(line);
		    	
		    	//if(i < 5 || i == 100000 || i == 200000){
		    		System.out.println(jsonPort.get("nguid"));
		    	//}
		    	//if (i > 200000){
		    	//	break;
		    	//}
		    }
		}catch(Exception e){
			System.err.println("Couldn't open the file");
			e.printStackTrace();
		}
 /*
      // Create the configuration for the HBaseAdmin
      Configuration con = HBaseConfiguration.create();
 
      // Create the HBaseAdmin
      HBaseAdmin admin = new HBaseAdmin(con);
 
      // Create the table descriptor
      HTableDescriptor tableDescriptor = new
          HTableDescriptor(TableName.valueOf("mytesttable"));
 
      // Create column families in the table descriptor
      tableDescriptor.addFamily(new HColumnDescriptor("mycolfamily1"));
      tableDescriptor.addFamily(new HColumnDescriptor("mycolfamily2"));
 
      // Tell the admin to create the table we described
      admin.createTable(tableDescriptor);
 
      System.out.println("Table created!");
 
      // Open the table we just created
      HTable ht = new HTable(con, "mytesttable");
 
      // Create a new row
      Put newRow = new Put(Bytes.toBytes("myrowkey"));
 
      // Populate columns of the new row
      newRow.add(Bytes.toBytes("mycolfamily1"),
                 Bytes.toBytes("mycol1"),
                 Bytes.toBytes("myvalue1"));
 
      // Add the new row to our table
      ht.put(newRow);
 
      System.out.println("Row added!");
 
      // Create a get class to retrieve our new row
      Get getRow = new Get(Bytes.toBytes("myrowkey"));
 
      // Get it
      Result res = ht.get(getRow);
 
      // Read the result
      byte [] val = res.getValue(Bytes.toBytes("mycolfamily1"),
                                 Bytes.toBytes("mycol1"));
 
 
      System.out.println("Result : " + Bytes.toString(val));
 
      // Disable the table
      admin.disableTable("mytesttable");
 
      // Delete the table
      admin.deleteTable("mytesttable");
 
      System.out.println("Table deleted!");
      */
   }  
   
   private static void putCounters(JSONObject jsonPort){
	   
	   String rowkey = jsonPort.get("nguid") + ":" + jsonPort.get("recordType")  + ":" + jsonPort.get("portNum")  + ":" + jsonPort.get("ts");
	   
	   Put newRow = new Put(Bytes.toBytes(rowkey));
	   
	   
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
		
		
   }
}