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

import com.badlogic.gdx.utils.Clipboard
import java.lang.Runnable

/**
 *
 *
 * An `Application` is the main entry point of your project. It sets up a window and rendering surface and manages the
 * different aspects of your application, namely [Graphics], [Audio], [Input] and [Files]. Think of an
 * Application being equivalent to Swing's `JFrame` or Android's `Activity`.
 *
 *
 *
 *
 * An application can be an instance of any of the following:
 *
 *  * a desktop application (see `JglfwApplication` found in gdx-backends-jglfw.jar)
 *  * an Android application (see `AndroidApplication` found in gdx-backends-android.jar)
 *  * a HTML5 application (see `GwtApplication` found in gdx-backends-gwt.jar)
 *  * an iOS application (see `IOSApplication` found in gdx-backends-robovm.jar)
 *
 * Each application class has it's own startup and initialization methods. Please refer to their documentation for more
 * information.
 *
 *
 *
 *
 * While game programmers are used to having a main loop, libgdx employs a different concept to accommodate the event based nature
 * of Android applications a little more. You application logic must be implemented in a [ApplicationListener] which has
 * methods that get called by the Application when the application is created, resumed, paused, disposed or rendered. As a
 * developer you will simply implement the ApplicationListener interface and fill in the functionality accordingly. The
 * ApplicationListener is provided to a concrete Application instance as a parameter to the constructor or another initialization
 * method. Please refer to the documentation of the Application implementations for more information. Note that the
 * ApplicationListener can be provided to any Application implementation. This means that you only need to write your program
 * logic once and have it run on different platforms by passing it to a concrete Application implementation.
 *
 *
 *
 *
 * The Application interface provides you with a set of modules for graphics, audio, input and file i/o.
 *
 *
 *
 *
 * [Graphics] offers you various methods to output visuals to the screen. This is achieved via OpenGL ES 2.0 or 3.0
 * depending on what's available an the platform. On the desktop the features of OpenGL ES 2.0 and 3.0 are emulated via desktop
 * OpenGL. On Android the functionality of the Java OpenGL ES bindings is used.
 *
 *
 *
 *
 * [Audio] offers you various methods to output and record sound and music. This is achieved via the Java Sound API on the
 * desktop. On Android the Android media framework is used.
 *
 *
 *
 *
 * [Input] offers you various methods to poll user input from the keyboard, touch screen, mouse and accelerometer.
 * Additionally you can implement an [InputProcessor] and use it with [Input.setInputProcessor] to
 * receive input events.
 *
 *
 *
 *
 * [Files] offers you various methods to access internal and external files. An internal file is a file that is stored near
 * your application. On Android internal files are equivalent to assets. On the desktop the classpath is first scanned for the
 * specified file. If that fails then the root directory of your application is used for a look up. External files are resources
 * you create in your application and write to an external storage. On Android external files reside on the SD-card, on the
 * desktop external files are written to a users home directory. If you know what you are doing you can also specify absolute file
 * names. Absolute filenames are not portable, so take great care when using this feature.
 *
 *
 *
 *
 * [Net] offers you various methods to perform network operations, such as performing HTTP requests, or creating server and
 * client sockets for more elaborate network programming.
 *
 *
 *
 *
 * The `Application` also has a set of methods that you can use to query specific information such as the operating
 * system the application is currently running on and so forth. This allows you to have operating system dependent code paths. It
 * is however not recommended to use this facilities.
 *
 *
 *
 *
 * The `Application` also has a simple logging method which will print to standard out on the desktop and to logcat on
 * Android.
 *
 *
 * @author mzechner
 */
interface Application {

    /** Enumeration of possible [Application] types
     *
     * @author mzechner
     */
    enum class ApplicationType {

        Android, Desktop, HeadlessDesktop, Applet, WebGL, iOS
    }

    /** @return the [ApplicationListener] instance
     */
    val applicationListener: ApplicationListener?

    /** @return the [Graphics] instance
     */
    val graphics: Graphics?

    /** @return the [Audio] instance
     */
    val audio: Audio?

    /** @return the [Input] instance
     */
    val input: Input?

    /** @return the [Files] instance
     */
    val files: Files?

    /** @return the [Net] instance
     */
    val net: Net?

    /** Logs a message to the console or logcat  */
    fun log(tag: String?, message: String?)

    /** Logs a message to the console or logcat  */
    fun log(tag: String?, message: String?, exception: Throwable?)

    /** Logs an error message to the console or logcat  */
    fun error(tag: String?, message: String?)

    /** Logs an error message to the console or logcat  */
    fun error(tag: String?, message: String?, exception: Throwable?)

    /** Logs a debug message to the console or logcat  */
    fun debug(tag: String?, message: String?)

    /** Logs a debug message to the console or logcat  */
    fun debug(tag: String?, message: String?, exception: Throwable?)

    /** Gets the log level.  */
    /** Sets the log level. [.LOG_NONE] will mute all log output. [.LOG_ERROR] will only let error messages through.
     * [.LOG_INFO] will let all non-debug messages through, and [.LOG_DEBUG] will let all messages through.
     * @param logLevel [.LOG_NONE], [.LOG_ERROR], [.LOG_INFO], [.LOG_DEBUG].
     */
    var logLevel: Int

    /** Sets the current Application logger. Calls to [.log] are delegated to this [ApplicationLogger]  */
    fun setApplicationLogger(applicationLogger: ApplicationLogger?)

    /** @return the current [ApplicationLogger]
     */
    fun getApplicationLogger(): ApplicationLogger?

    /** @return what [ApplicationType] this application has, e.g. Android or Desktop
     */
    val type: ApplicationType?

    /** @return the Android API level on Android, the major OS version on iOS (5, 6, 7, ..), or 0 on the desktop.
     */
    val version: Int

    /** @return the Java heap memory use in bytes
     */
    val javaHeap: Long

    /** @return the Native heap memory use in bytes
     */
    val nativeHeap: Long

    /** Returns the [Preferences] instance of this Application. It can be used to store application settings across runs.
     * @param name the name of the preferences, must be useable as a file name.
     * @return the preferences.
     */
    fun getPreferences(name: String?): Preferences?
    val clipboard: Clipboard?

    /** Posts a [Runnable] on the main loop thread.
     *
     * In a multi-window application, the [Gdx.graphics] and [Gdx.input] values may be
     * unpredictable at the time the Runnable is executed. If graphics or input are needed, they can be copied
     * to a variable to be used in the Runnable. For example:
     *
     * `
     * final Graphics graphics = Gdx.graphics;
     *
     * @param runnable the runnable.
    ` */
    fun postRunnable(runnable: Runnable?)

    /** Schedule an exit from the application. On android, this will cause a call to pause() and dispose() some time in the future,
     * it will not immediately finish your application.
     * On iOS this should be avoided in production as it breaks Apples guidelines */
    fun exit()

    /** Adds a new [LifecycleListener] to the application. This can be used by extensions to hook into the lifecycle more
     * easily. The [ApplicationListener] methods are sufficient for application level development.
     * @param listener
     */
    fun addLifecycleListener(listener: LifecycleListener?)

    /** Removes the [LifecycleListener].
     * @param listener
     */
    fun removeLifecycleListener(listener: LifecycleListener?)

    companion object {
        const val LOG_NONE = 0
        const val LOG_DEBUG = 3
        const val LOG_INFO = 2
        const val LOG_ERROR = 1
    }
}
