package com.github.asforest.mshell.permission

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.configuration.PresetsConfig
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.plugin.id

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

        val permission = if(preset !in this)
            registerPermission(MShellPlugin.permissionId(Prefix + preset))
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
        if(MShellPermissions.all.testPermission(permittee))
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
                    return it.value.contains(permittee)
                return false
            }
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