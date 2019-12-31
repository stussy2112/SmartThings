metadata {
	definition (name: "Zigbee Fan Controller - Fan Speed Child Device", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.switch") {
		capability "Actuator"
        capability "Switch"
        
        attribute "fanMode", "string"
	}
    
    tiles(scale:2) {    
    	standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
            state "on", label: '${name}', action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "turningOff"
     		state "off", label: '${name}', action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn", defaultState: true      
			state "turningOn", label: "turning on", action:"off", icon:"st.thermostat.fan-on", backgroundColor:"#00a0dc"
            state "turningOff", label:"turning off", action:"on", icon:"st.thermostat.fan-off", backgroundColor:"#ffffff"
        }
    	main(["switch"])
		details(["switch"]) 
    }
}

def getName() {
	return device.componentLabel
}

def installed() {
	log.info "Installed ${device.name} : ${device.deviceNetworkId}"
	sendEvent(name: "switch", value: "off")
}

def parse(String description) {
}

def off() {
	log.info "CHILD ${device.label} TURNED OFF"
    sendEvent(name: "switch", value: "off")
    parent.setFanSpeed(0)
}

def on() {
	log.info "CHILD ${device.label} TURNED ON"
    // Get just the integer part from the deviceNetworkId
    def speed = Integer.parseInt(device.deviceNetworkId.split("\\.")[1])
    
    sendEvent(name: "switch", value: "on")
    log.debug "Sending fanSpeed event: ${speed}"
    parent.setFanSpeed(speed)
}