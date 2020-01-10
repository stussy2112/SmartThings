metadata {
	definition (name: "Zigbee Fan Controller - Fan Speed Child Device", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.switch", vid: "generic-switch") {
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
    }
}

def installed() {
	log.info "Installed ${device.name} : ${device.deviceNetworkId}"
	sendEvent(name: "switch", value: "off")
}

def parse(String description) {
}

def off() {
	log.info "CHILD ${device.label} TURNED OFF, passing switch event to parent"
    parent.childOff(device.deviceNetworkId)
}

def on() {
	log.info "CHILD ${device.label} TURNED ON, passing switch event to parent"
    parent.childOn(device.deviceNetworkId)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.info "Refreshing from child: ${device.label}"
	// Intentionally left blank as parent should handle this
}

void refresh() {
	log.info "Refreshing from child fan switch..."
	parent.childRefresh(device.deviceNetworkId)
}