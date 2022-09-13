local log              = require "log"
local utils            = require "st.utils"
local fan_speed_helper = require "fan_speed_helper"

-- Capabilities
local capabilities = require "st.capabilities"
local FanSpeed     = capabilities.fanSpeed
-- local Switch       = capabilities.switch
-- local SwitchLevel  = capabilities.switchLevel

-- Zigbee Clusters
local clusters   = require "st.zigbee.zcl.clusters"
local FanControl = clusters.FanControl
local FanMode    = FanControl.attributes.FanMode
-- local FanModeSequence = FanControl.attributes.FanModeSequence
local Level      = clusters.Level
local OnOff      = clusters.OnOff

-- Capability Handlers

local capability_handlers = {}

local additional_fields = {
  state_change = true
}
local handle_refreshing = function(driver, device)
  log:debug("Executing 'handle_refreshing'")

  local capabilities = driver.cluster_configurations
  for _, capability in pairs(capabilities) do
    log:trace("<<--- Stuss --->>", utils.stringify_table(capability, "capability", true))
    for key, attr in pairs(capability) do
      log:trace("<<--- Stuss --->>", utils.stringify_table(attr, "attribute", true))
      device:send(attr:read(device))
    end
  end
end
capability_handlers.handle_refreshing = handle_refreshing

--- Handles the 'setLevel' event
local handle_set_light_level = function(driver, device, command)
  log:debug("Executing 'handle_set_light_level'", utils.stringify_table(command, "command", true))

  local requestedLevel = command.args.level

  local component = command.component
  if component == "main" then
    local fanSpeed = fan_speed_helper.map_switch_level_to_fan_speed(requestedLevel)
    device:send(FanMode:write(device, fanSpeed))
    return
  end

  local rate = tonumber(device.preferences.dimRate) or 0
  local query_delay = math.floor(rate / 10 + 0.5)
  -- The device supports levels 0-254
  local level = math.floor((requestedLevel / 100) * 254)
  if level < 20 then
    level = 0
  end
  device:send(Level.server.commands.MoveToLevelWithOnOff(device, level, rate))

  -- -- Need to do some endpoint mapping here
  -- local component = command.component
  -- local ep = device:get_endpoint_for_component_id(component)
  -- device:emit_event_for_endpoint(ep, SwitchLevel.level(requestedLevel))

  device.thread:call_with_delay(query_delay, function(d)
    device:refresh()
  end)
end
capability_handlers.handle_set_light_level = handle_set_light_level

--- Handles the 'setFanSpeed' event
local handle_set_fan_speed = function(driver, device, command)
  log:debug("Executing 'handle_set_fan_speed'")

  local fanSpeed = command.args.speed
  device:send(FanMode:write(device, fanSpeed))
end
capability_handlers.handle_set_fan_speed = handle_set_fan_speed

-- Handles the 'on' event
local handle_switch_on = function(driver, device, command)
  log:debug("Executing 'handle_switch_on'")

  -- Need to do some endpoint mapping here
  local component = command.component
  local ep = device:get_endpoint_for_component_id(component)

  if (component == "main") then
    local lastSpeed = device:get_field("lastSpeed") or FanMode.LOW
    device:send(FanMode:write(device, lastSpeed))
    return
  end

  device:send(OnOff.server.commands.On(device))
  --device:emit_event_for_endpoint(ep, Switch.switch.on(additional_fields))
  device:refresh()
end
capability_handlers.handle_switch_on = handle_switch_on

-- Handles the 'off' event
local handle_switch_off = function(driver, device, command)
  log:debug("Executing 'handle_switch_off'")

  -- Need to do some endpoint mapping here
  local component = command.component
  local ep = device:get_endpoint_for_component_id(component)

  if (component == "main") then
    local currSpeed = device:get_latest_state(component, FanSpeed.ID, 'fanSpeed', FanMode.LOW)
    device:set_field("lastSpeed", currSpeed)
    device:send(FanMode:write(device, FanMode.OFF))
    return
  end

  device:send(OnOff.server.commands.Off(device))
  --device:emit_event_for_endpoint(ep, Switch.switch.off(additional_fields))
  device:refresh()
end
capability_handlers.handle_switch_off = handle_switch_off

-- End Capability Handlers

return capability_handlers
