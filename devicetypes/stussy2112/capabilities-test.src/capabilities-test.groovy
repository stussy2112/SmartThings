/**
 *  Capabilities Test
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
	definition (name: "Capabilities Test", namespace: "stussy2112", author: "Sean Williams", cstHandler: true) {
		capability "Thermostat Fan Mode"
		capability "Activity Lighting Mode"
		capability "Activity Sensor"
		capability "Air Conditioner Fan Mode"
		capability "Air Conditioner Mode"
		capability "Air Purifier Fan Mode"
		capability "Air Quality Sensor"
		capability "Fan Oscillation Mode"
		capability "Fan Speed"
		capability "Ocf"
		capability "Stateless Fanspeed Button"
		capability "Stateless Fanspeed Mode Button"
		capability "Stateless Media Playback Button"
		capability "Stateless Power Button"
		capability "Stateless Power Toggle Button"
		capability "Stateless Robot Cleaner Action Button"
		capability "Stateless Robot Cleaner Home Button"
		capability "Stateless Robot Cleaner Toggle Button"
		capability "Stateless Set Channel Button"
		capability "Stateless Set Channel By Content Button"
		capability "Stateless Set Channel By Name Button"
		capability "Stateless Temperature Button"
		capability "Step Sensor"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'thermostatFanMode' attribute
	// TODO: handle 'supportedThermostatFanModes' attribute
	// TODO: handle 'lightingMode' attribute
	// TODO: handle 'activity' attribute
	// TODO: handle 'fanMode' attribute
	// TODO: handle 'supportedAcFanModes' attribute
	// TODO: handle 'airConditionerMode' attribute
	// TODO: handle 'supportedAcModes' attribute
	// TODO: handle 'airPurifierFanMode' attribute
	// TODO: handle 'supportedAirPurifierFanModes' attribute
	// TODO: handle 'airQuality' attribute
	// TODO: handle 'fanOscillationMode' attribute
	// TODO: handle 'fanSpeed' attribute
	// TODO: handle 'n' attribute
	// TODO: handle 'icv' attribute
	// TODO: handle 'dmv' attribute
	// TODO: handle 'di' attribute
	// TODO: handle 'pi' attribute
	// TODO: handle 'mnmn' attribute
	// TODO: handle 'mnml' attribute
	// TODO: handle 'mnmo' attribute
	// TODO: handle 'mndt' attribute
	// TODO: handle 'mnpv' attribute
	// TODO: handle 'mnos' attribute
	// TODO: handle 'mnhw' attribute
	// TODO: handle 'mnfv' attribute
	// TODO: handle 'mnsl' attribute
	// TODO: handle 'st' attribute
	// TODO: handle 'vid' attribute
	// TODO: handle 'availableFanspeedButtons' attribute
	// TODO: handle 'availableFanspeedModeButtons' attribute
	// TODO: handle 'availableMediaPlaybackButtons' attribute
	// TODO: handle 'availablePowerButtons' attribute
	// TODO: handle 'availablePowerToggleButtons' attribute
	// TODO: handle 'availableRobotCleanerActionButtons' attribute
	// TODO: handle 'availableRobotCleanerHomeButtons' attribute
	// TODO: handle 'availableRobotCleanerToggleButtons' attribute
	// TODO: handle 'availableTemperatureButtons' attribute
	// TODO: handle 'steps' attribute
	// TODO: handle 'goal' attribute

}

// handle commands
def fanOn() {
	log.debug "Executing 'fanOn'"
	// TODO: handle 'fanOn' command
}

def fanAuto() {
	log.debug "Executing 'fanAuto'"
	// TODO: handle 'fanAuto' command
}

def fanCirculate() {
	log.debug "Executing 'fanCirculate'"
	// TODO: handle 'fanCirculate' command
}

def setThermostatFanMode() {
	log.debug "Executing 'setThermostatFanMode'"
	// TODO: handle 'setThermostatFanMode' command
}

def setLightingMode() {
	log.debug "Executing 'setLightingMode'"
	// TODO: handle 'setLightingMode' command
}

def setFanMode() {
	log.debug "Executing 'setFanMode'"
	// TODO: handle 'setFanMode' command
}

def setAirConditionerMode() {
	log.debug "Executing 'setAirConditionerMode'"
	// TODO: handle 'setAirConditionerMode' command
}

def setAirPurifierFanMode() {
	log.debug "Executing 'setAirPurifierFanMode'"
	// TODO: handle 'setAirPurifierFanMode' command
}

def setFanOscillationMode() {
	log.debug "Executing 'setFanOscillationMode'"
	// TODO: handle 'setFanOscillationMode' command
}

def setFanSpeed() {
	log.debug "Executing 'setFanSpeed'"
	// TODO: handle 'setFanSpeed' command
}

def postOcfCommand() {
	log.debug "Executing 'postOcfCommand'"
	// TODO: handle 'postOcfCommand' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}

def setChannel() {
	log.debug "Executing 'setChannel'"
	// TODO: handle 'setChannel' command
}

def setChannelByContent() {
	log.debug "Executing 'setChannelByContent'"
	// TODO: handle 'setChannelByContent' command
}

def setChannelByName() {
	log.debug "Executing 'setChannelByName'"
	// TODO: handle 'setChannelByName' command
}

def setButton() {
	log.debug "Executing 'setButton'"
	// TODO: handle 'setButton' command
}