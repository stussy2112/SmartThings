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
import physicalgraph.device.HubAction

metadata {
	definition (name: "ZigBee Fan - Child Fan", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.fan", minHubCoreVersion: '000.025.0000') {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Fan Speed"
		capability "Health Check"

		command "fanOff"
		command "fanOn"
		command "lowerFanSpeed"
		command "setFanSpeed"
		command "raiseFanSpeed"
	}
    
	tiles(scale: 2) {
		multiAttributeTile(name: "fanSpeed", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
				attributeState "0", label: "Fan Off", action: "fanOn", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "adjusting"
				attributeState "1", label: "low", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "adjusting"
				attributeState "2", label: "medium", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#1e9cbb", nextState: "adjusting"
				attributeState "3", label: "high", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#184f9c", nextState: "adjusting"
				attributeState "4", label: "max", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#153591", nextState: "adjusting"
				attributeState "6", label: "Comfort Breeze™", action: "fanOff", icon: "st.thermostat.fan-on", backgroundColor: "#90d2a7", nextState: "turningOff"
				attributeState "adjusting", label: "Adjusting...", action: "fanOn", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
				attributeState "turningOff", label:"Turning Fan Off", action: "fanOn", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
				attributeState "turningOn", label:"Turning Fan On", action: "fanOff", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
			}
			tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "raiseFanSpeed"
				attributeState "VALUE_DOWN", action: "lowerFanSpeed"
			}
		}

		standardTile("refresh", "refresh", decoration: "flat", width: 6, height: 1) {
			state "default", label:"refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
	  	}
        
		childDeviceTiles("all")
	}
}

// Globals
private getDEFAULT_DELAY() { 100 }
private getFAN_CLUSTER() { 0x0202 }
private getFAN_ATTR_ID() { 0x0000 }

private Map getFanSpeedNames() { [ 0:"Fan Off", 1:"Low", 2:"Medium", 3:"High", 4:"Max", 5:"Off", 6:"Comfort Breeze™", 7:"Light" ] }

def installed() {
   log.info "Installed ${device.displayName}"
}

// Parse incoming device messages to generate events
public parse(String description) {
    log.info "Parsing: ${description}"
    
    List<HubAction> rtnVal = []
    
    Map event = zigbee.getEvent(description)
    log.trace "Got event: ${event}"
    if (event) {
        log.info "Handled event detected on controller, sending event"
        sendEvent(event)
    	return null
        //log.debug "Sending ${event} to ${getChildDevice("${device.deviceNetworkId}-ep7")}"
        //rtnVal << getChildDevice("${device.deviceNetworkId}-ep7")?.createEvent(name: event.name, value: event.value)
        //getChildDevice("${device.deviceNetworkId}-ep7")?.sendEvent(name: event.name, value: event.value)
    } else {
        // "read attr" events may need to set the fan speed
        if (description.startsWith("read attr -")) {
    		Map descMap = zigbee.parseDescriptionAsMap(description)
        	log.debug "Parsed map: ${descMap}"
            // Check if the command is for the fan
            if (descMap && descMap.clusterInt == FAN_CLUSTER) {
            	log.info "Fan event detected on controller: ${description}"
                Integer speed = Math.max(Integer.parseInt(descMap.value), 0)
                rtnVal << setFanSpeed(speed)
            }            
        }
    	// Handle a catchall description
        rtnVal << parseCatchAll(description)
        
    	log.info "Events sent: ${rtnVal.size()}"
        return rtnVal
        /*
        def descMap = zigbee.parseDescriptionAsMap(description)
        log.debug "Parsed map: ${descMap}"
        if (descMap && descMap.clusterInt == FAN_CLUSTER) {
        	if (descMap.attrInt == FAN_ATTR_ID) {
            	log.info "Fan event detected on controller: ${description}"
                def speed = Math.max(Integer.parseInt(descMap.value), 0)
            	syncChildFanDevices(speed)
            	rtnVal << createEvent(name: "fanSpeed", value: speed)
                //event = createEvent(name: "fanSpeed", value: speed)
            }
        }
    */
    }
    
    log.warn "DID NOT PARSE MESSAGE for description : $description"
    log.debug "Unparsed description: ${description}"
    return null
}

def off() {
    fanOff()
}

def on() {
    fanOn()
}

def fanOff() {
    log.info "Turning Fan Off";
    setFanSpeed(0)
}

def fanOn() {
    log.info "Turning Fan On";
    setFanSpeed(state.lastFanSpeed ?: 1)
}

def raiseFanSpeed() {
    log.info "Raising fan speed"
    Integer currentValue = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (4 == currentValue) { currentValue = 5 }
    setFanSpeed(Math.min(currentValue + 1, 6))
}

def lowerFanSpeed() {
    log.info "Lowering fan speed"
    Integer currentValue = device.currentValue("fanSpeed")?.intValue() ?: 0
    if (6 == currentValue) { currentValue = 5 }
    setFanSpeed(Math.max(currentValue - 1, 0))
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.info "Pinged: ${device.displayName}"
}

def poll() {
	log.info "Polling ${device.displayName}"
	refresh()
}

def refresh() {
	log.info "Refreshing: ${device.displayName}"
	parent.childRefresh(device.deviceNetworkId)
}

/**
 * Private members
 **/
private List refreshFan() {
    log.info "Refreshing fan..."
    def cmds = [ "st rattr 0x${device.deviceNetworkId} 0x01 0x0202 0x0000" ]
    //return zigbee.readAttribute(FAN_CLUSTER_ID, FAN_ATTR_ID)
    log.trace "Refresh Fan commands: ${cmds}"    
    return cmds
}

private List<HubAction> setFanSpeed(Integer speed) {
	Integer fanNow = device.currentValue("fanSpeed") ?: 0 
    log.info "${device.displayName}: Requested fanSpeed is ${speed}."
    
    List<HubAction> cmds = []
    // only update if the new fan speed is different than the current fan speed
    if (speed != fanNow) {
        state.lastFanSpeed = fanNow
		cmds << parent.setFanSpeed(speed)
    }
    
    cmds << parent.refreshFan().collect { new HubAction(it) }
    
    log.trace "Child Set Fan Speed Returning ${cmds.size()}"  
    return cmds
}