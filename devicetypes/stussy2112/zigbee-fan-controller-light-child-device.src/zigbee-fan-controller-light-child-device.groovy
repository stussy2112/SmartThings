/**
 *  Zigbee Fan Controller - Light Child Device
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
	definition (name: "Zigbee Fan Controller - Light Child Device", namespace: "stussy2112", author: "Sean Williams", runLocally: true, executeCommandsLocally: true, ocfDeviceType: "oic.d.light", mnmn: "SmartThings", vid:"generic-dimmer") {
		capability "Actuator"
		capability "Switch"
		capability "Switch Level"
		capability "Sensor"
		capability "Refresh"
	}
    
    tiles(scale:2) {    
    	multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn", defaultState: true
                attributeState "turningOn", label:'Turning on', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'Turning off', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }
    }
}

def installed() {
	log.info "Installed ${device.name} : ${device.deviceNetworkId}"
	// This is set to a default value, but it is the responsibility of the parent to set it to a more appropriate number
	//sendEvent(name: "checkInterval", value: 30 * 60, displayed: false, data: [protocol: "zigbee"])
    refresh()
}

// parse events into attributes
def parse(description) {
	log.info "${device.displayName} is parsing '${description}'"
    def event = zigbee.getEvent(description)
}

// handle commands
def on() {
    log.info "Executing 'on', Passing switch event to parent"
	parent.childOn(device.deviceNetworkId)
}

def off() {
    log.info "Executing 'off', passing switch event to parent"
	parent.childOff(device.deviceNetworkId)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.info "Pinging from child light..."
	return refresh()
}

def refresh() {
	log.info "Refreshing from child light..."
	parent.childRefresh(device.deviceNetworkId)
}

def setLevel(value, rate = null) {
    log.info "Executing 'setLevel' ${value} : ${rate}, passing level event to parent"
    parent.setLevel(value, rate)
}

def uninstalled() {
	parent.delete()
}