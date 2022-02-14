package com.github.asforest.mshell.permission

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.exception.system.PermissionServiceNotCompatibleException
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermitteeId
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

object MShellPermissions
{
    val all by lazy { PermissionService.INSTANCE.register(MShellPlugin.permissionId("all"), "MShell插件主权限") }
    val use by lazy { PermissionService.INSTANCE.register(MShellPlugin.permissionId("use"), "MShell插件部分使用权限", all) }

    /**
     * 获取权限授权表，用来判断某个权限都给到了哪些用户
     *
     * 因为 PermissionService 接口里没有提供根据权限ID获取被授权的用户列表，所以只能以反射拿取对应数据，此乃无奈之举。
     */
    @Suppress("UNCHECKED_CAST")
    val permissionMap: Map<PermissionId, Collection<PermitteeId>> by lazy {

        val permissionService = PermissionService.INSTANCE
        val psName = permissionService::class.qualifiedName

        if (!psName.equals("net.mamoe.mirai.console.internal.permission.BuiltInPermissionService", ignoreCase = true))
            throw PermissionServiceNotCompatibleException(permissionService)

        permissionService::class.declaredMemberProperties.first { it.name.equals("grantedPermissionsMap", ignoreCase = true) }
            .run { this as KProperty1<Any, Map<PermissionId, Collection<PermitteeId>>> }
            .also { it.isAccessible = true }
            .get(permissionService)
    }

}