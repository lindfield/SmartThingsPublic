/**
 *  FortrezZ MIMO2+ B-Side contact with delayed open
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
	log.debug "installed(): Settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "updated(): Settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize(){
	log.debug("initialize(): Device: ${settings.mimoDevice}")

	atomicState.realContact2 = false
	atomicState.delayContact = false

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
	log.debug("uninstalled()")
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def refresh2(child) {
	log.debug("refresh2()")
    atomicState.realContact2 = false
    atomicState.delayContact = false
    def ret = "${child}, ${mimoDevice.id}"
	log.info("Refresh ${mimoDevice.id} for ${child}")        
    mimoDevice.refresh()
    return ret
}

def events(evt) {
	log.debug("events(): Parsing ${evt.name}=${evt.value}")
    if (evt.name == "contact2")
    {
        if (atomicState.delayContact) 
        {
          	runIn(10*secondsDelay, recoveryEvent)
        }
    	if (evt.value == "open") 
        {
        	atomicState.realContact2 = true
            if (!atomicState.delayContact) 
            {
            	log.info("Delaying open event for $secondsDelay seconds")
               	atomicState.delayContact = true
				runIn(secondsDelay, delayedEvent)
            }
        } 
        else if (evt.value == "closed") 
        {
            atomicState.realContact2 = false
            if (!atomicState.delayContact) 
            {
            	def ch = getChildDevice(mimoDevice.id)
            	log.info("Event: contact=closed")
               	ch.eventParse(name: "contact", value: "closed")
            }          
        }
    }    
}

def delayedEvent() {
	log.debug("delayedEvent()")
	def ch = getChildDevice(mimoDevice.id)
    
    log.info("Delayed Event: contact=open")
	ch.eventParse(name: "contact", value: "open")
    
    if (!atomicState.realContact2)
    {
    	log.info("Delayed Event: contact=closed")
    	ch.eventParse(name: "contact", value: "closed")
    }
    atomicState.delayContact = false
}

def recoveryEvent() {
	log.debug("recoveryEvent()")
	if (atomicState.delayContact) 
    {
    	log.warning("Recovery: delayContact was true")
    	atomicState.delayContact = false

		if (atomicState.realContact2)
        {
            ch.eventParse(name: "contact", value: "open")
        }
        else
        {
            ch.eventParse(name: "contact", value: "closed")
        }
    }
}