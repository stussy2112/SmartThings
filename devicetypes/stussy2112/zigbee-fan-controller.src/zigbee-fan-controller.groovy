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

/*import physicalgraph.app.ChildDeviceWrapper
import physicalgraph.device.Device
import physicalgraph.device.HubAction
import physicalgraph.zigbee.zcl.DataType
	
// Globals
private getDEFAULT_DELAY() { 100 }
private getBIND_CLUSTER() { 0x8021 }
private getFAN_CLUSTER() { 0x0202 }
*/
metadata {
	definition (name: "ZigBee Fan Controller", namespace: "stussy2112", author: "Sean Williams", cstHandler: true, ocfDeviceType: "oic.d.fan", mnmn: "SmartThings", vid:"x.com.st.fanspeed") {
		capability "Actuator"
		capability "Refresh"
		capability "Health Check"
        
	  	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", model: "HDC52EastwindFan", deviceJoinName: "Zigbee Fan Controller", ocfDeviceType: "oic.d.fan"
	}

	tiles {

		standardTile("refresh", "device.switch", width: 6, height: 1, inactiveLabel: false, decoration: "flat") {
			state "default", label: "refresh", action: "refresh.refresh", icon: "st.secondary.refresh"
		}
	}
}
	
// Globals
private getDEFAULT_DELAY() { 100 }
private getBIND_CLUSTER() { 0x8021 }
private getFAN_CLUSTER() { 0x0202 }
private getFAN_ATTR_ID() { 0x0000 }

def refresh() {
	log.info "Refreshing '${device.displayName}': ZigBee Fan Controller"
}
    