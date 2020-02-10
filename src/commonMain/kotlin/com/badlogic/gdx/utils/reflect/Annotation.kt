package com.badlogic.gdx.utils.reflect

import java.lang.ClassNotFoundException
import java.lang.IllegalAccessException
import java.lang.InstantiationException
import java.lang.NoSuchFieldException
import java.lang.NoSuchMethodException
import java.lang.SecurityException
import java.lang.reflect.GenericArrayType
import java.lang.reflect.InvocationTargetException
import kotlin.jvm.Throws

/** Provides information about, and access to, an annotation of a field, class or interface.
 * @author dludwig
 */
class Annotation internal constructor(private val annotation: Annotation) {

    fun <T : Annotation?> getAnnotation(annotationType: java.lang.Class<T>): T? {
        return if (annotation.annotationType() == annotationType) {
            annotation as T
        } else null
    }

    val annotationType: java.lang.Class<out Annotation>
        get() = annotation.annotationType()
}
