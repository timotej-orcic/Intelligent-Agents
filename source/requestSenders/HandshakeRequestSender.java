package requestSenders;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import beans.AID;
import beans.AgentType;
import beans.AgentTypeDTO;
import beans.Host;

@Singleton
public class HandshakeRequestSender {
	private Client restClient;
	private WebTarget webTarget;
	private static String HTTP_URL = "http://";
	private static String NODE_URL = "/Inteligent_Agents/rest/handshake";
	
	@PostConstruct
	public void init() {
		restClient = ClientBuilder.newClient();
	}
	
	public Response getRunningAgents(String hostAddress) {
		webTarget = restClient.target(HTTP_URL + hostAddress + NODE_URL + "/app/agents/running");
		return webTarget.request(MediaType.APPLICATION_JSON).get();
	}
	
	public Response deleteAgents(Host host, List<AgentType> agentsToDelete) {
		webTarget = restClient.target(HTTP_URL + host.getHostAddress() + NODE_URL + "/app/node/{alias}");
		return webTarget.resolveTemplate("alias", host.getAlias()).request(MediaType.APPLICATION_JSON).post(Entity.entity(agentsToDelete, MediaType.APPLICATION_JSON));
	}
	
	public List<Host> registerSlaveNode(String url, Host newSlave) {
		List<Host> slaves = null;
		
		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/node");
		Response regResp = webTarget.request(MediaType.APPLICATION_JSON)
										.post(Entity.entity(newSlave, MediaType.APPLICATION_JSON));
		
		try {
			slaves = regResp.readEntity(new GenericType<List<Host>>(){});	
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Error fetching response body for list of slave nodes.");
			slaves = null;
		}
		
		return slaves;
	}
	
	public List<AgentTypeDTO> fetchAgentTypeList(String url) {
		List<AgentTypeDTO> retList = null;
		
		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/agents/classes");
		Response resp = webTarget.request().get();
		
		try {
			retList = resp.readEntity(new GenericType<List<AgentTypeDTO>>() {});
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Error fetching response body for agent type list");
			retList = null;
		}
		
		return retList;
	}
	
	public boolean sendNewAgentTypesToSlave(String url, List<AgentType> agents) {
		boolean success = true;

		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/agents/classes");
		Response resp = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(agents, MediaType.APPLICATION_JSON));
		
		try {
			success = resp.readEntity(boolean.class);
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Error posting agent types list to slave: " + url);
			success = false;
		}
		
		return success;
	}
	
	public boolean sendExistingSlavesToNewSlave(String url, List<Host> slaves) {
		boolean success = true;

		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/node");
		Response resp = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.entity(slaves, MediaType.APPLICATION_JSON));
		
		try {
			success = resp.readEntity(boolean.class);
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Error posting slave list to slave: " + url);
			success = false;
		}
		
		return success;
	}
	
	public boolean sendAllRunningAgentsToNewSlave(String url, List<AID> runningAgents) {
		boolean success = true;

		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/agents/running");
		Response resp = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(runningAgents, MediaType.APPLICATION_JSON));
		
		try {
			success = resp.readEntity(boolean.class);
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Error posting running agents list to slave: " + url);
			success = false;
		}
		
		return success;
	}

	public boolean sendAgentTypesToNewSlave(String url, List<AgentTypeDTO> allSupportedAgentTypes) {
		boolean success = true;

		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/agents/classes");
		Response resp = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(allSupportedAgentTypes, MediaType.APPLICATION_JSON));
		
		try {
			success = resp.readEntity(boolean.class);
		} catch(Exception e) {
			e.printStackTrace();
			success = false;
		}
		
		return success;
	}
	
	/***
	 * Brise cvor koristeci njegov alias ili pun url.
	 * @param url URL cvora kojem saljemo zahtev
	 * @param nodeAlias Cvor koji treba obrisati
	 * @param agentsToDelete Tipovi agenata cvora kog treba obrisati.
	 * @return Uspeh operacije
	 */
	public boolean deleteBadNode(String url, String nodeAlias) {
		boolean success = true;
		
		webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/node/" + nodeAlias);
		Response resp = webTarget.request(MediaType.APPLICATION_JSON).delete();
		
		try {
			success = resp.readEntity(boolean.class);
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Error deleting node.");
			success = false;
		}
		
		return success;
	}
	
	/***
	 * Metoda koja se koristi za Heartbeat protokol. Pinguje cvor na url-u.
	 * @param url URL cvora kog treba pingovati
	 * @return Status zivota cvora (true-ziv, false-nije ziv)
	 */
	public boolean isAlive(String url) {
		boolean success = true;

		try {
			webTarget = restClient.target(HTTP_URL + url + NODE_URL + "/node");
			Response resp = webTarget.request(MediaType.APPLICATION_JSON).get();
			
			try {
				success = resp.readEntity(boolean.class);
			} catch(Exception e) {
				e.printStackTrace();
				success = false;
			}	
		} catch(Exception e) {
			success = false;
		}
		
		return success;
	}
}
