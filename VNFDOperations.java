/*
 * Copyright (c) 2015 Huawei, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.nemo.user.processingmanager;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.common.rev151010.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.CreateVnfdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.Objects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.Operations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.TemplateDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.TemplateInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.objects.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.template.definitions.TemplateDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.template.definitions.TemplateDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.template.instances.TemplateInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.intent.rev151010.user.intent.template.instances.TemplateInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nemo.object.rev151010.connection.instance.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VNFDOperations {

	private Map<String, Map<String, String>> instanceNodeMap; // <nombreInstancia, <nombreNodo, tipoNodo>>
	private Map<String, String> nodeVnfUriMap; // <NombreNodo, vnfUri>
	private Map<String,LinkedList<String>> connectionConnPointMap; // <nombreConnection , List<nombreConnPoints>>
	private Map<String, List<String>> nodeVnfdInterfacesMap; // <nombreNode, List<vnfInterfaceValue>>
	private Map<String, String> connPointVnfdInterfaceMap;
	private List<String> instanceConnectionPointsMap;
	private Map<String, Map<String, String>> nodeConnectionPointsMap;


	public VNFDOperations() {
		instanceNodeMap = new HashMap<String, Map<String, String>>();
		nodeVnfUriMap = new HashMap<String, String>();
		connectionConnPointMap = new HashMap<String, LinkedList<String>>();
		nodeVnfdInterfacesMap = new HashMap<String, List<String>>();
		connPointVnfdInterfaceMap = new HashMap<String, String>();
		instanceConnectionPointsMap = new ArrayList<String>();
		nodeConnectionPointsMap = new HashMap<String, Map<String, String>>();
	}


	public void setInstanceNodes(TemplateInstanceName templateInstanceName, Map<NodeId, Node> nodeMap,
			Map<NodeId, Node> nodeDSMap) {
		Map<String, Map<String, String>> templateNodeMap = new HashMap<String, Map<String, String>>();
		Map<String, String> nodeTypeMap = new HashMap<String, String>();
		String[] nodeNames = new String[2];
		if (nodeMap.values() != null) {
			for (Node node : nodeMap.values()) {
				nodeNames = node.getNodeName().getValue().split("\\.");
				if (nodeNames[0].equals(templateInstanceName.getValue())) {
					nodeTypeMap.put(nodeNames[1], node.getNodeType().getValue());
				}

			}
		}
		if (nodeDSMap.values() != null) {
			for (Node node : nodeDSMap.values()) {
				nodeNames = node.getNodeName().getValue().split("\\.");
				if (nodeNames[0].equals(templateInstanceName.getValue()) && !nodeTypeMap.containsValue(nodeNames[1])) {
					nodeTypeMap.put(nodeNames[1], node.getNodeType().getValue());
				}
			}

		}

		templateNodeMap.put(templateInstanceName.getValue(), nodeTypeMap);

		for (String node1 : nodeTypeMap.keySet()) {
			Map<String, String> auxMap = new HashMap<String, String>();
			for (Node n : nodeMap.values()) {
				nodeNames = n.getNodeName().getValue().split("\\.");
				if (nodeNames[0].equals(node1)) {
					auxMap.put(nodeNames[1], n.getNodeType().getValue());
				}
			}
			if (nodeDSMap.values() != null) {
				for (Node node : nodeDSMap.values()) {
					nodeNames = node.getNodeName().getValue().split("\\.");
					if (nodeNames[0].equals(node1) && !auxMap.containsValue(nodeNames[1])) {
						auxMap.put(nodeNames[1], node.getNodeType().getValue());
					}
				}

			}
			if (!auxMap.isEmpty()) {
				templateNodeMap.put(node1, auxMap);
			}
		}
		System.out.println(templateNodeMap);
		templateNodeMap = replaceListValues(templateNodeMap, templateInstanceName.getValue());
		instanceNodeMap = templateNodeMap;
		System.out.println("instanceNodeMap "+instanceNodeMap);

	}

	public Map<String, Map<String, String>> getInstanceNodes() {
		if (!instanceNodeMap.isEmpty()) {
			return instanceNodeMap;
		} else {
			return null;
		}
	}

	private Map<String, Map<String, String>> replaceListValues(Map<String, Map<String, String>> templateNodeMap,
			String templateInstanceName) {
		Map<String, String> instaceNodeTypeMap = new HashMap<String, String>();

		for (Map<String, String> aux : templateNodeMap.values()) {
			instaceNodeTypeMap.putAll(aux);
		}

		for (String instance : templateNodeMap.keySet()) {
			instaceNodeTypeMap.remove(instance);
		}
		templateNodeMap.put(templateInstanceName, instaceNodeTypeMap);
		return templateNodeMap;

	}

	
	public void setNodeVnfUri(Map<TemplateName, TemplateDefinition> templateDefinitionMap,
			Map<TemplateName, TemplateDefinition> templateDefinitionDSMap,
			Map<String, Map<String, String>> instanceNodeMap, String instanceName) {
		//The map with the name of the instance has all the VNFs names
		for (String nodeName : instanceNodeMap.get(instanceName).keySet()) {
				if (checkNodeVnfUri(templateDefinitionMap, nodeName, instanceNodeMap.get(instanceName).get(nodeName)) != null) {
					nodeVnfUriMap.putAll(checkNodeVnfUri(templateDefinitionMap, nodeName, instanceNodeMap.get(instanceName).get(nodeName)));
				} else if (checkNodeVnfUri(templateDefinitionDSMap, nodeName, instanceNodeMap.get(instanceName).get(nodeName)) != null) {
					nodeVnfUriMap.putAll(checkNodeVnfUri(templateDefinitionDSMap, nodeName, instanceNodeMap.get(instanceName).get(nodeName)));
				}

		}
		System.out.println("nodeVnfUriMAp "+nodeVnfUriMap);
	}

	public Map<String, String> checkNodeVnfUri(Map<TemplateName, TemplateDefinition> templateDefinitionMap,
			String nodeName, String nodeType) {
		Map<String, String> nodeVnfUriMap = new HashMap<String, String>();
		if (templateDefinitionMap.containsKey(new TemplateName(nodeType))) {
			String vnfuri = null;
			vnfuri = templateDefinitionMap.get(new TemplateName(nodeType)).getVnfUriValue();
			if (vnfuri != null) {
				nodeVnfUriMap.put(nodeName, vnfuri);
			}
		}
		return nodeVnfUriMap.isEmpty() ? null : nodeVnfUriMap;

	}

	public Map<String, String> getNodeVnfUriMap() {
		if (!nodeVnfUriMap.isEmpty()) {
			return nodeVnfUriMap;
		} else {
			return null;
		}
	}
	
	public void setConnectionConnPointsName(Map<ConnectionId, Connection> connectionMap,
			Map<ConnectionId, Connection> connectionDSMap, Map<ConnectionPointId, ConnectionPoint> connectionPointMap,
			Map<ConnectionPointId, ConnectionPoint> connectionPointDSMap) {

		Map<String, LinkedList<String>> connectionsMap = new HashMap<String, LinkedList<String>>();

		List<EndNode> endNodes = new ArrayList<EndNode>();
		for (Connection c : connectionMap.values()) {
			String a = null;
			a = c.getConnectionName().getValue().split("\\.")[0];

			if (instanceNodeMap.containsKey(a)) {
				LinkedList<String> connPointList = new LinkedList<String>();
				endNodes = c.getEndNode();
				if (!endNodes.isEmpty()) {
					for (EndNode endNode : endNodes) {
						if (connectionPointMap.containsKey(new ConnectionPointId(endNode.getNodeId().getValue()))) {
							connPointList.add(connectionPointMap.get((new ConnectionPointId(endNode.getNodeId().getValue()))).getConnectionPointName().getValue());
						} else if (connectionPointDSMap.containsKey(new ConnectionPointId(endNode.getNodeId().getValue()))) {
							connPointList.add(connectionPointDSMap.get((new ConnectionPointId(endNode.getNodeId().getValue()))).getConnectionPointName().getValue());
						}
					}
				}
				connectionsMap.put(c.getConnectionName().getValue(), connPointList);
			}

			//System.out.println(connectionsMap);
		}

		for (Connection c1 : connectionDSMap.values()) {
			String a = null;
			a = c1.getConnectionName().getValue().split("\\.")[0];

			if (instanceNodeMap.containsKey(a) && !connectionsMap.containsKey(c1.getConnectionName().getValue())) {
				LinkedList<String> connPointList = new LinkedList<String>();
				endNodes = c1.getEndNode();
				if (!endNodes.isEmpty()) {
					for (EndNode endNode : endNodes) {
						if (connectionPointMap.containsKey(new ConnectionPointId(endNode.getNodeId().getValue()))) {
							connPointList.add(connectionPointMap.get((new ConnectionPointId(endNode.getNodeId().getValue()))).getConnectionPointName().getValue());
						} else if (connectionPointDSMap.containsKey(new ConnectionPointId(endNode.getNodeId().getValue()))) {
							connPointList.add(connectionPointDSMap.get((new ConnectionPointId(endNode.getNodeId().getValue()))).getConnectionPointName().getValue());
						}
					}
				}
				connectionsMap.put(c1.getConnectionName().getValue(), connPointList);
			}
			
			
		}
		connectionConnPointMap= connectionsMap;
		System.out.println("ConnectionsMaps "+connectionConnPointMap);
	}
	
	public Map<String, LinkedList<String>> getConnectionConnPointsName(){
		if (!connectionConnPointMap.isEmpty()) {
			return connectionConnPointMap;
		} else {
			return null;
		}
	}
	
	public void setConnPointVnfd(Map<ConnectionPointId, ConnectionPoint> connectionPointMap,
			Map<ConnectionPointId, ConnectionPoint> connectionPointDSMap){
		
		Map<String, String> connPointVnfdInterface = new HashMap<String, String>();
		for(ConnectionPoint c: connectionPointMap.values()){
			if ( c.getVnfdInterfaceName() != null){
				connPointVnfdInterface.put(c.getConnectionPointName().getValue(), c.getVnfdInterfaceName().getValue());
			}
		}
		
		for(ConnectionPoint c1:connectionPointDSMap.values()){
			if(!connPointVnfdInterface.containsKey(c1.getConnectionPointName().getValue() ) && c1.getVnfdInterfaceName() != null){
				connPointVnfdInterface.put(c1.getConnectionPointName().getValue(), c1.getVnfdInterfaceName().getValue());
			}
		}
		
		connPointVnfdInterfaceMap= connPointVnfdInterface;
		System.out.println("connPointVnfdInterfaceMap "+connPointVnfdInterfaceMap);
	}
	
	public Map<String, String> getConnPointVnfdInterface(){
		if (!connPointVnfdInterfaceMap.isEmpty()) {
			return connPointVnfdInterfaceMap;
		} else {
			return null;
		}
	}
	
	public void setNodeVnfdInterfaces( Map<ConnectionPointId, ConnectionPoint> connectionPointMap,
			Map<ConnectionPointId, ConnectionPoint> connectionPointDSMap, TemplateInstanceName instance){
		
		Map<String, List<String>> nodeVnfdInterfaceAuxMap = new HashMap<String, List<String>>();
		
		nodeVnfdInterfaceAuxMap.putAll( getVnfdInterfaces(instanceNodeMap.keySet(),connectionPointMap, connectionPointDSMap) );//puts the interfaces that the nodeModels have 
		nodeVnfdInterfaceAuxMap.putAll( getVnfdInterfaces(instanceNodeMap.get(instance.getValue()).keySet(),connectionPointMap, connectionPointDSMap) ); //puts the interfaces that the instance's nodes have
		
		nodeVnfdInterfacesMap.putAll(nodeVnfdInterfaceAuxMap);
		System.out.println("nodeVnfdInterfacesMap"+nodeVnfdInterfacesMap);
	}
	
	
	
	private Map<String, List<String>> getVnfdInterfaces(Set<String> nodes, Map<ConnectionPointId, ConnectionPoint> connectionPointMap,
			Map<ConnectionPointId, ConnectionPoint> connectionPointDSMap ){
		
		Map<String, List<String>> nodeVnfdInterfaceAuxMap = new HashMap<String, List<String>>();
		
		for(String nodeName:nodes ){
			List<String> connPointsList = new ArrayList<String>();
			if (connectionPointMap != null) {
				for (ConnectionPoint connPoint : connectionPointMap.values()) {
					//System.out.println(nodeName+" "+connPoint.getConnectionPointName()+ " " +connPoint.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName));
					if (connPoint.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName)) {
						if(connPoint.getVnfdInterfaceName() != null){
							connPointsList.add(connPoint.getVnfdInterfaceName().getValue());
						}
						//System.out.println(connPointsList);
					}
				}
			}
			
			if (connectionPointDSMap != null){
				for (ConnectionPoint connPointDS: connectionPointDSMap.values()) {
					//System.out.println(nodeName+" "+connPointDS.getConnectionPointName()+ "" +connPointDS.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName));
					if(connPointDS.getVnfdInterfaceName() != null){
						if (connPointDS.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName) && !connPointsList.contains(connPointDS.getVnfdInterfaceName().getValue())) {
							connPointsList.add(connPointDS.getVnfdInterfaceName().getValue());
						//System.out.println(connPointsList);
						}
					}
				}
			}
			
			if(!connPointsList.isEmpty()){
				nodeVnfdInterfaceAuxMap.put(nodeName, connPointsList);
			}
		}
		
		return nodeVnfdInterfaceAuxMap;
	}
	
	public Map<String, List<String>> getNodeVnfdInterfaces(){
		if (!nodeVnfdInterfacesMap.isEmpty()) {
			return nodeVnfdInterfacesMap;
		} else {
			return null;
		}
	}
	
	public void clear_vnfdOperations(){
		instanceNodeMap.clear();
		nodeVnfUriMap.clear();
		connectionConnPointMap.clear();
		nodeVnfdInterfacesMap.clear();
		connPointVnfdInterfaceMap.clear();
		instanceConnectionPointsMap.clear();
		nodeConnectionPointsMap.clear();
		
	}
	
	
	public void setNodeConnPoints( Map<ConnectionPointId, ConnectionPoint> connectionPointMap,
			Map<ConnectionPointId, ConnectionPoint> connectionPointDSMap, TemplateInstanceName instance){
		
		 Map<String, Map<String, String>> nodeConnPointAuxMap = new HashMap<String, Map<String, String>>();
		
		nodeConnPointAuxMap.putAll(getConnPoints(connectionPointMap, connectionPointDSMap, instance.getValue(), instanceNodeMap.keySet()) );//puts the interfaces that the nodeModels have 
		nodeConnPointAuxMap.putAll(getConnPoints(connectionPointMap, connectionPointDSMap,  instance.getValue(), instanceNodeMap.get(instance.getValue()).keySet()) ); //puts the interfaces that the instance's nodes have
		
		 nodeConnectionPointsMap.putAll(nodeConnPointAuxMap);
		System.out.println("[VNFDOperations] nodeConnectionPointsMap: "+ nodeConnectionPointsMap);
	}
	
	public Map<String, Map<String, String>> getConnPoints(Map<ConnectionPointId, ConnectionPoint> connectionPointMap, Map<ConnectionPointId, ConnectionPoint> connectionPointDSMap, String instanceName, Set<String> nodes) {
		Map<String,Map<String,String>> nodeConnPointAuxMap = new HashMap<String, Map<String,String>>();
		
		
		for (String nodeName : nodes) {
			List<String> instanceConnPointsList = new ArrayList<String>();
			Map<String,String> connPointsMap = new HashMap<String,String>();
			if (connectionPointMap != null) {
				for (ConnectionPoint connPoint : connectionPointMap.values()) {
					//System.out.println(nodeName+" "+connPoint.getConnectionPointName().getValue()+ " " +connPoint.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName));
					if (connPoint.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName)) {
						
						if (nodeName.equals(instanceName)) {
							instanceConnPointsList.add(connPoint.getConnectionPointName().getValue());
						}
						if(connPoint.getVnfdInterfaceName() != null){
							connPointsMap.put(connPoint.getConnectionPointName().getValue().split("\\.")[1], connPoint.getVnfdInterfaceName().getValue());
						}
						//System.out.println(connPointsMap);
					}
				}
			}

			if (connectionPointDSMap != null) {
				for (ConnectionPoint connPointDS : connectionPointDSMap.values()) {
					// System.out.println(nodeName+" "+connPointDS.getConnectionPointName().getValue()+ " " +connPointDS.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName));

					if (connPointDS.getConnectionPointName().getValue().split("\\.")[0].equals(nodeName)
							&& !connPointsMap.containsKey(connPointDS.getConnectionPointName().getValue().split("\\.")[1])) {
						
						if (nodeName.equals(instanceName)) {
							instanceConnPointsList.add(connPointDS.getConnectionPointName().getValue());
						}
						if(connPointDS.getVnfdInterfaceName() != null){
							connPointsMap.put(connPointDS.getConnectionPointName().getValue().split("\\.")[1], connPointDS.getVnfdInterfaceName().getValue());
						}
						 //System.out.println(connPointsMap);
					}

				}
			}
		

		if (!instanceConnPointsList.isEmpty()) {
			instanceConnectionPointsMap.addAll(instanceConnPointsList);
			System.out.println("[VNFDOperations] instanceConnectionPointsMap: "+ instanceConnectionPointsMap);
		}
		
		if(!connPointsMap.isEmpty()){
			nodeConnPointAuxMap.put(nodeName, connPointsMap);
		}
		}
		return nodeConnPointAuxMap;
	}
	
	public  List<String> getInstanceConnPoints(){
		if (!instanceConnectionPointsMap.isEmpty()) {
			return instanceConnectionPointsMap;
		} else {
			return null;
		}
	}
	
	public  Map<String, Map<String, String>> getNodeConnPoints(){
		if (!nodeConnectionPointsMap.isEmpty()) {
			return nodeConnectionPointsMap;
		} else {
			return null;
		}
	}
		
	
}
