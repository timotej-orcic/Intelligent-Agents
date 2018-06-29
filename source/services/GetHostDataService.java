package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.ws.rs.core.Response;

import org.jboss.as.cli.CommandLineException;
import org.json.simple.parser.ParseException;

import beans.Host;
import controllers.RestController;

//@Stateless
public class GetHostDataService implements Runnable {

	private RestHandshakeService restHandshakeService;
	
	private AgentsService agentsService;
	
	private Host host;
	
	private String mainNodeDetails;
	
	private String ip;
	
	private String hostname;
	
	public GetHostDataService (String ip, String hostname) {
		this.hostname = hostname;
		this.ip = ip;
		this.mainNodeDetails = "";
		this.host = null;
		this.restHandshakeService = new RestHandshakeService();
		this.agentsService = new AgentsService();
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			this.host = getHostData();
		} catch (InstanceNotFoundException e) {
			e.printStackTrace();
		} catch (AttributeNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (ReflectionException e) {
			e.printStackTrace();
		} catch (MBeanException e) {
			e.printStackTrace();
		} catch (CommandLineException e) {
			e.printStackTrace();
		}
		this.mainNodeDetails = getMainNodeDetails();
		Host mainNode = new Host(this.mainNodeDetails, "mainNode");
		this.agentsService.setMainNode(mainNode);
		
		//i am a slave node, initialize handshake
		if(!this.mainNodeDetails.equals(this.host.getHostAddress())) {
			//add me to the slaves list
			this.agentsService.getSlaveNodes().add(this.host);
			
			//start REST handshake
			
		}
	}
	
	public Host getHostData() throws CommandLineException, InstanceNotFoundException, AttributeNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException {
		Host ret = new Host();
		String port;
		String host;
		String portOffset;
		
		port =  ManagementFactory.getPlatformMBeanServer()
				   .getAttribute(new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=http"), "port")
				   .toString();
		host = ManagementFactory.getPlatformMBeanServer()
								.getAttribute(new ObjectName("jboss.as:interface=public"), "inet-address")
								.toString();
		
		portOffset = ManagementFactory.getPlatformMBeanServer()
						.getAttribute(new ObjectName("jboss.as:socket-binding-group=standard-sockets"), "port-offset")
						.toString();	
		
		int portValue = Integer.parseInt(port) + Integer.parseInt(portOffset);
		
		String address = this.ip.toString().split("/")[1] + ":" + portValue;
		String alias = host + "/" + this.hostname;
		ret.setAlias(alias);
		ret.setHostAddress(address);
		
		return ret;
    }
	
	@SuppressWarnings("resource")
	public String getMainNodeDetails() {
		String absolutePath = GetHostDataService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	    String newPath = absolutePath.substring(1);
	    String fullResourcePath = newPath + "/../mainNodeData/mainNodeInfo.txt";
		File file = new File(fullResourcePath);
		
		String line = null;
		try {
			FileReader nodesData = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(nodesData);
			line = bufferedReader.readLine();

	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
		
		if(line == null)
			return "ERROR";
		else
			return line;
	}
}
