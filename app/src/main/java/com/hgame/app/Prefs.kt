package com.hgame.app

import android.content.Context
import android.content.SharedPreferences

/** Persistent settings, favourites, recent games and records. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("hgame", Context.MODE_PRIVATE)
    }

    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()

    var vibration: Boolean
        get() = sp.getBoolean("vibration", true)
        set(v) = sp.edit().putBoolean("vibration", v).apply()

    fun best(id: String): Int? = if (sp.contains("best_$id")) sp.getInt("best_$id", 0) else null
    fun setBest(id: String, v: Int) = sp.edit().putInt("best_$id", v).apply()

    fun option(id: String, key: String, default: Int) = sp.getInt("opt_${id}_$key", default)
    fun setOption(id: String, key: String, v: Int) = sp.edit().putInt("opt_${id}_$key", v).apply()

    fun getInt(id: String, key: String, default: Int) = sp.getInt("data_${id}_$key", default)
    fun putInt(id: String, key: String, v: Int) = sp.edit().putInt("data_${id}_$key", v).apply()

    fun favorites(): Set<String> = sp.getStringSet("favs", emptySet())!!.toSet()
    fun toggleFavorite(id: String) {
        val s = favorites().toMutableSet()
        if (!s.remove(id)) s.add(id)
        sp.edit().putStringSet("favs", s).apply()
    }

    fun recent(): List<String> = sp.getString("recent", "")!!.split(',').filter { it.isNotEmpty() }
    fun pushRecent(id: String) {
        val l = (listOf(id) + recent().filter { it != id }).take(12)
        sp.edit().putString("recent", l.joinToString(",")).apply()
    }

    fun plays(id: String) = sp.getInt("plays_$id", 0)
    fun addPlay(id: String) = sp.edit().putInt("plays_$id", plays(id) + 1).apply()
}
