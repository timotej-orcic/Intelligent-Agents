package registrators;

import java.util.List;

import javax.ejb.Singleton;

import beans.Host;
import beans.enums.NodeType;

@Singleton
public class NodeRegistrator {

	private NodeType nodeType;
	private List<Host> slaves;
	private Host master;
	private Host thisNodeInfo;
	
	public boolean setSlavesSentFromMaster(List<Host> slavesList) {
		Host thisNode = thisNodeInfo;
		boolean success = true;
		
		try {
			if(nodeType.equals(NodeType.SLAVE)) {
				if(slavesList.indexOf(thisNode) > -1) {
					slavesList.remove(thisNode);
				}
				
				slaves = slavesList;
			} else {
				success = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		}
		
		return success;
	}
	
	public NodeType getNodeType() {
		return nodeType;
	}
	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}
	public List<Host> getSlaves() {
		return slaves;
	}
	public void setSlaves(List<Host> slaves) {
		this.slaves = slaves;
	}
	public Host getMaster() {
		return master;
	}
	public void setMaster(Host master) {
		this.master = master;
	}
	public Host getThisNodeInfo() {
		return thisNodeInfo;
	}
	public void setThisNodeInfo(Host thisNodeInfo) {
		this.thisNodeInfo = thisNodeInfo;
	}
}