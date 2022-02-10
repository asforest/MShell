package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException
import com.github.asforest.mshell.session.user.GroupUser

class UserNotConnectedException : AbstractBusinessException
{
    constructor() : super("当前并未连接到当前会话上")

    constructor(group: GroupUser) : super("$group 还未连接到当前会话上")
}