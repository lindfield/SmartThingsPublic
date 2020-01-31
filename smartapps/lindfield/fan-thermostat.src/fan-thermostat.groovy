/**
 *  3-speed Fan Thermostat
 *
 *  Copyright © 2019 Paul Lindfield
 *
 *  Based on SmartThings thermostat
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
    name: "Fan Thermostat",
    namespace: "lindfield",
    author: "Paul Lindfield",
    description: "Adjust fan speed based on temperature sensor",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/thermostat/fan-auto.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/thermostat/fan-auto@2x.png"
)

preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
        section("Monitor which temperature sensor?") {
            input "temperatureSensor", "capability.temperatureMeasurement", title: "Temperature sensor", multiple: false
        }
        section("Control which fan?") {
            input "fanSwitch", "capability.switch", title: "Fan switch", multiple: false
        }
        section("Set fan speed when the temperature rises above...") {
            input "tempThreshold1", "decimal", title: "Low speed temperature threshold"
            input "tempThreshold2", "decimal", title: "Medium speed temperature threshold"
            input "tempThreshold3", "decimal", title: "High speed temperature threshold"
        }
        section("Run fan only when mode is...") {
           input "activeModes", "mode", title: "Which mode(s)", multiple: true
        }
        section("Rename the app") {
        	label title: "Assign a name", required: false
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(temperatureSensor, "temperature", temperatureHandler)
    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt) {
    if (activeModes.contains(evt.value))
    {
    	log.trace "Activated for mode ${evt.value}"
    	/* The temperature state value includes the unit, unlike the event value, which does not!
        adjustFanSpeed(temperatureSensor.temperature) */
        adjustFanSpeed(state.temperature)
    }
    else
    {
    	log.trace "Deactivated for mode ${evt.value}"
        adjustFanSpeed(tempThreshold1)
    }
}

def temperatureHandler(evt) {
	log.trace "Temperature: $evt.doubleValue, $evt"
    state.temperature = evt.doubleValue
    if (activeModes.contains(location.mode))
    {
    	adjustFanSpeed(evt.doubleValue)
    }
}

def adjustFanSpeed(double temperature)
{	
	def speed
    if (temperature > tempThreshold3)
    {
    	speed = 3
	}      
    else if (temperature > tempThreshold2)
    {
    	speed = 2
	}
    else if (temperature > tempThreshold1)
    {
        speed = 1
    }
    else
    {
    	speed = 0;
    }

    if (state.speed != speed)
    {
        state.speed = speed
        fanSwitch.setFanSpeed(speed)            
        log.trace "Fan speed changed to ${speed}"
    }
    else
    {
        log.trace "Fan speed not changed (previously set to ${speed})"
    }
}