/*
 * Copyright © 2017 Denis Shurygin. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */

package ru.pocketbyte.locolaser.resource

import ru.pocketbyte.locolaser.config.WritingConfig
import ru.pocketbyte.locolaser.resource.entity.ResMap
import ru.pocketbyte.locolaser.summary.FileSummary

import java.io.IOException

/**
 * Represent resources for specified platform.
 *
 * @author Denis Shurygin
 */
interface PlatformResources {

    companion object {
        val BASE_LOCALE = "base"
    }

    /**
     * Read resources map from the resource files. Keys from the result map duplicate resource locale's.
     * @return Resources map. Keys from the map duplicate resource locale's.
     */
    fun read(locales: Set<String>): ResMap

    @Throws(IOException::class)
    fun write(map: ResMap, writingConfig: WritingConfig?)

    fun summaryForLocale(locale: String): FileSummary
}