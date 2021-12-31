package com.github.asforest.mshell.model

data class EnvironmentalPreset(
    var name: String,
    var shell: String = "",
    var charset: String = "",
    var workdir: String = "",
    var env: HashMap<String, String> = HashMap(),
    var exec: String = ""
) {
    override fun toString(): String
    {
        return "Preset[shell=$shell, charset=$charset, workdir=$workdir, env=$env, exec=$exec]"
    }
}