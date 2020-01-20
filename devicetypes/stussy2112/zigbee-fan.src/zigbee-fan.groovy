/**
 *	Copyright 2019 Sean Williams
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "ZigBee Fan", namespace: "stussy2112", author: "Sean Williams", mnmn: "SmartThings", vid:"x.com.st.fanspeed", ocfDeviceType: "oic.d.fan", minHubCoreVersion: '000.025.0000', genericHandler: "Zigbee") {
		capability "Switch Level"
		capability "Switch"
		capability "Fan Speed"
		capability "Health Check"
		capability "Actuator"
		capability "Refresh"
		capability "Sensor"

		command "fanOff"
		command "fanOn"
		command "raiseFanSpeed"
		command "lowerFanSpeed"
        command "setBrightnessLevel"
		command "setFanSpeed"

		attribute "fanLevel", "number"
        attribute "lightLevel", "number"
        
	  	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", manufacturer: "King Of Fans, Inc.", model: "HDC52EastwindFan", deviceJoinName: "Zigbee Fan Controller"
	}

  	preferences {
        input("dimRate", "enum", title: "Dim Rate", options: ["Instant", "Normal", "Slow", "Very Slow"], defaultValue: "Instant", required: false, displayDuringSetup: true)
    	page(name: "rebuildChildren", title: "This does not display on DTH preference page")
        	section("section") {
            	input(name: "refreshChildren", type: "bool", title: "Delete & Recreate all child devices?\n\nTypically used after modifying the parent device name " +
              		"above to give all child devices the new name.\n\nPLEASE NOTE: Child Devices must be removed from any smartApps BEFORE attempting this " +
              		"process or 'An unexpected error' occurs attempting to delete the child's.")
      		}
  	}

	tiles(scale: 2) {
		/*multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:"Turning on light", action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:"Turning off light", action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL", range:"(20..100)") {
				attributeState "level", action:"setBrightnessLevel"
			}
		}*/
		multiAttributeTile(name: "fanSpeed", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "off", action: "fanOn", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "adjusting"
				attributeState "1", label: "low", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "2", label: "medium", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#1e9cbb", nextState: "adjusting"
				attributeState "3", label: "high", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#184f9c", nextState: "adjusting"
				attributeState "4", label: "max", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#153591", nextState: "adjusting"
				attributeState "6", label: "Breeze", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#90d2a7", nextState: "turningOff"
				attributeState "adjusting", label: "Adjusting...", action: "fanOn", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
				attributeState "turningOff", label:"Turning Fan Off", action: "fanOff", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
                attributeState "turningOn", label:"Turning Fan On", action: "fanOn", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
			}
            
			/*tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "off", action: "switch.on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff"
				attributeState "1", label: "low", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc"
				attributeState "2", label: "medium", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc"
				attributeState "3", label: "high", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc"
			}*/
			tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "raiseFanSpeed"
				attributeState "VALUE_DOWN", action: "lowerFanSpeed"
			}
		}
        
        childDeviceTiles("all", width: 6, height: 1)

    	standardTile("refresh", "refresh", decoration: "flat", width: 6, height: 1) {
		  state "default", label:"refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
	  	}
        
        /*main "switch"
		details(["switch", "fanSpeed", childDeviceTiles("all", width: 6, height: 1)])*/
	}
}

import physicalgraph.app.ChildDeviceWrapper
import physicalgraph.device.Device
import physicalgraph.device.HubAction
import physicalgraph.zigbee.zcl.DataType
	
// Globals
private getDEFAULT_DELAY() { 100 }
private getBIND_CLUSTER() { 0x8021 }
private getFAN_CLUSTER() { 0x0202 }
private getFAN_ATTR_ID() { 0x0000 }
private getGROUPS_CLUSTER() { 0x0004 }
private getSCENES_CLUSTER() { 0x0005 }
private getHOME_AUTOMATION_CLUSTER() { 0x0104 }

private Map getChildDeviceNames() {	[ 0:"Fan Off", 1:"Low", 2:"Medium", 3:"High", 4:"Max", 5:"Off", 6:"Comfort Breeze™", 7:"Light", 8:"Fan" ] }
private Map getChildDeviceSpecs() {
	[
    	0: [ name:"Fan Off", createChild: false, data: [ fanSpeed: 0, speedThreshold: 40 ] ],
        1: [ name: "Low", typeName: "Zigbee Fan Controller - Fan Speed Child Device", isComponent: true, createChild: false, componentName: "fanMode1", syncInfo: [ (FAN_CLUSTER): [name: "switch", trigger: "fanSpeed"] ], data: [ fanSpeed: 1, speedThreshold: 40 ] ],
        2: [ name: "Medium", typeName: "Zigbee Fan Controller - Fan Speed Child Device", isComponent: true, createChild: false, componentName: "fanMode2", syncInfo: [ (FAN_CLUSTER): [name: "switch", trigger: "fanSpeed"] ], data: [ fanSpeed: 2, threshold: 60 ] ],
        3: [ name: "High", typeName: "Zigbee Fan Controller - Fan Speed Child Device", isComponent: true, createChild: false, componentName: "fanMode3", syncInfo: [ (FAN_CLUSTER): [name: "switch", trigger: "fanSpeed"] ], data: [ fanSpeed: 3, threshold: 80 ] ],
        4: [ name: "Max", typeName: "Zigbee Fan Controller - Fan Speed Child Device", isComponent: true, createChild: false, componentName: "fanMode4", syncInfo: [ (FAN_CLUSTER): [name: "switch", trigger: "fanSpeed" ] ], data: [ fanSpeed: 4, threshold: 100 ] ],
        5: [ name: "Off", createChild: false ],
        6: [ name: "Breeze", typeName: "Zigbee Fan Controller - Fan Speed Child Device", isComponent: true, createChild: true, componentName: "fanMode6", syncInfo: [ (FAN_CLUSTER): [name: "switch", trigger: "fanSpeed"] ], data: [ fanSpeed: 6 ] ],
        7: [ name: "Light", typeName: "Zigbee Fan Controller - Light Child Device", isComponent: true, createChild: true, componentName: "fanLight", syncInfo: [ (zigbee.ONOFF_CLUSTER): [name: "switch", trigger: "switch" ], (zigbee.LEVEL_CONTROL_CLUSTER): [name: "level", trigger: "level"] ] ]
    ]
}

private Map getClusterInfo() { [ (zigbee.ONOFF_CLUSTER):[ name: "Switch", dataType: DataType.BOOLEAN ], (zigbee.LEVEL_CONTROL_CLUSTER):[name: "Level", dataType: DataType.UINT8], (FAN_CLUSTER):[name:"Fan", dataType: DataType.ENUM8], (BIND_CLUSTER):[name: "Bind"] ] }
private Map getBindResults() { [ 0:"Success", 132:"Not Supported", 130:"Invalid Endpoint", 140:"Table Full" ] }
private Map getDimRates() { [ "Instant":0, "Normal":35, "Slow":50, "Very Slow":100 ] }
private Map getSpeedThresholds() { [ 20:0, 40:1, 60:2, 80:3, 100:4, 1000:6 ] }
private Map getFanSpeeds() { [ 0:[name: "Fan Off", threshold: 20], 1:[name: "Low", threshold: 40], 2:[name: "Medium", threshold: 60], 3:[name: "High", threshold: 80], 4:[name: "Max", threshold: 100], 6:[name: "Breeze", threshold: 1000], 7:"Light" ] }

def configure() {
	log.info "Configuring Reporting and Bindings."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	//sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    
    // OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
    Integer minReportTime = 0
    Integer maxReportTime = 300
    def cmds = [
    	// Bindings for Fan Controller
        zigbee.addBinding(zigbee.ONOFF_CLUSTER),
        //"zdo bind 0x${device.deviceNetworkId} 1 1 0x006 {${device.zigbeeId}} {}", "delay ${DEFAULT_DELAY}",
        zigbee.addBinding(zigbee.LEVEL_CONTROL_CLUSTER),
        //"zdo bind 0x${device.deviceNetworkId} 1 1 0x008 {${device.zigbeeId}} {}", "delay ${DEFAULT_DELAY}",
        zigbee.addBinding(FAN_CLUSTER),        
		//"zdo bind 0x${device.deviceNetworkId} 1 1 0x202 {${device.zigbeeId}} {}", "delay ${DEFAULT_DELAY}",
        // Configure reporting for the ON/OFF switch
        zigbee.onOffConfig(minReportTime, maxReportTime),
       	// Configure reporting for the Dimmer
        zigbee.levelConfig(minReportTime, maxReportTime),
        // Configure reporting for the fan (0x0202)
        zigbee.configureReporting(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, minReportTime, maxReportTime, null),
	  	//Get current values from the device
        refresh()
    ]

    return cmds
}

def installed() {
	log.info "Installed ${device.displayName}"
    
	state.dimRate = 0
	if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
    	sendEvent(name: "level", value: 100)
	}
    
    setFanSpeed(1)
    state.lastFanSpeed = 1
    for (i in 1..3) {
    	sendEvent(name: "switch", value: "on")
    	sendEvent(name: "switch", value: "off")
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.info "Parsing: ${description}"
    log.trace zigbee.parseDescriptionAsMap(description)
    
    List<HubAction> rtnVal = []
    Map event = zigbee.getEvent(description)
    log.trace "Got event: ${event}"
    if (event) {
    	rtnVal << event
        log.info "Light event detected on controller"
        //getChildDevice("${device.deviceNetworkId}-ep7")?.sendEvent(name: event.name, value: event.value)
        
        childDeviceSpecs.findAll { it.value.syncInfo && it.value.syncInfo?.collect { it.value.trigger }.contains(event.name) }
        	.each { 
                log.debug "Sending ${event ?: descMap} to ${getChildDevice(it.key)}"
                getChildDevice(it.key)?.sendEvent(name: event.name, value: event.value)
            }
        sendEvent(event)
    } else {
        // Check if the command is for the fan
        def descMap = zigbee.parseDescriptionAsMap(description)
        log.debug "Parsed map: ${descMap}"
        if (descMap && descMap.clusterInt == FAN_CLUSTER) {
        	if (descMap.attrInt == FAN_ATTR_ID) {
            	log.info "Fan event detected on controller: ${description}"
                Integer speed = Math.max(Integer.parseInt(descMap.value), 0)
            	syncChildFanDevices(speed)
                sendEvent(name: "fanSpeed", value: speed)
            	rtnVal << createEvent(name: "fanSpeed", value: speed)
            }
        }
    }
    
    // Handle a catchall description
    rtnVal << parseCatchAll(description)

	if (0 < rtnVal.size()) {
    	log.info "Events sent: ${rtnVal}"
    	/*log.info "Event sent: ${event}"
        sendEvent(event)*/
    } else {     
    	log.warn "DID NOT PARSE MESSAGE for description : $description"
		log.debug "Unparsed description: ${description}"
    }
    
    return rtnVal?.flatten().collect { new HubAction(it) }
}

def updated() {
	log.info "Updated..."
    
    state.dimRate = dimRates[dimRate] ?: 0
    
    if (refreshChildren) {
    	deleteChildren()
        device.updateSetting("refreshChildren", false)
    }
    
	if (!getChildDevices()) {
    	//createChildDevices()
    } else if (state.oldLabel != device.label) {
    	log.trace "Updating child labels"
        //childDeviceSpecs.findAll { it.createChild }.each { key, value -> getChildDevice(key)?.label = "${device.displayName} ${value.name}" }
    	//childDeviceNames.findAll { ![0,5].contains(it.key) }.each { key, value -> getChildDevice("${device.deviceNetworkId}-ep${key}")?.label = "${device.displayName} ${value}" }
    	state.oldLabel = device.label
    	response(refresh() + configure())
    }    
}

def fanOff() {
	log.info "Turning Fan Off";
    return setFanLevel(0)
}

def fanOn() {
	log.info "Turning Fan On";
    return setFanLevel(state.lastFanLevel ?: 35)
}

def lightOff() {
    log.info("Turning Off Light")
	//List cmds = ["st cmd 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x00 {}", "delay ${DEFAULT_DELAY}", refreshLight()].flatten()
	List cmds = [zigbee.on(), refreshLight()].flatten()
    log.trace "Light off commands: ${cmds}"
    return cmds
}

def lightOn() {
    log.info("Turning On Light")    
	//List cmds = ["st cmd 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x01 {}", "delay ${DEFAULT_DELAY}", refreshLight()].flatten()
	List cmds = [zigbee.off(), refreshLight()].flatten()
    log.trace "Light on commands: ${cmds}"
    return cmds
}

def off() {
	log.info "Turning Off";
    List cmds = fanOff()
    log.trace "Off commands: ${cmds}"
    return cmds
}

def on() {
	log.info "Turning On";
    List cmds = fanOn()
    log.trace "On commands: ${cmds}"
    return cmds
}

def setBrightnessLevel(value, rate = null) {
	log.debug "Setting brightness level to ${value}"

    Integer level = Math.max(Math.min(value?.intValue() ?: 0, 100), 0)
    rate = Math.max(Math.min(rate?.intValue() ?: 0, state?.dimRate), 0)
    
    log.info "Adjusting Light Brightness: ${level} : ${rate}"
    sendEvent(name: "lightLevel", value: level, rate: rate)
    List cmds = zigbee.setLevel(level, rate) << refreshLight()
    log.trace "Set Brightness Level Returning ${cmds}"    
    return cmds.flatten()
}

def setFanLevel(value) {
    Integer fanNow = device.currentValue("fanLevel") ?: 0    
	log.info "Requested fanLevel is ${value}. Current Fan speed is ${fanNow}"
    
    state.lastFanLevel = fanNow
    Integer speed = fanSpeeds.find { it.value.threshold.intValue() >= Math.max(Math.min(value?.intValue() ?: 0, 1000), 0) }.key
    
    sendEvent(name: "fanLevel", value: value)
    return setFanSpeed(speed)
}

def setFanSpeed(speed) {	
    Integer fanNow = device.currentValue("fanSpeed") ?: 0    //save fanspeed before changing speed so it can be resumed when turned back on    
	log.info "Requested fanSpeed is ${speed}. Current Fan speed is ${fanNow}"	    
        
    List cmds = []
    Integer setSpeed = Math.max(Math.min(speed?.intValue() ?: 0, 6), 0)
    if (speed != fanNow) {
    	// only update if the new fan speed is different than the current fan speed
        state.lastFanSpeed = fanNow
        log.trace "state.lastFanSpeed set to ${state.lastFanSpeed}"
        if (setSpeed == 5) { setSpeed = 4 }
        state.fanState = 0 < setSpeed// ? "on" : "off"
        
	    log.info "Adjusting Fan Speed to ${childDeviceNames[setSpeed]}"
    	sendEvent(name: "fanSpeed", value: setSpeed)
        cmds << zigbee.writeAttribute(FAN_CLUSTER, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", setSpeed))
        //cmds << "st wattr 0x${device.deviceNetworkId} 0x01 ${FAN_CLUSTER} ${FAN_ATTR_ID} 0x0030 {${String.format("%02d", setSpeed)}}" << "delay ${DEFAULT_DELAY}"
    }
    
    cmds << refreshFan()
    log.trace "Set Fan Speed Returning ${cmds}"  
    return cmds?.flatten()
}

def setLevel(value, rate = null) {
	log.debug "Setting level to ${value}"
    return setBrightnessLevel(value, rate)
}

def raiseFanSpeed() {
	log.info "Raising fan speed"
	Integer currentValue = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (4 == currentValue) { currentValue = 5 }
    
    return setFanLevel(getFanLevel(Math.min(currentValue + 1, 6)))
}

def lowerFanSpeed() {
	log.info "Lowering fan speed"
	Integer currentValue = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (6 == currentValue) { currentValue = 5 }
    
    return setFanLevel(getFanLevel(Math.max(currentValue - 1, 0)))
}

/**
  *  Child handling
  */
public childOff(String deviceNetworkId) {
	log.info "Parent recieved 'off' command from ${deviceNetworkId}"
    
    def endpointId = getEndpointId(deviceNetworkId)
    Map childDeviceSpec = childDeviceSpecs[endpointId]
    Boolean isFanSpeed = childDeviceSpec.syncInfo.keySet().any { FAN_CLUSTER == it }
    
    List cmds = childDeviceSpecs[endpointId].syncInfo.keySet().any { FAN_CLUSTER == it } ? fanOff() : lightOff()
    return cmds.flatten()
}

public childOn(String deviceNetworkId) {
	log.info "Parent recieved 'on' command from ${deviceNetworkId}"
    
    Integer endpointId = getEndpointId(deviceNetworkId)
    Map childDeviceSpec = childDeviceSpecs[endpointId]
    Boolean isFanSpeed = childDeviceSpec.syncInfo.keySet().any { FAN_CLUSTER == it }
    
    List cmds = childDeviceSpec.syncInfo.keySet().any { FAN_CLUSTER == it } ? setFanLevel(getFanLevel(endpointId)) : lightOn()
    return cmds.flatten()
}

public childRefresh(String deviceNetworkId) {
	log.info "Parent recieved 'refresh' command from ${deviceNetworkId}"
    Integer endpointId = getEndpointId(deviceNetworkId)
    
    Map childDeviceSpec = childDeviceSpecs[getEndpointId(deviceNetworkId)]
    List<Map> cmds = childDeviceSpec?.syncInfo.collect { zigbee.readAttribute(it.key, 0x000) }.flatten()
    
    return cmds
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.info "Pinged..."
	return zigbee.onOffRefresh()
}

def poll() {
	log.info "Polling..."
	return refresh()
}

def refresh() {
	log.info "Refreshing..."
    List cmds = [ refreshLight(), "delay ${DEFAULT_DELAY}", refreshFan() ].flatten()
    //List cmds = [ zigbee.onOffRefresh(), zigbee.levelRefresh(), refreshFan() ].flatten()
    log.trace "Refresh commands: ${cmds}"
    return cmds
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
    log.info "Creating Child Devices..."
    childDeviceSpecs.findAll { key, value -> value.createChild }
    	.each { key, value ->
            String networkId = "${device.deviceNetworkId}-ep${key}"
            /*Map data = value.data ?: [:] << [endpointId: key]
            data["syncInfo"] = value.syncInfo?.collect { [cluster: it.key] << it.value } ?: [:]
            log.trace "${networkId}: ${data}"*/
            Map properties = [completedSetup: true, label: "${device.displayName} ${value.name}", isComponent: value.isComponent, componentName: value.componentName, componentLabel: value.name ]
            ChildDeviceWrapper childDevice = createChildDevice("stussy2112", value.typeName, networkId, properties)
        }
    
    syncChildFanDevices(device.currentValue("fanSpeed"))
}

private void deleteChildren() {	
    log.info "Deleting children"
    getChildDevices()?.each {c ->
    	log.trace "Deleting ${c.deviceNetworkId}"
  		deleteChildDevice(c.deviceNetworkId)
    }
}

private findChildDeviceSpecs(List<Integer> clusters) {
	log.info "Finding child device specs for ${clusters}"
    
	List<Integer> childEndpoints = getChildDevices()?.collect { getEndpointId(it.deviceNetworkId) }
    def rtnVal = childDeviceSpecs.findAll { key, value -> childEndpoints.contains(key) && 0 < value.syncInfo.keySet().intersect(clusters ?: []).size() }
    
	log.trace "findChildDeviceSpecs: ${rtnVal}"
    return rtnVal
}

private ChildDeviceWrapper getChildDevice(Integer endpointId) { 
	ChildDeviceWrapper child = getChildDevices().find { Integer.parseInt(it.deviceNetworkId[-1]) == endpointId }
    if (!child) {
        log.error "Child device for ${endpointId} not found"
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

private Integer getEndpointId(String deviceNetworkId) {
    def deviceId = deviceNetworkId.split("\\-ep")[1] as Integer
}

private Integer getFanLevel(Integer speed) {
	log.debug "Getting fan level for ${speed}"
    Integer correctedSpeed = speed?.intValue() ?: 0
    if (5 == correctedSpeed) { correctedSpeed = 4 }
    
    return fanSpeeds.find { it.key.intValue() >= Math.max(Math.min(correctedSpeed, 1000), 0) }.value.threshold
}

private List parseCatchAll(String description) {
	List cmds = []
    
    if (description?.startsWith("catchall:")) {
		log.info "Parsing catchall message: ${description}"
        def parsed = zigbee.parse(description)
        log.trace "Parsed catchall: ${parsed}"
        
        // profile id for responses is 0104 (0x0104) - Home Automation
        if (0x0104 == parsed.profileId) {
            if (0 < parsed.data.size()) {
        		// if the data payload is '0', all was good and there is nothing to do
            	if (0x0000 == parsed.data[0]) {
                	log.debug "${clusterInfo[parsed.clusterId].name} event executed successfully."
                } else {
                	log.warn "${clusterInfo[parsed.clusterId].name} returned ${parsed.data}"
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
						cmds << createEvent(name:"switch", value: 0x00 == parsed.data[-1] ? "off" : "on")
                        break
                    case (zigbee.LEVEL_CONTROL_CLUSTER):
                    	
                        break					
                    case (FAN_CLUSTER):
                    	//Fan change
                    	//catchall: 0104 0202 01 01 0000 00 77E9 00 00 0000 04 01 00
    					//SmartShield(text: null, manufacturerId: 0x0000, direction: 0x01, data: [0x00], number: null, isManufacturerSpecific: false, messageType: 0x00, senderShortId: 0x77e9, isClusterSpecific: false, sourceEndpoint: 0x01, profileId: 0x0104, command: 0x04, clusterId: 0x0202, destinationEndpoint: 0x01, options: 0x0000)
                    	//[raw:0104 0202 01 01 0000 00 77E9 00 00 0000 04 01 00, profileId:0104, clusterId:0202, sourceEndpoint:01, destinationEndpoint:01, options:0000, messageType:00, dni:77E9, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:04, direction:01, data:[00], clusterInt:514, commandInt:4]                                   
                    	cmds << createEvent(name:"fan", value: 0x00 == parsed.data[-1] && state.fanState ? "on" : "off")
                        //cmds << createEvent(name:"fanSpeed", device.currentFanSpeed)
                        break
                    case (GROUPS_CLUSTER):
                        //String addToGroupCmd = "st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))} 00}"
                        break
                }
                
            }    	
        } else {
            log.trace "${clusterInfo[parsed.clusterId] ?: parsed.clusterId} Event Generated"
            
            if (0x8021 == parsed.clusterId && 0 == parsed.data.size()) {
                log.error "Bind result not present."
            } else {
                def bindResult = parsed.data[1]
                log.debug "Bind response: ${parsed.data}: ${bindResults[bindResult]}"
				cmds << createEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
                //cmds << [ name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID] ]
            }
        }
    }
    
    log.trace "parseCatchAll returning ${cmds}"
    return cmds.flatten()
}

private List refreshFan() {
	log.info "Refreshing fan..."
    //List cmds = [ "st rattr 0x${device.deviceNetworkId} 0x01 0x0202 0x0000" ]
    List cmds = zigbee.readAttribute(FAN_CLUSTER, FAN_ATTR_ID)
    log.trace "Refresh Fan commands: ${cmds}"    
    return cmds
}

private List refreshLight() {
	log.info "Refreshing light..."
    List cmds = zigbee.onOffRefresh() + zigbee.levelRefresh()
//    List cmds = ["st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x0000", "delay ${DEFAULT_DELAY}", "st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.LEVEL_CONTROL_CLUSTER} 0x0000"]
    
    log.trace "Refresh light commands: ${cmds}"
	return cmds.flatten()
}

private void syncChildFanDevices(speed) {
	log.info "Syncing child fans: ${speed}"
    Map fanSpeedSpecs = findChildDeviceSpecs([FAN_CLUSTER])
    
	getChildDevices()?.findAll { fanSpeedSpecs.keySet().contains(getEndpointId(it.deviceNetworkId)) }
    	.collect { [spec: fanSpeedSpecs[getEndpointId(it.deviceNetworkId)], childDevice: it ] }
        .each {
            String eventName = it.spec.syncInfo[FAN_CLUSTER].name            
            def eventValue = "switch" == eventName ? speed == it.spec.data?.fanSpeed ? "on" : "off" : speed
            if (eventValue != it.childDevice.currentValue(eventName)) {
                Map event = it.childDevice.createEvent(name:eventName, value:eventValue)
                log.trace "Sending child event '${event}' to ${it.childDevice}"
                it.childDevice.sendEvent(event)
            }            
        }
}