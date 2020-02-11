package com.badlogic.gdx.assets.loaders.resolvers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

class AbsoluteFileHandleResolver : com.badlogic.gdx.assets.loaders.FileHandleResolver {
    override fun resolve(fileName: String): FileHandle {
        return Gdx.files.absolute(fileName)
    }
}
