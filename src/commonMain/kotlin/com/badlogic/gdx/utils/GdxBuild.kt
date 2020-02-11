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
package com.badlogic.gdx.utils

import kotlin.jvm.Throws

/**
 * Builds the JNI wrappers via gdx-jnigen.
 *
 * @author mzechner
 */
object GdxBuild {

    @Throws(java.lang.Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val JNI_DIR = "jni"
        val LIBS_DIR = "libs"

        // generate C/C++ code
        NativeCodeGenerator().generate("src", "bin", JNI_DIR, arrayOf("**/*"), null)
        val excludeCpp = arrayOf("iosgl/**")

        // generate build scripts, for win32 only
        // custom target for testing purposes
        val win32home: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.Windows, false)
        win32home.compilerPrefix = ""
        win32home.buildFileName = "build-windows32home.xml"
        win32home.excludeFromMasterBuildFile = true
        win32home.cppExcludes = excludeCpp
        val win32: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.Windows, false)
        win32.cppExcludes = excludeCpp
        val win64: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.Windows, true)
        win64.cppExcludes = excludeCpp
        val lin32: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.Linux, false)
        lin32.cppExcludes = excludeCpp
        val lin64: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.Linux, true)
        lin64.cppExcludes = excludeCpp
        val android: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.Android, false)
        android.linkerFlags += " -llog"
        android.cppExcludes = excludeCpp
        val mac64: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.MacOsX, true)
        mac64.cppExcludes = excludeCpp
        val ios: BuildTarget = BuildTarget.newDefaultTarget(TargetOs.IOS, false)
        ios.headerDirs = arrayOf("iosgl")
        AntScriptGenerator().generate(BuildConfig("gdx", "../target/native", LIBS_DIR, JNI_DIR), mac64, win32home, win32,
            win64, lin32, lin64, android, ios)

        // build natives
        // BuildExecutor.executeAnt("jni/build-windows32home.xml", "-v");
        // BuildExecutor.executeAnt("jni/build.xml", "pack-natives -v");
    }
}
