/**
 *  MIMO2 Device Handler
 *
 *  Copyright 2016 FortrezZ, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "FortrezZ MIMO2+", namespace: "lindfield", author: "FortrezZ, LLC") {
		capability "Contact Sensor"
		capability "Alarm"
		capability "Lock"
        capability "Refresh"
        
        attribute "powered", "string"
        attribute "contact2", "string"
        
        /* relay1 (alarm) */
		command "both"
		command "off"
        
        /* relay2 (lock) */
        command "lock"
        command "unlock"
        
        fingerprint deviceId: "0x2100", inClusters: "0x5E,0x86,0x72,0x5A,0x59,0x71,0x98,0x7A"
	}
           
	tiles {
         standardTile("relay1", "device.alarm", decoration: "flat") {
            state "both", label: "Siren On", action: "off", icon: "st.alarm.alarm.alarm", backgroundColor: "#ffa500"            
			state "off", label: "Siren Off", action: "both", icon: "", backgroundColor: "#ffffff"
        }
         standardTile("relay2", "device.lock", inactiveLabel: false, decoration: "flat") {
            state "locked", label: "LEDs On", action: "unlock", icon: "st.illuminance.illuminance.light", backgroundColor: "#ff5a00"
			state "unlocked", label: 'LEDs Off', action: "lock", icon: "", backgroundColor: "#ffffff"
        }
        standardTile("contact1", "device.contact", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e86d13"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#00a0dc"
		}
        standardTile("contact2", "device.contact2", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e86d13"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#00a0dc"
		}
        standardTile("refresh", "command.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        standardTile("powered", "device.powered", inactiveLabel: false) {
			state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ffa81e"
		}
        standardTile("blankA", "device.blank", inactiveLabel: true, decoration: "flat") {
        	state("blank", label: 'A: Windows')
        }
        standardTile("blankB", "device.blank", inactiveLabel: true, decoration: "flat") {
        	state("blank", label: 'B: Doors')
        }        
		main (["contact1"])
		details(["blankA", "contact1", "relay1", "blankB", "contact2", "relay2", "refresh", "powered"])
	}
}

// parse events into attributes
def parse(String description) 
{
	def result = null
	def cmd = zwave.parse(description)
    
    if (cmd.CMD == "7105") {				//Mimo sent a power loss report
    	log.debug "Device lost power"
    	sendEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
    } else {
    	sendEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
    }
    
	if (cmd) {
    	def eventReturn = zwaveEvent(cmd)
        if(eventReturn in physicalgraph.device.HubMultiAction) {
            result = eventReturn
        	log.debug "Parse returned event ${result} ($cmd.CMD)"
        } else {
        	result = createEvent(eventReturn)
            log.debug "Parse created event ${result} ($cmd.CMD)"
        }
	}
	return result
}

def updated() // neat built-in smartThings function which automatically runs whenever any setting inputs are changed in the preferences menu of the device handler
{
    if (state.count == 1) // this bit with state keeps the function from running twice ( which it always seems to want to do) (( oh, and state.count is a variable which is nonVolatile and doesn't change per every parse request.
    {
        state.count = 0
        log.debug "Settings Updated..."
        return response(delayBetween([
            configure(), // the response() function is used for sending commands in response to an event, without it, no zWave commands will work for contained function
            refresh()
            ], 200))
    }
    else {state.count = 1}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) // basic set is essentially our digital sensor for SIG1 and SIG2 - it doesn't use an endpoint so we are having it send a multilevelGet() for SIG1 and SIG2 to see which one triggered.
{
	log.debug "BasicSet"
	return response(refresh())
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) // event to get the state of the digital sensor SIG1 and SIG2
{
	log.debug "SensorBinaryReport"
	return response(refresh())
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) // event for seeing the states of relay 1 and relay 2
{
	// possible cmd values are 255 and 0 (0 is false)
	def stdEvent = [:]
    if (endPoint == 3) {
        stdEvent.name = "alarm"
    	if (cmd.value) {
    		stdEvent.value = "both"
    	} else {
    		stdEvent.value = "off"
        }
    }
    else if (endPoint == 4) {
        stdEvent.name = "lock"
		if (cmd.value) {
    		stdEvent.value = "locked"
        } else {
        	stdEvent.value = "unlocked"
        }
    }   
    log.debug "SwitchBinaryReport: $stdEvent.name $stdEvent.value"
    return stdEvent    
}
   
def zwaveEvent (int endPoint, physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) // sensorMultilevelReport is used to report the value of the analog voltage for SIG1
{
    def stdEvent = [:]
    def voltageVal = CalculateVoltage(cmd.scaledSensorValue) // saving the scaled Sensor Value used to enter into a large formula to determine actual voltage value
    if (endPoint == 1) {
        stdEvent.name = "contact"
        if (voltageVal < 2) {
            stdEvent.value = "closed"
        } else {
            stdEvent.value = "open"
        }
    }
    else if (endPoint == 2) {
        stdEvent.name = "contact2"	// Doors
        if (voltageVal < 2) {
            stdEvent.value = "closed"
        } else {
            stdEvent.value = "open"
        } 
    }
    log.debug "SensorMultilevelReport: $stdEvent.name $stdEvent.value"
    return stdEvent
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) { //standard security encapsulation event code (should be the same on all device handlers)
    def encapsulatedCommand = cmd.encapsulatedCommand()
    log.debug ("SecurityMessageEncapsulation: $encapsulatedCommand")
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    }
}

// MultiChannelCmdEncap and MultiInstanceCmdEncap are ways that devices
// can indicate that a message is coming from one of multiple subdevices
// or "endpoints" that would otherwise be indistinguishable
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand()
    log.debug ("MultiChannelCmdEncap from endpoint $cmd.sourceEndPoint: $encapsulatedCommand")
    if (encapsulatedCommand) {
        return zwaveEvent(cmd.sourceEndPoint, encapsulatedCommand)
    }
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd) 
{
    log.debug "MultiChannelAssociationReport: $cmd.groupingIdentifier"
    //return [:]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) 
{
	// Handles all Z-Wave commands we aren't interested in
    log.debug("Un-parsed Z-Wave message ${cmd}")
	return [:]
}

def CalculateVoltage(ADCvalue) // used to calculate the voltage based on the collected Scaled sensor value of the multilevel sensor event
{
    def volt = (((2.396*(10**-17))*(ADCvalue**5)) - ((1.817*(10**-13))*(ADCvalue**4)) + ((5.087*(10**-10))*(ADCvalue**3)) - ((5.868*(10**-7))*(ADCvalue**2)) + ((9.967*(10**-4))*(ADCvalue)) - (1.367*(10**-2)))
	return volt.round(1)
}
	

def configure() {
	log.debug "Configuring...." 
    def sig1 = 0x40
    def sig2 = 0x40   
    
    return delayBetween([
        encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]), 0),
        encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:2, nodeId:[zwaveHubNodeId]), 0),

        encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:2, nodeId:[zwaveHubNodeId]), 1),
        encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:2, nodeId:[zwaveHubNodeId]), 2),
        encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 3),
        encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 4),

        secure(zwave.configurationV1.configurationSet(configurationValue: [sig1], parameterNumber: 3, size: 1)), // sends a multiLevelSensor report every 30 seconds for SIG1
        secure(zwave.configurationV1.configurationSet(configurationValue: [sig2], parameterNumber: 9, size: 1)), // sends a multiLevelSensor report every 30 seconds for SIG2
        secure(zwave.configurationV1.configurationSet(configurationValue: "0", parameterNumber: 1, size: 1)),
        secure(zwave.configurationV1.configurationSet(configurationValue: "0", parameterNumber: 2, size: 1)),
    ], 200)
}

// Relay 1 on
def both() {	
	return encap(zwave.basicV1.basicSet(value: 0xff), 3) // physically changes the relay from on to off and requests a report of the relay
    // oddly, smartThings automatically sends a switchBinaryGet() command whenever the above basicSet command is sent, so we don't need to send one here.
}

// Relay 1 off
def off() {
    return encap(zwave.basicV1.basicSet(value: 0x00), 3) // physically changes the relay from on to off and requests a report of the relay
	// oddly, smartThings automatically sends a switchBinaryGet() command whenever the above basicSet command is sent, so we don't need to send one here.
}

// Relay 2 on
def lock() {
   return encap(zwave.basicV1.basicSet(value: 0xff), 4)
   // oddly, smartThings automatically sends a switchBinaryGet() command whenever the above basicSet command is sent, so we don't need to send one here.
}

// Relay 2 off
def unlock() {
    return encap(zwave.basicV1.basicSet(value: 0x00), 4)
   // oddly, smartThings automatically sends a switchBinaryGet() command whenever the above basicSet command is sent, so we don't need to send one here.
}

def refresh() {
	log.debug "Refresh"
	return delayBetween([
         encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), 1),// requests a report of the anologue input voltage for SIG1
         encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), 2),// requests a report of the anologue input voltage for SIG2
         encap(zwave.switchBinaryV1.switchBinaryGet(), 3), //requests a report of the relay to make sure that it changed for Relay 1
         encap(zwave.switchBinaryV1.switchBinaryGet(), 4), //requests a report of the relay to make sure that it changed for Relay 2
       ],200)
}

def refreshZWave() {
	log.debug "Refresh (Z-Wave Response)"
	return delayBetween([
         encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), 1),// requests a report of the anologue input voltage for SIG1
         encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), 2),// requests a report of the anologue input voltage for SIG2
         encap(zwave.switchBinaryV1.switchBinaryGet(), 3), //requests a report of the relay to make sure that it changed for Relay 1
         encap(zwave.switchBinaryV1.switchBinaryGet(), 4) //requests a report of the relay to make sure that it changed for Relay 2
       ],200)
}

private secureSequence(commands, delay=200) { // decided not to use this
	return delayBetween(commands.collect{ secure(it) }, delay)
}

private secure(physicalgraph.zwave.Command cmd) { //take multiChannel message and securely encrypts the message so the device can read it
	return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private encap(cmd, endpoint) { // takes desired command and encapsulates it by multiChannel and then sends it to secure() to be wrapped with another encapsulation for secure encryption
	if (endpoint) {
		return secure(zwave.multiChannelV3.multiChannelCmdEncap(bitAddress: false, sourceEndPoint:0, destinationEndPoint: endpoint).encapsulate(cmd))
	} else {
		return secure(cmd)
	}
}