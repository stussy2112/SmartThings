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
	definition (name: "ZigBee Fan Controller", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.fan", genericHandler: "Zigbee") {
		capability "Actuator"
		capability "Configuration"
		capability "Fan Speed"
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
				/*attributeState "off", label: "Fan Off", action: "fanOn", icon: "st.Lighting.light24", backgroundColor: "#ffffff", nextState: "adjusting"
				attributeState "on", label: "Fan On", action: "fanOff", icon: "st.Lighting.light24", backgroundColor: "#00a0dc", nextState: "adjusting"*/
				attributeState "0", label: "Fan Off", action: "fanOn", icon: "st.Lighting.light24", backgroundColor: "#ffffff", nextState: "adjusting"
				attributeState "1", label: "low", action: "setFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "2", label: "medium", action: "setFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "3", label: "high", action: "setFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "4", label: "max", action: "setFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "6", label: "Comfort Breeze™", action: "switch.off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "adjusting", label: "Adjusting Fan", action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
				attributeState "turningOff", label:"Turning Fan Off", action:"fanOn", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
			}
			tileAttribute ("device.level", label: "brightness", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "raiseFanSpeed"
				attributeState "VALUE_DOWN", action: "lowerFanSpeed"
			}
		}
        
        childDeviceTiles("all")
	}
}

// Globals
private getFAN_CLUSTER_ID() { 0x0202 }
private getFAN_ATTR_ID() { 0x0000 }

private getChildDeviceName() {
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

def configure() {
	log.info "Configuring Reporting and Bindings."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	//sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    
    // OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
    def cmds = [
      	/*Set long poll interval
      	"raw 0x0020 {11 00 02 02 00 00 00}",
        "delay 100",
      	"send 0x${device.deviceNetworkId} 1 1",
        "delay 100",
        
	  	"raw 0x0020 {11 00 02 1C 00 00 00}",
        "delay 100",
      	"send 0x${device.deviceNetworkId} 1 1",
        "delay 100",*/
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
	
    updated()
	if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
		sendEvent(name: "level", value: 100)
	}
    
    sendEvent(name: "fanSpeed", value: 1)
    state.lastFanSpeed = 1
    for (i in 1..2) {
    	sendEvent(name: "switch", value: "on")
    	sendEvent(name: "switch", value: "off")
    }
    
    response(refresh())
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.info "Parsing: ${description}"
    
    def event = zigbee.getEvent(description)
	log.debug "Got event: ${event}"
    if (event) {
        log.info "Light event detected on controller"
        log.debug "Sending ${event} to ${getChildDevice("${device.deviceNetworkId}-ep7")}"
        getChildDevice("${device.deviceNetworkId}-ep7")?.sendEvent(event)
    }
    
    if (description?.startsWith("catchall:")) {
    	def descMap = zigbee.parseDescriptionAsMap(description)
    	log.debug "Parsed catchall map: ${descMap}"
        if (descMap.data && descMap.data[0] == "00") {
        	log.debug "Device Reporting config response: ${zigbee.parse(description)}"
            event = createEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
        }
    }

    // Check if the command is for the fan
    def descMap = zigbee.parseDescriptionAsMap(description)
    log.debug "Parsed map: ${descMap}"
    if (descMap && descMap.clusterInt == FAN_CLUSTER_ID && descMap.attrInt == FAN_ATTR_ID) {
        log.info "Fan event detected on controller"
        def speed = Math.max(Integer.parseInt(descMap.value), 0)
        event = createEvent(name: "fanSpeed", value: speed)
        syncChildFanDevices(speed)
    }

	if (event) {
    	log.debug "Event sent: ${event}"
        sendEvent(event)
        return
    }
    
    log.warn "DID NOT PARSE MESSAGE for description : $description"
	log.debug "parsedDescriptionAsMap: ${descMap}"
}

def updated() {
	log.info "Updated..."
    
    def dimRates = ["Instant":0, "Normal":35, "Slow":50,"Very Slow":100]
    state.dimRate = dimRates[dimRate] ?: 0
    
    if (refreshChildren) {
    	deleteChildren()
        device.updateSetting("refreshChildren", false)
    }
    
    createChildDevices()
    
	if (state.oldLabel != device.label) {
    	updateChildrenLabels()
    	state.oldLabel = device.label
    }
    
    response(refresh() + configure())
}

def off() {
    log.info("Turning Off Light")
    def cmds = zigbee.off() + zigbee.onOffRefresh() + zigbee.levelRefresh()
    return cmds
}

def on() {
    log.info("Turning On Light")    
    def cmds = zigbee.on() + zigbee.onOffRefresh() + zigbee.levelRefresh()
    return cmds
}

def fanOff() {
	log.info "Turning Fan Off";
    return setFanSpeed(0)
}

def fanOn() {
	log.info "Turning Fan On";
    return setFanSpeed(state.lastFanSpeed)
}

def setFanSpeed(speed) {	
    def fanNow = device.currentValue("fanSpeed") ?: 0    //save fanspeed before turning off so it can be resumed when turned back on    
	log.info "Requested fanSpeed is ${speed}. Current Fan speed is ${fanNow}"	    
    
    def setSpeed = Math.max(Math.min(speed?.intValue() ?: 0, 6), 0)
    if (setSpeed == 5) { 
        setSpeed = 4 
    }
    
    // only update if the new fan speed is different than the current fan speed
    def cmds = []
    if (speed != fanNow) {
        state.lastFanSpeed = fanNow
	    log.info "Adjusting Fan Speed to ${childDeviceName[setSpeed]}"        
        cmds << zigbee.writeAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID, DataType.ENUM8, String.format("%02d", setSpeed))
    }
    
    return cmds << refreshFan()
}

def setLevel(value, rate = null) {
	log.debug "value is ${value}"

    def level = Math.max(Math.min(value?.intValue() ?: 0, 100), 0)
    log.info "Adjusting Light Brightness: ${level}"
    def cmds = [ zigbee.setLevel(level, rate ?: state?.dimRate ?: 0) ]
    if (0 < level) {
    	cmds += zigbee.on()
    } else {
    	cmds += zigbee.off()
    }
    
    cmds += zigbee.levelRefresh()
    cmds += zigbee.onOffRefresh()
    
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
	return setFanSpeed(Math.max(currentValue - 1, 0))
}

// Child handling
def childOff(String deviceNetworkId) {
	log.info "Parent recieved 'off' command from ${deviceNetworkId}"
    def endpointId = getEndpointId(deviceNetworkId)
    // 1..6 is a fan, 7 is the light
    return (1..6).contains(endpointId) ? setFanSpeed(0) : off()
}

def childOn(String deviceNetworkId) {
	log.info "Parent recieved 'on' command from ${deviceNetworkId}"
    def endpointId = getEndpointId(deviceNetworkId)
    // 1..6 is a fan, 7 is the light
    return (1..6).contains(endpointId) ? setFanSpeed(endpointId) : on()
}

def childRefresh(String deviceNetworkId) {
	log.info "Parent recieved 'refresh' command from ${deviceNetworkId}"
    def endpointId = getEndpointId(deviceNetworkId)
    // 1..6 is a fan, 7 is the light    
    return (1..6).contains(endpointId) ? refreshFan() : zigbee.onOffRefresh() + zigbee.levelRefresh()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.info "Pinged..."
	return zigbee.onOffRefresh()
}

def refresh() {
	log.info "Refreshing..."
    def cmds = [ zigbee.readAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID), zigbee.onOffRefresh(), zigbee.levelRefresh() ]
    return cmds
    //return refreshFan() + refreshLight()
}

def refreshFan() {
	log.info "Refreshing fan..."
	//syncChildFanDevices(device.currentValue("fanSpeed"))
    return zigbee.readAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID)
}

def refreshLight() {
	log.info "Refreshing light..."
	/*def childLight = getChildDevice("${device.deviceNetworkId}-ep7")
    if (childLight) {
    	log.debug "Sending child events"
    	childLight.sendEvent(name: "level", value: device.currentValue("level"), linkText: "${childLight.displayName}",  descriptionText: "${device.displayName} level is ${device.currentValue("level")}")
    	childLight.sendEvent(name: "switch", value: device.currentValue("switch"), linkText: "${childLight.displayName}",  descriptionText: "${device.displayName} switch is ${device.currentValue("switch")}")
    }*/
	return zigbee.onOffRefresh() + zigbee.levelRefresh()
}

private createChildDevice(String namespace = "stussy2112", String typeName, String deviceNetworkId, Map properties) {
    def childDevice = getChildDevice(deviceNetworkId)

    if (!childDevice) {
        log.debug "Creating child ${namespace}.${typeName} - ${deviceNetworkId} : ${properties}"
    	return addChildDevice(namespace, typeName, deviceNetworkId, device.hubId, properties)
    } else {
        log.debug "Child exists: ${deviceNetworkId}"
    }
}

private createChildDevices() {
    log.info "Creating Child Devices..."
    childDeviceName.findAll { 
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
    	log.debug "Deleting ${c.deviceNetworkId}"
  		deleteChildDevice(c.deviceNetworkId)
    }
}

private getChildDevice(String deviceNetworkId) {
    def child = childDevices.find { it.deviceNetworkId == deviceNetworkId }
    log.debug 
    if (!child) {
        log.error "Child device ${deviceNetworkId} not found"
    }
    return child
}

private Integer getEndpointId(String deviceNetworkId) {
    def deviceId = deviceNetworkId.split("\\-ep")[1] as Integer
}

private void sendLightEvent(Map event) {
	log.info "Sending Light event: ${event}"
    def childLight = getChildDevice("${device.deviceNetworkId}-ep7")
    if (childLight) {
    	log.debug event.descriptionText
    	childLight.sendEvent(name:event.name, value:event.value, linkText: "${childLight.displayName}",  descriptionText: event.descriptionText?.replaceAll(device.displayName, childLight.displayName))
    }
	sendEvent(event)
}

private void syncChildFanDevices(speed) {
	log.info "Syncing child fans: ${speed}"
    getChildDevices().findAll { (1..6).contains(getEndpointId(it.deviceNetworkId)) }.each { 
    	def eventValue = speed == getEndpointId(it.deviceNetworkId) ? "on" : "off"
        if (eventValue != it.currentValue("switch")) {
        	log.debug "Sending child event '${eventValue}' to ${it}"
    		it.sendEvent(name:"switch", value:eventValue)
        }
    }
}

private void updateChildrenLabels() {
	log.info "Updating child labels"    
    childDeviceName.findAll { ![0,5].contains(it.key) }.each { key, value -> getChildDevice("${device.deviceNetworkId}-ep${key}")?.label = "${device.displayName} ${value}" }
}