package com.badlogic.gdx.assets.loaders.resolvers

class AbsoluteFileHandleResolver : com.badlogic.gdx.assets.loaders.FileHandleResolver {
    override fun resolve(fileName: String): com.badlogic.gdx.files.FileHandle? {
        return com.badlogic.gdx.Gdx.files.absolute(fileName)
    }
}
