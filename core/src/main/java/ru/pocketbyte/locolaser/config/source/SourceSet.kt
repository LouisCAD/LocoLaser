/*
 * Copyright © 2017 Denis Shurygin. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */

package ru.pocketbyte.locolaser.config.source

import ru.pocketbyte.locolaser.resource.entity.ResMap

import java.util.ArrayList

/**
 * @author Denis Shurygin
 */
class SourceSet(
        sourceConfig: SourceConfig,
        private val mSources: Set<Source>,
        private val mDeafaultSource: Source
) : Source(sourceConfig) {

    override val modifiedDate: Long
        get() = mSources
                .map { it.modifiedDate }
                .max() ?: 0

    override fun read(): Source.ReadResult {
        var missedValues: MutableList<Source.MissedValue>? = null
        var resMap: ResMap? = null
        for (source in mSources) {
            val result = source.read()
            if (result.missedValues != null) {
                if (missedValues == null)
                    missedValues = ArrayList()
                missedValues.addAll(result.missedValues)
            }
            resMap = if (resMap != null)
                resMap.merge(result.items)
            else
                ResMap(result.items)
        }
        return Source.ReadResult(resMap, missedValues)
    }

    override fun write(resMap: ResMap) {
        mDeafaultSource.write(resMap)
    }

    override fun close() {
        for (source in mSources)
            source.close()
    }
}