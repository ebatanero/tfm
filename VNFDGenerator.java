package example.yang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class VNFDGeneratorOSM {
	static final List<String> PARAMS = Arrays.asList("connection-point", "internal-vld", "vdu");

	// Auxiliary variables
	private Map<String, List<Map<String, String>>> nodeConnectionPointsMap = new HashMap<String, List<Map<String, String>>>(); // connection-points
																																// associated
																																// with
																																// each
																																// node
	private Map<String, List<Map<String, Object>>> nodeInternalVldMap = new HashMap<String, List<Map<String, Object>>>(); // internal-vld
																															// associated
																															// with
																															// each
																															// node
	private Map<String, List<Map<String, Object>>> nodeVdu = new HashMap<String, List<Map<String, Object>>>(); // vdu
																												// associated
																												// with
																												// each
																												// node
	private Map<String, LinkedList<String>> internalConnectionsMap = new HashMap<String, LinkedList<String>>(); // <connectionName,<connPointsNames>>
	private Map<String, LinkedList<String>> externalConnectionsMap = new HashMap<String, LinkedList<String>>(); // <connectionName,
																												// <connPointsNames>>
	private Map<String, Map<String, String>> cpSubstituteMap = new HashMap<String, Map<String, String>>(); // <nodeName,
																											// Map<cpOldName,
																											// cpNewName>>
	private Map<String, Map<String, Object>> noExtInterfaceMap = new HashMap<String, Map<String, Object>>(); // <nodeName, Map<vmName, List<Map<oldNameInterface, Map<The external-interfaces that are not defined as external in the nodeModel>>>>>
	private Map<String, Map<String, Object>> newInternalInterfaceMap = new HashMap<String, Map<String, Object>>(); // <nodeName, Map<vmName, List<NewInternal Interfaces>>>
	private Map<String, Map<String, Object>> newInternalConnPointMap = new HashMap<String, Map<String, Object>>(); // <nodeName, List<New InternalConnectionPoints>
	private Map<String, LinkedList<String>> internalConnectionInternalConnPointMap = new HashMap<String, LinkedList<String>>(); // <connectionName,<internalConnPointsNames>>
	// final yaml variables
	private List<Map<String, Object>> connectionPoint = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> vdu = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> internal_vld = new ArrayList<Map<String, Object>>();

	public VNFDGeneratorOSM() {

	}

	public String readUrl(String vnfUri) throws IOException {

		URL website = new URL(vnfUri);
		String file = null;
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		file = "vnf.yaml";
		FileOutputStream fos = new FileOutputStream(file);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		return file;
	}

	// @SuppressWarnings("unchecked")
	/*
	 * public void readYAML(String file, Map<String, List<String>>
	 * nodeVnfdInterfacesMap, String nodeName) throws IOException {
	 */
	public void readYAML(String file, String nodeName) throws IOException {

		Map<String, Map<String, String>> nameTypeMap = new HashMap<String, Map<String, String>>();
		InputStream input;
		try {
			input = new FileInputStream(new File(file));
			Yaml yaml = new Yaml();

			for (Object data : yaml.loadAll(input)) {
				Map<String, Object> map = new HashMap<String, Object>();
				map = (Map<String, Object>) data;
				System.out.println(map);
				// testDumpWriter(map);
				Set<String> keys = map.keySet();
				for (String a : keys) {

					// System.out.println(a);
					Map<String, Object> vnfd = (Map<String, Object>) map.get(a);
					System.out.println(vnfd);
					List<Map<String, Object>> vnfdList = new ArrayList<Map<String, Object>>();
					for (String b : vnfd.keySet()) {
						vnfdList = (List<Map<String, Object>>) vnfd.get(b);
						Iterator<Map<String, Object>> itr = vnfdList.iterator();
						while (itr.hasNext()) {
							Map<String, Object> vnf = (Map<String, Object>) itr.next();
							System.out.println(vnf);
							for (String p : vnf.keySet()) {
								if (p.contentEquals(PARAMS.get(0))) {
									// System.out.println(vnf.get(p).getClass());
									// //java.util.ArrayList
									List<Map<String, String>> connPointList = new ArrayList<Map<String, String>>();
									connPointList = (List<Map<String, String>>) vnf.get(p);
									nodeConnectionPointsMap.put(nodeName, connPointList);
									System.out.println("[VNFDGeneratorOSM] node VNFD ConnectionPointsMap: "
											+ nodeConnectionPointsMap);

								}
								if (p.contentEquals(PARAMS.get(1))) {
									// System.out.println(vnf.get(p).getClass());//class
									// java.util.ArrayList
									List<Map<String, Object>> internal_vld = (List<Map<String, Object>>) vnf.get(p);
									nodeInternalVldMap.put(nodeName, internal_vld);
									System.out.println("[VNFDGeneratorOSM] nodeInternalVldMap: " + nodeInternalVldMap);
								}
								if (p.contentEquals(PARAMS.get(2))) {
									// System.out.println(vnf.get(p).getClass());
									// //class java.util.ArrayList
									List<Map<String, Object>> vdu = (List<Map<String, Object>>) vnf.get(p);
									nodeVdu.put(nodeName, vdu);

									System.out.println("[VNFDGeneratorOSM] nodeVdu: " + nodeVdu);
								}

							}
						}
					}
				}
			}

			input.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Get the maps which contains the external and internal connections that
	 * had been defined in NodeModels
	 */

	public void parseConnections(Map<String, LinkedList<String>> connectionConnPointMap, String instanceName) {
		System.out.println("connectionConnPointMap" + connectionConnPointMap);
		Map<String, LinkedList<String>> instanceConnectionsMap = new HashMap<String, LinkedList<String>>();

		for (String c : connectionConnPointMap.keySet()) {
			if (c.split("\\.")[0].equals(instanceName)) {
				instanceConnectionsMap.put(c, connectionConnPointMap.get(c));
			}
		}
		System.out.println("[VNFDGeneratorOSM] instanceConnectionsMap: " + instanceConnectionsMap);

		for (String connName : instanceConnectionsMap.keySet()) {
			LinkedList<String> endNodes = new LinkedList<String>();
			endNodes = instanceConnectionsMap.get(connName);
			Boolean external = false;
			for (String endNode : endNodes) {
				if (endNode.split("\\.")[0].equals(instanceName)) {
					externalConnectionsMap.put(connName, endNodes);
					external = true;

				}
			}
			if (!external) {
				internalConnectionsMap.put(connName, endNodes);
			}

		}
		// Internal connection management

		for (String connection : connectionConnPointMap.keySet()) {
			setAuxConnectionPoints(connection, internalConnectionsMap, instanceName, connectionConnPointMap);
		}

		// External connection management
		for (String connection : connectionConnPointMap.keySet()) {
			setAuxConnectionPoints(connection, externalConnectionsMap, instanceName, connectionConnPointMap);
		}
		System.out.println("[VNFDGeneratorOSM] externalConnectionsMap: " + externalConnectionsMap);
		System.out.println("[VNFDGeneratorOSM] internalConnectionsMap: " + internalConnectionsMap);
	}

	/*
	 * Replacing the Nodemodel's connectionPoints with the final
	 * connectionPoints
	 */

	private void setAuxConnectionPoints(String connection, Map<String, LinkedList<String>> mapConnections,
			String instanceName, Map<String, LinkedList<String>> connectionConnPointMap) {
		for (String intConn : mapConnections.keySet()) {
			// System.out.println(intConn);
			// Comparing with the other nodeModels
			if (!connection.split("\\.")[0].equals(instanceName)) {
				// Use iterator to access to the contents of connections
				ListIterator<String> itr = mapConnections.get(intConn).listIterator();
				while (itr.hasNext()) {
					Object element = itr.next();
					String connFirst = connectionConnPointMap.get(connection).getFirst();
					String connLast = connectionConnPointMap.get(connection).getLast();
					if (element.equals(connFirst)) {
						itr.set(connLast);
					}

					if (element.equals(connLast)) {
						itr.set(connFirst);
					}

				}
			}
		}
	}

	/*
	 * Generating connection-point section for the VNFD.yaml
	 */

	public void setVnfdConnectionPoint(Map<String, Map<String, String>> nodeConnectionPointsMap,
			List<String> instanceConnectionPointsList) {
		// Rename the VirtualMachines
		for (String instanceCP : instanceConnectionPointsList) {
			// System.out.println("Instance CP "+instanceCP);
			// System.out.println(externalConnectionsMap);
			for (String connection : externalConnectionsMap.keySet()) {
				// System.out.println("connection "+connection);
				String connFirst = externalConnectionsMap.get(connection).getFirst();
				// System.out.println("connFirst "+connFirst);
				String connLast = externalConnectionsMap.get(connection).getLast();
				// System.out.println("connLast "+connLast);
				// System.out.println(instanceCP.equals(connFirst));
				if (instanceCP.equals(connFirst)) {
					// System.out.println("nodeConnectionPointsMap
					// "+nodeConnectionPointsMap);
					for (String node : nodeConnectionPointsMap.keySet()) {
						// System.out.println("node "+node);
						Map<String, String> aux = new HashMap<String, String>();
						// System.out.println("nodeConnectionPointsMap.get(node):
						// " +nodeConnectionPointsMap.get(node));
						// System.out.println("nodeConnectionPointsMap.get(node).get(connLast:
						// " +nodeConnectionPointsMap.get(node).get(connLast));
						if (nodeConnectionPointsMap.get(node).containsKey(connLast.split("\\.")[1])) {
							String oldName = nodeConnectionPointsMap.get(node).get(connLast.split("\\.")[1]);
							aux.put(oldName, instanceCP.split("\\.")[1]);
							// System.out.println("aux "+aux);
						}
						if (!aux.isEmpty()) {
							cpSubstituteMap.put(node, aux);
						}
					}

				}
				// System.out.println(instanceCP.equals(connLast));
				if (instanceCP.equals(connLast)) {
					// System.out.println("nodeConnectionPointsMap
					// "+nodeConnectionPointsMap);
					for (String node : nodeConnectionPointsMap.keySet()) {
						// System.out.println("node "+node);
						Map<String, String> aux = new HashMap<String, String>();
						// System.out.println("nodeConnectionPointsMap.get(node)"
						// +nodeConnectionPointsMap.get(node));
						// System.out.println("nodeConnectionPointsMap.get(node).get(connLast:
						// "
						// +nodeConnectionPointsMap.get(node).get(connFirst.split("\\.")[1]));
						if (nodeConnectionPointsMap.get(node).containsKey(connFirst.split("\\.")[1])) {
							String oldName = nodeConnectionPointsMap.get(node).get(connFirst.split("\\.")[1]);
							aux.put(oldName, instanceCP.split("\\.")[1]);
							// System.out.println("aux "+aux);
						}
						if (!aux.isEmpty()) {
							cpSubstituteMap.put(node, aux);
						}
					}
				}
			}

		}
		System.out.println("[VNFDGeneratorOSM] cpSubstituteMap: " + cpSubstituteMap);

		for (String cpName : instanceConnectionPointsList) {
			connectionPoint.add(generateConnectionPoint(cpName.split("\\.")[1]));
		}
		System.out.println("[VNFDGeneratorOSM] connectionPoint: " + connectionPoint);

	}

	private Map<String, Object> generateConnectionPoint(String cpName) {
		Map<String, Object> aux = new HashMap<String, Object>();
		aux.put("name", cpName);
		aux.put("type", "VPORT");

		return aux;

	}

	/*
	 * Replacing vnfd-connection-point-ref inside external interface section
	 * according to the CP defined in the NodeModel
	 */
	private String replaceVnfdConnPointRefNames() {
		String erroInfo = null;
		for (String nodeName : cpSubstituteMap.keySet()) {
			for (String cpName : cpSubstituteMap.get(nodeName).keySet()) {
				Boolean cpExists = false;
				System.out.println("nodeName " + nodeName);
				if (nodeVdu.get(nodeName) != null) {
					List<Map<String, Object>> nodeVduList = nodeVdu.get(nodeName);
					List<Map<String, String>> deleteExtInterfaceList = new  ArrayList<Map<String,String>>();
					System.out.println("nodeVduList" + nodeVduList);
					System.out.println(nodeVduList.size());
					Iterator<Map<String, Object>> itr = nodeVduList.listIterator();
					while (itr.hasNext()) {
						Map<String, Object> vm = itr.next();
						System.out.println("vm"+vm);
						List<Map<String, String>> extInterface = (List<Map<String, String>>) vm.get("external-interface");
						System.out.println(extInterface.size());
						System.out.println("vm.get(external-interface) " + vm.get("external-interface"));
						
						for (Map<String, String> extInterfaceMap : extInterface) {
							
							System.out.println("extInterfaceMap " + extInterfaceMap);
							String oldName = extInterfaceMap.get("vnfd-connection-point-ref");
							System.out.println("oldname:" + oldName);
							if (cpName.equals(oldName)) {
								cpExists = true;
								// System.out.println("Añado oldname:" +
								// oldName);
								extInterfaceMap.put("vnfd-connection-point-ref",
										cpSubstituteMap.get(nodeName).get(oldName));
							} else {
								 System.out.println("No Añado oldname:" +oldName);
								 deleteExtInterfaceList.add(extInterfaceMap);
								 System.out.println("deleteExtInterfaceList " + deleteExtInterfaceList);

								if (noExtInterfaceMap.containsKey(nodeName)) {
									Map<String, Object> aux = noExtInterfaceMap.get(nodeName);
									if (aux.containsKey((String) vm.get("name"))){
										List<Map<String, Object>> listAux = (List<Map<String, Object>>) aux.get((String) vm.get("name"));
										Map<String, Object> aux2 = new HashMap<String, Object>();
										aux2.put(oldName, extInterfaceMap);
										listAux.add(aux2);
										
									}else{
										List<Map<String, Object>> listAux = new ArrayList<Map<String,Object>>();
										Map<String, Object> aux2 = new HashMap<String, Object>();
										aux2.put(oldName, extInterfaceMap);
										listAux.add(aux2);
										aux.put((String) vm.get("name"), listAux);
									}
									

								} else {
									Map<String, Object> aux = new HashMap<String, Object>();
									Map<String, Object> aux2 = new HashMap<String, Object>();
									List<Map<String, Object>> listAux = new ArrayList<Map<String,Object>>();
									aux.put(oldName, extInterfaceMap);
									listAux.add(aux);
									aux2.put((String) vm.get("name"), listAux);

									noExtInterfaceMap.put(nodeName, aux2);
									System.out.println("noExtInterfaceMap " + noExtInterfaceMap);
									
								}
								

							}
							
						}
						//Delete all the external interfaces that are not defined as external interfaces in the NodeModel
						for(Map<String, String> extInterfaceMap: deleteExtInterfaceList){
							extInterface.remove(extInterfaceMap);
						}
						if(extInterface.isEmpty()){
							vm.remove("external-interface");
						}
						
						System.out.println("Vm despues de borrrar"+vm);
					}

					if (!cpExists) {
						erroInfo = "The ConnectionPoint " + cpName + " does not exist";
						return erroInfo;
					}
					

					
				}
			}
		}

		System.out.println("[VNFDGeneratorOSM] nodeVdu " + nodeVdu);
		System.out.println("[VNFDGeneratorOSM] noExtInterfaceMap " + noExtInterfaceMap);
		return erroInfo;
	}

	/*
	 * Getting the internal-interface and internal-connection-point according to
	 * internal connection
	 */
	private String setAuxInternalInterface(Map<String, Map<String, String>> nodeConnectionPointsMap) {
		String erroInfo = null;
		// internal-interface according to internal connection
		for (String nodeName : nodeConnectionPointsMap.keySet()) {
			List<Map<String, Object>> intInterfaceList = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> intConnPointList = new ArrayList<Map<String, Object>>();
			for (String connection : internalConnectionsMap.keySet()) {
				System.out.println("connection " + connection);
				String connFirst = internalConnectionsMap.get(connection).getFirst();
				System.out.println("connFirst " + connFirst);
				String connLast = internalConnectionsMap.get(connection).getLast();
				System.out.println("connLast " + connLast);
				
				if (nodeName.equals(connFirst.split("\\.")[0])) {
					erroInfo = setConnPointMatch(nodeConnectionPointsMap, nodeName, connFirst, connection);
					if(erroInfo != null){
						return erroInfo;
					}
				}
				if (nodeName.equals(connLast.split("\\.")[0])) {
					
					erroInfo =setConnPointMatch(nodeConnectionPointsMap, nodeName, connLast, connection);
					if(erroInfo != null){
						return erroInfo;
					}
				}
			}

		}
		System.out.println("[VNFDGeneratorOSM] newInternalInterfaceMap: " + newInternalInterfaceMap);
		System.out.println("[VNFDGeneratorOSM] newInternalConnPointMap: " + newInternalConnPointMap);
		System.out.println(
				"[VNFDGeneratorOSM] internalConnectionInternalConnPointMap: " + internalConnectionInternalConnPointMap);
		return erroInfo;
	}
	private String setConnPointMatch(Map<String, Map<String, String>> nodeConnectionPointsMap, String nodeName, String connPoint, String connection){
		System.out.println(nodeName);
		String erroInfo = null;
		String interfaceName = nodeConnectionPointsMap.get(nodeName).get(connPoint.split("\\.")[1]);
		//Map<String, String> interfaceMap = new HashMap<String, String>();
		System.out.println(interfaceName);
		Boolean interfaceExists = false;
		for (String vmName : noExtInterfaceMap.get(connPoint.split("\\.")[0]).keySet()) {
			List<Map<String, Object>> interfacesList = (List<Map<String, Object>>) noExtInterfaceMap.get(connPoint.split("\\.")[0]).get(vmName);
			for (Map<String, Object> oldInterfaceMap : interfacesList) {
				if (oldInterfaceMap.get(interfaceName) != null) {
					Map<String, Object> interfaceMap = (Map<String, Object>) oldInterfaceMap.get(interfaceName);
					System.out.println("interfaceMap"+interfaceMap);
					interfaceExists = true;
					String newInternalInterfaceName=(String) interfaceMap.get("name");
					
					setNewInternalInterface(nodeName, vmName, newInternalInterfaceName);
					setNewInternalConnPoint(nodeName, vmName, newInternalInterfaceName);
					setIntConnectionIntConnPoint(connection, newInternalInterfaceName);
				}
			}
		}
			if(!interfaceExists) {
				erroInfo = "The Connection Point " + interfaceName + " in the ConnectionPoint (NEMO) "
						+ connPoint + " does not exist";
				return erroInfo;
			}
		return erroInfo;
	}
	private void setNewInternalInterface(String nodeName, String vmName, String newInternalInterfaceName){
		if(newInternalInterfaceMap.containsKey(nodeName)){
			Map<String, Object> aux = newInternalInterfaceMap.get(nodeName);
			if(aux.containsKey(vmName)){
				List<Map<String, Object>> listAux = (List<Map<String, Object>>) aux.get(vmName);
				listAux.add(generateInternalInterface(newInternalInterfaceName));
			}else{
				List<Map<String, Object>> listAux = new ArrayList<Map<String,Object>>();
				listAux.add(generateInternalInterface(newInternalInterfaceName));
				aux.put(vmName, listAux);
			}
		}else{
			Map<String, Object> aux = new HashMap<String, Object>();
			List<Map<String, Object>> listAux = new ArrayList<Map<String,Object>>();
			listAux.add(generateInternalInterface(newInternalInterfaceName));
			aux.put(vmName, listAux);
			newInternalInterfaceMap.put(nodeName, aux);
		}
		
		System.out.println("newInternalInterfaceMap "+newInternalInterfaceMap);
	}
	
	
	private Map<String, Object> generateInternalInterface(String intName) {
		Map<String, Object> internal_interface = new LinkedHashMap<String, Object>();
		internal_interface.put("name", intName);
		Map<String, String> virtual_interface = new HashMap<String, String>();
		virtual_interface.put("type", "VIRTIO");
		internal_interface.put("virtual-interface", virtual_interface);
		internal_interface.put("vdu-internal-connection-point-ref", intName);

		return internal_interface;
	}

	private void setNewInternalConnPoint(String nodeName, String vmName, String newInternalInterfaceName){
		if(newInternalConnPointMap.containsKey(nodeName)){
			Map<String, Object> aux = newInternalConnPointMap.get(nodeName);
			if(aux.containsKey(vmName)){
				List<Map<String, Object>> listAux = (LinkedList<Map<String, Object>>) aux.get(vmName);
				listAux.add( generateInternalConnectionPoint(newInternalInterfaceName));
			}else{
				List<Map<String, Object>> listAux = new ArrayList<Map<String,Object>>();
				listAux.add( generateInternalConnectionPoint(newInternalInterfaceName));
				aux.put(vmName, listAux);
			}
		}else{
			Map<String, Object> aux = new HashMap<String, Object>();
			List<Map<String, Object>> listAux = new LinkedList<Map<String,Object>>();
			listAux.add( generateInternalConnectionPoint(newInternalInterfaceName));
			aux.put(vmName, listAux);
			newInternalConnPointMap.put(nodeName, aux);
		}
		
		System.out.println("newInternalConnPointMap "+newInternalConnPointMap);
	}
	private Map<String, Object> generateInternalConnectionPoint(String intName) {
		Map<String, Object> internal_connection_point = new LinkedHashMap<String, Object>();
		internal_connection_point.put("name", intName);
		internal_connection_point.put("id", intName);
		internal_connection_point.put("type", "VPORT");

		return internal_connection_point;
	}
	
	private void setIntConnectionIntConnPoint(String connection, String newInternalInterfaceName ){
		// Set internal ConnectionPoints (internal-vld)
		if (internalConnectionInternalConnPointMap.containsKey(connection)) {
			LinkedList<String> connPointList = internalConnectionInternalConnPointMap
					.get(connection);
			connPointList.add(newInternalInterfaceName);
		} else {
			LinkedList<String> connPointList = new LinkedList<String>();
			connPointList.add(newInternalInterfaceName);
			internalConnectionInternalConnPointMap.put(connection, connPointList);
		}
	}
	/*
	 * Generating vdu section
	 */

	public String setVfndVdu(Map<String, Map<String, String>> nodeConnectionPointsMap) {
		String erroInfo = null;
		// Replacing connection-point names (external-interface)
		erroInfo = replaceVnfdConnPointRefNames();
		if (erroInfo != null) {
			return erroInfo;
		}
		// Generating new internal interface and internal-connection-point for each
		// node
		erroInfo = setAuxInternalInterface(nodeConnectionPointsMap);
		if (erroInfo != null) {
			return erroInfo;
		}
		// Generating vdu section using: newInternalInterfaceMap and
		// newInternalConnPointMap
		int i = 1;
		for (String nodeName : nodeVdu.keySet()) {
			System.out.println("NodeName:" + nodeName);
			List<Map<String, Object>> vdu_vm = nodeVdu.get(nodeName);
			for (Map<String, Object> vm : vdu_vm) {
				System.out.println("vm "+vm);
				String vduVmName = (String)vm.get("name");
				//Adding new internal interfaces
				if (vm.containsKey("internal-interface")) {
				
					List<Map<String,Object>> intInterfaceList = (List<Map<String,Object>>) vm.get("internal-interface");
					checkNewInternalSection(newInternalInterfaceMap,nodeName, vduVmName, intInterfaceList);
					System.out.println("intInterfaceList " + intInterfaceList);

				} else {
					List<Map<String,Object>> intInterfaceList = new LinkedList<Map<String,Object>>();
					checkNewInternalSection(newInternalInterfaceMap,nodeName, vduVmName, intInterfaceList);
					vm.put("internal-interface",intInterfaceList );
					System.out.println("intInterfaceList " + intInterfaceList);
				}
				
				//Adding new internal connection points
				if (vm.containsKey("internal-connection-point")) {
					
					List<Map<String,Object>> intConnPointList = (List<Map<String,Object>>) vm.get("internal-connection-point");
					checkNewInternalSection(newInternalConnPointMap, nodeName, vduVmName, intConnPointList);
					System.out.println("intInterfaceList " + intConnPointList);

				} else {
					List<Map<String,Object>> intConnPointList = new LinkedList<Map<String,Object>>();
					checkNewInternalSection(newInternalConnPointMap,nodeName, vduVmName, intConnPointList);
					vm.put("internal-connection-point",intConnPointList );
					System.out.println("intInterfaceList " + intConnPointList);
				}

				String vm_id_name = "Ref_VM" + i;
				vm.put("id", vm_id_name);
				vm.put("name", vm_id_name);
				i++;

			}

		}
		
		
		// Adding all the vnfcs
		for (String nodeName : nodeVdu.keySet()) {
			vdu.addAll(nodeVdu.get(nodeName));
		}

		System.out.println(nodeVdu);
		System.out.println(vdu);

		return erroInfo;
	}

	private void checkNewInternalSection(Map<String, Map<String, Object>> map, String nodeName, String vduVmName, List<Map<String,Object>> intInterfaceList){
		if( map.get(nodeName)!= null){
			Map<String,Object> newIntInterfaceList = new LinkedHashMap<String,Object>();
			newIntInterfaceList = map.get(nodeName);
			for(String vmName: newIntInterfaceList.keySet()){
				if (vmName.equals(vduVmName)){
					List<Map<String,Object>> interfaceList = (List<Map<String,Object>>) newIntInterfaceList.get(vmName);
					for(Map<String, Object> newInterfaceMap : interfaceList){
						intInterfaceList.add(newInterfaceMap);
					}
				}
			}

		}
	}
	/*
	 * Generating internal-vld section
	 * 
	 */

	public void setVnfdInternalVld() {
		int i = 1;
		for (String node : nodeInternalVldMap.keySet()) {
			System.out.println(nodeInternalVldMap);
			List<Map<String, Object>> vld = nodeInternalVldMap.get(node);
			Iterator<Map<String, Object>> itr = vld.iterator();
			while (itr.hasNext()) {
				Map<String, Object> aux = itr.next();
				String vl_id_name = "VL" + i;
				aux.put("id", vl_id_name);
				aux.put("name", vl_id_name);
				aux.put("short-name", vl_id_name);
				i++;
				internal_vld.add(aux);
			}

		}

		for (String connection : internalConnectionInternalConnPointMap.keySet()) {
			LinkedList<String> intConn = internalConnectionInternalConnPointMap.get(connection);
			String vl_id_name = "VL" + i;

			String id_ref1 = intConn.getFirst();
			// System.out.println(id_ref1);
			String id_ref2 = intConn.getLast();
			// System.out.println(id_ref2);
			internal_vld.add(generateInternalVld(connection, vl_id_name, id_ref1, id_ref2));

			i++;
		}

		System.out.println("internal-vld " + internal_vld);
	}

	private Map<String, Object> generateInternalVld(String description, String id, String id_ref1, String id_ref2) {
		Map<String, Object> internalVld = new LinkedHashMap<String, Object>();
		String descr = "Internal VL from connection " + description + " generated by vibNEMO";
		internalVld.put("description", descr);
		internalVld.put("id", id);
		internalVld.put("name", id);
		internalVld.put("short-name", id);
		internalVld.put("type", "ELAN");
		List<Map<String, String>> intConnPointList = new LinkedList<Map<String, String>>();
		Map<String, String> idRef = new HashMap<String, String>();
		idRef.put("id-ref", id_ref1);
		intConnPointList.add(idRef);
		idRef = new HashMap<String, String>();
		idRef.put("id-ref", id_ref2);
		intConnPointList.add(idRef);
		internalVld.put("internal-connection-point", intConnPointList);

		return internalVld;
	}

	public String generateVNFD(String instanceName, String results_path) {
		String erroInfo = null;
		Map<String, Object> vnf = new LinkedHashMap<String, Object>();
		List<Object> vnfdList = new LinkedList<Object>();
		Map<String, Object> vnfd = new LinkedHashMap<String, Object>();
		Map<String, Object> vnfdCatalog = new LinkedHashMap<String, Object>();
		String finalName = null;
		String id = null;
		id = "Ref_Vnf_" + instanceName.replace(".", "_");
		finalName = "Ref_VNF_" + instanceName.replace(".", "_");
		String description = "This is a complex VNF Descriptor generated by VIBNEMO";

		vnf.put("connection-point", connectionPoint);
		vnf.put("description", description);
		vnf.put("id", id);
		vnf.put("name", finalName);
		vnf.put("short-name", finalName);
		vnf.put("internal-vld", internal_vld);
		vnf.put("vdu", vdu);
		vnf.put("vendor", "ETSI");
		vnf.put("version", "1.0");
		vnfdList.add(vnf);
		vnfd.put("vnfd", vnfdList);
		vnfdCatalog.put("vnfd:vnfd-catalog", vnfd);
		System.out.println(vnfdCatalog);
		try {
			dumpWriter(vnfdCatalog, instanceName, results_path);
		} catch (IOException e) {
			erroInfo = "The result path introduced does not exist";
			// e.printStackTrace();
		}
		return erroInfo;

	}

	public void dumpWriter(Object data, String instanceName, String results_path) throws IOException {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		// options.setExplicitStart(true);
		String path2 = System.getProperty("user.dir");
		System.out.println(path2);
		String finalName = "Ref_" + instanceName.replace(".", "_") + "_vnfd";
		System.out.println(finalName);
		Yaml yaml = new Yaml(options);
		StringWriter writer = new StringWriter();
		yaml.dump(data, writer);
		System.out.println(writer.toString());
		FileWriter fw = new FileWriter(new File(results_path, finalName + ".yaml"));
		fw.write(writer.toString());
		fw.close();
	}

	public void clean_vnfdGenerator() {
		nodeConnectionPointsMap.clear();
		nodeInternalVldMap.clear();
		nodeVdu.clear();
		internalConnectionsMap.clear();
		externalConnectionsMap.clear();
		cpSubstituteMap.clear();
		noExtInterfaceMap.clear();
		newInternalInterfaceMap.clear();
		newInternalConnPointMap.clear();
		internalConnectionInternalConnPointMap.clear();
		connectionPoint.clear();
		vdu.clear();
		internal_vld.clear();
	}
}
