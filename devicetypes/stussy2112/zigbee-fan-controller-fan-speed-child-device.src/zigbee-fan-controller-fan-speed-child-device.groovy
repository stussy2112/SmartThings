metadata {
	definition (name: "Zigbee Fan Controller - Fan Speed Child Device", namespace: "stussy2112", author: "Sean Williams", ocfDeviceType: "oic.d.switch", runLocally: true, executeCommandsLocally: true, genericHandler: "Zigbee") {
        capability "Switch"
	}
    tiles(scale:2) {
    
    	standardTile("fanSpeed", "device.switch", width: 2, height: 2, canChangeIcon: true) {
     		state "off", label:"off", action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn"
			state "off01", label: "low", action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn"
           	state "off02", label: "medium", action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn"
			state "off03", label: "high", action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn"
			state "off04", label: "max", action: "on", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "turningOn"
            state "on01", label: "low", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#79b821", nextState: "turningOff"
           	state "on02", label: "medium", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#79b821", nextState: "turningOff"
			state "on03", label: "high", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#79b821", nextState: "turningOff"
			state "on04", label: "max", action: "off", icon: "st.thermostat.fan-on", backgroundColor: "#79b821", nextState: "turningOff"
			state "adjusting", label: "Adjusting Fan", action:"off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc"
            state "turningOff", label:"Turning Fan Off", action:"on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"adjusting"
        }
    	main(["fanSpeed"])
        
    }
}

def off() {
	parent.fanOff()
}

def on() {
	log.info "CHILD ${getDataValue('speedVal')} TURNED ON"    
    parent.setFanSpeed(getDataValue("speedVal"))
}