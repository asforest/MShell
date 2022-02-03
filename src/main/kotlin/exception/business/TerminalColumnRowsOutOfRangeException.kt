package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class TerminalColumnRowsOutOfRangeException(columns: Int, rows: Int)
    : AbstractBusinessException("终端宽度或者终端高度不正确: Columns: $columns x Rows: rows")