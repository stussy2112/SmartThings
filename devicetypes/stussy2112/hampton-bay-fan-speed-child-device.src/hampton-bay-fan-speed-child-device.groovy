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
	definition (name: "Hampton Bay Fan Speed Child Device", namespace: "stussy2112", author: "Sean Williams", mcdSync: true, runLocally: false, executeCommandsLocally: false, ocfDeviceType: "oic.d.switch", vid: "generic-switch") {
		capability "Actuator"
        capability "Switch"
		capability "Sensor"
		capability "Refresh"
		capability "Health Check"
        
		attribute "fanMode", "string"
	}
    
    tiles(scale:2) {    
    	standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
            state "on", label: '${name}', action: "off", icon: "st.Lighting.light24", backgroundColor: "#00a0dc", nextState: "turningOff"
     	    state "off", label: '${name}', action: "on", icon: "st.Lighting.light24", backgroundColor: "#ffffff", nextState: "turningOn", defaultState: true
	    	state "turningOn", label: "turning on", action:"off", icon:"st.Lighting.light24", backgroundColor:"#00a0dc"
            state "turningOff", label:"turning off", action:"on", icon:"st.Lighting.light24", backgroundColor:"#ffffff"
        }

    	standardTile("refresh", "device.refresh", decoration: "flat", width: 6, height: 1) {
		  state "default", label:"refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
	  	}
    }
}

def installed() {
    log.info "Installed ${device.name} : ${device.deviceNetworkId}"
    // This is set to a default value, but it is the responsibility of the parent to set it to a more appropriate number
    sendEvent(name: "checkInterval", value: 30 * 60, displayed: false, data: [protocol: "zigbee"])
}

def parse(String description) {
    log.debug "Parsing '${description}'"
    // TODO: handle 'on' attribute
}

def off() {
    log.debug "Executing 'off', passing switch event to parent"
    parent.childOff(device)
}

def on() {
    log.debug "Executing 'on', passing switch event to parent"
    //parent.childOn(device.deviceNetworkId)
    parent.childOn(device)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    log.debug "Executing 'ping()' from child: ${device.label}"
    refresh()
    // Intentionally left blank as parent should handle this
}

void refresh() {
    log.debug "Executing 'refresh(), passing request to parent"
    parent.childRefresh(device)
}