/*
 * Copyright © 2017 Denis Shurygin. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */

package ru.pocketbyte.locolaser.resource

import ru.pocketbyte.locolaser.config.WritingConfig
import ru.pocketbyte.locolaser.resource.entity.ResMap
import ru.pocketbyte.locolaser.resource.file.ResourceFile

import java.io.File
import java.io.IOException

/**
 * @author Denis Shurygin
 */
abstract class AbsPlatformResources(
        /** Resource directory path. */
        val directory: File,
        /** Resource name. */
        val name: String
) : PlatformResources {

    protected abstract fun getResourceFiles(locales: Set<String>): Array<ResourceFile>?

    override fun read(locales: Set<String>): ResMap {
        val resMap = ResMap()
        getResourceFiles(locales)?.forEach {
            resMap.merge(it.read())
        }
        return resMap
    }

    @Throws(IOException::class)
    override fun write(map: ResMap, writingConfig: WritingConfig?) {
        getResourceFiles(map.keys)?.forEach {
            it.write(map, writingConfig)
        }
    }
}
