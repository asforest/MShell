package com.github.asforest.mshell.permission

import net.mamoe.mirai.console.data.PluginDataExtensions
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermitteeId

object PermissionUtil
{
    val grantings: MutableCollection<PermitteeId> by lazy {
        grantedPermissionsMap
            .filter { it.key == MShellPermissions.all.id }
            .firstNotNullOfOrNull { it.value } ?: mutableListOf()
    }

    val grantedPermissionsMap: PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermitteeId>> by lazy {
        val clazz = Class.forName("net.mamoe.mirai.console.internal.permission.BuiltInPermissionService")
        val ins = clazz.getDeclaredField("INSTANCE").get(null)
        val config = clazz.getDeclaredField("config").also { it.isAccessible = true }.get(ins)
        val grantedPermissionsMap: PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermitteeId>> =
            config.javaClass.getDeclaredMethod("getGrantedPermissionMap").also { it.isAccessible = true }.invoke(config)
                    as PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermitteeId>>
        grantedPermissionsMap
    }
}