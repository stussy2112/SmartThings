local device_management = require "st.zigbee.device_management"
local log               = require "log"
local utils             = require "st.utils"

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

-- Lifecycle Handlers

local function component_to_endpoint(device, component_id)
  log:debug("Executing 'component_to_endpoint'", component_id)
  if (component_id == "light") then
    return 1
  end
  return device.fingerprinted_endpoint_id
  -- local ep_num = component_id:match("switch(%d)")
  -- return ep_num and tonumber(ep_num) or device.fingerprinted_endpoint_id
end

local function endpoint_to_component(device, ep)
  log:debug("Executing 'endpoint_to_component'", ep)
  if (ep == 1) then
    return "light"
  end
  return "main"
  -- local switch_comp = string.format("switch%d", ep)
  -- if device.profile.components[switch_comp] ~= nil then
  --   return switch_comp
  -- else
  --   return "main"
  -- end
end

local lifecycle_handlers = {}

local handle_configuring = function(driver, device)
  log:debug("Executing 'handle_configure'")
end
lifecycle_handlers.handle_configuring = handle_configuring

local cluster_configurations = {
  [Switch.ID] = {
    {
      cluster = OnOff.ID,
      attribute = OnOff.attributes.OnOff.ID,
      minimum_interval = 0,
      maximum_interval = 300,
      data_type = OnOff.attributes.OnOff.base_type,
      reportable_change = 1
    }
  },
  [SwitchLevel.ID] =
  {
    cluster = Level.ID,
    attribute = Level.attributes.CurrentLevel.ID,
    minimum_interval = 0,
    maximum_interval = 300,
    data_type = Level.attributes.CurrentLevel.base_type,
    reportable_change = 1
  },
  -- [FanSpeed.ID] = {
  --   {
  --     cluster = FanControl.ID,
  --     attribute = FanMode.ID,
  --     minimum_interval = 0,
  --     maximum_interval = 300,
  --     data_type = FanMode.base_type,
  --     reportable_change = 1
  --   },
  --   {
  --     cluster = FanControl.ID,
  --     attribute = FanModeSequence.ID,
  --     minimum_interval = 0,
  --     maximum_interval = 300,
  --     data_type = FanModeSequence.base_type,
  --     reportable_change = 1
  --   }
  -- }
}

local handle_added = function(driver, device, event, args)
  log:debug("Executing 'handle_added'")

  -- device:emit_event(FanSpeed.fanSpeed(FanMode.AUTO))
  device:send(FanMode:write(device, FanMode.LOW))
  device:refresh()
  -- mark device as online so it can be controlled from the app
  device:online()
end
lifecycle_handlers.handle_added = handle_added

local handle_infoChanged = function(driver, device, event, args)
  log:debug("Executing 'handle_infoChanged'")
  if device.preferences then
    local dim_onOff = tonumber(device.preferences.dimOnOff)
    local dim_rate = tonumber(device.preferences.dimRate)
    if dim_onOff == 0 then
      dim_rate = 0
    end

    device:send(Level.attributes.OnOffTransitionTime:write(device, dim_rate))
  end
end
lifecycle_handlers.handle_infoChanged = handle_infoChanged

local handle_init = function(driver, device, event, args)
  log:debug("Executing 'handle_init'")

  device:set_component_to_endpoint_fn(component_to_endpoint)
  device:set_endpoint_to_component_fn(endpoint_to_component)

  for _, capability in pairs(driver.cluster_configurations) do
    for _, attr in pairs(capability) do
      device:add_configured_attribute(attr)
      device:add_monitored_attribute(attr)
    end
  end

  device:send(FanMode:write(device, FanMode.LOW))
  device:refresh()
  -- mark device as online so it can be controlled from the app
  device:online()
end
lifecycle_handlers.handle_init = handle_init

local function handle_device_removed(driver, device)
  log:trace("Executing 'device_removed'")

  for _, capability in pairs(driver.cluster_configurations) do
    for _, attr in pairs(capability) do
      device:remove_configured_attribute(attr.cluster, attr)
      device:remove_monitored_attribute(attr.cluster, attr)
    end
  end
end

lifecycle_handlers.handle_device_removed = handle_device_removed

-- End Lifecycle Handlers
return lifecycle_handlers
