local log          = require "log"

-- Zigbee Clusters
local clusters        = require "st.zigbee.zcl.clusters"
local FanControl      = clusters.FanControl
local FanMode         = FanControl.attributes.FanMode
local FanModeSequence = FanControl.attributes.FanModeSequence
local Level           = clusters.Level
local OnOff           = clusters.OnOff

local levels_for_4_speed = {
  OFF = 0,
  LOW = 25,
  MEDIUM = 50,
  HIGH = 75,
  MAX = 100,
}

local function map_fan_speed_to_switch_level(speed)
  if speed == FanMode.OFF then
    return levels_for_4_speed.OFF -- 0
  elseif speed == FanMode.LOW then
    return levels_for_4_speed.LOW -- 25
  elseif speed == FanMode.MEDIUM then
    return levels_for_4_speed.MEDIUM -- 50
  elseif speed == FanMode.HIGH then
    return levels_for_4_speed.HIGH -- 75
  elseif speed == FanMode.ON then
    return levels_for_4_speed.MAX -- 99
  else
    log.error(string.format("4 speed fan driver: invalid fan speed: %d", speed))
  end
end

local function map_switch_level_to_fan_speed(level)
  if (level == 0) then
    return FanMode.OFF
  elseif (levels_for_4_speed.OFF < level and level <= levels_for_4_speed.LOW) then
    return FanMode.LOW
  elseif (levels_for_4_speed.LOW < level and level <= levels_for_4_speed.MEDIUM) then
    return FanMode.MEDIUM
  elseif (levels_for_4_speed.MEDIUM < level and level <= levels_for_4_speed.HIGH) then
    return FanMode.HIGH
  elseif (levels_for_4_speed.HIGH < level and level <= levels_for_4_speed.MAX) then
    return FanMode.ON
  else
    log.error(string.format("invalid level: %d", level))
  end
end

local fan_speed_helper = {
  levels_for_4_speed = levels_for_4_speed,
  map_switch_level_to_fan_speed = map_switch_level_to_fan_speed,
  map_fan_speed_to_switch_level = map_fan_speed_to_switch_level
}

return fan_speed_helper