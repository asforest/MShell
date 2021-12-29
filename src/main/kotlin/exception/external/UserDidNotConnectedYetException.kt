package com.github.asforest.mshell.exception.external

import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.session.user.GroupUser

class UserDidNotConnectedYetException : BaseExternalException
{
    constructor() : super("当前并未连接到一个会话上")

    constructor(group: GroupUser) : super("$group 还未连接到一个会话上")
}