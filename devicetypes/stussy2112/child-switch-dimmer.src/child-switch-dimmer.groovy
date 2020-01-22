/**
 *  Hampton Bay Fan Light
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
	definition (name: "Child Switch Dimmer", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.light", mnmn:"SmartThings", vid: "generic-dimmer") {
		capability "Actuator"
		capability "Switch"
		capability "Switch Level"
		capability "Refresh"
		capability "Health Check"
		capability "Light"
	}
    preferences {
        input("dimRate", "enum", title: "Dim Rate", options: [[0:"Instant"], [35:"Normal"], [50:"Slow"], [100:"Very Slow"]], defaultValue: 35, default: 35, required: true, displayDuringSetup: true)
    }

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {    
    	multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: false) {
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn", defaultState: true
                attributeState "turningOn", label:'Turning on', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'Turning off', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL", label: "Brightness", range:"(20..100)") {
                attributeState "level", action:"switch level.setLevel"
            }
        }
		standardTile("refresh", "device.switch", decoration: "flat", width: 6, height: 1) {
		  state "default", label:"refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
	  	}

		main "switch"
		details(["switch", "refresh"])
	}
}

private static final Map getDimRates() { [ "Instant":0, "Normal":35, "Slow":50, "Very Slow":100 ] }

def installed() {
	log.debug "Installed ${device.name} : ${device.deviceNetworkId}"
    state.dimRate = 35
    device.updateSetting("dimRate", state.dimRate)
	// This is set to a default value, but it is the responsibility of the parent to set it to a more appropriate number
	sendEvent(name: "checkInterval", value: 30 * 60, displayed: false, data: [protocol: "zigbee"])
}

def updated() {
    log.debug "Executing 'updated()' ${device.name} : ${device.deviceNetworkId}"
    state.dimRate = dimRate == null ? 35 : Integer.parseInt(dimRate)
    device.updateSetting("dimRate", state.dimRate)	
    log.debug "Dim rate: entered = ${dimRate}, actual = ${state.dimRate}"
}

def refresh() {
	log.debug "Executing 'refresh(), passing request to parent"
    parent.childRefresh(device.deviceNetworkId)
}

def on() {
    log.debug "Executing 'on', passing switch event to parent, sending event"
	parent.childOn(device.deviceNetworkId)
}

def off() {
    log.debug "Executing 'off', passing switch event to parent"
	parent.childOff(device.deviceNetworkId)
}

def uninstalled() {
	log.debug "Executing 'uninstalled()'"
}

// handle commands
def setLevel(value, rate = null) {
    rate = rate == null ? state.dimRate : rate
    log.debug "Executing 'setLevel': ${value},${rate}, passing level event to parent"
    parent.childSetLevel(device.deviceNetworkId, value, rate)
}