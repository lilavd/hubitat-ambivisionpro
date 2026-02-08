# hubitat-ambivisionpro
[REAMDE.md](https://github.com/user-attachments/files/25164216/REAMDE.md)
# AmbiVision PRO - Hubitat Driver

A comprehensive Hubitat Elevation driver for controlling AmbiVision PRO ambient lighting controllers via UDP communication.

## Features

- **Full device control** via standard Hubitat capabilities
- **Automatic device discovery** via UDP broadcast
- **Multiple operating modes**:
  - Capture mode (screen/video sync)
  - Mood mode (ambient lighting effects)
  - Audio mode (music visualization)
- **RGB color control** with HSV conversion
- **Brightness control** (0-100%)
- **Sub-mode selection** for each main mode
- **Periodic connection monitoring**

## Installation

### Method 1: Hubitat Package Manager (HPM)
*Coming soon - this driver will be submitted to HPM*

### Method 2: Manual Installation

1. In your Hubitat web interface, navigate to **Drivers Code**
2. Click **+ New Driver**
3. Copy the entire contents of `ambivision-pro-driver.groovy` and paste it into the driver editor
4. Click **Save**
5. Navigate to **Devices**
6. Click **+ Add Device**
7. Click **Virtual**
8. Enter a device name (e.g., "AmbiVision PRO")
9. Select **AmbiVision PRO** from the Type dropdown
10. Click **Save Device**

## Configuration

### Automatic Discovery (Recommended)

1. Open your newly created AmbiVision PRO device
2. Ensure **Auto-discover on Initialize** is enabled (default)
3. Click **Initialize** or **Discover**
4. The driver will automatically find your device on the network
5. Check the device attributes to confirm the IP address was discovered

### Manual IP Configuration

If auto-discovery doesn't work:

1. Find your AmbiVision PRO's IP address from your router
2. Enter it in the **Device IP Address** preference
3. Optionally disable **Auto-discover on Initialize**
4. Click **Save Preferences**

## Usage

### Basic Control

#### Switch Commands
```groovy
// Turn on (uses last mode or defaults to Mood)
device.on()

// Turn off
device.off()
```

#### Brightness Control
```groovy
// Set brightness (0-100%)
device.setLevel(75)
```

#### Color Control
```groovy
// Set color using HSV (Hue, Saturation, Level)
device.setColor([hue: 50, saturation: 100, level: 80])

// Set just hue (0-100)
device.setHue(25)

// Set just saturation (0-100)
device.setSaturation(90)
```

### Mode Control

#### Main Modes
```groovy
// Set main operating mode
device.setMode("Capture")  // Screen/video sync
device.setMode("Mood")     // Ambient effects
device.setMode("Audio")    // Music visualization
device.setMode("Off")      // Turn off
```

#### Capture Sub-Modes
```groovy
device.setCaptureSubMode("Intelligent")
device.setCaptureSubMode("Smooth")
device.setCaptureSubMode("Fast")
device.setCaptureSubMode("Average")
device.setCaptureSubMode("User")
```

#### Mood Sub-Modes
```groovy
device.setMoodSubMode("Manual")   // Manual color control
device.setMoodSubMode("Disco")
device.setMoodSubMode("Rainbow")
device.setMoodSubMode("Nature")
device.setMoodSubMode("Relax")
```

#### Audio Sub-Modes
```groovy
device.setAudioSubMode("Level Bins")
device.setAudioSubMode("Mixed Bins")
device.setAudioSubMode("Lamp")
device.setAudioSubMode("Strobo")
device.setAudioSubMode("Freq Bins")
```

### Rule Machine Examples

#### Scene: Movie Time
1. Set mode to Capture
2. Set sub-mode to Smooth
3. Set brightness to 60%

#### Scene: Party Mode
1. Set mode to Audio
2. Set sub-mode to Strobo
3. Set brightness to 100%

#### Scene: Relaxing Evening
1. Set mode to Mood
2. Set sub-mode to Manual
3. Set color to warm orange (hue: 8, saturation: 100, level: 60)

## Device Attributes

The driver exposes the following attributes you can use in rules:

| Attribute | Type | Description |
|-----------|------|-------------|
| `switch` | string | "on" or "off" |
| `level` | number | Brightness level (0-100) |
| `hue` | number | Color hue (0-100) |
| `saturation` | number | Color saturation (0-100) |
| `color` | object | Current color map |
| `mode` | string | Current main mode |
| `subMode` | string | Current sub-mode |
| `ipAddress` | string | Device IP address |
| `deviceId` | string | Unique device ID |
| `firmwareVersion` | string | Firmware version |

## Troubleshooting

### Device not discovered

1. Ensure the AmbiVision PRO is on the same network as your Hubitat hub
2. Check that UDP port 45457 is not blocked by your firewall
3. Try manually entering the IP address in preferences
4. Click **Discover** to manually trigger discovery
5. Enable debug logging and check the logs for errors

### Commands not working

1. Verify the IP address is correct in device attributes
2. Ensure there's a 500ms delay between mode changes (driver handles this automatically)
3. Check that the device is on and responsive
4. Review logs with debug logging enabled

### Color control not working

Color control requires:
- Device to be in **Mood** mode
- Sub-mode set to **Manual**

The driver automatically switches to these settings when you use color commands, but there may be a ~1 second delay.

### Connection issues

- The driver performs periodic discovery every 30 seconds when auto-discover is enabled
- If the device IP changes, the driver will automatically update
- Consider setting a static IP for your AmbiVision PRO in your router

## Communication Protocol

The driver communicates with the AmbiVision PRO using:
- **Protocol**: UDP
- **Port**: 45457
- **Commands**: Text-based protocol per AmbiVision PRO specification

### Command Format Examples

```
AmbiVision22                              // Set to Mood mode
AmbiVision31                              // Set to Manual sub-mode
AmbiVision4 OVERALL_BRIGHTNESS={75} \n    // Set brightness to 75%
AmbiVision1 R{255} G{128} B{0} \n         // Set RGB color
```

## Debugging

Enable debug logging in device preferences to see:
- Discovery messages
- Command transmission
- Response parsing
- Error messages

Debug logging automatically disables after 30 minutes.

## Compatibility

- **Hubitat Elevation**: Firmware 2.3.0 or later recommended
- **AmbiVision PRO**: All firmware versions
- Tested with firmware V.36

## Known Limitations

- No status feedback (device doesn't report current state back to Hubitat)
- Color control only works in Mood/Manual mode
- Mode changes require 500ms delays (handled automatically)
- Discovery uses broadcast UDP (may not work across VLANs)

## Contributing

Contributions are welcome! Please submit pull requests or open issues on GitHub.

### To-Do List
- [ ] Add HPM manifest
- [ ] Add state polling if AmbiVision PRO adds status endpoints
- [ ] Add scenes/presets support
- [ ] Add transition effects
- [ ] Add advanced audio mode configuration

## Credits

- Driver developed for the Hubitat community
- Based on AmbiVision PRO communication protocol documentation
- Inspired by other community drivers

## License

Licensed under the Apache License, Version 2.0

## Support

For issues, questions, or feature requests:
- Open an issue on GitHub
- Post in the [Hubitat Community Forum](https://community.hubitat.com/)

## Changelog

### Version 1.0.1 (2024)
- Fixed Field import issue
- Fixed parse method for UDP response handling
- Added hex to IP conversion
- Initial public release

### Version 1.0.0 (2024)
- Initial development version
- Core functionality implemented

---

**Enjoy your AmbiVision PRO with Hubitat! 🎨💡**
