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
 *  , vid:"x.com.st.fanspeed", vid:"generic-dimmer"
 */
metadata {
	definition (name: "Hampton Bay Fan Controller", namespace: "stussy2112", author: "Sean Williams", runLocally: false, executeCommandsLocally: false, ocfDeviceType: "oic.d.fan", mcdSync: true, mnmn: "SmartThings", vid:"x.com.st.fanspeed", minHubCoreVersion: '000.025.00000') {
		capability "Actuator"
		capability "Switch"
		capability "Switch Level"
		capability "Fan Speed"
		capability "Configuration"
		capability "Refresh"
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

		attribute "fanState", "string"
        attribute "lightState", "string"
		attribute "fanLevel", "number"
        attribute "lightLevel", "number"
        
	  	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", manufacturer: "King Of Fans, Inc.", model: "HDC52EastwindFan", deviceJoinName: "Hampton Bay Fan Controller"
	}

  	preferences {
        input("resumeLastFromBreeze", "bool", title: "Resume Last Setting when 'Breeze' turned off", defaultValue: true, required: false, displayDuringSetup: true)
        input("createSpeedSwitches", "bool", title: "Create speed switches", defaultValue: false, required: false, displayDuringSetup: true)
  	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "fanSpeed", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "off", action: "fanOn", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "adjusting"
				attributeState "1", label: "low", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "2", label: "medium", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#1e9cbb", nextState: "adjusting"
				attributeState "3", label: "high", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#184f9c", nextState: "adjusting"
				attributeState "4", label: "max", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#153591", nextState: "adjusting"				
				attributeState "6", label: "Breeze", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#90d2a7", nextState: "adjusting"
				attributeState "adjusting", label: "Adjusting...", action: "fanOff", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
				attributeState "turningOff", label:"Turning Fan Off", action: "fanOff", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
                attributeState "turningOn", label:"Turning Fan On", action: "fanOn", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
			}
			tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "raiseFanSpeed"
				attributeState "VALUE_DOWN", action: "lowerFanSpeed"
			}
			tileAttribute ("lightLevel", key: "SLIDER_CONTROL", label: "Brightness") {
				attributeState "lightLevel", action:"setLightLevel"
			}
		}
        
        childDeviceTiles("all", width: 6, height: 1)

    	standardTile("refresh", "device.refresh", decoration: "flat", width: 6, height: 1) {
		  state "default", label:"refresh", action:"refresh", icon:"st.secondary.refresh"
	  	}
	}
}

// Imports
import physicalgraph.app.ChildDeviceWrapper
import physicalgraph.device.cache.DeviceDTO
import physicalgraph.device.*
import physicalgraph.zigbee.zcl.DataType

// Globals
private int getDEFAULT_DELAY() { 100 }
private getBIND_CLUSTER() { 0x8021 }
private int getCHILD_ENPOINT_ID_OFFSET() { 2 }
private getFAN_CLUSTER() { 0x0202 }
private getFAN_ATTR_ID() { 0x0000 }
private getGROUPS_CLUSTER() { 0x0004 }
private getSCENES_CLUSTER() { 0x0005 }
private getHOME_AUTOMATION_CLUSTER() { 0x0104 }

private Map getChildDeviceSpecs() {
	[
    	0: [ name: (fanSpeeds[0].name), createChild: false, data: [ fanSpeed: 0 ] ],
        1: [ name: (fanSpeeds[1].name), required: false, typeName: "Hampton Bay Fan Speed Child Device", isComponent: true, createChild: true, componentName: "fanMode1", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 1 ] ],
        2: [ name: (fanSpeeds[2].name), required: false, typeName: "Hampton Bay Fan Speed Child Device", isComponent: true, createChild: true, componentName: "fanMode2", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 2 ] ],
        3: [ name: (fanSpeeds[3].name), required: false, typeName: "Hampton Bay Fan Speed Child Device", isComponent: true, createChild: true, componentName: "fanMode3", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 3 ] ],
        4: [ name: (fanSpeeds[4].name), required: false,typeName: "Hampton Bay Fan Speed Child Device", isComponent: true, createChild: true, componentName: "fanMode4", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 4 ] ],
        5: [ name: "Off", required: false, createChild: false ],
        6: [ name: (fanSpeeds[6].name), required: true, typeName: "Hampton Bay Fan Speed Child Device", isComponent: true, createChild: true, componentName: "fanMode6", syncInfo: [ (FAN_CLUSTER):"fanSpeed"], data: [ fanSpeed: 6 ] ],
        7: [ name: "Light", required: true, typeName: "Hampton Bay Fan Light", isComponent: false, createChild: true, componentName: "fanLight", syncInfo: [ (zigbee.ONOFF_CLUSTER):"lightState", (zigbee.LEVEL_CONTROL_CLUSTER):"lightLevel" ] ]
    ]
}

private Map getCommandInfo() { [ 0x00:"Read", 0x01:"Read Response", 0x02:"Write", 0x04:"Write Response", 0x05:"Write, No Response", 0x06:"Configure Reporting", 0x07:"Configure Reporting Response", 0x0B:"Default Response" ] }
private Map getClusterInfo() { [ (zigbee.ONOFF_CLUSTER):[ name: "Switch", dataType: DataType.BOOLEAN ], (zigbee.LEVEL_CONTROL_CLUSTER):[name: "Level", dataType: DataType.UINT8], (FAN_CLUSTER):[name:"Fan", dataType: DataType.ENUM8], (BIND_CLUSTER):[name: "Bind"], 0x0020:[name:"Polling", dataType:DataType.UINT8] ] }
private Map getBindResults() { [ 0:"Success", 132:"Not Supported", 130:"Invalid Endpoint", 140:"Table Full" ] }
private Map getFanSpeeds() { [ 0:[name: "Fan Off", threshold: 0], 1:[name: "Low", threshold: 33], 2:[name: "Medium", threshold: 66], 3:[name: "High", threshold: 99], 4:[name: "Max", threshold: 100], 6:[name: "Breeze", threshold: 101] ] }

def getInitialEndpoint () { Integer.parseInt(zigbee.endpointId, 10) }

def configure() {
	log.debug "Configuring Reporting and Bindings."
    
	configureHealthCheck()
    
    // OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
    Integer minReportTime = 0
    Integer maxReportTime = 300
    def cmds = [
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
        //"send 0x${device.deviceNetworkId} 1 1", "delay 100",
	  	//Get current values from the device
        refresh()
    ].flatten()
    
    // Add bindings and reporting for the child devices that control the on/off cluster
    cmds << findChildDevicesByClusterData(zigbee.ONOFF_CLUSTER).collect {
    	zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, 0x10, minReportTime, maxReportTime, null, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
    }.flatten()
    
    cmds << findChildDevicesByClusterData(zigbee.LEVEL_CONTROL_CLUSTER).collect {
    	zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, 0x10, minReportTime, maxReportTime, null, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
    }.flatten()
    
    log.trace "'configure()' returning: ${cmds}"
    return cmds
}

def installed() {
	log.debug "Installed ${device.displayName}"
    
    state.resumeLastFromBreeze = true
    device.updateSetting("resumeLastFromBreeze", true)
    state.createSpeedSwitches = false
    device.updateSetting("createSpeedSwitches", false)
    state.lastFanSpeed = fanSpeeds.keySet().min()
    
	if ((null == device.currentState("lightLevel")?.value) || (0 == device.currentState("lightLevel")?.value)) {
        setLightLevel(100)
	}
    
    setFanLevel(fanSpeeds[1].threshold)
    for (i in 1..3) {
    	lightOn()
        lightOff()
    }
    
    response(configure())
}

// parse message from device into events that SmartThings platform can understand
def parse(String description) {
	log.debug "Parsing '${description}'"
    
    List<Map> events = []
    
    Map event = zigbee.getEvent(description)
    def descMap = zigbee.parseDescriptionAsMap(description)
    int cluster = descMap?.clusterInt ?: zigbee.ONOFF_CLUSTER
    if (event) {
    	log.info "Defined event detected from controller"
		// NOTE: For certain descriptions, the values for the events need to be changed to report correctly
        // To handle this, send the correct events for the switch and level clusters to the child light device
        /*
        	switch - This should be the on/off state of the FAN (zigbee.OnOffRefresh gives the LIGHT state). Create a "lightState" event instead
            level - This should be the level of the FAN (zigbee.levelRefresh gives the LIGHT level. Create a "lightLevel" event instead
            on/off - This is the switch event for the LIGHT. Send this directly, as it will not update any attribute on the handler
        */
        Map eventNameMap = [ "switch":"lightState", "level":"lightLevel"]
        if (description.startsWith("on/off")) {
        	// Update the lightState with the event value
            // NOTE: This is necessary b/c "on/off" doesn't update any value on the handler
            sendEvent(name: eventNameMap[event.name], value: event.value)        	
        }
        
        events << createEvent(name: eventNameMap[event.name], value: event.value)
    } else if (description?.startsWith('read attr -') && FAN_CLUSTER == cluster && FAN_ATTR_ID == descMap?.attrInt) {
		// handle 'fanSpeed' attribute
        log.info "Fan message detected from controller: ${description}"
        Number fanSpeed = Math.max(Integer.parseInt(descMap.value), 0)
        events << createFanEvents((Math.max(Integer.parseInt(descMap.value), 0)))
    } else {
    	events << parseCatchAll(description)
    }
    events = events?.flatten()
    events?.each { syncChildDevices(cluster, it) }
    
	if (0 >= events?.size()) {
    	log.warn "DID NOT PARSE MESSAGE for description : ${description}"
    }
    
    log.debug "'parse()' returning: ${events}"
    return events
}

def updated() {
	log.debug "Updating ${device.displayName} : ${device.deviceNetworkId}"
    
    state.resumeLastFromBreeze = resumeLastFromBreeze
    log.debug "Resume from breeze: entered = ${resumeLastFromBreeze}, actual = ${state.resumeLastFromBreeze}"
    boolean removeSpeedSwitches = !createSpeedSwitches && state.createSpeedSwitches
    log.trace "Remove Speed Switches: ${removeSpeedSwitches}"
    state.createSpeedSwitches = createSpeedSwitches
    log.debug "Create Speed Switches: entered = ${createSpeedSwitches}, actual = ${state.createSpeedSwitches}"
    
    if (removeSpeedSwitches) {
    	log.trace "Should delete children"
		deleteChildren()
    }
    
	if (!getChildDevices() || state.createSpeedSwitches) {
    	log.trace "Should create children"
    	createChildDevices()
    } else if (state.oldLabel != device.label) {
    	log.trace "Updating child labels"
        getChildDevices().each { it.label = "${device.displayName} ${findChildDeviceSpecs(it)?.name}" }
    	state.oldLabel = device.label
    }
    
    response(refresh() + configure())
}

// handle commands
def fanOff() {
	log.debug "Executing 'fanOff'";
    return setFanLevel(0)
}

def fanOn() {
	log.debug "Executing 'fanOn'";
    return setFanLevel((state.lastFanLevel == null ? fanSpeeds[1].threshold : state.lastFanLevel))
}

public List<HubAction> lightOff() {
    log.debug "Executing 'lightOff()'"
    //sendEvent(name: "lightState", value: "off")
	//List<String> cmds = ["st cmd 0x${device.deviceNetworkId} 0x01 0x0006 0x00 {}", "delay ${DEFAULT_DELAY}"].flatten()
    List cmds = [ zigbee.off() ].flatten()
    return cmds.collect { new HubAction(it) }
}

public List<HubAction> lightOn() {
    log.debug "Executing 'lightOn()'"
    //sendEvent(name: "lightState", value: "on")
    //List<String> cmds = ["st cmd 0x${device.deviceNetworkId} 0x01 0x0006 0x01 {}", "delay ${DEFAULT_DELAY}"].flatten()
	List cmds = [ zigbee.on() ].flatten()
    return cmds.collect { new HubAction(it) }
}

def on() {
	log.debug "Executing 'on'"
    return fanOn()
}

def off() {
	log.debug "Executing 'off'"
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

def refresh() {
	log.debug "Executing 'refresh()'"
    List<HubAction> cmds = [ refreshLight(), refreshFan() ].flatten()
    getChildDevices()?.each { it.refresh() }
    //List cmds = [ zigbee.onOffRefresh(), zigbee.levelRefresh(), refreshFan() ].flatten()
    return cmds
}

public List<HubAction> setLightLevel(Number value, rate = null) {
    Integer lightNow = Math.max(device.currentValue("lightLevel")?.intValue() ?: 0, 0)    //save fanspeed before changing speed so it can be resumed when turned back on    
	log.debug "Requested lightLevel is ${value}. Current lightLevel is ${lightNow}"

    Integer level = Math.max(Math.min(value.intValue(), 100), 20)
    rate = Math.max(Math.min((rate == null ? 0 : rate.intValue()), 100), 0)
    
    state.lastLightLevel = lightNow
    
    log.info "Adjusting Light Brightness: ${level} : ${rate}"
    List cmds = [ zigbee.setLevel(level, rate) ].flatten()
    
    return cmds.collect { new HubAction(it) }
}

public List<HubAction> setFanLevel(Number level) {
    Number fanNow = Math.max(device.currentValue("fanLevel")?.intValue() ?: 0, 0)
    List<Integer> thresholds = fanSpeeds.values().collect { it.threshold }
    level = Math.max(Math.min(level.intValue(), thresholds.max()), thresholds.min())   
	log.debug "Requested fanLevel is ${level}. Current fanLevel is ${fanNow}"
    
    List<HubAction> cmds = []
    // only update if the new fan level is different than the current fan level
    if (level != fanNow) {
    	state.lastFanLevel = fanNow
    	sendEvent(name: "fanLevel", value: level) // NOTE: This is necessary
    	cmds << setFanSpeed(findFanSpeedByLevel(level))
    }
    
    return cmds
}

public List<HubAction> setFanSpeed(Number speed) {
    int fanNow = Math.max(device.currentValue("fanSpeed")?.intValue() ?: 0, 0)    //save fanspeed before changing speed so it can be resumed when turned back on    
	log.debug "Requested fanSpeed is ${speed}. Current Fan speed is ${fanNow}"	    
        
    List<HubAction> cmds = []
    def keySet = fanSpeeds.keySet()
    speed = Math.max(Math.min(speed?.intValue() ?: 0, keySet.max()), keySet.min())
    // Set the attribute values
    createFanEvents(speed).each { sendEvent(it) } // NOTE: This is necessary to get the reporting of values correct
    // only update if the new fan speed is different than the current fan speed
    if (speed != fanNow) {
        state.lastFanSpeed = fanNow        
	    log.info "Adjusting Fan Speed to ${fanSpeeds[speed].name}"
        cmds << zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", speed))
        //cmds << "st wattr 0x${device.deviceNetworkId} 0x01 ${FAN_CLUSTER} ${FAN_ATTR_ID} 0x0030 {${String.format("%02d", setSpeed)}}" << "delay ${DEFAULT_DELAY}"
    }
    cmds = cmds?.flatten() 
    return cmds?.collect { new HubAction(it) }
}

def setLevel(value, rate = null) {
	log.debug "Executing 'setLevel()': ${value}, ${rate}"
    return setFanLevel(value)
}

public List<HubAction> raiseFanSpeed() {
	log.debug "Executing 'raiseFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (fanSpeeds.keySet().max() == currentSpeed) { currentSpeed = fanSpeeds.keySet().min() - 1 }     
    return setFanLevel(findFanLevelBySpeed(currentSpeed + 1))
}

public List<HubAction> lowerFanSpeed() {
	log.debug "Executing 'lowerFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0    
    if (fanSpeeds.keySet().max() == currentSpeed) { currentSpeed-- }
    return setFanLevel(findFanLevelBySpeed(currentSpeed - 1))
}

/**
  *  Child handling
  */
public void childOff(DeviceDTO childDevice) {
	log.debug "Executing 'childOff(): ${childDevice}"
    
    List<HubAction> cmds = []
    
    if (childDevice) {
    	// Determine the cluster that needs to be controlled
        List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work
        if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
            cmds << lightOff()
        } else if (clusters.contains(FAN_CLUSTER)) {
            boolean resumeRequested = state.resumeLastFromBreeze
            boolean hasLastFanSpeed = 0 < state.lastFanSpeed
            boolean shouldResume = resumeRequested && fanSpeeds.keySet().max() == Integer.parseInt(childDevice.getDataValue('fanSpeed')) && hasLastFanSpeed
            log.trace "shouldResume: ${resumeRequested} - ${hasLastFanSpeed} - ${shouldResume}"
            cmds << (shouldResume ? fanOn() : fanOff())
        }
    }
    
    cmds = cmds?.flatten()
    log.trace "'childOff()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)   
}

public void childOn(DeviceDTO childDevice) {
	log.debug "Executing 'childOn(): ${childDevice}"
    
    List<HubAction> cmds = []
    
    if (childDevice) {
    	// Determine the cluster that needs to be controlled
        List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }  // NOTE: Single quotes necessary for this to work
        
        if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
            cmds << lightOn()
        } else if (clusters.contains(FAN_CLUSTER)) {
            cmds << setFanLevel(findFanLevelBySpeed(Integer.parseInt(childDevice.getDataValue('fanSpeed'))))
        }
    }
        
    cmds = cmds?.flatten()
    log.trace "'childOn()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)
}

public void childRefresh(DeviceDTO childDevice) {
	log.debug "Executing 'childRefresh(): ${childDevice}"
    
    List<HubAction> cmds = []
    
    if (childDevice) {
    	// Determine the cluster that needs to be controlled
        List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work
        cmds << (clusters.disjoint([zigbee.ONOFF_CLUSTER, zigbee.LEVEL_CONTROL_CLUSTER]) ? refreshFan() : refreshLight())
    }
    
    cmds = cmds?.flatten()
    log.trace "'childRefresh()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)
}

def childSetLevel(DeviceDTO childDevice, Number level, Number rate = null) {
	log.debug "Executing 'childSetLevel(): ${childDevice}, level = ${level}, rate = ${rate}"
    
    List<HubAction> cmds = []
    
    if (childDevice) {
    	// Determine the cluster that needs to be controlled
        List<String> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work
        
        if (clusters.contains(zigbee.LEVEL_CONTROL_CLUSTER)) {
            cmds << setLightLevel(level, rate)
        } else if (clusters.contains(FAN_CLUSTER)) {
            cmds << setFanLevel(level, rate)
        }
    }
    
    cmds = cmds?.flatten()
    log.trace "'childSetLevel()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)
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
    childDeviceSpecs.findAll {
        (state.createSpeedSwitches && it.value.createChild) || it.value.required
    }
    .each { key, value ->
    	int endPointId = key.intValue() + CHILD_ENPOINT_ID_OFFSET
        String networkId = "${device.deviceNetworkId}-ep${endPointId}"
        Map data = (value.data ?: [:]) << [endpointId: "${endPointId}", required: value.required, requested: state.createSpeedSwitches, clusters: (value.syncInfo?.collect { "${it.key}" }?.join(",")), manufacturer: "King of Fans, Inc." ]
        Map properties = [completedSetup: true, label: "${device.displayName} ${value.name}", isComponent: value.isComponent, componentName: value.componentName, componentLabel: value.name, data: data ]
        ChildDeviceWrapper childDevice = createChildDevice("stussy2112", value.typeName, networkId, properties)
    }
    
    syncChildDevices(FAN_CLUSTER, [name: "fanSpeed", value: device.currentValue("fanSpeed")])
    syncChildDevices(zigbee.ONOFF_CLUSTER, [name: "lightState", value: device.currentValue("lightState")])
    syncChildDevices(zigbee.LEVEL_CONTROL_CLUSTER, [name: "lightLevel", value: device.currentValue("lightLevel")])
}

private List<Map> createFanEvents(Number fanSpeed = null) {
    log.debug "Executing 'createFanEvents()': fanSpeed = ${fanSpeed}"
    
    fanSpeed = Math.max((fanSpeed == null ? device.currentValue("fanSpeed") : fanSpeed)?.intValue() ?: 0, 0)
    String fanState = 0 < fanSpeed ? "on" : "off"
    // Adjust the fan level to insure that it falls in the correct range
    int fanLevel = Math.max(device.currentValue("fanLevel")?.intValue() ?: 0, 0)
    int maxLevel = findFanLevelBySpeed(fanSpeed)
    int minLevel = findFanLevelBySpeed(fanSpeed - 1)
    if (!((minLevel + 1)..maxLevel).contains(fanLevel)) {
    	fanLevel = maxLevel
    }
    //fanLevel = Math.max(Math.min(fanLevel, maxLevel), minLevel + 1)
    if (1 == fanLevel) { fanLevel = 0 }
    List<Map> events = [
        createEvent(name: "fanSpeed", value: fanSpeed),
        createEvent(name: "fanState", value: fanState),
        createEvent(name: "fanLevel", value: fanLevel),
        createEvent(name: "switch", value: fanState),
        createEvent(name: "level", value: fanLevel)
    ]
    
    log.trace "'createFanEvents()' returning: ${events}"
    return events?.flatten()
}

private Map configureHealthCheck() {
	log.debug "Executing 'createHealthCheckEvent()'"
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
    Map healthEvent = createEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    getChildDevices().each {
    	it.sendEvent(healthEvent)
    }
    
    sendEvent(healthEvent)
    log.trace "configureHealthCheck() returning: ${healthEvent}"
    return healthEvent
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

private List<ChildDeviceWrapper> findChildDevicesByClusterData(Integer cluster) {
	return getChildDevices().findAll { it.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }.contains(cluster) }
}

private Map findChildDeviceSpecs(ChildDeviceWrapper child) {
	return childDeviceSpecs[Integer.parseInt(child.getDataValue('endpointId')) - CHILD_ENPOINT_ID_OFFSET]
}

private int findFanLevelBySpeed(Number speed) {
    Number correctedSpeed = Math.max(speed.intValue(), 0)
    def keySet = fanSpeeds.keySet()
    // Check that the speed is within the range
    //correctedSpeed = (keySet.min()..keySet.max()).contains(correctedSpeed) ? keySet.contains(correctedSpeed) ? correctedSpeed : keySet[-2] : keySet.min()
    int checkValue = Math.max(Math.min(correctedSpeed, keySet.max()), keySet.min());
    return fanSpeeds.find { it.key.intValue() >= checkValue }.value.threshold
}

private int findFanSpeedByLevel(Number level) {
    Number correctedValue = Math.max(level.intValue(), 0)
	List<Integer> thresholds = fanSpeeds.values().collect { it.threshold }
    int checkValue = Math.max(Math.min(correctedValue, thresholds.max()), thresholds.min())
    return fanSpeeds.find { it.value.threshold.intValue() >= checkValue }.key
}

private ChildDeviceWrapper getChildDevice(Integer deviceSpecKey) { 
	ChildDeviceWrapper child = getChildDevices().find { Integer.parseInt(it.deviceNetworkId[-1]) == deviceSpecKey + 2 }
    if (!child) {
        log.error "Child device for ${deviceSpecKey} not found"
    }
    return child
}

private ChildDeviceWrapper getChildDevice(String deviceNetworkId) {
    ChildDeviceWrapper child = getChildDevices().find { it.deviceNetworkId == deviceNetworkId }
    if (!child) {
        log.error "Child device ${deviceNetworkId} not found"
    }
    return child
}

private List parseCatchAll(String description) {
	log.debug "Executing 'parseCatchAll()': ${description}"
	List events = []
    
    if (description?.startsWith("catchall:")) {
        def parsed = zigbee.parse(description)
        log.trace "Parsed catchall: ${parsed}"
        
        // profile id for responses is 0104 (0x0104) - Home Automation
        if (HOME_AUTOMATION_CLUSTER == parsed.profileId) {
            if (0 < parsed.data.size()) {
        		// if the data payload is '0', all was good and there is nothing to do
            	if (0x0000 == parsed.data[0]) {
                	log.info "${clusterInfo[parsed.clusterId].name} responded with ${commandInfo[parsed.command.intValue()]}."
                } else {
                	log.warn "${clusterInfo[parsed.clusterId].name} ${commandInfo[parsed.command.intValue()]} returned ${parsed.data}"
                }
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
						events << createEvent(name:"lightState", value: 0x00 == parsed.data[-1] ? "off" : "on")
                        break
                    case (zigbee.LEVEL_CONTROL_CLUSTER):                    	
                        break					
                    case (FAN_CLUSTER):
                    	//Fan change
                    	//catchall: 0104 0202 01 01 0000 00 77E9 00 00 0000 04 01 00
    					//SmartShield(text: null, manufacturerId: 0x0000, direction: 0x01, data: [0x00], number: null, isManufacturerSpecific: false, messageType: 0x00, senderShortId: 0x77e9, isClusterSpecific: false, sourceEndpoint: 0x01, profileId: 0x0104, command: 0x04, clusterId: 0x0202, destinationEndpoint: 0x01, options: 0x0000)
                    	//[raw:0104 0202 01 01 0000 00 77E9 00 00 0000 04 01 00, profileId:0104, clusterId:0202, sourceEndpoint:01, destinationEndpoint:01, options:0000, messageType:00, dni:77E9, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:04, direction:01, data:[00], clusterInt:514, commandInt:4]                                   
                        List<Map> fanEvents = []
                        // If the event was successfull AND it was a write command response
                        if (0x00 == parsed.data[-1] && 0x04 != parsed.command) {
                            fanEvents << createFanEvents()
                        }
                    	events << fanEvents
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
                    log.debug "Bind response: ${commandInfo[parsed.command]}"
                    events << configureHealthCheck()
                    //cmds << [ name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID] ]
                }
            }
        }
    }
    
    events = events.flatten().findAll { it.isStateChange.toBoolean() }
    log.debug "'parseCatchAll' returning ${events}"
    return events
}

private List<HubAction> refreshFan() {
    //List cmds = [ "st rattr 0x${device.deviceNetworkId} 0x01 0x0202 0x0000" ]
    List cmds = [ zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID) ].flatten()
    return cmds.collect { new HubAction(it) }
}

private List<HubAction> refreshLight() {
	//List cmds = ["st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x0000", "delay ${DEFAULT_DELAY}", "st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.LEVEL_CONTROL_CLUSTER} 0x0000"]    
    List cmds = [ zigbee.onOffRefresh(), zigbee.levelRefresh() ].flatten()
	return cmds.collect { new HubAction(it) }
}

private void syncChildDevices(Integer cluster, Map event) {
	log.debug "'syncChildDevices(): cluster = ${cluster}, event = ${event}"
    
    Map eventNameMap = [ "lightState":"switch", "lightLevel":"level", "fanSpeed":"switch", "fanLevel":"level"]
    findChildDevicesByClusterData(cluster).findAll{event.name == findChildDeviceSpecs(it)?.syncInfo[cluster]}
    .each {
        Map childEvent = FAN_CLUSTER == cluster
        	? it.createEvent(name:eventNameMap[event.name], value:((event.value as int) == Integer.parseInt(it.getDataValue('fanSpeed')) ? "on" : "off"))
        	: it.createEvent(name:eventNameMap[event.name], value:event.value)
        log.debug "Sending ${childEvent} to child ${it}"
        it.sendEvent(childEvent)
    }
}