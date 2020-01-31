/**
 *  FortrezZ MIMO2+ B-Side
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
definition(
    name: "FortrezZ MIMO2+ B-Side with Delayed open",
    namespace: "lindfield",
    author: "Paul Lindfield",
    description: "Creates child device for MIMO2 contact2 (B-side) with delayed open (for doors)",
    category: "Convenience",
    iconUrl: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square-200-1.png",
    iconX2Url: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square-500.png",
    iconX3Url: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square.png",
    singleInstance: true)


preferences {
	section("Title") {
		input(name: "mimoDevice", type: "capability.alarm", title: "MIMO2 (Alarm) devices", description: null, required: true, submitOnChange: true, multiple: false)
        input(name: "secondsDelay", type: "number", title: "Delay for door opening events (seconds)", required: true, default: "5")
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize(){
	log.debug("Device: ${settings.mimoDevice}")

   	subscribe(mimoDevice, "contact2", events)
        
    try {
        def existingDevice = getChildDevice(mimoDevice.id)
        if(!existingDevice) {
            log.debug("Device ID: ${existingDevice}")
            def childDevice = addChildDevice("lindfield", "FortrezZ MIMO2+ B-Side", mimoDevice.id, null, [name: "ChildDevice.${mimoDevice.id}", label: "${mimoDevice.name} B-Side (Delayed)", completedSetup: true])
        }
    } catch (e) {
        log.error "Error creating device: ${e}"
    }
    
    getChildDevices().each {
    	def test = it
        def search = settings.devices.find { getChildDevice(it.id).id == test.id }
        if(!search) {
        	removeChildDevices(test)
        }
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def refresh2(child) {
	log.debug("refresh2")
    atomicState.realContact2 = false
    atomicState.delayContact = false
    def ret = "${child}, ${mimoDevice.id}"
	log.debug("Refresh ${mimoDevice.id} for ${child}")        
    mimoDevice.refresh()
    return ret
}

def events(evt) {
	def ch = getChildDevice(mimoDevice.id)
    
	log.debug("Parsing ${evt.name}=${evt.value}")
    if (evt.name == "contact2")
    {
    	if (evt.value == "open") 
        {
        	atomicState.realContact2 = true
            if (!atomicState.delayContact) 
            {
            	log.debug("Delaying open event for $secondsDelay seconds")
               	atomicState.delayContact = true
				runIn(secondsDelay, delayedEvent)
            }
        } 
        else if (evt.value == "closed") 
        {
            atomicState.realContact2 = false
            if (!atomicState.delayContact) 
            {
            	log.debug("Event: contact=closed")
               	ch.eventParse(name: "contact", value: "closed")
            }
        }
    }    
}

def delayedEvent() {
	def ch = getChildDevice(mimoDevice.id)
    
    log.debug("Delayed Event: contact=open")
	ch.eventParse(name: "contact", value: "open")
    if (atomicState.realContact2 == false)
    {
    	log.debug("Delayed Event: contact=closed")
    	ch.eventParse(name: "contact", value: "closed")
    }
    atomicState.delayContact = false
}