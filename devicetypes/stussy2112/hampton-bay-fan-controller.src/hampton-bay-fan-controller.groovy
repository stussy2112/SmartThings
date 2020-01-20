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
	definition (name: "Hampton Bay Fan Controller", namespace: "stussy2112", author: "Sean Williams", runLocally: false, executeCommandsLocally: false, ocfDeviceType: "oic.d.fan", mnmn: "SmartThings", vid:"x.com.st.fanspeed", minHubCoreVersion: '000.025.00000') {
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

		//attribute "fanSwitch", "string"
        attribute "lightSwitch", "string"
		attribute "fanLevel", "number"
        attribute "lightLevel", "number"
        
	  	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", manufacturer: "King Of Fans, Inc.", model: "HDC52EastwindFan", deviceJoinName: "Hampton Bay Fan Controller"
	}

  	preferences {
		input (name:"offBeforeChange", type:"bool", title: "Turn fan off before changing speed", description: "Some fans need to go to the 'off' state before selecting a new speed", defaultValue: true, default: false, required: true, displayDuringSetup: true)
        //input (name:"supportedSpeeds", type:"enum", title:"Supported fan speeds", description: "Some fans do not support all the speeds provided by the controller. Please select the speeds that your fan supports.", options: [[1:"Low"], [2:"Medium"], [3:"High"], [4:"Maximum"], [6:"Breeze"]], multiple: true, required: true, displayDuringSetup: true)
        input (name:"createSpeedSwitches", type:"bool", title:"Create speed switches", description: "Additional child switches will be created for the supported speeds of your fan.", defaultValue: false, default: false, required: true, displayDuringSetup: true)
        input (name: "resumeLast", type:"bool", title: "Resume Last Setting when speed switch is turned off", defaultValue: false, default: false, required: true, displayDuringSetup: true)
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "fanSpeed", type: "lighting", width: 6, height: 4, canChangeIcon: false) {
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
//import physicalgraph.device.cache.DeviceDTO
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
private static final Map getFanSpeeds() { [ 0:[name: "Fan Off", threshold: 0], 1:[name: "Low", threshold: 33], 2:[name: "Medium", threshold: 66], 3:[name: "High", threshold: 99], 4:[name: "Maximum", threshold: 100], 6:[name: "Breeze", threshold: 101] ] }
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
        7: [ name: "Light", typeInfo: [namespace: "stussy2112", typeName: "Hampton Bay Fan Light"], isComponent: false, required: true, componentName: "fanLight", syncInfo: [ (zigbee.ONOFF_CLUSTER):"lightSwitch", (zigbee.LEVEL_CONTROL_CLUSTER):"lightLevel" ] ]
    ]
}

private final Map getClusterInfo() { [ (zigbee.ONOFF_CLUSTER):[ name: "Switch", dataType: DataType.BOOLEAN, shouldRefresh: true ], (zigbee.LEVEL_CONTROL_CLUSTER):[name: "Level", dataType: DataType.UINT8, shouldRefresh: true], (FAN_CLUSTER):[name:"Fan", dataType: DataType.ENUM8, shouldRefresh: true], (BIND_CLUSTER):[name: "Bind"], 0x0020:[name:"Polling", dataType:DataType.UINT8] ] }
private final List<Integer> getDefaultSupportedSpeeds() { fanSpeeds.keySet() as int[] }
private int getInitialEndpoint () { Integer.parseInt(zigbee.endpointId, 10) }

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
    ]
    
    // Add bindings and reporting for the child devices that control the on/off cluster
    cmds += findChildDevicesByClusterData(zigbee.ONOFF_CLUSTER).collect {
    	log.debug "Configuring ON/OFF reporting for ${it}"
    	zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, DataType.BOOLEAN, minReportTime, maxReportTime, null, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
    }
    
    cmds += findChildDevicesByClusterData(zigbee.LEVEL_CONTROL_CLUSTER).collect {
    	log.debug "Configuring LEVEL reporting for ${it}"
    	zigbee.configureReporting(zigbee.LEVEL_CONTROL_CLUSTER, 0x0000, DataType.UINT8, minReportTime, maxReportTime, null, [destEndpoint: Integer.parseInt(it.getDataValue('endpointId'))])	
    }
    
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
    //device.updateSetting("supportedSpeeds", state.preferences["supportedSpeeds"].collect { fanSpeeds[it].name })
    
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
    
    List events = []
    
    Map descMap = zigbee.parseDescriptionAsMap(description)
    int cluster = descMap?.clusterInt ?: zigbee.ONOFF_CLUSTER
    Map event = zigbee.getEvent(description)
    if (event) {
    	log.info "Defined event detected from controller: ${description}"
        // 
		// NOTE: For certain descriptions, the values for the events need to be changed to report correctly
        // To handle this, send the correct events for the switch and level clusters to the child light device
        /*
        	switch - This should be the on/off state of the FAN (zigbee.OnOffRefresh gives the LIGHT state). Create a "lightSwitch" event instead
            level - This should be the level of the FAN (zigbee.levelRefresh gives the LIGHT level. Create a "lightLevel" event instead
            on/off - This is the switch event for the LIGHT. Send this directly, as it will not update any attribute on the handler
        */
        Map eventNameMap = [ "switch":"lightSwitch", "level":"lightLevel", "on/off":"lightSwitch"]        
        events += createEvent(name: eventNameMap[event.name], value: event.value)
    }
    else if (description?.startsWith('read attr -') && FAN_CLUSTER == cluster && FAN_ATTR_ID == descMap?.attrInt) {
		// handle 'fanSpeed' attribute
        log.info "Fan message detected from controller: ${description}"
        Number fanSpeed = Math.max(Integer.parseInt(descMap.value), 0)
        events = createFanEvents((Math.max(Integer.parseInt(descMap.value), 0)))
    } else {
    	events = parseCatchAll(description)
    }
    
    events.each { syncChildDevices(cluster, it) }
    
	if (0 >= events.size()) {
    	log.warn "DID NOT PARSE MESSAGE for description : ${description}"
    }
    
    log.debug "'parse()' returning: ${events}"
    return events
}

def updated() {
	log.debug "Updating ${device.displayName} : ${device.deviceNetworkId}"
    
    // If the speed switches were previously created and user is selecting 'false', remove the switches
    if (!createSpeedSwitches && state.preferences["createSpeedSwitches"]) {
    	log.debug "Should delete children"
		deleteChildren()
    }
    
    //log.trace "SupportedSpeeds preference (before): ${supportedSpeeds}"
    //defaultSupportedSpeeds.intersect(supportedSpeeds as int[])
    //List<Integer> updatedSupportedSpeeds = (fanSpeeds.keySet() as int[]).intersect(supportedSpeeds ?: [])
    //fanSpeeds.keySet().intersect(state.preferences["supportedSpeeds"].keySet())
    
    // Store the preferences
    /*supportedSpeeds.each { s -> log.trace "Here >> ${s}" }
    def updatedSpeeds = supportedSpeeds.collect { s -> 
    	log.trace "Working preference: ${s}"
    	fanSpeeds.find {
        	log.trace "Finding: ${it}:${it.value.name == s}"
        	it.value.name == s 
        }?.key 
    }
    log.trace "UpdatedSpeeds: ${updatedSpeeds}"*/
    
    state.preferences = [
    	createSpeedSwitches: (createSpeedSwitches == null ? false : createSpeedSwitches),
    	resumeLast: (resumeLast == null ? false : resumeLast),
        //supportedSpeeds: (supportedSpeeds == null ? defaultSupportedSpeeds : supportedSpeeds.collect { s -> fanSpeeds.find { it.value.name == s }?.key }),
        //supportedSpeeds: (supportedSpeeds == null ? fanSpeeds.collect { [(it.key): (it.value.name)] } : supportedSpeeds),
    	//supportedSpeeds: (supportedSpeeds == null ? ["Low","Medium","High","Max","Breeze"] : supportedSpeeds),
        supportedSpeeds: defaultSupportedSpeeds,
        offBeforeChange: (turnOffBeforeSpeedChange == null ? false : turnOffBeforeSpeedChange)
    ]
    log.debug "Updated Preferences: ${state.preferences}"
    state.preferences.each {
    	log.trace "Updated preference: ${it}"
        state[it.key] = it.value
        device.updateSetting(it.key, it.value)
    }    
    
    /*device.updateSetting("offBeforeChange", state.preferences["offBeforeChange"].toBoolean())
    device.updateSetting("resumeLast", state.preferences["resumeLast"].toBoolean())
    device.updateSetting("createSpeedSwitches", state.preferences["createSpeedSwitches"].toBoolean())
    log.trace "SupportedSpeeds preference: ${state.preferences["supportedSpeeds"]}"*/
        
	if (!getChildDevices() || state.preferences["createSpeedSwitches"]) {
    	log.debug "Should create children"
    	createChildDevices()
    } else if (state.oldLabel != device.label) {
    	//log.trace "Updating child labels"
        //getChildDevices().each { it.label = "${device.displayName} ${findChildDeviceSpecs(it)?.name}" }
    	state.oldLabel = device.label
    }
    
    response(refresh() + configure())
}

// handle commands
public List<HubAction> fanOff() {
	//log.debug "Executing 'fanOff'";
    return setFanLevel(0)
}

public List<HubAction> fanOn() {
	//log.debug "Executing 'fanOn'";
    return setFanLevel((state.lastFanLevel == null ? fanSpeeds[1].threshold : state.lastFanLevel))
}

public List<HubAction> lightOff(int endpoint = 1) {
    //log.debug "Executing 'lightOff()': endpoint = ${endpoint}"
    return zigbee.off().collect{ new HubAction(it) }
    //return lightOnOff(0x00, endpoint)
}

public List<HubAction> lightOn(int endpoint = 1) {
    //log.debug "Executing 'lightOn()': endpoint = ${endpoint}"
    return zigbee.on().collect{ new HubAction(it) }
    //return lightOnOff(0x01, endpoint)
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
	log.debug "Executing 'refresh()': endpoints = ${endpoints}"
    
    def deviceClusters = clusterInfo.findAll { it.value.shouldRefresh }.keySet()
    def readClusters = []
    
    if (!endpoints.contains(initialEndpoint)) {
    	endpoints.collect { findChildDeviceSpecs(it) }
        	.findAll { !it.syncInfo?.keySet()?.disjoint(deviceClusters) }
        	.each { readClusters += it.syncInfo?.keySet() }
    } else {
    	readClusters = deviceClusters
    }
    
    List cmds = deviceClusters.intersect(readClusters).collect { zigbee.readAttribute(it, 0x00) }.flatten()    
    
	/*for (ep in endpoints) {
    	// If the endpoint is a child command, read the correct information
    	Map childDeviceSpecs = findChildDeviceSpecs(ep)
        log.trace "refresh(): ${ep} = ${childDeviceSpecs}"
    	if (childDeviceSpecs != null) {
        	readClusters = childDeviceSpecs.syncInfo?.keySet().intersect(deviceClusters)
        	if (0 >= readClusters) {
        		log.debug "Endpoint ${ep} does not control clusters on this device"
            	continue
        	} else {
                readClusters.each { cmds += zigbee.readAttribute(it, 0x00, [destEndpoint: ep]) }
            }
        }
    }*/
    
    log.trace "'refresh()': Returning ${cmds} for ${endpoints}"
    return cmds.collect { new HubAction(it) }
}

public List<HubAction> setLightLevel(Number value, rate = null) {
    Integer lightNow = Math.max(device.currentValue("lightLevel")?.intValue() ?: 0, 0)
	log.debug "Requested lightLevel is ${value}. Current lightLevel is ${lightNow}"

    Integer level = Math.max(Math.min(value.intValue(), 100), 20)
    
    List<HubAction> cmds = []
    // only update if the new level is different than the current level
    if (level != fanNow) {    
    	//state.lastLightLevel = lightNow     //save light level before changing so it can be resumed when turned back on    
        rate = Math.max(Math.min((rate == null ? 0 : rate.intValue()), 100), 0)    
    	log.info "Adjusting Light Brightness: ${level} : ${rate}"
    	cmds = zigbee.setLevel(level, rate).collect { new HubAction(it) }
    }
    
    return cmds
}

public List<HubAction> setFanLevel(Number level) {
    Number fanNow = Math.max(device.currentValue("fanLevel")?.intValue() ?: 0, 0)
    List<Integer> thresholds = fanSpeeds.values().collect { it.threshold }
    level = Math.max(Math.min(level.intValue(), thresholds.max()), thresholds.min())   
	//log.debug "Requested fanLevel is ${level}. Current fanLevel is ${fanNow}"
    
    List<HubAction> cmds = []
    // only update if the new fan level is different than the current fan level
    if (level != fanNow) {
    	state.lastFanLevel = fanNow    //save fan level before changing so it can be resumed when turned back on    
    	sendEvent(name: "fanLevel", value: level) // NOTE: This is necessary b/c fan level is not defined in any capability
    	cmds = setFanSpeed(findFanSpeedByLevel(level))
    }
    
    return cmds
}

public List<HubAction> setFanSpeed(Number speed) {
    int fanNow = Math.max(device.currentValue("fanSpeed")?.intValue() ?: 0, 0)
	//log.debug "Executing 'setFanSpeed': fanSpeed = ${speed}. Current Fan speed is ${fanNow}"	    
        
    List<HubAction> cmds = []
 	// NOTE: This may be necessary to get the reporting of values correct
    //createFanEvents(speed).each { sendEvent(it) }
    // only update if the new fan speed is different than the current fan speed
    if (speed != fanNow) {
    	speed = convertToSupportedSpeed(speed?.intValue() ?: 0, fanNow)
        state.lastFanSpeed = fanNow    //save fanspeed before changing speed so it can be resumed when turned back on
	    log.info "Adjusting Fan Speed to ${fanSpeeds[speed]}"
        if (0 < speed && state.offBeforeChange?.toBoolean()) {
        	cmds = zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", 0)) + "delay ${DEFAULT_DELAY}"
        }
        cmds += zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", speed))
        //cmds << "st wattr 0x${device.deviceNetworkId} 0x01 ${FAN_CLUSTER} ${FAN_ATTR_ID} 0x0030 {${String.format("%02d", setSpeed)}}" << "delay ${DEFAULT_DELAY}"
    } else  {
    	cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)
    }
    
    log.trace "'setFanSpeed()' returning: ${cmds}"
    return cmds.collect { new HubAction(it) }
}

public List<HubAction> setLevel(value, rate = null) {
	//log.debug "Executing 'setLevel()': ${value}, ${rate}"
    return setFanLevel(value)
}

public List<HubAction> raiseFanSpeed() {
	//log.debug "Executing 'raiseFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
    return setFanLevel(findFanLevelBySpeed(currentSpeed + 1))
}

public List<HubAction> lowerFanSpeed() {
	//log.debug "Executing 'lowerFanSpeed()'"
	int currentSpeed = device.currentValue("fanSpeed")?.intValue() ?: 0
    return setFanLevel(findFanLevelBySpeed(currentSpeed - 1))
}

/**
  *  Child handling
  */
public void childOff(String dni) {
	log.debug "Executing 'childOff(): ${dni}"
    
    List<HubAction> cmds = []
    
    ChildDeviceWrapper childDevice = getChildDevice(dni)
    if (childDevice) {
    	// Determine the cluster that needs to be controlled
        List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) } // NOTE: Single quotes necessary for this to work
        if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
        	int endpoint = getChildDeviceEndpoint(childDevice)
            cmds = lightOff(getChildDeviceEndpoint(childDevice))
        } else if (clusters.contains(FAN_CLUSTER)) {
            boolean resumeRequested = state.preferences["resumeLast"].toBoolean()
            boolean hasLastFanSpeed = 0 < state.lastFanSpeed
            boolean shouldResume = resumeRequested && hasLastFanSpeed //&& fanSpeeds.keySet().max() == Integer.parseInt(childDevice.getDataValue('fanSpeed'))
            log.trace "shouldResume: ${resumeRequested} - ${hasLastFanSpeed} - ${shouldResume}"
            cmds = shouldResume ? fanOn() : fanOff()
        }
    }
    
    log.trace "'childOff()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)   
}

public void childOn(String dni) {
	log.debug "Executing 'childOn(): ${dni}"
    
    List<HubAction> cmds = []
    
    ChildDeviceWrapper childDevice = getChildDevice(dni)
    if (childDevice) {
    	// Determine the cluster that needs to be controlled
        List<Integer> clusters = childDevice.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }  // NOTE: Single quotes necessary for this to work
        
        if (clusters.contains(zigbee.ONOFF_CLUSTER)) {
        	cmds = lightOn(getChildDeviceEndpoint(childDevice))
        } else if (clusters.contains(FAN_CLUSTER)) {
            cmds = setFanLevel(findFanLevelBySpeed(Integer.parseInt(childDevice.getDataValue('fanSpeed'))))
        }
    }
        
    log.trace "'childOn()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)
}

public void childRefresh(String dni) {
	log.debug "Executing 'childRefresh(): ${dni}"
    
    List<HubAction> cmds = []
    
    ChildDeviceWrapper childDevice = getChildDevice(dni)
    if (childDevice) {
        cmds = refresh([getChildDeviceEndpoint(childDevice)])
        childDevice.sendEvent(name: "refresh")
    }
    
    cmds = cmds
    log.trace "'childRefresh()': Sending ${cmds} for child ${childDevice}"
    sendHubCommand(cmds, DEFAULT_DELAY)
}

public void childSetLevel(String dni, Number level, Number rate = null) {
	log.debug "Executing 'childSetLevel(): ${childDevice}, level = ${level}, rate = ${rate}"
    
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
    
    //cmds = cmds?.flatten()
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
    
    childDeviceSpecs.findAll { key, value ->
    	// Sync the available speeds with the switches
        /*log.trace value.createChild
        if (rtnVal && value.data && value.data["fanSpeed"]) {        	
        	Boolean shouldCreate = supportedSpeeds.contains(value.data["fanSpeed"].intValue())
        	log.trace "Should Create: ${value}"
        	rtnVal = rtnVal && shouldCreate
        }*/
        Boolean rtnVal = value.createChild || value.required
        log.trace "Should Create: ${value}"
        return rtnVal
    }
    .each { key, value ->
    	log.trace "Create Child: ${value}"
    	int endPointId = key.intValue() + CHILD_ENPOINT_ID_OFFSET
        String networkId = "${device.deviceNetworkId}:0${endPointId}"
        Map data = (value.data ?: [:]) << [endpointId: "${endPointId}", required: value.required, requested: state.createSpeedSwitches, clusters: (value.syncInfo?.collect { "${it.key}" }?.join(",")), manufacturer: "King of Fans, Inc." ]
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
    int maxLevel = findFanLevelBySpeed(fanSpeed)
    int minLevel = findFanLevelBySpeed(fanSpeed - 1)
    if (!((minLevel + 1)..maxLevel).contains(fanLevel)) {
    	fanLevel = maxLevel
    }
    //fanLevel = Math.max(Math.min(fanLevel, maxLevel), minLevel + 1)
    if (1 == fanLevel) { fanLevel = 0 }
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

private Map configureHealthCheck() {
	log.debug "Executing 'createHealthCheckEvent()'"
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
    Map healthEvent = [name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
    getChildDevices().each {
    	it.sendEvent(healthEvent)
    }
    
    log.trace "configureHealthCheck() returning: ${healthEvent}"
    return healthEvent
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

private List<ChildDeviceWrapper> findChildDevicesByClusterData(Integer cluster) {
	return getChildDevices().findAll { it.getDataValue('clusters').split(',').collect { Integer.parseInt(it) }.contains(cluster) }
}

private Map findChildDeviceSpecs(ChildDeviceWrapper child) {
    return findChildDeviceSpecs(Integer.parseInt(child?.getDataValue('endpointId')))
}

private Map findChildDeviceSpecs(int endpointId = -1) {
	return childDeviceSpecs[endpointId - CHILD_ENPOINT_ID_OFFSET]
}

private int findFanLevelBySpeed(Number speed) {
    Number correctedSpeed = convertToSupportedSpeed(Math.max(speed.intValue(), 0))
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
    int speed = fanSpeeds.find { it.value.threshold.intValue() >= checkValue }.key
    return convertToSupportedSpeed(speed)
}

private int getChildDeviceEndpoint(ChildDeviceWrapper childDevice) {
	return childDevice?.getDataValue('endpointId') as Integer
}

private ChildDeviceWrapper getChildDevice(int deviceSpecKey) { 
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

private List<HubAction> lightOnOff(value, int endpoint = 1) {
    log.debug "Executing 'lightOnOff()', value = ${value}, endpoint = ${endpoint}"
	List cmds = []
        
    if (initialEndpoint != endpoint) {
        Map childDeviceSpecs = findChildDeviceSpecs(endpoint)
        if (childDeviceSpecs && !childDeviceSpecs.syncInfo.keySet().contains(zigbee.ONOFF_CLUSTER)) {
            log.debug "Endpoint ${endpoint} does not control the ${clusterInfo[zigbee.ONOFF_CLUSTER].name}"
            return cmds
        } /*else {
    		cmds = zigbee.command(zigbee.ONOFF_CLUSTER, value, "", [destEndpoint: endpoint])    	
        }*/
    }
    
    cmds = zigbee.command(zigbee.ONOFF_CLUSTER, value, "")
    
    log.trace "'lightOnOff()': Sending ${cmds} for ${endpoint}"
	return cmds.collect { new HubAction(it) }
}

private List parseCatchAll(String description) {
	log.debug "Executing 'parseCatchAll()': ${description}"
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
                            events = createFanEvents().findAll { it.isStateChange.toBoolean() }
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
                    events << configureHealthCheck()
                    //cmds << [ name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID] ]
                }
            }
        }
    }
    
    //events = events.findAll { it.isStateChange.toBoolean() }
    log.debug "'parseCatchAll' returning ${events}"
    return events
}

private List<HubAction> refreshFan() {
    //List cmds = [ "st rattr 0x${device.deviceNetworkId} 0x01 0x0202 0x0000" ]
    List cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)
    return cmds.collect { new HubAction(it) }
}

private List<HubAction> refreshLight() {
	//List cmds = ["st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x0000", "delay ${DEFAULT_DELAY}", "st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.LEVEL_CONTROL_CLUSTER} 0x0000"]    
    List cmds = [ zigbee.onOffRefresh(), zigbee.levelRefresh() ].flatten()
	return cmds.collect { new HubAction(it) }
}

private void syncChildDevices(Integer cluster, Map event) {
	//log.debug "Executing 'syncChildDevices(): cluster = ${cluster}, event = ${event}"
    
    // NOTE: Map [from event name] to [child device event name]
    Map eventNameMap = [ "lightSwitch":"switch", "lightLevel":"level", "fanSpeed":"switch", "fanLevel":"level"]
    findChildDevicesByClusterData(cluster).findAll{ event.name == findChildDeviceSpecs(it)?.syncInfo[cluster] }
    .each {
    	// event.name = fanSpeed || lightSwitch || lightLevel        
    	Map childEvent = FAN_CLUSTER == cluster
        	? it.createEvent(name:eventNameMap[event.name], value:((event.value ?: 0 as int) == Integer.parseInt(it.getDataValue('fanSpeed')) ? "on" : "off"))
        	: it.createEvent(name:eventNameMap[event.name], value:event.value)
        log.debug "Sending ${childEvent} TO child ${it}"
        it.sendEvent(childEvent)
    }
}