local log = require "log"
local utils = require "st.utils"
local fan_speed_helper = require "fan_speed_helper"

-- Capabilities
local capabilities = require "st.capabilities"
local FanSpeed     = capabilities.fanSpeed
local Switch       = capabilities.switch
local SwitchLevel  = capabilities.switchLevel

-- Zigbee Clusters
local clusters        = require "st.zigbee.zcl.clusters"
local FanControl      = clusters.FanControl
local FanMode         = FanControl.attributes.FanMode
local FanModeSequence = FanControl.attributes.FanModeSequence
local OnOff           = clusters.OnOff
local Level           = clusters.Level

-- Zigbee Handlers

local SUPPORTED_FAN_MODES = {
  [FanModeSequence.LOW_MED_HIGH]      = { "FAN_MODE_LOW", "FAN_MODE_MEDIUM", "FAN_MODE_HIGH" },
  [FanModeSequence.LOW_HIGH]          = { "FAN_MODE_LOW", "FAN_MODE_HIGH" },
  [FanModeSequence.LOW_MED_HIGH_AUTO] = { "FAN_MODE_LOW", "FAN_MODE_MEDIUM", "FAN_MODE_HIGH", "FAN_MODE_AUTO" },
  [FanModeSequence.LOW_HIGH_AUTO]     = { "FAN_MODE_LOW", "FAN_MODE_HIGH", "FAN_MODE_AUTO" },
  [FanModeSequence.ON_AUTO]           = { "FAN_MODE_ON", "FAN_MODE_AUTO" },
  -- [0x05]                              = { "FAN_MODE_LOW", "FAN_MODE_MEDIUM", "FAN_MODE_HIGH", "FAN_MODE_OFF", "FAN_MODE_ON" }
  [0x05]                              = { "FAN_MODE_OFF", "FAN_MODE_LOW", "FAN_MODE_MEDIUM", "FAN_MODE_HIGH",
    "FAN_MODE_ON" },
  -- [0x05]                              = { FanMode.OFF, FanMode.LOW, FanMode.MEDIUM, FanMode.HIGH, FanMode.AUTO }
}

local additional_fields = {
  state_change = true
}

local zigbee_handlers = {}

local handle_read_fanmode = function(driver, device, value, zb_rx)
  log:debug("Executing 'handle_read_fanmode'")
  device:emit_event(FanSpeed.fanSpeed(value.value))

  local fanLevel = fan_speed_helper.map_fan_speed_to_switch_level(value.value)
  device:emit_event(SwitchLevel.level(fanLevel))

  local switchEvent = Switch.switch.off(additional_fields)
  if (value.value ~= FanMode.OFF) then
    switchEvent = Switch.switch.on(additional_fields)
  end
  device:emit_event(switchEvent)
end
zigbee_handlers.handle_read_fanmode = handle_read_fanmode


local handle_fanmode_sequence = function(driver, device, value, zb_rx)
  log:debug("Executing 'handle_fanmode_sequence' -->>", value.value)
  return SUPPORTED_FAN_MODES[value.value]
end
zigbee_handlers.handle_fanmode_sequence = handle_fanmode_sequence

-- End Zibee Handlers

return zigbee_handlers
