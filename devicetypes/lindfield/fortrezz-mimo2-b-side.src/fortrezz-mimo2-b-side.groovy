/**
 *  FortrezZ MIMO2+ B-Side
 */
metadata {
	definition (name: "FortrezZ MIMO2+ B-Side", namespace: "lindfield", author: "lindfield", ocfDeviceType: "x.com.st.d.sensor.contact", runLocally: true) {
		capability "Contact Sensor"
        capability "Refresh"
	}
            
	tiles(scale: 2) {
		multiAttributeTile(name: "contact", type: "generic", width: 6, height: 4) {
			tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
				attributeState("open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e86d13")
				attributeState("closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC")
			}
		}
        
        standardTile("refresh", "command.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh.refresh"
		}       

		main "contact"
		details (["contact", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def eventParse(evt) {
    if (evt.name == "contact")
    {
    	if (evt.value == "open") 
        {
        	log.debug "Event: open"
			sendEvent(name: "contact", value: "open")
        } 
        else if (evt.value == "closed") 
        {
        	log.debug "Event: closed"
           	sendEvent(name: "contact", value: "closed")
        }
    }
}

def refresh() {
    log.debug("Refresh")
	parent.refresh2(device.id)
}