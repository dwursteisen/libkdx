/**
 * Platform independent wrappers for file handles and file streaming.
 *
 * Use [com.badlogic.gdx.Gdx.files] to get a reference to the [com.badlogic.gdx.Files] implementation
 * to create and look up files.
 *
 */
package com.badlogic.gdx.files

import java.io.FileInputStream
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.io.BufferedReader
import java.io.IOException
import java.nio.channels.FileChannel.MapMode
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.ByteOrder
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.io.FilenameFilter
import java.lang.UnsupportedOperationException

// This is a doc-only file and has no actual content.
