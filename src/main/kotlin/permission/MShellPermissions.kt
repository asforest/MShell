package com.github.asforest.mshell.permission

import com.github.asforest.mshell.MShellPluing
import net.mamoe.mirai.console.permission.PermissionService

object MShellPermissions
{
    val all by lazy {
        PermissionService.INSTANCE.register(MShellPluing.permissionId("all"), "MShell插件主权限")
    }
}