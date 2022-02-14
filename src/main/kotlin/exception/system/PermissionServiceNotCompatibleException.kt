package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException
import net.mamoe.mirai.console.permission.PermissionService

class PermissionServiceNotCompatibleException(val ps: PermissionService<*>)
    : AbstractSystemException("MShell is not compatible with the current PermissionService: ${PermissionService.INSTANCE::class.simpleName}, please use BuiltInPermissionService to enable MShell plugin.")