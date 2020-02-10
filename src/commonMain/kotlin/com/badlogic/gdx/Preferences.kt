/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogic.gdx

/**
 *
 *
 * A Preference instance is a hash map holding different values. It is stored alongside your application (SharedPreferences on
 * Android, LocalStorage on GWT, on the desktop a Java Preferences file in a ".prefs" directory will be created, and on iOS an
 * NSMutableDictionary will be written to the given file). CAUTION: On the desktop platform, all libgdx applications share the same
 * ".prefs" directory. To avoid collisions use specific names like "com.myname.game1.settings" instead of "settings"
 *
 *
 *
 *
 * Changes to a preferences instance will be cached in memory until [.flush] is invoked.
 *
 *
 *
 *
 * Use [Application.getPreferences] to look up a specific preferences instance. Note that on several backends the
 * preferences name will be used as the filename, so make sure the name is valid for a filename.
 *
 *
 * @author mzechner
 */
interface Preferences {

    fun putBoolean(key: String?, `val`: Boolean): Preferences?
    fun putInteger(key: String?, `val`: Int): Preferences?
    fun putLong(key: String?, `val`: Long): Preferences?
    fun putFloat(key: String?, `val`: Float): Preferences?
    fun putString(key: String?, `val`: String?): Preferences?
    fun put(vals: Map<String?, *>?): Preferences?
    fun getBoolean(key: String?): Boolean
    fun getInteger(key: String?): Int
    fun getLong(key: String?): Long
    fun getFloat(key: String?): Float
    fun getString(key: String?): String?
    fun getBoolean(key: String?, defValue: Boolean): Boolean
    fun getInteger(key: String?, defValue: Int): Int
    fun getLong(key: String?, defValue: Long): Long
    fun getFloat(key: String?, defValue: Float): Float
    fun getString(key: String?, defValue: String?): String?

    /** Returns a read only Map<String></String>, Object> with all the key, objects of the preferences.  */
    fun get(): Map<String?, *>?
    operator fun contains(key: String?): Boolean
    fun clear()
    fun remove(key: String?)

    /** Makes sure the preferences are persisted.  */
    fun flush()
}
