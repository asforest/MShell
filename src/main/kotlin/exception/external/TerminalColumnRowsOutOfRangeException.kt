package com.github.asforest.mshell.exception.external

class TerminalColumnRowsOutOfRangeException(columns: Int, rows: Int)
    : BaseExternalException("终端宽度或者终端高度不正确: Columns: $columns x Rows: rows")