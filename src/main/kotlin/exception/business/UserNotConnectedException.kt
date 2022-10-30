package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException
import com.github.asforest.mshell.session.SessionUser

class UserNotConnectedException : AbstractBusinessException
{
    constructor() : super("当前并未连接到任何会话上")

    constructor(group: SessionUser.GroupUser) : super("$group 还未连接到当前会话上")
}