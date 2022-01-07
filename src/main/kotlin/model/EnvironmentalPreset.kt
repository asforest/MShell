package com.github.asforest.mshell.model

data class EnvironmentalPreset(
    var name: String,
    var command: String = "",
    var charset: String = "",
    var workdir: String = "",
    var env: HashMap<String, String> = HashMap(),
    var input: String = "",
    var singleInstance: Boolean = false,
    var columns: Int = 80,
    var rows: Int = 25
) {
    override fun toString(): String
    {
        return "[shell=$command, charset=$charset, workdir=$workdir, env=$env, exec=$input, singleInstance=$singleInstance, ColumnRows=$columns x $rows]"
    }
}