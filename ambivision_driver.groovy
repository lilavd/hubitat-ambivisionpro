/**
 *  AmbiVision PRO Device Driver
 *  
 *  Copyright 2024
 *  
 *  Licensed under the Apache License, Version 2.0
 *  
 *  Description:
 *  Hubitat driver for AmbiVision PRO ambient lighting controller
 *  Communicates via UDP on port 45457
 *
 *  Version: 1.0.1
 */

import groovy.transform.Field

metadata {
    definition(
        name: "AmbiVision PRO",
        namespace: "community",
        author: "Community Driver",
        importUrl: ""
    ) {
        capability "Switch"
        capability "SwitchLevel"
        capability "ColorControl"
        capability "Refresh"
        capability "Initialize"
        
        command "setMode", [[name:"mode*", type:"ENUM", constraints:["Capture", "Mood", "Audio", "Off"]]]
        command "setCaptureSubMode", [[name:"subMode*", type:"ENUM", constraints:["Intelligent", "Smooth", "Fast", "Average", "User"]]]
        command "setMoodSubMode", [[name:"subMode*", type:"ENUM", constraints:["Manual", "Disco", "Rainbow", "Nature", "Relax"]]]
        command "setAudioSubMode", [[name:"subMode*", type:"ENUM", constraints:["Level Bins", "Mixed Bins", "Lamp", "Strobo", "Freq Bins"]]]
        command "discover"
        
        attribute "mode", "string"
        attribute "subMode", "string"
        attribute "firmwareVersion", "string"
        attribute "deviceId", "string"
        attribute "ipAddress", "string"
    }
    
    preferences {
        input name: "deviceIP", type: "text", title: "Device IP Address", description: "Enter IP if known, or use Discover", required: false
        input name: "autoDiscover", type: "bool", title: "Auto-discover on Initialize", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

// Constants for modes
@Field static final Map MODES = [
    CAPTURE: 1,
    MOOD: 2,
    AUDIO: 3,
    TURN_OFF: 4
]

@Field static final Map CAPTURE_SUBMODES = [
    INTELLIGENT: 1,
    SMOOTH: 2,
    FAST: 3,
    AVERAGE: 4,
    USER: 5
]

@Field static final Map MOOD_SUBMODES = [
    MANUAL: 1,
    DISCO: 2,
    RAINBOW: 3,
    NATURE: 4,
    RELAX: 5
]

@Field static final Map AUDIO_SUBMODES = [
    LEVEL_BINS: 1,
    MIXED_BINS: 2,
    LAMP: 3,
    STROBO: 4,
    FREQ_BINS: 5
]

@Field static final int UDP_PORT = 45457
@Field static final String PING_MESSAGE = "AmbiVisionPing"

// Lifecycle Methods
def installed() {
    log.info "AmbiVision PRO driver installed"
    initialize()
}

def updated() {
    log.info "AmbiVision PRO driver updated"
    unschedule()
    if (logEnable) runIn(1800, logsOff)
    initialize()
}

def initialize() {
    log.info "Initializing AmbiVision PRO"
    state.clear()
    
    if (autoDiscover && !deviceIP) {
        runIn(1, discover)
    } else if (deviceIP) {
        sendEvent(name: "ipAddress", value: deviceIP)
        if (logEnable) log.debug "Using configured IP: ${deviceIP}"
    }
    
    // Schedule periodic discovery to maintain connection (every 30 seconds)
    if (autoDiscover) {
        schedule("0/30 * * * * ?", discover)
    }
}

def refresh() {
    if (logEnable) log.debug "Refresh called"
    discover()
}

// Discovery
def discover() {
    if (logEnable) log.debug "Starting device discovery..."
    
    try {
        def hubAction = new hubitat.device.HubAction(
            PING_MESSAGE,
            hubitat.device.Protocol.LAN,
            [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
             destinationAddress: "255.255.255.255:${UDP_PORT}"]
        )
        sendHubCommand(hubAction)
    } catch (Exception e) {
        log.error "Discovery failed: ${e.message}"
    }
}

def parse(String description) {
    if (logEnable) log.debug "Parsing: ${description}"
    
    try {
        def msg = parseLanMessage(description)
        
        if (msg?.payload) {
            def response = new String(hubitat.helper.HexUtils.hexStringToByteArray(msg.payload))
            if (logEnable) log.debug "Decoded payload: ${response}"
            
            if (response?.contains("AmbiVision")) {
                // Parse: AmbiVision(605533_V.18) MagicLink(21393430v7)
                def matcher = response =~ /AmbiVision\((\d+)_V\.(\d+)\)/
                if (matcher.find()) {
                    def deviceId = matcher[0][1]
                    def fwVersion = matcher[0][2]
                    
                    // Convert hex IP to dotted decimal
                    def ip = convertHexToIP(msg.ip)
                    
                    sendEvent(name: "deviceId", value: deviceId)
                    sendEvent(name: "firmwareVersion", value: "V.${fwVersion}")
                    sendEvent(name: "ipAddress", value: ip)
                    
                    if (txtEnable) log.info "AmbiVision PRO discovered: ID=${deviceId}, FW=V.${fwVersion}, IP=${ip}"
                }
            }
        }
    } catch (Exception e) {
        log.error "Error parsing discovery response: ${e.message}"
    }
}

private String convertHexToIP(hex) {
    try {
        [
            Integer.parseInt(hex[0..1], 16),
            Integer.parseInt(hex[2..3], 16),
            Integer.parseInt(hex[4..5], 16),
            Integer.parseInt(hex[6..7], 16)
        ].join(".")
    } catch (Exception e) {
        log.error "Error converting hex to IP: ${e.message}"
        return null
    }
}

// Switch Capability
def on() {
    if (logEnable) log.debug "Turning on (setting to last mode or Mood)"
    def lastMode = state.lastMode ?: "Mood"
    setMode(lastMode)
    sendEvent(name: "switch", value: "on")
}

def off() {
    if (logEnable) log.debug "Turning off"
    setMode("Off")
    sendEvent(name: "switch", value: "off")
}

// SwitchLevel Capability
def setLevel(level, duration=0) {
    if (logEnable) log.debug "Setting brightness to ${level}%"
    
    level = Math.max(0, Math.min(100, level as int))
    
    def command = "AmbiVision4 OVERALL_BRIGHTNESS={${level}} \n"
    sendCommand(command)
    
    sendEvent(name: "level", value: level, unit: "%")
    if (txtEnable) log.info "Brightness set to ${level}%"
}

// ColorControl Capability
def setColor(colorMap) {
    if (logEnable) log.debug "Setting color: ${colorMap}"
    
    // Ensure we're in Mood mode with Manual sub-mode
    if (device.currentValue("mode") != "Mood" || device.currentValue("subMode") != "Manual") {
        setMode("Mood")
        pauseExecution(500)
        setMoodSubMode("Manual")
        pauseExecution(500)
    }
    
    def hue = colorMap.hue ? Math.max(0, Math.min(100, colorMap.hue as int)) : device.currentValue("hue") ?: 0
    def saturation = colorMap.saturation ? Math.max(0, Math.min(100, colorMap.saturation as int)) : device.currentValue("saturation") ?: 100
    def level = colorMap.level ? Math.max(0, Math.min(100, colorMap.level as int)) : device.currentValue("level") ?: 100
    
    // Convert HSV to RGB
    def rgb = hubitat.helper.ColorUtils.hsvToRGB([hue, saturation, level])
    def r = rgb[0]
    def g = rgb[1]
    def b = rgb[2]
    
    def command = "AmbiVision1 R{${r}} G{${g}} B{${b}} \n"
    sendCommand(command)
    
    sendEvent(name: "hue", value: hue)
    sendEvent(name: "saturation", value: saturation)
    sendEvent(name: "level", value: level)
    sendEvent(name: "color", value: colorMap)
    
    if (txtEnable) log.info "Color set to RGB(${r},${g},${b})"
}

def setHue(hue) {
    if (logEnable) log.debug "Setting hue to ${hue}"
    setColor([hue: hue, saturation: device.currentValue("saturation") ?: 100, level: device.currentValue("level") ?: 100])
}

def setSaturation(saturation) {
    if (logEnable) log.debug "Setting saturation to ${saturation}"
    setColor([hue: device.currentValue("hue") ?: 0, saturation: saturation, level: device.currentValue("level") ?: 100])
}

// Mode Commands
def setMode(modeName) {
    if (logEnable) log.debug "Setting mode to: ${modeName}"
    
    def modeMap = [
        "Capture": MODES.CAPTURE,
        "Mood": MODES.MOOD,
        "Audio": MODES.AUDIO,
        "Off": MODES.TURN_OFF
    ]
    
    def modeValue = modeMap[modeName]
    if (modeValue == null) {
        log.error "Invalid mode: ${modeName}"
        return
    }
    
    def command = "AmbiVision2${modeValue}"
    sendCommand(command)
    
    if (modeName != "Off") {
        state.lastMode = modeName
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }
    
    sendEvent(name: "mode", value: modeName)
    if (txtEnable) log.info "Mode set to ${modeName}"
}

def setCaptureSubMode(subModeName) {
    if (logEnable) log.debug "Setting Capture sub-mode to: ${subModeName}"
    
    def subModeMap = [
        "Intelligent": CAPTURE_SUBMODES.INTELLIGENT,
        "Smooth": CAPTURE_SUBMODES.SMOOTH,
        "Fast": CAPTURE_SUBMODES.FAST,
        "Average": CAPTURE_SUBMODES.AVERAGE,
        "User": CAPTURE_SUBMODES.USER
    ]
    
    def subModeValue = subModeMap[subModeName]
    if (subModeValue == null) {
        log.error "Invalid Capture sub-mode: ${subModeName}"
        return
    }
    
    def command = "AmbiVision3${subModeValue}"
    sendCommand(command)
    
    sendEvent(name: "subMode", value: subModeName)
    if (txtEnable) log.info "Capture sub-mode set to ${subModeName}"
}

def setMoodSubMode(subModeName) {
    if (logEnable) log.debug "Setting Mood sub-mode to: ${subModeName}"
    
    def subModeMap = [
        "Manual": MOOD_SUBMODES.MANUAL,
        "Disco": MOOD_SUBMODES.DISCO,
        "Rainbow": MOOD_SUBMODES.RAINBOW,
        "Nature": MOOD_SUBMODES.NATURE,
        "Relax": MOOD_SUBMODES.RELAX
    ]
    
    def subModeValue = subModeMap[subModeName]
    if (subModeValue == null) {
        log.error "Invalid Mood sub-mode: ${subModeName}"
        return
    }
    
    def command = "AmbiVision3${subModeValue}"
    sendCommand(command)
    
    sendEvent(name: "subMode", value: subModeName)
    if (txtEnable) log.info "Mood sub-mode set to ${subModeName}"
}

def setAudioSubMode(subModeName) {
    if (logEnable) log.debug "Setting Audio sub-mode to: ${subModeName}"
    
    def subModeMap = [
        "Level Bins": AUDIO_SUBMODES.LEVEL_BINS,
        "Mixed Bins": AUDIO_SUBMODES.MIXED_BINS,
        "Lamp": AUDIO_SUBMODES.LAMP,
        "Strobo": AUDIO_SUBMODES.STROBO,
        "Freq Bins": AUDIO_SUBMODES.FREQ_BINS
    ]
    
    def subModeValue = subModeMap[subModeName]
    if (subModeValue == null) {
        log.error "Invalid Audio sub-mode: ${subModeName}"
        return
    }
    
    def command = "AmbiVision3${subModeValue}"
    sendCommand(command)
    
    sendEvent(name: "subMode", value: subModeName)
    if (txtEnable) log.info "Audio sub-mode set to ${subModeName}"
}

// Communication
private sendCommand(String command) {
    def ip = device.currentValue("ipAddress") ?: deviceIP
    
    if (!ip) {
        log.error "No IP address configured or discovered. Please run discover or set IP manually."
        return
    }
    
    if (logEnable) log.debug "Sending command to ${ip}: ${command}"
    
    try {
        def hubAction = new hubitat.device.HubAction(
            command,
            hubitat.device.Protocol.LAN,
            [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
             destinationAddress: "${ip}:${UDP_PORT}",
             ignoreResponse: true]
        )
        sendHubCommand(hubAction)
    } catch (Exception e) {
        log.error "Failed to send command: ${e.message}"
    }
}

// Utility
def logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}