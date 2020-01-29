/**
 *  Hampton Bay Fan Controller
 *
 *  Copyright 2020 Sean Williams
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  , vid:"x.com.st.fanspeed", vid:"generic-dimmer", mnmn: "SmartThings", vid:"x.com.st.fanspeed"
 */
metadata {
	definition (name: "Hampton Bay Fan Controller", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.fan", genericHandler: "Zigbee") {
		capability "Switch Level"
		capability "Switch"
		capability "Fan Speed"
		capability "Configuration"
		capability "Health Check"

		command "fanOff"
		command "fanOn"
		command "raiseFanSpeed"
		command "lowerFanSpeed"
		command "lightOff"
		command "lightOn"
		command "setLightLevel"
		command "setFanLevel"
		command "setFanSpeed"
		command "low"
		command "medium"
		command "high"
		command "maximum"
		command "breeze"

		attribute "lightSwitch", "string"
		attribute "fanLevel", "number"
		attribute "lightLevel", "number"

		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", manufacturer: "King Of Fans, Inc.", model: "HDC52EastwindFan", deviceJoinName: "Hampton Bay Fan Control"
	}

	preferences {
		input (name:"offBeforeChange", type:"bool", title: "Turn fan off before changing speed", description: "Some fans need to go to the 'off' state before selecting a new speed", defaultValue: false, default: false, required: true, displayDuringSetup: true)
		input (name:"createSpeedSwitches", type:"bool", title:"Create speed switches", description: "Additional child switches will be created for the supported speeds of your fan.", defaultValue: false, default: false, required: true, displayDuringSetup: true)
		input (name: "resumeLast", type:"bool", title: "Resume Last Setting when speed switch is turned off", defaultValue: false, default: false, required: true, displayDuringSetup: true)
	}

	tiles(scale:2) {
		multiAttributeTile(name: "fanSpeed", type: "generic", width: 6, height: 4, canChangeIcon: false) {
			tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "off", action: "switch.on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOff"
				attributeState "1", label: "low", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "2", label: "medium", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#1e9cbb", nextState: "turningOff"
				attributeState "3", label: "high", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#184f9c", nextState: "turningOff"
				attributeState "4", label: "max", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#153591", nextState: "turningOff"
				attributeState "6", label: "Breeze", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#90d2a7", nextStae: "turningOff"
				attributeState "adjusting", label: "Adjusting...", action: "switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
				attributeState "turningOff", label:"Turning Fan Off", action: "switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
				attributeState "turningOn", label:"Turning Fan On", action: "switch.off", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
			}
			tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "raiseFanSpeed"
				attributeState "VALUE_DOWN", action: "lowerFanSpeed"
			}
		}
		childDeviceTiles("children", width: 6, height: 1)
		main "fanSpeed"
		details(["fanSpeed", "children"])
	}
}

// Imports
import physicalgraph.app.ChildDeviceWrapper
import physicalgraph.device.*
import physicalgraph.zigbee.zcl.DataType

// Constants
private static final int getDEFAULT_DELAY() { 100 }
private static final getBIND_CLUSTER() { 0x8021 }
private static final int getCHILD_ENPOINT_ID_OFFSET() { 2 }
private static final getFAN_CLUSTER() { 0x0202 }
private static final getFAN_ATTR_ID() { 0x0000 }
private static final getGROUPS_CLUSTER() { 0x0004 }
private static final getHOME_AUTOMATION_CLUSTER() { 0x0104 }
private static final getSCENES_CLUSTER() { 0x0005 }
private static final Map getCommandInfo() { [ 0x00:"Read", 0x01:"Read Response", 0x02:"Write", 0x04:"Write Response", 0x05:"Write, No Response", 0x06:"Configure Reporting", 0x07:"Configure Reporting Response", 0x0B:"Default Response" ] }
private static final Map getBindResults() { [ 0:"Success", 132:"Not Supported", 130:"Invalid Endpoint", 140:"Table Full" ] }
private static final Map getFanSpeeds() { [ 0:[name: "Fan Off", min: 0, max: 0], 1:[name: "Low", display: 25, min: 1, max: 49], 2:[name: "Medium", min: 50, max: 74], 3:[name: "High", min: 75, max: 99], 4:[name: "Maximum", min: 100, max: 100], 6:[name: "Breeze", min: 101, max: 100] ] }
private static final Map getSpeedSwitchTypeInfo() { [namespace: "smartthings", typeName: "Child Switch"] }

private final Map getChildDeviceSpecs() {
	[
		0: [ name: (fanSpeeds[0].name), createChild: false, data: [ fanSpeed: 0 ] ],
		1: [ name: (fanSpeeds[1].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.supportedSpeeds.contains(1) && state.createSpeedSwitches), componentName: "fanMode1", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 1 ] ],
		2: [ name: (fanSpeeds[2].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.supportedSpeeds.contains(2) && state.createSpeedSwitches), componentName: "fanMode2", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 2 ] ],
		3: [ name: (fanSpeeds[3].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.supportedSpeeds.contains(3) && state.createSpeedSwitches), componentName: "fanMode3", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 3 ] ],
		4: [ name: (fanSpeeds[4].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.supportedSpeeds.contains(4) && state.createSpeedSwitches), componentName: "fanMode4", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 4 ] ],
		5: [ name: "Off", createChild: false ],
		6: [ name: (fanSpeeds[6].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, required: (state.supportedSpeeds.contains(6)), componentName: "fanMode6", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 6 ] ],
		7: [ name: "Light", typeInfo: [namespace: "stussy2112", typeName: "Child Switch Dimmer"], isComponent: false, required: true, componentName: "fanLight", syncInfo: [ (zigbee.ONOFF_CLUSTER):"lightSwitch", (zigbee.LEVEL_CONTROL_CLUSTER):"lightLevel" ] ]
	]
}

private final Map getClusterInfo() { [ (zigbee.ONOFF_CLUSTER):[ name: "Switch", dataType: DataType.BOOLEAN, shouldRefresh: true ], (zigbee.LEVEL_CONTROL_CLUSTER):[name: "Level", dataType: DataType.UINT8, shouldRefresh: true], (FAN_CLUSTER):[name:"Fan", dataType: DataType.ENUM8, shouldRefresh: true], (BIND_CLUSTER):[name: "Bind"], 0x0020:[name:"Polling", dataType:DataType.UINT8] ] }
private final List<Integer> getDefaultSupportedSpeeds() { fanSpeeds.keySet() as int[] }
private int getInitialEndpoint () { Integer.parseInt(zigbee.endpointId, 10) }

def configure() {
	log.debug "Configuring Reporting and Bindings."

	// OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
	int minReportTime = 0
	int maxReportTime = 300
	List cmds = [
		//Set long poll interval
		//"raw 0x0020 {11 00 02 02 00 00 00}", "delay 100",
		//"send 0x${device.deviceNetworkId} 1 1", "delay 100",
		// Configure reporting for the ON/OFF switch
		// NOTE: Configure reporting adds the bindings as well as setting up reporting
		zigbee.onOffConfig(minReportTime, maxReportTime),
		// Configure reporting for the Dimmer
		zigbee.levelConfig(minReportTime, maxReportTime),
		// Configure reporting for the fan (0x0202)
		zigbee.configureReporting(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, minReportTime, maxReportTime, null),
		//Set long poll interval
		//"raw 0x0020 {11 00 02 1C 00 00 00}", "delay 100",
		//"send 0x${device.deviceNetworkId} 1 1", "delay 100"
	]

	// Add bindings and reporting for the child devices that control the on/off cluster
	cmds += findChildDevicesByClusterData(zigbee.ONOFF_CLUSTER).collect {
		log.debug "Configuring ON/OFF reporting for endpoint ${Integer.parseInt(it.getDataValue('endpointId'))}: ${it}"
		zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, DataType.BOOLEAN, minReportTime, maxReportTime, null, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
	}

	cmds += findChildDevicesByClusterData(zigbee.LEVEL_CONTROL_CLUSTER).collect {
		log.debug "Configuring LEVEL reporting for endpoint ${Integer.parseInt(it.getDataValue('endpointId'))}: ${it}"
		zigbee.configureReporting(zigbee.LEVEL_CONTROL_CLUSTER, 0x0000, DataType.UINT8, minReportTime, maxReportTime, 0x01, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
	}

	cmds += findChildDevicesByClusterData(FAN_CLUSTER).collect {
		log.debug "Configuring FAN reporting for endpoint ${Integer.parseInt(it.getDataValue('endpointId'))}: ${it}"
		zigbee.configureReporting(FAN_CLUSTER, 0x0000, DataType.ENUM8, minReportTime, maxReportTime, null, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
	}

	//Get current values from the device
	cmds += refresh()
	
	configureHealthCheck()
	
	log.trace "'configure()' returning: ${cmds}"
	return cmds
}

def installed() {
	log.debug "Installed ${device.displayName}"
	
	state.preferences = [
		createSpeedSwitches: false,
		resumeLast: false,
		//supportedSpeeds: ["Low","Medium","High","Max","Breeze"],
		supportedSpeeds: (defaultSupportedSpeeds),
		offBeforeChange: false
	]
	log.debug "Installed Preferences: ${state.preferences}"
	state.preferences.each {
		log.trace "Installed preference: ${it}"
		device.updateSetting(it.key, it.value)
		state[it.key] = it.value
	}
	
	state.lastFanSpeed = fanSpeeds.keySet().min()
	setFanSpeed(1)
}

// parse message from device into events that SmartThings platform can understand
def parse(String description) {
	//log.debug "Parsing '${description}'"
	
	List<Map> events = []
	
	Map descMap = zigbee.parseDescriptionAsMap(description)
	int cluster = descMap?.clusterInt ?: zigbee.ONOFF_CLUSTER
	Map event = zigbee.getEvent(description)
	if (event) {
		//log.info "Defined event detected from controller: ${description}"
		// NOTE: For certain descriptions, the values for the events need to be changed to report correctly
		// To handle this, send the correct events for the switch and level clusters to the child light device
		/*
			switch - This should be the on/off state of the FAN (zigbee.OnOffRefresh gives the LIGHT state). Create a "lightSwitch" event instead
			level - This should be the level of the FAN (zigbee.levelRefresh gives the LIGHT level. Create a "lightLevel" event instead
			on/off - This is the switch event for the LIGHT. Send this directly, as it will not update any attribute on the handler
		*/
		Map eventNameMap = [ "switch":"lightSwitch", "level":"lightLevel", "on/off":"lightSwitch"]
		events << createEvent(name: eventNameMap[event.name], value: event.value)
	}
	else if (description?.startsWith('read attr -') && FAN_CLUSTER == cluster && FAN_ATTR_ID == descMap?.attrInt) {
		// handle 'fanSpeed' attribute
		//log.info "Fan message detected from controller: ${description}"
		events = createFanEvents((Math.max(Integer.parseInt(descMap.value), 0)))
	} else {
		events = parseCatchAll(description)
	}
	
	events.each { syncChildDevices(cluster, it) }
	
	if (0 >= events.size()) {
		log.warn "DID NOT PARSE MESSAGE for description : ${description}"
	}
	
	return events
}

def updated() {
	log.debug "Updating ${device.displayName} : ${device.deviceNetworkId}"
	
	// If the speed switches were previously created and user is selecting 'false', remove the switches
	if (!createSpeedSwitches && state.preferences && state.preferences["createSpeedSwitches"]) {
		log.debug "Should delete children"
		deleteChildren()
	}
	
	state.preferences = [
		createSpeedSwitches: (createSpeedSwitches == null ? false : createSpeedSwitches),
		resumeLast: (resumeLast == null ? false : resumeLast),
		supportedSpeeds: defaultSupportedSpeeds,
		offBeforeChange: (offBeforeChange == null ? false : offBeforeChange)
	]
	log.debug "Updated Preferences: ${state.preferences}"
	state.preferences.each {
		log.trace "Updated preference: ${it}"
		state[it.key] = it.value
		device.updateSetting(it.key, it.value)
	}
		
	if (!getChildDevices() || state.preferences["createSpeedSwitches"]) {
		log.debug "Should create children"
		createChildDevices()
	} else if (state.oldLabel != device.label) {
		//log.trace "Updating child labels"
		// Find the child devices that are components
		device.getChildDevices().findAll { it.isComponent }
			.each { 
				// Update the name of the child devices
				def specs = findChildDeviceSpecs(getChildDevice(it.deviceNetworkId))
				log.trace "Updating to: ${device.displayName} ${specs.name}"
				it.label = "${device.displayName} ${specs.name}"
			}
		state.oldLabel = device.label
	}
	
	response(configure())
}

// handle commands
public List<HubAction> fanOff() {
	//log.debug "Executing 'fanOff'";
	return setFanSpeed(0)
}

public List<HubAction> fanOn() {
	//log.debug "Executing 'fanOn'";
	//return setFanLevel((state.lastFanLevel == null ? fanSpeeds[1].threshold : state.lastFanLevel))
	return setFanSpeed((state.lastFanSpeed == null ? 1 : state.lastFanSpeed))
}

public List<HubAction> lightOff(int endpoint = 1) {
	return lightOnOff(0x00, endpoint)
}

public List<HubAction> lightOn(int endpoint = 1) {
	return lightOnOff(0x01, endpoint)
}

public List<HubAction> on(int endpoint = 1) {
	//log.debug "Executing 'on': endpoint = ${endpoint}"
	return fanOn()
}

public List<HubAction> off(int endpoint = 1) {
	//log.debug "Executing 'off': endpoint = ${endpoint}"
	return fanOff()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.debug "Executing 'ping()'"
	refresh()
}

def poll() {
	log.debug "Executing 'poll()'"
	refresh()
}

def refresh(List<Integer> endpoints = [1]) {
	//log.debug "Executing 'refresh()': endpoints = ${endpoints}"
	
	List<Integer> deviceClusters = clusterInfo.findAll { it.value.shouldRefresh }.keySet() as int[]
	List<Integer> readClusters = []
	List cmds = []
	// If refreshing the main endpoint, refresh all clusters and the children
	/*if (endpoints.contains(initialEndpoint)) {
		endpoints = device.getChildDevices().collect { Integer.parseInt(it.getDataValue('endpointId')) }
		cmds = deviceClusters.flatten().unique().collect { zigbee.readAttribute(it, 0x0000, [destEndpoint: initialEndpoint]) }.flatten()
	}*/
	
	if (1 == endpoints.size() && endpoints.contains(initialEndpoint)) {		
		// If only refreshing the main endpoint, refresh all clusters and the children
		endpoints += device.getChildDevices().collect { Integer.parseInt(it.getDataValue('endpointId')) }
		readClusters = deviceClusters
	}
	
	// Collect child info
	def validChildInfo = endpoints.collect { [specs: (findChildDeviceSpecs(it)), endpoint: it ] }
		.findAll { it.specs && !it.specs.syncInfo?.keySet()?.disjoint(deviceClusters) }
		.collect { [clusters: it.specs.syncInfo?.keySet(), endpoint: it.endpoint] }

	// Create the commands for the initial/main endpoint
	readClusters << validChildInfo.collect { it.clusters }.flatten().unique()
	cmds = readClusters.flatten().unique().collect { zigbee.readAttribute(it, 0x0000, [destEndpoint: initialEndpoint]) }.flatten()
	// Create the commands for the child endpoints
	//cmds += validChildInfo.collect { info -> info.clusters.collect { zigbee.readAttribute(it, 0x0000, [destEndpoint: info.endpoint]) }.flatten() }.flatten()
	
	//log.trace "'refresh()': Returning ${cmds} for ${endpoints}"
	return cmds.collect { new HubAction(it) }
}

public List<HubAction> setLightLevel(Number value, rate = null) {
	Integer lightNow = Math.max(device.currentValue("lightLevel")?.intValue() ?: 0, 0)
	//log.debug "Requested lightLevel is ${value}. Current lightLevel is ${lightNow}"

	Integer level = Math.max(Math.min(value.intValue(), 100), 0)
	
	List cmds = []
	// only update if the new level is different than the current level
	if (level != lightNow) {
		//state.lastLightLevel = lightNow	 //save light level before changing so it can be resumed when turned back on
		rate = Math.max(Math.min((rate == null ? 0 : rate.intValue()), 100), 0)
		log.info "Adjusting Light Brightness: ${level} : ${rate}"
		cmds = zigbee.setLevel(level, rate)
	}
	// NOTE: Add "refresh" to handle not reporting correctly
	cmds += zigbee.levelRefresh() + zigbee.onOffRefresh()
	
	//log.trace "'setLightLevel()' returning: ${cmds}"
	return cmds.collect { new HubAction(it) }
}

public List<HubAction> setFanLevel(Number level = 0) {
	Number fanNow = Math.max(device.currentValue("fanLevel")?.intValue() ?: 0, 0)
	List<Integer> maxLevels = fanSpeeds.values().collect { it.max }
	level = Math.max(Math.min(level.intValue(), maxLevels.max()), maxLevels.min())   
	log.debug "Requested fanLevel is ${level}. Current fanLevel is ${fanNow}"
	
	List cmds = []
	// only update if the new fan level is different than the current fan level
	if (level != fanNow) {
		state.lastFanLevel = fanNow	//save fan level before changing so it can be resumed when turned back on
		sendEvent(name: "fanLevel", value: level) // NOTE: This is necessary b/c fan level is not defined in any capability
		cmds = setFanSpeed(findFanSpeedByLevel(level))
	} else  {
		cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID).collect { new HubAction(it) }
	}
	
	return cmds
}

public List<HubAction> setFanSpeed(Number speed) {
	int fanNow = Math.max(device.currentValue("fanSpeed")?.intValue() ?: 0, 0)
	log.debug "Executing 'setFanSpeed': fanSpeed = ${speed}. Current Fan speed is ${fanNow}"
		
	List cmds = []
 	// NOTE: This may be necessary to get the reporting of values correct
	//createFanEvents(speed).each { sendEvent(it) }
	// only update if the new fan speed is different than the current fan speed
	if (speed != fanNow) {
		speed = convertToSupportedSpeed(speed?.intValue() ?: 0, fanNow)
		state.lastFanSpeed = fanNow    //save fanspeed before changing speed so it can be resumed when turned back on
		log.info "Adjusting Fan Speed to ${fanSpeeds[speed]}"
		if (0 < fanNow && 0 < speed && state.offBeforeChange?.toBoolean()) {
			log.debug "Turning off before changing speed"
			cmds = zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", 0))
		}
		cmds += zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", speed))
	} else  {
		cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)
	}
	
	//log.trace "'setFanSpeed()' returning: ${cmds}"
	return cmds.collect { new HubAction(it) }
}

public List<HubAction> setLevel(value, rate = null) {
	//log.debug "Executing 'setLevel()': ${value}, ${rate}"
	return setFanLevel(value)
}

public List<HubAction> raiseFanSpeed() {
	//log.debug "Executing 'raiseFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
	return setFanSpeed(currentSpeed + 1)
}

public List<HubAction> lowerFanSpeed() {
	//log.debug "Executing 'lowerFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
	return setFanSpeed(currentSpeed - 1)
}

public List<HubAction> low() {
	return setFanLevel((fanSpeeds[1].display))
}

public List<HubAction> medium() {
	return setFanLevel((fanSpeeds[2].min))
}

public List<HubAction> high() {
	return setFanLevel((fanSpeeds[3].min))
}

public List<HubAction> max() {
	return setFanLevel((fanSpeeds[4].min))
}
/**
  * Child handling
  */
public void childOff(String dni) {
	//log.debug "Executing 'childOff(): ${dni}"
	
	List<HubAction> cmds = []
	
	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		// Determine the cluster that needs to be controlled
		List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work
		if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
			int endpoint = getChildDeviceEndpoint(childDevice)
			cmds = lightOff(endpoint)
		} else if (clusters.contains(FAN_CLUSTER)) {
			boolean shouldResume = state.preferences["resumeLast"].toBoolean() && 0 < state.lastFanSpeed
			cmds = shouldResume ? fanOn() : fanOff()
		}
	}
	
	//log.trace "'childOff()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

public void childOn(String dni) {
	//log.debug "Executing 'childOn(): ${dni}"
	
	List<HubAction> cmds = []
	
	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		// Determine the cluster that needs to be controlled
		List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }  // NOTE: Single quotes necessary for this to work
		
		if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
			int endpoint = getChildDeviceEndpoint(childDevice)
			cmds = lightOn(endpoint)
		} else if (clusters.contains(FAN_CLUSTER)) {
			cmds = setFanSpeed(Integer.parseInt(childDevice.getDataValue('fanSpeed')))
		}
	}
		
	//log.trace "'childOn()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

public void childRefresh(String dni) {
	//log.debug "Executing 'childRefresh(): ${dni}"
	
	List<HubAction> cmds = []
	
	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		cmds = refresh([getChildDeviceEndpoint(childDevice)])
	}
	
	//log.trace "'childRefresh()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

public void childSetLevel(String dni, Number level, Number rate = null) {
	//log.debug "Executing 'childSetLevel(): ${dni}, level = ${level}, rate = ${rate}"
	
	List<HubAction> cmds = []

	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		// Determine the cluster that needs to be controlled
		List<String> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work

		if (clusters.contains(zigbee.LEVEL_CONTROL_CLUSTER)) {
			cmds = setLightLevel(level, rate)
		} else if (clusters.contains(FAN_CLUSTER)) {
			cmds = setFanLevel(level, rate)
		}
	}

	//log.trace "'childSetLevel()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

/**
 * Private members
 **/
private ChildDeviceWrapper createChildDevice(String namespace = "stussy2112", String typeName, String deviceNetworkId, Map properties) {
	if (!getChildDevice(deviceNetworkId)) {
		log.debug "Creating child ${namespace}.${typeName} - ${deviceNetworkId} : ${properties}"
		return addChildDevice(namespace, typeName, deviceNetworkId, device.hubId, properties)
	} else {
		log.debug "Child exists: ${deviceNetworkId}"
	}
}

private void createChildDevices() {
	log.debug "Executing 'createChildDevices()'"

	childDeviceSpecs.findAll { key, value ->
		Boolean rtnVal = value.createChild || value.required
		log.trace "Should Create: ${value} : ${rtnVal}"
		return rtnVal
	}
	.each { key, value ->
		log.trace "Create Child: ${value}"
		int endPointId = key.intValue() + CHILD_ENPOINT_ID_OFFSET
		String networkId = "${device.deviceNetworkId}:0${endPointId}"
		Map data = (value.data ?: [:]) << [endpointId: "${String.format("%02d", endPointId)}", required: value.required, requested: state.createSpeedSwitches, clusters: (value.syncInfo?.collect { "${it.key}" }?.join(",")), manufacturer: "King Of Fans, Inc." ]
		Map properties = [completedSetup: true, label: "${device.displayName} ${value.name}", isComponent: value.isComponent, componentName: value.componentName, componentLabel: value.name, data: data ]
		createChildDevice(value.typeInfo.namespace, value.typeInfo.typeName, networkId, properties)
	}

	syncChildDevices(FAN_CLUSTER, [name: "fanSpeed", value: device.currentValue("fanSpeed")])
	syncChildDevices(zigbee.ONOFF_CLUSTER, [name: "lightSwitch", value: device.currentValue("lightSwitch")])
	syncChildDevices(zigbee.LEVEL_CONTROL_CLUSTER, [name: "lightLevel", value: device.currentValue("lightLevel")])
}

private List<Map> createFanEvents(Number fanSpeed = null) {
	//log.debug "Executing 'createFanEvents()': fanSpeed = ${fanSpeed}"

	fanSpeed = Math.max((fanSpeed == null ? device.currentValue("fanSpeed") : fanSpeed)?.intValue() ?: 0, 0)
	String fanSwitch = 0 < fanSpeed ? "on" : "off"
	// Adjust the fan level to insure that it falls in the correct range
	int fanLevel = Math.max(device.currentValue("fanLevel")?.intValue() ?: 0, 0)
	if (!(fanSpeeds[fanSpeed].min..fanSpeeds[fanSpeed].max).contains(fanLevel)) {
		fanLevel = fanSpeeds[fanSpeed].display ?: fanSpeeds[fanSpeed].min
	}
	//fanLevel = Math.max(Math.min(fanLevel, maxLevel), minLevel + 1)
	List<Map> events = [
		//createEvent(name: "fanSwitch", value: fanSwitch),
		createEvent(name: "fanSpeed", value: fanSpeed),
		createEvent(name: "fanLevel", value: fanLevel),
		createEvent(name: "switch", value: fanSwitch),
		createEvent(name: "level", value: fanLevel)
	]

	//log.trace "'createFanEvents()' returning: ${events}"
	return events
}

private void configureHealthCheck() {
	log.debug "Executing 'createHealthCheckEvent()'"

	Map healthEvent = [:]

	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	healthEvent = [name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
	sendEvent(healthEvent)
	getChildDevices().each {
		it.sendEvent(healthEvent)
	}
}

private int convertToSupportedSpeed(int requestedSpeed, int currentSpeed = 0) {
	int fanNow = Math.max(currentSpeed, device.currentValue("fanSpeed")?.intValue() ?: 0)
	//log.debug "Executing 'convertToSupportedSpeed()': requestedSpeed = ${requestedSpeed}, currentSpeed = ${currentSpeed} (using ${fanNow})"
	def keySet = fanSpeeds.keySet()
	int speed = Math.max(Math.min(requestedSpeed, keySet.max()), keySet.min())

	// Check that the requested speed is supported. the min speed (off) should always be supported
	if (fanNow != speed && keySet.min() < speed) {
		List<Integer> supportedSpeeds = state.preferences["supportedSpeeds"]
		while (keySet.min() < speed && keySet.max() + 1 > speed && !supportedSpeeds.contains(speed)) {
			//log.trace "Calculating supported speed for ${speed}"
			if (fanNow < speed) {
				speed++
			} else if (fanNow > speed) {
				speed--
			}
		}
	}

	if(keySet.max() < speed) { speed = keySet.min() }
	//log.trace "'convertToSupportedSpeed()': returning = ${speed}"
	return speed
}

private void deleteChildren() {	
	getChildDevices()?.each {c ->
		if (c.getDataValue('required').toBoolean())
		{
			log.warn "${c.deviceNetworkId} is required. NOT deleting"
		} else {
			log.debug "Deleting ${c.deviceNetworkId}"
			deleteChildDevice(c.deviceNetworkId)
		}
	}
}

private List<ChildDeviceWrapper> findChildDevicesByClusterData(int cluster) {
	return getChildDevices().findAll { it.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }.contains(cluster) }
}

private Map findChildDeviceSpecs(ChildDeviceWrapper child) {
	return findChildDeviceSpecs(Integer.parseInt(child?.getDataValue('endpointId')))
}

private Map findChildDeviceSpecs(int endpointId = -1) {
	return childDeviceSpecs[endpointId - CHILD_ENPOINT_ID_OFFSET]
}

private int findFanLevelBySpeed(Number speed) {
	//log.trace "'findFanLevelBySpeed()': speed = ${speed}"
	Number supportedSpeed = convertToSupportedSpeed(speed.intValue())
	int fanLevel = fanSpeeds[supportedSpeed].display //fanSpeeds.find { it.key.intValue() >= supportedSpeed }.value.threshold
	//log.trace "supportedSpeed: ${supportedSpeed}, fanLevel: ${fanLevel}"
	return fanLevel
}

private int findFanSpeedByLevel(Number level) {
	Number correctedLevel = Math.max(level.intValue(), 0)
	int speed = fanSpeeds.find { it.value.min.intValue() <= correctedLevel && it.value.max.intValue() >= correctedLevel }?.key ?: 0
	log.trace "correctedLevel: ${correctedLevel}, speed: ${speed}"
	return convertToSupportedSpeed(speed)
}

private int getChildDeviceEndpoint(ChildDeviceWrapper childDevice) {
	return childDevice?.getDataValue('endpointId') as Integer
}

private ChildDeviceWrapper getChildDevice(String deviceNetworkId) {
	ChildDeviceWrapper child = getChildDevices().find { it.deviceNetworkId == deviceNetworkId }
	if (!child) {
		log.error "Child device ${deviceNetworkId} not found"
	}
	return child
}

private List<HubAction> lightOnOff(value, int endpoint = 1) {
	//log.debug "Executing 'lightOnOff()', value = ${value}, endpoint = ${endpoint}"
	List cmds = []

	// Send the command for the endpoint
	if (initialEndpoint != endpoint) {
		Map childDeviceSpecs = findChildDeviceSpecs(endpoint)
		if (childDeviceSpecs && !childDeviceSpecs.syncInfo.keySet().contains(zigbee.ONOFF_CLUSTER)) {
			log.debug "Endpoint ${endpoint} does not control the ${clusterInfo[zigbee.ONOFF_CLUSTER].name}"
			return cmds.collect { new HubAction(it) }
		}/* else {
			//{${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))}
			cmds = zigbee.command(zigbee.ONOFF_CLUSTER, value, "", [destEndpoint: endpoint])
		}*/
	}

	// NOTE: Add "refresh" to handle not reporting correctly
	cmds = zigbee.command(zigbee.ONOFF_CLUSTER, value, "") + zigbee.onOffRefresh()

	//log.trace "'lightOnOff()': Sending ${cmds} for ${endpoint}"
	return cmds.collect { new HubAction(it) }
}

private List parseCatchAll(String description) {
	//log.debug "Executing 'parseCatchAll()': ${description}"
	List<Map> events = []

	if (description?.startsWith("catchall:")) {
		def parsed = zigbee.parse(description)
		//log.trace "Parsed catchall: ${parsed}"

		// profile id for responses is 0104 (0x0104) - Home Automation
		if (HOME_AUTOMATION_CLUSTER == parsed.profileId) {
			if (0 < parsed.data.size()) {
				// if the data payload is '0', all was good and there is nothing to do
				log.info "${clusterInfo[parsed.clusterId].name} responded with ${commandInfo[parsed.command.intValue()]} : ${parsed.data}."
				switch (parsed.clusterId) {
					case (zigbee.ONOFF_CLUSTER):
						//0x10 = 16, 0x01 = 1
						//Light Off
						//catchall: prof clus se de opti ty clus mf mc cmdi dr 
						//catchall: 0104 0006 01 01 0000 00 77E9 00 00 0000 01 01 0000001000
						//SmartShield(text: null, manufacturerId: 0x0000, direction: 0x01, data: [0x00, 0x00, 0x00, 0x10, 0x00], number: null, isManufacturerSpecific: false, messageType: 0x00, senderShortId: 0x77e9, isClusterSpecific: false, sourceEndpoint: 0x01, profileId: 0x0104, command: 0x01, clusterId: 0x0006, destinationEndpoint: 0x01, options: 0x0000)
						//[raw:0104 0006 01 01 0000 00 77E9 00 00 0000 01 01 0000001000, profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0000, messageType:00, dni:77E9, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:01, direction:01, attrId:0000, resultCode:00, encoding:10, value:00, isValidForDataType:true, data:[00, 00, 00, 10, 00], clusterInt:6, attrInt:0, commandInt:1]
						//Light On
						//catchall: 0104 0006 01 01 0000 00 77E9 00 00 0000 01 01 0000001001
						//00,00,00,10,01
						//SmartShield(text: null, manufacturerId: 0x0000, direction: 0x01, data: [0x00, 0x00, 0x00, 0x10, 0x01], number: null, isManufacturerSpecific: false, messageType: 0x00, senderShortId: 0x77e9, isClusterSpecific: false, sourceEndpoint: 0x01, profileId: 0x0104, command: 0x01, clusterId: 0x0006, destinationEndpoint: 0x01, options: 0x0000)
						//[raw:0104 0006 01 01 0000 00 77E9 00 00 0000 01 01 0000001001, profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0000, messageType:00, dni:77E9, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:01, direction:01, attrId:0000, resultCode:00, encoding:10, value:01, isValidForDataType:true, data:[00, 00, 00, 10, 01], clusterInt:6, attrInt:0, commandInt:1]
						events << createEvent(name:"lightSwitch", value: 0x00 == parsed.data[-1] ? "off" : "on")
						break
					case (zigbee.LEVEL_CONTROL_CLUSTER):
						break					
					case (FAN_CLUSTER):
						//Fan change
						//catchall: 0104 0202 01 01 0000 00 77E9 00 00 0000 04 01 00
						//SmartShield(text: null, manufacturerId: 0x0000, direction: 0x01, data: [0x00], number: null, isManufacturerSpecific: false, messageType: 0x00, senderShortId: 0x77e9, isClusterSpecific: false, sourceEndpoint: 0x01, profileId: 0x0104, command: 0x04, clusterId: 0x0202, destinationEndpoint: 0x01, options: 0x0000)
						//[raw:0104 0202 01 01 0000 00 77E9 00 00 0000 04 01 00, profileId:0104, clusterId:0202, sourceEndpoint:01, destinationEndpoint:01, options:0000, messageType:00, dni:77E9, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:04, direction:01, data:[00], clusterInt:514, commandInt:4]
						//List<Map> fanEvents = []
						// If the event was successfull AND it was NOT a write command response
						if (0x00 == parsed.data[-1] && 0x04 != parsed.command) {
							events = createFanEvents()
						}
						//events << fanEvents
						break
					case (GROUPS_CLUSTER):
						//String addToGroupCmd = "st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))} 00}"
						break
				}
				
			}
		} else {
			log.info "${clusterInfo[parsed.clusterId.intValue()] ?: parsed.clusterId} Event Generated"
			if (BIND_CLUSTER == parsed.clusterId) {
				if (0 == parsed.data.size()) {
					log.error "Bind result not present."
				} else {
					log.debug "Bind response: ${commandInfo[parsed.command] ?: parsed.command}"
					configureHealthCheck()
					//cmds << [ name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID] ]
				}
			}
		}
	}

	//log.debug "'parseCatchAll' returning ${events}"
	return events
}

private void syncChildDevices(int cluster, Map event) {
	//log.debug "Executing 'syncChildDevices(): cluster = ${cluster}, event = ${event}"

	// NOTE: Map [from event name] to [child device event name]
	Map eventNameMap = [ "lightSwitch":"switch", "lightLevel":"level", "fanSpeed":"switch", "fanLevel":"level"]
	findChildDevicesByClusterData(cluster).findAll { event.name == findChildDeviceSpecs(it)?.syncInfo[cluster] }
		.each {
			// event.name = fanSpeed || lightSwitch || lightLevel
			Map childEvent = FAN_CLUSTER == cluster
				? it.createEvent(name:eventNameMap[event.name], value:((event.value ?: 0 as int) == Integer.parseInt(it.getDataValue('fanSpeed')) ? "on" : "off"))
				: it.createEvent(name:eventNameMap[event.name], value:event.value, isStateChange: true)
			//log.debug "Sending ${childEvent} TO child ${it}"
			it.sendEvent(childEvent)
		}
}