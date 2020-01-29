/**
 *  Zigbee Fan Controller
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
 */
metadata {
	definition (name: "Zigbee Fan Controller", namespace: "stussy2112", author: "Sean Williams", cstHandler: true, executeCommandsLocally: false, ocfDeviceType: "oic.d.fan", genericHandler: "Zigbee") {
		capability "Switch Level"
		capability "Switch"
		capability "Fan Speed"
		capability "Configuration"
		capability "Health Check"
		capability "Refresh"

		attribute "lightSwitch", "string"
		attribute "fanLevel", "number"
		attribute "lightLevel", "number"
        
		command "raiseFanSpeed"
		command "lowerFanSpeed"

		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", manufacturer: "King Of Fans, Inc.", model: "HDC52EastwindFan", deviceJoinName: "Hampton Bay Fan Control", mnmn: "SmartThings", vid:"x.com.st.fanspeed"
	}

	preferences {
		input (name:"offBeforeChange", type:"bool", title: "Turn fan off before changing speed", description: "Some fans need to go to the 'off' state before selecting a new speed", defaultValue: false, default: false, required: true, displayDuringSetup: true)
		input (name:"createSpeedSwitches", type:"bool", title:"Create speed switches", description: "Additional child switches will be created for the supported speeds of your fan.", defaultValue: false, default: false, required: true, displayDuringSetup: true)
		input (name: "resumeLast", type:"bool", title: "Resume Last Setting when speed switch is turned off", defaultValue: true, default: true, required: true, displayDuringSetup: true)
	}

	tiles(scale:2) {
		multiAttributeTile(name: "fanSpeed", type: "generic", width: 6, height: 4, canChangeIcon: false) {
			tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "off", action: "switch.on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "1", label: "low", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "2", label: "medium", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#1e9cbb", nextState: "turningOff"
				attributeState "3", label: "high", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#184f9c", nextState: "turningOff"
				attributeState "4", label: "max", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#153591", nextState: "turningOff"
				attributeState "6", label: "Breeze", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#90d2a7", nextState: "turningOff"
				attributeState "turningOff", label:"Turning Fan Off", action: "switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:"Turning Fan On", action: "switch.off", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
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

import physicalgraph.app.ChildDeviceWrapper
import physicalgraph.device.*
import physicalgraph.zigbee.zcl.DataType

private static final int getCHECK_INTERVAL() { 2 * 10 * 60 + 1 * 60 }
private static final int getCHILD_ENPOINT_ID_OFFSET() { 2 }
private static final getBIND_CLUSTER() { 0x8021 }
private static final getFAN_CLUSTER() { 0x0202 }
private static final getFAN_ATTR_ID() { 0x0000 }
private static final getGROUPS_CLUSTER() { 0x0004 }

private final Map getClusterInfo() { [ (zigbee.ONOFF_CLUSTER):[ name: "Switch", dataType: DataType.BOOLEAN, shouldRefresh: true ], (zigbee.LEVEL_CONTROL_CLUSTER):[name: "Level", dataType: DataType.UINT8, shouldRefresh: true], (FAN_CLUSTER):[name:"Fan", dataType: DataType.ENUM8, shouldRefresh: true], (BIND_CLUSTER):[name: "Bind"], 0x0020:[name:"Polling", dataType:DataType.UINT8] ] }
private static final Map getFanSpeeds() { [ 0:[name: "Fan Off", level: 0], 1:[name: "Low", min: 1, max: 49, level: 25], 2:[name: "Medium", min: 50, max: 74], 3:[name: "High", min: 75, max: 99], 4:[name: "Maximum", level: 100], 6:[name: "Breeze", level: 101] ] }
private static final Map getSpeedSwitchTypeInfo() { [namespace: "smartthings", typeName: "Child Switch"] }

private final Map getChildDeviceSpecs() {
	[
		0: [ name: (fanSpeeds[0].name), createChild: false, data: [ fanSpeed: 0 ] ],
		1: [ name: (fanSpeeds[1].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.createSpeedSwitches), componentName: "fanMode1", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 1 ] ],
		2: [ name: (fanSpeeds[2].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.createSpeedSwitches), componentName: "fanMode2", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 2 ] ],
		3: [ name: (fanSpeeds[3].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.createSpeedSwitches), componentName: "fanMode3", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 3 ] ],
		4: [ name: (fanSpeeds[4].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, createChild: (state.createSpeedSwitches), componentName: "fanMode4", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 4 ] ],
		5: [ name: "Off", createChild: false ],
		6: [ name: (fanSpeeds[6].name), typeInfo: (speedSwitchTypeInfo), isComponent: true, required: true, componentName: "fanMode6", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 6 ] ],
		7: [ name: "Light", typeInfo: [namespace: "stussy2112", typeName: "Child Switch Dimmer"], isComponent: false, required: true, componentName: "fanLight", syncInfo: [ (zigbee.ONOFF_CLUSTER):"lightSwitch", (zigbee.LEVEL_CONTROL_CLUSTER):"lightLevel" ] ]
	]
}

public configure() {
	log.debug "Configuring Reporting and Bindings."

	// OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
	int minReportTime = 0
	int maxReportTime = 300
	// NOTE: Configure reporting adds the bindings as well as setting up reporting
	List cmds = zigbee.onOffConfig(minReportTime, maxReportTime) + 
		zigbee.levelConfig(minReportTime, maxReportTime) + 
		zigbee.configureReporting(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, minReportTime, maxReportTime, null)

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

public installed() {
	log.debug "Installed ${device.displayName}"

	state.preferences = [
		createSpeedSwitches: false,
		resumeLast: true,
		offBeforeChange: false
	]
	log.debug "Installed Preferences: ${state.preferences}"
	state.preferences.each {
		log.trace "Installed preference: ${it}"
		device.updateSetting(it.key, it.value)
	}

	state.lastFanSpeed = fanSpeeds.keySet().min()
	response(setFanLevel(fanSpeeds[1].level ?: fanSpeeds[1].min))
}

// parse events into attributes
public parse(String description) {
	log.debug "Parsing '${description}'"

	def events = []
	Map descMap = zigbee.parseDescriptionAsMap(description)
	log.trace descMap
	int cluster = descMap?.clusterInt ?: zigbee.ONOFF_CLUSTER

	def event = zigbee.getEvent(description)
	if (event) {
		log.info "Defined event detected from controller: ${description}"
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
	else if (FAN_CLUSTER == cluster) {
		log.info "Fan message detected from controller: ${description}"
		// handle 'fanSpeed' attribute
		if (description.startsWith("read attr -") && FAN_ATTR_ID == descMap?.attrInt) {
			events = createFanEvents((Math.max(Integer.parseInt(descMap.value), 0)))
		}
		else if (description.startsWith("catchall:")) {
			// do a refresh
			sendHubCommand(zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID).collect { new HubAction(it) })
			return
		}
	} else if (description.startsWith("catchall:") && BIND_CLUSTER == cluster) {
		log.info "Bind message detected from controller: ${description}"
		configureHealthCheck()
		return
	}

	events.each { syncChildDevices(cluster, it) }

	log.trace "'parse()' returning: ${events}"
	return events
}

public updated() {
	log.debug "Updating ${device.displayName} : ${device.deviceNetworkId}"

	// If the speed switches were previously created and user is selecting 'false', remove the switches
	if (!createSpeedSwitches && state.preferences && state.preferences["createSpeedSwitches"]) {
		log.debug "Should delete children"
		deleteChildren()
	}

	state.preferences = [
		createSpeedSwitches: (createSpeedSwitches == null ? false : createSpeedSwitches),
		resumeLast: (resumeLast == null ? true : resumeLast),
		offBeforeChange: (offBeforeChange == null ? false : offBeforeChange)
	]
	log.debug "Updated Preferences: ${state.preferences}"
	state.preferences.each {
		log.trace "Updated preference: ${it}"
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
public fanOn() {
	int speed = state.preferences.resumeLast.toBoolean() ? state.lastFanSpeed?.intValue() ?: 0 : 1    
	return setFanSpeed(speed)
}

public fanOff() {
	return setFanSpeed(0)
}

public lightOff() {
    return zigbee.off()
}

public lightOn(int endpoint = 1) {
    return zigbee.on()
}

public lowerFanSpeed() {
	//log.debug "Executing 'lowerFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
    return setFanSpeed(currentSpeed - 1)
}

public on() {
	log.debug "Executing 'on'"
	return fanOn()
}

public off() {
	log.debug "Executing 'off'"
	return fanOff()
}

public raiseFanSpeed() {
	//log.debug "Executing 'raiseFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
	return setFanSpeed(currentSpeed + 1)
}

public refresh(endpoints = [1]) {
	log.debug "Executing 'refresh()': endpoints = ${endpoints}"
	List<String> cmds = zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)
	log.trace "'refresh()' returning: ${cmds}"
	return cmds
}

public setLevel(level, rate = null) {
	log.debug "Executing 'setLevel': level = ${level}"
	return setFanLevel(level)
}

public setFanLevel(int level) {
	log.debug "Executing 'setFanLevel': level = ${level}"
	int fanNow = Math.max(Integer.parseInt(device.currentValue("fanLevel") ?: "0"), 0)
	level = Math.max(Math.min(level.intValue(), fanSpeeds.values().collect { it.max ?: it.level }.max()), fanSpeeds.values().collect { it.min ?: it.level }.min())   
	log.trace "Requested fanLevel is ${level}. Current fanLevel is ${fanNow}"

	List<String> cmds = []
	// only update if the new fan level is different than the current fan level
	if (level != fanNow) {
		state.lastFanLevel = fanNow    //save fan level before changing so it can be resumed when turned back on    
		sendEvent(name: "fanLevel", value: level) // NOTE: This is necessary b/c fan level is not defined in any capability
		cmds = setFanSpeed(fanSpeeds.find { it.value.level == level || it.value.min <= level && it.value.max >= level }?.key)
	} else {
		cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)//.collect { new HubAction(it) }
	}

	return cmds
}

public setFanSpeed(int speed) {
	log.debug "Executing 'setFanSpeed': speed = ${speed}"
	int fanNow = Math.max(device.currentValue("fanSpeed")?.intValue() ?: 0, 0)
	List<Integer> speeds = fanSpeeds.keySet() as List<Integer>
	speed = Math.max(Math.min(speed.intValue(), speeds.max()), speeds.min())   
	log.debug "Requested fanSpeed is ${speed}. Current fanLevel is ${fanNow}"

	def cmds = []
	
	if (speed != fanNow) {
		state.lastFanSpeed = fanNow
		cmds = zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", speed))
	} else {
		cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)
	}

	log.trace "'setFanSpeed()' returning: ${cmds}"
	return cmds
}

public setLightLevel(int level, int rate = 0) {
	log.debug "Executing 'setLevel': level = ${level}, rate = ${rate}"
	return zigbee.setLevel(level, rate) + zigbee.levelRefresh() + zigbee.onOffRefresh()
}

/**
  * Child handling
  */
public childOff(String dni) {
	//log.debug "Executing 'childOff(): ${dni}"

	List<HubAction> cmds = []

	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		// Determine the cluster that needs to be controlled
		List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work
		if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
			//int endpoint = getChildDeviceEndpoint(childDevice)
			cmds = lightOff()
		} else if (clusters.contains(FAN_CLUSTER)) {
			boolean shouldResume = state.preferences["resumeLast"].toBoolean() && 0 < state.lastFanSpeed
			cmds = shouldResume ? fanOn() : fanOff()
		}
	}
	
	//log.trace "'childOff()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

public childOn(String dni) {
	//log.debug "Executing 'childOn(): ${dni}"

	List<HubAction> cmds = []

	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		// Determine the cluster that needs to be controlled
		List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }  // NOTE: Single quotes necessary for this to work
		if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
			//int endpoint = getChildDeviceEndpoint(childDevice)
			cmds = lightOn()
		} else if (clusters.contains(FAN_CLUSTER)) {
			cmds = setFanSpeed(Integer.parseInt(childDevice.getDataValue('fanSpeed')))
		}
	}

	//log.trace "'childOn()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

public childRefresh(String dni) {
	//log.debug "Executing 'childRefresh(): ${dni}"

	List<HubAction> cmds = []

	ChildDeviceWrapper childDevice = getChildDevice(dni)
	if (childDevice) {
		cmds = refresh([getChildDeviceEndpoint(childDevice)])
	}

	//log.trace "'childRefresh()': Sending ${cmds} for child ${childDevice}"
	sendHubCommand(cmds)
}

public childSetLevel(String dni, Number level, Number rate = null) {
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

private void configureHealthCheck() {
	log.debug "Executing 'createHealthCheckEvent()'"

	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	Map healthEvent = [name: "checkInterval", value: CHECK_INTERVAL, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
	sendEvent(healthEvent)
	getChildDevices().each {
		it.sendEvent(healthEvent)
	}
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

	childDeviceSpecs.findAll { it.value.createChild || it.value.required }
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

private List<Map> createFanEvents(int fanSpeed = null) {
	log.debug "Executing 'createFanEvents()': fanSpeed = ${fanSpeed}"

	fanSpeed = Math.max((fanSpeed == null ? Integer.parseInt(device.currentValue("fanSpeed")) : fanSpeed), 0)
	// Adjust the fan level to insure that it falls in the correct range
	int minLevel = fanSpeeds[fanSpeed].min ?: fanSpeeds[fanSpeed].level
	int maxLevel = fanSpeeds[fanSpeed].max ?: fanSpeeds[fanSpeed].level
	//int fanLevel = Math.max(Math.min(Integer.parseInt(device.currentValue("fanLevel") ?: "0"), maxLevel), minLevel)
	int fanLevel = device.currentValue("fanLevel")?.intValue() ?: 0
	if (!(minLevel..maxLevel).contains(fanLevel)) {
		fanLevel = fanSpeeds[fanSpeed].level ?: minLevel
	}

	String fanSwitch = 0 < fanSpeed ? "on" : "off"
	List<Map> events = [
		//createEvent(name: "fanSwitch", value: fanSwitch),
		createEvent(name: "fanSpeed", value: fanSpeed),
		createEvent(name: "fanLevel", value: fanLevel),
		createEvent(name: "switch", value: fanSwitch),
		createEvent(name: "level", value: fanLevel)
	]

	log.trace "'createFanEvents()' returning: ${events}"
	return events
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

private ChildDeviceWrapper getChildDevice(String deviceNetworkId) {
	ChildDeviceWrapper child = getChildDevices().find { it.deviceNetworkId == deviceNetworkId }
	if (!child) {
		log.error "Child device ${deviceNetworkId} not found"
	}
	return child
}

private int getChildDeviceEndpoint(ChildDeviceWrapper childDevice) {
	return childDevice?.getDataValue('endpointId') as Integer
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