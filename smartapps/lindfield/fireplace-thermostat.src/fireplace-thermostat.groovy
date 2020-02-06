/**
 *  Fireplace Thermostat
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
    name: "Fireplace Thermostat",
    namespace: "lindfield",
    author: "Paul Lindfield",
    description: "Control a fireplace in conjunction with any temperature sensor.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home29-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home29-icn@2x.png",
    pausable: true
)

preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
        section("Select the thermostat enable switch..."){
            input "enableSwitch", "capability.switch", required: true, title: "Thermostat switch (maybe virtual)", multiple: false
        }
        section("Select the fireplace enable switch / relay... "){
            input "outlets", "capability.switch", title: "Fireplace Relay(s)", multiple: true
        }    
        section("Select the temperature sensor... "){
            input "sensor", "capability.temperatureMeasurement", title: "Temperature Sensor"
        }
        section("Set the desired temperature..."){
            input "setpoint", "decimal", title: "Temperature Setpoint"
        }
        section("Turn on fireplace only when mode is...") {
            input "activeModes", "mode", title: "Which mode(s)", multiple: true
            input "turnSwitchOff", "bool", title: "Turn off switch when leave mode(s)?", required: true, defaultValue: true
        }
        section("Rename the app") {
            label title: "Assign a name", required: false
        }
    }
}

def installed()
{
	initialize() 
}

def updated()
{
	unsubscribe()
    initialize()
}

def initialize() 
{
	subscribe(enableSwitch, "switch", switchHandler)
	subscribe(sensor, "temperature", temperatureHandler)
    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt) {
	log.debug "modeChangeHandler"
	if (enableSwitch.currentSwitch == "on")
    {
        if (activeModes.contains(evt.value))
        {
            thermostat(sensor.currentTemperature)
        }
        else
        {
            fireplaceOff()
            if (turnSwitchOff)
            {
            	log.debug "Disabling $enableSwitch"
                enableSwitch.off()
            }
        }
    }
}

def switchHandler(evt) 
{
	log.debug "switchHandler"
	if (activeModes.contains(location.mode))
    {
        if (evt.value == "on")
        {
            thermostat(sensor.currentTemperature)
        } 
        else
        {
            fireplaceOff()
        }
    }
}

def temperatureHandler(evt)
{
	if ((enableSwitch.currentSwitch == "on") &&
        (activeModes.contains(location.mode)))
    {
        thermostat(evt.floatValue)
    }
}
    
private thermostat(currentTemp)
{
    float tempDiff = currentTemp - setpoint
    float threshold = 0.1f
    log.debug "Current: $currentTemp Difference: $tempDiff Threshold: $threshold"
    if (tempDiff <= -threshold) 
    {
        fireplaceOn()
    }
    else if (tempDiff >= threshold) 
    {
        fireplaceOff()
    }
}

private fireplaceOn()
{
    log.debug "Turning $outlets on"
    outlets.on()
}

private fireplaceOff()
{
    log.debug "Turning $outlets off"
    outlets.off()
}