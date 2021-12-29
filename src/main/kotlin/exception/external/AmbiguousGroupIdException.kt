package com.github.asforest.mshell.exception.external

import net.mamoe.mirai.contact.Group

class AmbiguousGroupIdException (groupIdInputed: Long, group1: Group, group2: Group)
    : BaseExternalException("不准确的群聊号码: $groupIdInputed，" +
        "因为此号码能同时匹配到一个以上的群聊: ${group1.id}(${group1.name}) 和 ${group2.id}(${group2.name})，" +
        "请输入更为详细的群聊号码")