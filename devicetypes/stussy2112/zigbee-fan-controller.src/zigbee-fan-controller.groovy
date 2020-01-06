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

import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "ZigBee Fan Controller", namespace: "stussy2112", author: "Sean Williams", runLocally: true, executeCommandsLocally: false, mcdSync: true, ocfDeviceType: "oic.d.fan", genericHandler: "Zigbee") {
		capability "Actuator"
		capability "Configuration"
		capability "Fan Speed"
        capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
		capability "Health Check"

		command "fanOff"
		command "fanOn"
		command "setFanSpeed"
		command "raiseFanSpeed"
		command "lowerFanSpeed"

	  	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", manufacturer: "King Of Fans, Inc.", model: "HDC52EastwindFan", deviceJoinName: "Zigbee Fan Controller", ocfDeviceType: "oic.d.fan"
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
		multiAttributeTile(name: "fanSpeed", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "Fan Off", action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "adjusting"
				attributeState "1", label: "low", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "2", label: "medium", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "3", label: "high", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "4", label: "max", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "6", label: "Comfort Breeze™", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "adjusting", label: "Adjusting Fan", action: "on", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
				attributeState "turningOff", label:"Turning Fan Off", action: "on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
                attributeState "turningOn", label:"Turning Fan On", action: "off", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
			}
			tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "raiseFanSpeed"
				attributeState "VALUE_DOWN", action: "lowerFanSpeed"
			}
			tileAttribute ("device.level", label: "Brightness", key: "SLIDER_CONTROL", range:"(20..100)") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
        
        /*controlTile ("levelSliderControl", "device.level", label: "Brightness", "slider", height: 1, width: 6, range:"(20..100)") {
            state "level", action:"switch level.setLevel"
        }*/
        
        childDeviceTiles("all")
	}
}

// Globals
private getFAN_CLUSTER_ID() { 0x0202 }
private getFAN_ATTR_ID() { 0x0000 }
private getDEFAULT_DELAY() { 100 }

private Map getChildDeviceNames() {
	[
    	0:"Fan Off",
	    1:"Low",
    	2:"Medium",
    	3:"High",
  		4:"Max",
    	5:"Off",
    	6:"Comfort Breeze™",
    	7:"Light"
    ]
}

private Map getBindResults() {
	[ 
    	0:"Success", 
        132:"Not Supported", 
        130:"Invalid Endpoint", 
        140:"Table Full" 
    ]
}

private Map getDimRates() { 
	[
    	"Instant":0,
        "Normal":35,
        "Slow":50,
        "Very Slow":100
    ]
}

def configure() {
	log.info "Configuring Reporting and Bindings."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	//sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    
    // OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
    def cmds = [
        // Configure reporting for the ON/OFF switch
        zigbee.onOffConfig(0, 300),
       	// Configure reporting for the Dimmer
        zigbee.levelConfig(0, 300),
        // Configure reporting for the fan (0x0202)
        zigbee.configureReporting(FAN_CLUSTER_ID, FAN_ATTR_ID, DataType.ENUM8, 0, 300, null),
	  	//Get current values from the device
        refresh()
    ]

    return cmds
}

def installed() {
	log.info "Installed ${device.displayName}"
    
	state.dimRate = 0
    createChildDevices()
	if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
    	sendEvent(name: "level", value: 100)
	}
    
    setFanSpeed(1)
    for (i in 1..2) {
    	sendEvent(name: "switch", value: "on")
    	sendEvent(name: "switch", value: "off")
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.info "Parsing: ${description}"
    
    Map event = parseCatchAllMessage(description) ?: zigbee.getEvent(description)
    log.trace "Got event: ${event}"
    if (event) {
        log.info "Light event detected on controller"
        log.debug "Sending ${event} to ${getChildDevice("${device.deviceNetworkId}-ep7")}"
        getChildDevice("${device.deviceNetworkId}-ep7")?.sendEvent(name: event.name, value: event.value)
    } else {
        // Check if the command is for the fan
        def descMap = zigbee.parseDescriptionAsMap(description)
        log.debug "Parsed map: ${descMap}"
        if (descMap && descMap.clusterInt == FAN_CLUSTER_ID) {
        	if (descMap.attrInt == FAN_ATTR_ID) {
            	log.info "Fan event detected on controller: ${description}"
                def speed = Math.max(Integer.parseInt(descMap.value), 0)
            	syncChildFanDevices(speed)
            	event = createEvent(name: "fanSpeed", value: speed)
            } else {
            	return
            }
        }
    }

	if (event) {
    	log.info "Event sent: ${event}"
        sendEvent(event)
        return
    }
    
    log.warn "DID NOT PARSE MESSAGE for description : $description"
	log.debug "Unparsed description: ${description}"    
}

def updated() {
	log.info "Updated..."
    
    state.dimRate = dimRates[dimRate] ?: 0
    
    if (refreshChildren) {
    	deleteChildren()
        device.updateSetting("refreshChildren", false)
    }
    
	if (!childDevices) {
    	createChildDevices()
    } else if (state.oldLabel != device.label) {
    	log.trace "Updating child labels"    
    	childDeviceNames.findAll { ![0,5].contains(it.key) }.each { key, value -> getChildDevice("${device.deviceNetworkId}-ep${key}")?.label = "${device.displayName} ${value}" }
    	state.oldLabel = device.label
    	response(refresh() + configure())
    }    
}

def lightOff() {
    log.info("Turning Off Light")
    def cmds = zigbee.off() + refreshLight()
    log.trace "Light off commands: ${cmds}"
    return cmds
}

def lightOn() {
    log.info("Turning On Light")    
    def cmds = zigbee.on() + "st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x0000"//refreshLight()
    log.trace "Light on commands: ${cmds}"
    return cmds
}

def off() {
	log.info "Turning Fan Off";
    def cmds = setFanSpeed(0)
    log.trace "Fan Off commands: ${cmds}"
    return cmds
}

def on() {
	log.info "Turning Fan On";
    def cmds = setFanSpeed(state.lastFanSpeed ?: 1)
    log.trace "Fan On commands: ${cmds}"
    return cmds
}

def setFanSpeed(speed) {	
    def fanNow = device.currentValue("fanSpeed") ?: 0    //save fanspeed before changing speed so it can be resumed when turned back on    
	log.info "Requested fanSpeed is ${speed}. Current Fan speed is ${fanNow}"	    
        
    def cmds = []
    def setSpeed = Math.max(Math.min(speed?.intValue() ?: 0, 6), 0)
    if (speed != fanNow) {
    	// only update if the new fan speed is different than the current fan speed
        state.lastFanSpeed = fanNow
        log.trace "state.lastFanSpeed set to ${state.lastFanSpeed}"
        if (setSpeed == 5) { setSpeed = 4 }
	    log.info "Adjusting Fan Speed to ${childDeviceNames[setSpeed]}"        
        //cmds << zigbee.writeAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", setSpeed))
        //cmds << "st wattr 0x${device.deviceNetworkId} 0x01 0x0202 0x0000 0x0030 {${String.format("%02d", setSpeed)}}"
        // TODO: Figure out how to send a response for the fan of 01 (on) or 00 (off)
        cmds << "st wattr 0x${device.deviceNetworkId} 0x01 ${FAN_CLUSTER_ID} ${FAN_ATTR_ID} 0x0030 {${String.format("%02d", setSpeed)}}" << "delay ${DEFAULT_DELAY}"
    }
    
    cmds << refreshFan()
    log.trace "Set Fan Speed Returning ${cmds}"    
    return cmds
}

def setLevel(value, rate = null) {
	log.debug "Setting level to ${value}"

    def level = Math.max(Math.min(value?.intValue() ?: 0, 100), 0)
    rate = Math.max(Math.min(rate?.intValue() ?: 0, state?.dimRate), 0)
    log.info "Adjusting Light Brightness: ${level} : ${rate}"
    def cmds = zigbee.setLevel(level, rate) << refreshLight()
    log.trace "Set Level Returning ${cmds}"    
    return cmds
}

def raiseFanSpeed() {
	log.info "Raising fan speed"
	def currentValue = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (4 == currentValue) { currentValue = 5 }
	return setFanSpeed(Math.min(currentValue + 1, 6))
}

def lowerFanSpeed() {
	log.info "Lowering fan speed"
	def currentValue = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (6 == currentValue) { currentValue = 5 }
	return setFanSpeed(Math.max(currentValue - 1, 0))
}

// Child handling
def childOff(String deviceNetworkId) {
	log.info "Parent recieved 'off' command from ${deviceNetworkId}"
    def endpointId = getEndpointId(deviceNetworkId)
    // 1..6 is a fan, 7 is the light
    return (1..6).contains(endpointId) ? off() : lightOff() //setFanSpeed(0) : off()
}

def childOn(String deviceNetworkId) {
	log.info "Parent recieved 'on' command from ${deviceNetworkId}"
    def endpointId = getEndpointId(deviceNetworkId)
    // 1..6 is a fan, 7 is the light
    return (1..6).contains(endpointId) ? setFanSpeed(endpointId) : lightOn()
}

def childRefresh(String deviceNetworkId) {
	log.info "Parent recieved 'refresh' command from ${deviceNetworkId}"
    def endpointId = getEndpointId(deviceNetworkId)
    // 1..6 is a fan, 7 is the light    
    return (1..6).contains(endpointId) ? refreshFan() : refreshLight()
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
    //ArrayList cmds = refreshLight() << "delay ${DEFAULT_DELAY}" << refreshFan()
    ArrayList cmds = [] << refreshFan()
    //ArrayList cmds = [ zigbee.readAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID), zigbee.onOffRefresh(), zigbee.levelRefresh() ]
    log.trace "Refresh commands: ${cmds}"
    return cmds
}

private refreshFan() {
	log.info "Refreshing fan..."
    def cmds = "st rattr 0x${device.deviceNetworkId} 0x01 0x0202 0x0000"
    //return zigbee.readAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID)
    log.trace "Refresh Fan commands: ${cmds}"    
    return cmds
}

private refreshLight() {
	log.info "Refreshing light..."
    //ArrayList cmds = zigbee.onOffRefresh() + zigbee.levelRefresh()
    ArrayList cmds = [
    	"st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.ONOFF_CLUSTER} 0x0000",
        "delay ${DEFAULT_DELAY}",
        "st rattr 0x${device.deviceNetworkId} 0x01 ${zigbee.LEVEL_CONTROL_CLUSTER} 0x0000",
    ]
    log.trace "Refresh light commands: ${cmds}"
	return cmds
}

private createChildDevice(String namespace = "stussy2112", String typeName, String deviceNetworkId, Map properties) {
    if (!getChildDevice(deviceNetworkId)) {
        log.debug "Creating child ${namespace}.${typeName} - ${deviceNetworkId} : ${properties}"
    	return addChildDevice(namespace, typeName, deviceNetworkId, device.hubId, properties)
    } else {
        log.debug "Child exists: ${deviceNetworkId}"
    }
}

private void createChildDevices() {
    log.info "Creating Child Devices..."
    childDeviceNames.findAll { 
    	![0,5].contains(it.key) 
    }.each { key, value -> 
        def networkId = "${device.deviceNetworkId}-ep${key}"
        def namespace = "stussy2112"
        def typeName = "Zigbee Fan Controller - Fan Speed Child Device"
        def properties = [completedSetup: true, label: "${device.displayName} ${value}", isComponent: true, componentName: "fanMode${key}", componentLabel: value]

        if (7 == key) {
            typeName = "Zigbee Fan Controller - Light Child Device"
            properties = [completedSetup: true, label: "${device.displayName} ${value}", isComponent: false, componentName: "fanLight", componentLabel: value]
        }
        def childDevice = createChildDevice(typeName, networkId, properties)
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

private getChildDevice(String deviceNetworkId) {
    def child = getChildDevices().find { it.deviceNetworkId == deviceNetworkId }
    if (!child) {
        log.error "Child device ${deviceNetworkId} not found"
    }
    return child
}

private Integer getEndpointId(String deviceNetworkId) {
    def deviceId = deviceNetworkId.split("\\-ep")[1] as Integer
}

private Map parseCatchAllMessage(String description) {
	Map rtnVal = [:]    
    
    if (description?.startsWith("catchall:")) {
		log.info "Parsing catchall message: ${description}"
        def parsed = zigbee.parse(description)
        log.trace "Parsed catchall: ${parsed}"
        Map clusterNames = [ 0x0006:"Light", 0x0008:"Level", 0x0202:"Fan", 0x8021:"Bind" ]
        // profile id for responses is 0104 (0x0104) - Home Automation
        if (0x0104 == parsed.profileId) {
            if (0 < parsed.data.size()) {
        		// if the data payload is '0', all was good and there is nothing to do
            	if (0x0000 == parsed.data[0]) {
                	log.debug "${clusterNames[parsed.clusterId]} event executed successfully."
                } else {
                	log.warn "${clusterNames[parsed.clusterId]} returned ${parsed.data}"
                }
            }    	
        } else {
            log.trace "${clusterNames[parsed.clusterId] ?: parsed.clusterId} Event Generated"
            
            if (0x8021 == parsed.clusterId && 0 == parsed.data.size()) {
                log.error "Bind result not present."
            } else {
                def bindResult = parsed.data[1]
                log.debug "Bind response: ${parsed.data}: ${bindResults[bindResult]}"
                rtnVal = [ name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID] ]
            }
        }
    }
    
    return rtnVal
}

private void syncChildFanDevices(speed) {
	log.info "Syncing child fans: ${speed}"
    getChildDevices().findAll { (1..6).contains(getEndpointId(it.deviceNetworkId)) }.each { 
    	def eventValue = speed == getEndpointId(it.deviceNetworkId) ? "on" : "off"
        if (eventValue != it.currentValue("switch")) {
        	log.trace "Sending child event '${eventValue}' to ${it}"
    		it.sendEvent(name:"switch", value:eventValue)
        }
    }
}

private void updateChildrenLabels() {
	log.info "Updating child labels"    
    childDeviceNames.findAll { ![0,5].contains(it.key) }.each { key, value -> getChildDevice("${device.deviceNetworkId}-ep${key}")?.label = "${device.displayName} ${value}" }
}