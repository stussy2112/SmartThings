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
	definition (name: "Hampton Bay Fan Light", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.switch", runLocally: false, executeCommandsLocally: false, mnmn:"SmartThings", vid: "generic-dimmer") {
		capability "Actuator"
		capability "Configuration"
		capability "Switch"
		capability "Switch Level"
		capability "Refresh"
		capability "Health Check"
        capability "Light"
		capability "Sensor"
	}
    preferences {
    	section {
        	input("dimRate", "enum", title: "Dim Rate", options: ["Instant", "Normal", "Slow", "Very Slow"], defaultValue: "Instant", required: true, displayDuringSetup: true)
        }
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
	}
}

private Map getDimRates() { [ "Instant":0, "Normal":35, "Slow":50, "Very Slow":100 ] }

// parse events into attributes
def configure() {
    state.dimRate = dimRates[dimRate]?.intValue() ?: dimRates["Instant"]
    device.updateSetting("dimRate", state.dimRate)
    log.debug "Dim rate: entered = ${dimRate}, actual = ${state.dimRate}"
    response(refresh())
}

def installed() {
	log.debug "Installed ${device.name} : ${device.deviceNetworkId}"
	// This is set to a default value, but it is the responsibility of the parent to set it to a more appropriate number
	sendEvent(name: "checkInterval", value: 30 * 60, displayed: false, data: [protocol: "zigbee"])
    response(configure())
}

def updated() {
    log.debug "Executing 'updated()' ${device.name} : ${device.deviceNetworkId}"
    state.dimRate = dimRates[dimRate]?.intValue() ?: dimRates["Instant"]
    log.debug "Dim rate: entered = ${dimRate}, actual = ${state.dimRate}"
}

def on() {
    log.debug "Executing 'on', passing switch event to parent, sending event"
	parent.childOn(device.deviceNetworkId)
}

def off() {
    log.debug "Executing 'off', passing switch event to parent"
	parent.childOff(device.deviceNetworkId)
}

def ping() {
	log.debug "Executing 'ping()'"
    refresh()
	// Intentionally left blank as parent should handle this
}

def uninstalled() {
	log.debug "Executing 'uninstalled()'"
}

// handle commands
def setLevel(value, rate = null) {
    log.debug "Executing 'setLevel': ${value},${rate}, passing level event to parent"
    rate = Math.max(Math.min(rate?.intValue() ?: 0, state.dimRate), 0)
    parent.childSetLevel(device.deviceNetworkId, value, rate)
}

def refresh() {
	log.debug "Executing 'refresh(), passing request to parent"
    parent.childRefresh(device.deviceNetworkId)
}
/*
def setState(_variable, _value){
  state."$_variable" = _value
}

def getState (_variable){
  state."$_variable" = _value
}*/