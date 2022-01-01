package com.github.asforest.mshell.permission

import com.github.asforest.mshell.MShellPlugin
import net.mamoe.mirai.console.data.PluginDataExtensions
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermitteeId

object MShellPermissions
{
    val all by lazy { PermissionService.INSTANCE.register(MShellPlugin.permissionId("all"), "MShell插件主权限") }
    val use by lazy { PermissionService.INSTANCE.register(MShellPlugin.permissionId("use"), "MShell插件部分使用权限") }

    val permissionMap: PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermitteeId>> by lazy {
        val clazz = Class.forName("net.mamoe.mirai.console.internal.permission.BuiltInPermissionService")
        val ins = clazz.getDeclaredField("INSTANCE").get(null)
        val config = clazz.getDeclaredField("config").also { it.isAccessible = true }.get(ins)
        val grantedPermissionsMap: PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermitteeId>> =
            config.javaClass.getDeclaredMethod("getGrantedPermissionMap").also { it.isAccessible = true }.invoke(config)
                    as PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermitteeId>>
        grantedPermissionsMap
    }


}