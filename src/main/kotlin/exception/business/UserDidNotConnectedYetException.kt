package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException
import com.github.asforest.mshell.session.user.GroupUser

class UserDidNotConnectedYetException : AbstractBusinessException
{
    constructor() : super("当前并未连接到一个会话上")

    constructor(group: GroupUser) : super("$group 还未连接到一个会话上")
}