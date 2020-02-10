package com.badlogic.gdx.assets.loaders.resolvers

import java.util.Locale

class LocalFileHandleResolver : com.badlogic.gdx.assets.loaders.FileHandleResolver {
    override fun resolve(fileName: String?): com.badlogic.gdx.files.FileHandle? {
        return com.badlogic.gdx.Gdx.files.local(fileName)
    }
}
