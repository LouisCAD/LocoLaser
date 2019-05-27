/*
 * Copyright © 2017 Denis Shurygin. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */

package ru.pocketbyte.locolaser.resource.entity

import ru.pocketbyte.locolaser.utils.LogUtils
import ru.pocketbyte.locolaser.utils.TextUtils

/**
 * @author Denis Shurygin
 */
enum class Quantity {

    ZERO, ONE, TWO, FEW, MANY, OTHER;

    companion object {
        val QUANTITY_OTHER: Set<Quantity> = setOf(OTHER)

        @JvmOverloads
        fun fromString(quantity: String?, fallback: Quantity? = OTHER): Quantity? {
            if (quantity == null)
                return fallback

            return if (quantity.isNotEmpty()) {
                when (quantity.trim { it <= ' ' }) {
                    "zero" -> ZERO
                    "one" -> ONE
                    "two" -> TWO
                    "few" -> FEW
                    "many" -> MANY
                    "other" -> OTHER
                    else -> {
                        LogUtils.err("Quantity.fromString: Unknown quantity '$quantity'.")
                        fallback
                    }
                }
            } else {
                fallback
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            ZERO -> "zero"
            ONE -> "one"
            TWO -> "two"
            FEW -> "few"
            MANY -> "many"
            else -> "other"
        }
    }
}