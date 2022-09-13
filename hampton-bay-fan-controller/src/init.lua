local log = require "log"
local utils = require "st.utils"
local ZigbeeDriver = require "st.zigbee"
local ZigbeeDefaults = require "st.zigbee.defaults"

-- Capabilities
local capability_handlers = require "capability_handlers"
local capabilities        = require "st.capabilities"
local FanSpeed            = capabilities.fanSpeed
local Switch              = capabilities.switch
local SwitchLevel         = capabilities.switchLevel

-- Zigbee Clusters
local clusters        = require "st.zigbee.zcl.clusters"
local FanControl      = clusters.FanControl
local FanMode         = FanControl.attributes.FanMode
local FanModeSequence = FanControl.attributes.FanModeSequence
local OnOff           = clusters.OnOff
local Level           = clusters.Level

local zigbee_handlers = require "zigbee_handlers"
local lifecycle_handlers = require "lifecycle_handlers"
local zcl_commands = require "st.zigbee.zcl.global_commands"

local function parseZigbeeRxMsg(driver, device, zb_rx)
  log:debug("parseZigbeeRxMsg", utils.stringify_table(zb_rx, "zb_rx", true))

end

local driver_template = {
  supported_capabilities = {
    FanSpeed,
    Switch,
    SwitchLevel,
    capabilities.refresh,
    capabilities.firmwareUpdate
  },
  cluster_configurations = {
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
    [SwitchLevel.ID] = {
      {
        cluster = Level.ID,
        attribute = Level.attributes.CurrentLevel.ID,
        minimum_interval = 0,
        maximum_interval = 300,
        data_type = Level.attributes.CurrentLevel.base_type,
        reportable_change = 1
      }
    },
    [FanSpeed.ID] = {
      {
        cluster = FanControl.ID,
        attribute = FanMode.ID,
        minimum_interval = 0,
        maximum_interval = 300,
        data_type = FanMode.base_type,
        reportable_change = 1
      }
    }
  },
  zigbee_handlers = {
    attr = {
      [FanControl.ID] = {
        [FanMode.ID] = zigbee_handlers.handle_read_fanmode,
        --[FanModeSequence.ID] = zigbee_handlers.handle_fanmode_sequence
      }
    }
  },
  lifecycle_handlers = {
    init = lifecycle_handlers.handle_init,
    --added = lifecycle_handlers.handle_added,
    removed = lifecycle_handlers.handle_device_removed,
    -- doConfigure = lifecycle_handlers.handle_configuring,
    infoChanged = lifecycle_handlers.handle_infoChanged
  },
  capability_handlers = {
    -- Switch command handler
    [Switch.ID] = {
      [Switch.commands.on.NAME] = capability_handlers.handle_switch_on,
      [Switch.commands.off.NAME] = capability_handlers.handle_switch_off
    },
    -- Switch Level command handler
    [SwitchLevel.ID] = {
      [SwitchLevel.commands.setLevel.NAME] = capability_handlers.handle_set_light_level,
    },
    -- Fan Speed handlers
    [FanSpeed.ID] = {
      [FanSpeed.commands.setFanSpeed.NAME] = capability_handlers.handle_set_fan_speed
    },
    -- [capabilities.refresh.ID] = {
    --   [capabilities.refresh.commands.refresh.NAME] = capability_handlers.handle_refreshing
    -- },
  }
}

ZigbeeDefaults.register_for_default_handlers(driver_template, driver_template.supported_capabilities)
local hampton_bay_fan_controller = ZigbeeDriver("Hampton Bay Fan Controller", driver_template)
hampton_bay_fan_controller:run()
