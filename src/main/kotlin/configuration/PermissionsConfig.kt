package com.github.asforest.mshell.configuration

import java.lang.ClassCastException

object PermissionsConfig : YamlConfig("group-permissions.yml")
{
    private var presetGranting = mutableMapOf<String, ArrayList<Long>>()

    /**
     * 添加一个授权
     */
    fun addGrant(preset: String, id: Long): Boolean
    {
        if(testGrant(preset, id))
            return false

        this[preset].add(id)

        write()
        return true
    }

    /**
     * 移除一个授权
     */
    fun removeGrant(preset: String, id: Long): Boolean
    {
        if(!testGrant(preset, id))
            return false

        this[preset].remove(id)

        if(this[preset].isEmpty())
            presetGranting.remove(preset)

        write()
        return true
    }

    /**
     * 测试有没有权限
     */
    fun testGrant(preset: String, id: Long): Boolean
    {
        return preset in presetGranting && (this[preset].contains(id) || this[preset].contains(0))
    }

    /**
     * 获取被当前授权的用户数
     */
    fun getUserCountOfPreset(preset: String): Int
    {
        if(preset !in presetGranting)
            return 0
        
        return presetGranting[preset]!!.size
    }

    /**
     * 获取所有的授权情况
     */
    fun getAllPersetGrantings(preset: String? = null): Map<String, ArrayList<Long>>
    {
        return presetGranting.filter { preset == null || it.key == preset }
    }

    private operator fun get(preset: String): MutableList<Long>
    {
        presetGranting.putIfAbsent(preset, ArrayList())
        return presetGranting[preset]!!
    }

    private fun validate()
    {
        presetGranting.keys.map { it }.forEach { key ->
            if(key !in PresetsConfig.presets)
                presetGranting.remove(key)
        }
    }

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        presetGranting.clear()

        for (entry in deserialized)
        {
            val temp = ArrayList<Long>()

            for (id in entry.value as ArrayList<Any>)
            {
                temp += try {
                    id as Long
                } catch (e: ClassCastException) {
                    (id as Int).toLong()
                }
            }

            presetGranting[entry.key] = temp
        }
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized.putAll(presetGranting)
    }
}