package controllers;

import java.util.ArrayList;
import java.util.Iterator;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import beans.AgentType;
import beans.Host;
import factories.AgentsFactory;
import interfaces.AgentInterface;
import requestSenders.RestHandshakeRequestSender;
import services.AgentsService;
import services.RestHandshakeService;
import sun.management.Agent;

@Path("/app")
public class RestController {

	@Inject
	private AgentsService agentsService;
	
	@Inject
	private RestHandshakeService restHandshakeService;
	
	@Inject
	private RestHandshakeRequestSender requestSender;
	
	@GET
	@Path("/node")
	@Produces(MediaType.TEXT_PLAIN)
	public String node() {
		
		return "ACTIVE";
	}
	
	@GET
	@Path("/agents/classes")
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<AgentType> getAllAgentTypes(){
		
		ArrayList<AgentType> retList = new ArrayList<AgentType>();
		
		for (Iterator<AgentType> i = agentsService.getAllSupportedAgentTypes().iterator(); i.hasNext();)
		    retList.add(i.next());
		
		return retList;
	}
	
	@GET
	@Path("/agents/running")
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<AgentInterface> getRunningAgents(){
		
		ArrayList<AgentInterface> retList = new ArrayList<AgentInterface>();
		
		for (Iterator<AgentInterface> i = agentsService.getRunningAgents().iterator(); i.hasNext();)
		    retList.add(i.next());
		
		return retList;
	}
	
	@PUT
	@Path("/agents/running/{type}/{name}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean putNewAgent(@PathParam(value = "type") String type, @PathParam(value = "name") String name){
		
		boolean retVal = true;
		
		Host myHostData = agentsService.getMainNode();
		for (Iterator<AgentType> i = agentsService.getAllSupportedAgentTypes().iterator(); i.hasNext();) {
			AgentType item = i.next();
			if(item.getName().equals(name)) {
				AgentInterface myAgent = AgentsFactory.createAgent(name, myHostData, item);
			}
		}
		
		return retVal;
	}
	
	@POST
	@Path("/agents/running")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<AgentInterface> postRunningAgents(String myHostAddress){
		
		ArrayList<AgentInterface> retList = new ArrayList<AgentInterface>();
		
		//add my running agents
		for (Iterator<AgentInterface> i = agentsService.getRunningAgents().iterator(); i.hasNext();)
		    retList.add(i.next());

		//only sending to other slaves (if I am the main node I will skip the slave who initiated the call)
		for (Iterator<Host> h = agentsService.getSlaveNodes().iterator(); h.hasNext();) {
			Host item = h.next();
			if(!item.getHostAddress().equals(myHostAddress)) {
				Response resp = requestSender.getRunningAgents(item.getHostAddress());
				ArrayList<AgentInterface> respAgents = resp.readEntity(new GenericType<ArrayList<AgentInterface>>() {});
				for(Iterator<AgentInterface> ra = respAgents.iterator(); ra.hasNext();)
					retList.add(ra.next());
			}
		}
		
		//i am a slave node, send also to the main node
		if(!agentsService.getMainNode().getHostAddress().equals("ME")) {
			Host main = agentsService.getMainNode();
			Response resp = requestSender.getRunningAgents(main.getHostAddress());
			ArrayList<AgentInterface> respAgents = resp.readEntity(new GenericType<ArrayList<AgentInterface>>() {});
			for(Iterator<AgentInterface> ra = respAgents.iterator(); ra.hasNext();)
				retList.add(ra.next());
		}
		
		return retList;
	}
	
	@SuppressWarnings("unlikely-arg-type")
	@DELETE
	@Path("/node/{alias}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteNode(@PathParam(value = "alias") String alias, ArrayList<AgentType> agentsToDelete){
		
		boolean retVal = true;
				
		boolean hasDeleted = agentsService.getSlaveNodes().removeIf(x -> x.getAlias().equals(alias));
		boolean hasRemoved = true;
		if(!agentsToDelete.isEmpty())
			hasRemoved = agentsService.getAllSupportedAgentTypes().remove(agentsToDelete);
			
		if(!hasDeleted || !hasRemoved)
			retVal = false;
			
		return retVal;
	}
}
