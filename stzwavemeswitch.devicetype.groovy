/**
 *  ZWave-Me Switch
 *
 *  Copyright 2015 Michael Backes
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

import groovy.json.JsonSlurper

preferences {
        input("ip", "string", title:"IP Address", description: "192.168.1.150", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "8000", defaultValue: 8000 , required: true, displayDuringSetup: true)
        input("deviceNum", "string", title:"Device ID", description: "1", defaultValue: 1 , required: true, displayDuringSetup: true)
        input("instance", "string", title:"Instance", description: "0", defaultValue: 0 , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "ZWave-Me Switch", namespace: "derbackes", author: "Michael Backes") {
		capability "Switch"
        capability "Polling"
		capability "Refresh"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
        standardTile("button", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "off", label: 'Off', icon: "st.Electronics.electronics18", action:"switch.on", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', icon: "st.Electronics.electronics18", action:"switch.off", backgroundColor: "#79b821", nextState: "off"
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
//        standardTile("theSwitch", "device.switch", inactiveLabel: false, decoration: "flat") {
//        	state "off", action:"refresh.on", icon: "st.secondary.refresh"
//            state "on", action:"refresh.off", icon: "st.Electronics.electronics18"
//        }
        main "button"
        details(["button", "refresh","theSwitch"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    log.debug descMap
    def body = new String(descMap["body"].decodeBase64())
    log.debug "body: ${body}"
    def slurper = new JsonSlurper()
    if (body == "null") {
    	// For some dumb reason, null = worked ok
        log.debug "Looks good on command"
        // Go poll the status, because we don't know if it was turned on or off
        def myUri = "/ZWaveAPI/Run/devices[" + deviceNum + "].instances[" + instance + "].commandClasses[37].data.level"
    	postAction(myUri)
    } else {
	    def result = slurper.parseText(body)
    	log.debug "result: ${result}"
	    if (result.containsKey("value")) {
    	    log.debug "result: ${result.value}"
    		if(result.value) {
	        	log.debug "It is on"
		    	sendEvent(name: "switch", value: "on")
        	} else {
	        	log.debug "It is OFF"
    	    	sendEvent(name: "switch", value: "off")
        	}
	    } else {
    		log.debug "It is NOT FOUND"
    		sendEvent(name: "switch", value: "off")
	    }
	}
}

// handle commands
def poll() {
	log.debug "Executing 'polling' on switch"
    // http://IP:PORT/ZWaveAPI/Run/devices[3].instances[0].commandClasses[37].data.level
	def myUri = "/ZWaveAPI/Run/devices[" + deviceNum + "].instances[" + instance + "].commandClasses[37].data.level"
    postAction(myUri)
}

def refresh() {
	log.debug "Executing 'refresh' in switch"
    // http://IP:PORT/ZWaveAPI/Run/devices[3].instances[0].commandClasses[37].data.level
	def myUri = "/ZWaveAPI/Run/devices[" + deviceNum + "].instances[" + instance + "].commandClasses[37].data.level"
    postAction(myUri)
}

def on() {
	log.debug "Executing 'on'"
	// TODO: handle 'on' command
    //http://IP:PORT/ZWaveAPI/Run/devices[3].instances[0].commandClasses[37].Set(255)
    def myUri = "/ZWaveAPI/Run/devices[" + deviceNum + "].instances[" + instance + "].commandClasses[37].Set(255)"
    postAction(myUri)
}

def off() {
	log.debug "Executing 'off' for sure"
	// TODO: handle 'off' command#
    def myUri = "/ZWaveAPI/Run/devices[" + deviceNum + "].instances[" + instance + "].commandClasses[37].Set(0)"
    postAction(myUri)
}


// ------------------------------------------------------------------

private postAction(uri){
  setDeviceNetworkId(ip,port)

  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri
  )//,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
  hubAction
}


// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

private encodeCredentials(username, password){
	log.debug "Encoding credentials"
	def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    //log.debug "ASCII credentials are ${userpassascii}"
    //log.debug "Credentials are ${userpass}"
    return userpass
}

private getHeader(userpass){
	log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    headers.put("Authorization", userpass)
    //log.debug "Headers are ${headers}"
    return headers
}

private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
    //def randomIdPart = System.currentTimeMillis()
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
