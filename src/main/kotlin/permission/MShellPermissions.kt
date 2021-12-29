package com.github.asforest.mshell.permission

import com.github.asforest.mshell.MShellPlugin
import net.mamoe.mirai.console.permission.PermissionService

object MShellPermissions
{
    val all by lazy {
        PermissionService.INSTANCE.register(MShellPlugin.permissionId("all"), "MShell插件主权限")
    }
}