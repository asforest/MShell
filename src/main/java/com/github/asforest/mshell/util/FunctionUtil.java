package com.github.asforest.mshell.util;

import com.github.asforest.mshell.session.SessionUser;

public class FunctionUtil
{
    public static boolean SessionUserEquals(SessionUser sessionUser, Object obj)
    {
        if(obj == sessionUser)
            return true;
        if(!(obj instanceof SessionUser))
            return false;
        return ((SessionUser) obj).getId().equals(sessionUser.getId());
    }
}
