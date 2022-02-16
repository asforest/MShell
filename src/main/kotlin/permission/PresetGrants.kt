package com.github.asforest.mshell.permission

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.business.NoPermissionToUsePresetExcetption
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

object PresetGrants : Map<String, Collection<PermitteeId>>
{
    const val Prefix = "preset."

    override val entries: Set<Map.Entry<String, Collection<PermitteeId>>>
        get() = allPresetGrantings
            .mapKeys { it.key.name.substring(Prefix.length) }
            .entries

    override val keys: Set<String>
        get() = allPresetGrantings.keys.map { it.name.substring(Prefix.length) }.toSet()

    override val size: Int
        get() = allPresetGrantings.size

    override val values: Collection<Collection<PermitteeId>>
        get() = allPresetGrantings.values

    val allPresetGrantings get() = MShellPermissions.permissionMap
            .filter { it.key.namespace == MShellPlugin.id && it.key.name.startsWith(Prefix) }

    override fun containsKey(key: String): Boolean
    {
        return MShellPermissions.permissionMap.any { it.key.name == Prefix + key }
    }

    override fun containsValue(value: Collection<PermitteeId>): Boolean
    {
        return value in values
    }

    override fun get(key: String): Collection<PermitteeId>?
    {
        return allPresetGrantings[PermissionId.parseFromString("${MShellPlugin.id}:$Prefix$key")]
    }

    override fun isEmpty(): Boolean
    {
        return MShellPermissions.permissionMap
            .any { it.key.namespace == MShellPlugin.id && it.key.name.startsWith(Prefix) }
    }

    /**
     * 添加一个用户的preset的使用权
     */
    fun addGrant(preset: String, qqnumber: Long): Boolean
    {
        if(isGranted(preset, qqnumber))
            return false

        val presetPid = MShellPlugin.permissionId(Prefix + preset)
        val permission = if(PermissionService.INSTANCE[presetPid] == null)
            registerPermission(presetPid)
        else
            preset2permission(preset)

        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        permittee.permit(permission)

        // 添加use权限
        if(!use.testPermission(permittee))
            permittee.permit(use)

        return true
    }

    /**
     * 移除一个用户的preset的使用权
     */
    fun removeGrant(preset: String, qqnumber: Long): Boolean
    {
        if(!isGranted(preset, qqnumber))
            return false

        val permission = preset2permission(preset)
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        permittee.cancel(permission, false)

        // 移除use权限
        if(use.testPermission(permittee))
            permittee.cancel(use, false)

        return true
    }

    /**
     * 测试是否拥有一个preset的使用权（管理员永远返回true）
     */
    fun testGrant(preset: String, qqnumber: Long): Boolean
    {
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)

        // 管理员总算有权限
        if(MShellPermissions.root.testPermission(permittee))
            return true

        return isGranted(preset, qqnumber)
    }

    /**
     * 测试是否拥有一个preset的使用权（管理员也一视同仁，不会有特权）
     */
    fun isGranted(preset: String, qqnumber: Long): Boolean
    {
        val grants = this[preset] ?: return false
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        return grants.contains(permittee) && use.testPermission(permittee)
    }

    /**
     * 测试是否拥有任意一个preset的使用权（管理员也一视同仁，不会有特权）
     */
    fun isGranted(qqnumber: Long): Boolean
    {
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)

        return MShellPermissions.permissionMap
            .any {
                if(it.key.namespace == MShellPlugin.id && it.key.name.startsWith(Prefix))
                    it.value.contains(permittee)
                else
                    false
            }
    }

    /**
     * 获取所有可用的环境预设
     */
    fun getAvailablePresets(user: SessionUser): List<Preset>
    {
        if (user is SessionUser.GroupUser)
            return listOf()

        if (user is SessionUser.ConsoleUser)
            return PresetsConfig.presets.values.toList()

        val friendUser = user as SessionUser.FriendUser

        return PresetsConfig.presets.values.filter {
            testGrant(it.name, friendUser.user.id)
        }
    }

    /**
     * 判断用户是否拥有某个预设的权限
     */
    fun isPresetAvailable(preset: Preset, user: SessionUser): Boolean
    {
        // GroupUser 没有任何权限
        if (user is SessionUser.GroupUser)
            return false

        // ConsoleUser 拥有所有权限
        if (user is SessionUser.ConsoleUser)
            return true

        val friendUser = user as SessionUser.FriendUser
        return testGrant(preset.name, friendUser.user.id)
    }

    /**
     * 检查一个用户是否能使用**指定环境预设**或者**默认的环境预设**
     * @throws NoPermissionToUsePresetExcetption 当没有权限使用这个预设时
     */
    fun useDefaultPreset(preset: String?, user: SessionUser): Preset
    {
        val _preset = SessionManager.useDefaultPreset(preset)

        // GroupUser 没有任何权限
        if (user is SessionUser.GroupUser)
            throw NoPermissionToUsePresetExcetption(preset)

        // ConsoleUser 拥有所有权限
        if (user is SessionUser.ConsoleUser)
            return _preset

        // FriendUser 需要进一步鉴权
        val presetsAvailable = PresetGrants.getAvailablePresets(user)
        val qq = (user as SessionUser.FriendUser).user.id

        // 如果指定的预设是没有权限的话，再检查一下有没有其它可用的预设
        if(!PresetGrants.testGrant(_preset.name, qq))
        {
            // 如果默认预设不可用，则选取唯一可用的Preset，如果没有或者是可用数量的大于2，抛异常
            // 如果明确指定了一个Preset，则抛异常
            if(presetsAvailable.size == 1 && preset == null)
                return presetsAvailable.first()

            throw NoPermissionToUsePresetExcetption(_preset.name)
        }

        // 如果有权限则直接返回
        return _preset
    }

    /**
     * 注册所有preset.yml里面有的preset使用权限
     */
    fun registerAllPresetPermissions()
    {
        // 注册权限
        for (name in PresetsConfig.presets.keys)
        {
            val pid = preset2permissionId(name)
            if(PermissionService.INSTANCE[pid] == null)
                registerPermission(pid)
        }

        val allPermittees = values.flatten()

        // 删除多余的use权限
        val users = MShellPermissions.permissionMap
            .filterKeys { it == use.id }
            .firstNotNullOfOrNull { it }
            ?.value ?: mutableListOf()
        val tobeCanceled = mutableListOf<PermitteeId>()
        for (user in users)
        {
            if(user !in allPermittees)
                tobeCanceled += user
        }
        tobeCanceled.forEach { it.cancel(use, false) }


        // 补齐缺少的use权限
        for (permittee in allPermittees)
        {
            if(!use.testPermission(permittee))
                permittee.permit(use)
        }
    }

    private val use get() = MShellPermissions.use

    private fun registerPermission(pid: PermissionId): Permission
    {
        return PermissionService.INSTANCE.register(pid, "MShell插件预设使用权限")
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun preset2permission(preset: String): Permission
    {
        return PermissionService.INSTANCE[preset2permissionId(preset)]!!
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun preset2permissionId(preset: String): PermissionId
    {
        return PermissionId.parseFromString("${MShellPlugin.id}:$Prefix$preset")
    }
}