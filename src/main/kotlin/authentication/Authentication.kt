package com.github.asforest.mshell.authentication

import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.external.NoPermissionToUsePresetExcetption
import com.github.asforest.mshell.model.EnvironmentalPreset
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.SessionManager

object Authentication
{
    /**
     * 使用默认的环境预设
     */
    fun useDefaultPreset(preset: String?, qqnumber: Long): EnvironmentalPreset
    {
        // 先尝试获取默认的Preset
        val defPreset = SessionManager.useDefaultPreset(preset)

        // 鉴权
        if(!PresetGrants.testGrant(defPreset.name, qqnumber))
        {
            val presetsAvailable = getAvailablePresets(qqnumber)

            // 如果默认Preset不可用，则选取唯一可用的Preset，如果没有或者是可用数量的大于2，抛异常
            // 如果明确指定了一个Preset，则抛异常
            if(presetsAvailable.size == 1 && preset == null)
                return presetsAvailable.first()

            throw NoPermissionToUsePresetExcetption(defPreset.name)
        }

        return defPreset
    }

    /**
     * 获取所有可用的环境预设
     */
    fun getAvailablePresets(qqnumber: Long): List<EnvironmentalPreset>
    {
        return PresetsConfig.presets.values.filter { PresetGrants.testGrant(it.name, qqnumber) }
    }
}