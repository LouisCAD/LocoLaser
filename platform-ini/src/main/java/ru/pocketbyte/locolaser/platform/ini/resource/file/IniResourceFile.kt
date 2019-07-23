package ru.pocketbyte.locolaser.platform.ini.resource.file

import ru.pocketbyte.locolaser.config.WritingConfig
import ru.pocketbyte.locolaser.resource.entity.*
import ru.pocketbyte.locolaser.resource.file.ResourceStreamFile
import ru.pocketbyte.locolaser.utils.PluralUtils
import java.io.File
import java.io.IOException
import java.io.LineNumberReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

class IniResourceFile(file:File, private val mLocales:Set<String>):ResourceStreamFile(file) {

    companion object {

        var GENERATED_COMMENT = "# AUTO-GENERATED FILE. DO NOT MODIFY.\r\n" +
                "#\r\n" +
                "# This file was automatically generated by the LocoLaser tool.\r\n" +
                "# It should not be modified by hand.\r\n\r\n"

        private const val COMMENT_SINGLE_LINE = "#"
        private const val LOCALE_PATTERN = "^\\s*\\[(.+)\\]\\s*$"
        private const val KEY_VALUE_PATTERN = "((?:[^\"]|\\\\\")+) = ((?:[^\"]|\\\\\")*)"
        private const val PLURAL_KEY_PATTERN = "((?:[^\"]|\\\\\")+) = \\{\\[ plural\\(n\\) \\]\\}"
        private const val PLURAL_VALUE_PATTERN = "((?:[^\"]|\\\\\")+)\\[([a-z]*)\\]\\s*= ((?:[^\"]|\\\\\")*)"

        internal fun toPlatformValue(string:String):String {
            return string
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
        }

        internal fun fromPlatformValue(string:String):String {
            return string
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
        }
    }

    override fun read():ResMap? {
        if (file.exists())
        {
            val resMap = ResMap()

            val localeMatcher = Pattern.compile(LOCALE_PATTERN).matcher("")
            val pluralKeyMatcher = Pattern.compile(PLURAL_KEY_PATTERN).matcher("")
            val pluralValueMatcher = Pattern.compile(PLURAL_VALUE_PATTERN).matcher("")
            val keyValueMatcher = Pattern.compile(KEY_VALUE_PATTERN).matcher("")

            val path = Paths.get(file.toURI())
            try
            {
                Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader-> LineNumberReader(reader).use { lineReader->

                    var currentLocale:String? = null
                    var currentLocaleMap:ResLocale? = null
                    var line:String?
                    var pluralItem:ResItem? = null
                    var comment:StringBuilder? = null

                    do {
                        line = lineReader.readLine()

                        if (line != null) {
                            localeMatcher.reset(line)
                            if (localeMatcher.find() && localeMatcher.groupCount() == 1) {
                                currentLocale = localeMatcher.group(1)

                                if (!mLocales.contains(currentLocale)) {
                                    currentLocale = null
                                    continue
                                }

                                currentLocaleMap = resMap.get(currentLocale)
                                if (currentLocaleMap == null) {
                                    currentLocaleMap = ResLocale()
                                    resMap.put(currentLocale, currentLocaleMap)
                                }

                                continue
                            }
                        }

                        // No locale found. Keep looking for locale section
                        if (currentLocale == null || currentLocaleMap == null)
                            continue

                        if (line != null && line.startsWith(COMMENT_SINGLE_LINE)) {
                            val commentString = line.substring(COMMENT_SINGLE_LINE.length).trim { it <= ' ' }
                            if (comment == null)
                                comment = StringBuilder(commentString)
                            else
                                comment.append("\n").append(commentString)
                            continue
                        }

                        if (pluralItem != null) {
                            if (line != null) {
                                pluralValueMatcher.reset(line)
                                if (pluralValueMatcher.find() && pluralValueMatcher.groupCount() == 3) {
                                    val key = pluralValueMatcher.group(1)
                                    val quantity = PluralUtils.quantityFromString(pluralValueMatcher.group(2))
                                    val value = pluralValueMatcher.group(3)
                                    if (key == pluralItem.key && quantity != null && value != null) {
                                        val commentString = comment?.toString()
                                        pluralItem.addValue(ResValue(
                                                fromPlatformValue(value), commentString, quantity))
                                    }
                                    comment = null
                                    continue
                                }
                            }

                            if (pluralItem.values.isNotEmpty()) {
                                currentLocaleMap.put(pluralItem)
                            }
                            pluralItem = null
                        }


                        if (line != null) {
                            pluralKeyMatcher.reset(line)
                            if (pluralKeyMatcher.find() && pluralKeyMatcher.groupCount() == 1) {
                                pluralItem = ResItem(pluralKeyMatcher.group(1))
                                comment = null
                                continue
                            }

                            keyValueMatcher.reset(line)
                            if (keyValueMatcher.find() && keyValueMatcher.groupCount() == 2) {
                                val commentString = comment?.toString()
                                val item = ResItem(keyValueMatcher.group(1))
                                item.addValue(ResValue(fromPlatformValue(keyValueMatcher.group(2)), commentString))
                                currentLocaleMap.put(item)
                                comment = null
                                continue
                            }
                        }

                        // Nothing found
                        comment = null
                    } while (line != null) }
                }
            }
            catch (e:IOException) {
                // Do nothing
                e.printStackTrace()
            }

            return resMap
        }
        return null
    }



    @Throws(IOException::class)
    override fun write(resMap:ResMap, writingConfig:WritingConfig?) {
        open()

        writeStringLn(GENERATED_COMMENT)
        writeln()

        for (locale in mLocales)
        {

            writeString("[")
            writeString(locale)
            writeStringLn("]")

            val items = resMap.get(locale) ?: continue

            for (key in items.keys)
            {
                val resItem = items[key]
                if (resItem != null)
                {
                    if (resItem.isHasQuantities)
                    {
                        writeString(resItem.key)
                        writeStringLn(" = {[ plural(n) ]}")

                        for (value in resItem.values)
                        {
                            if (isCommentShouldBeWritten(value, writingConfig))
                            {
                                writeString(COMMENT_SINGLE_LINE)
                                writeString(" ")
                                writeStringLn(value.comment!!.replace("\n", "\n$COMMENT_SINGLE_LINE "))
                            }
                            writeString(resItem.key)
                            writeString("[")
                            writeString(value.quantity.toString())
                            writeString("] = ")
                            writeStringLn(toPlatformValue(value.value))
                        }
                    }
                    else
                    {
                        val value = resItem.valueForQuantity(Quantity.OTHER)
                        if (value != null)
                        {
                            if (isCommentShouldBeWritten(value, writingConfig))
                            {
                                writeString(COMMENT_SINGLE_LINE)
                                writeString(" ")
                                writeStringLn(value.comment!!.replace("\n", "\n$COMMENT_SINGLE_LINE "))
                            }
                            writeString(resItem.key)
                            writeString(" = ")
                            writeStringLn(toPlatformValue(value.value))
                        }
                    }
                }
            }
        }

        close()
    }

    private fun isCommentShouldBeWritten(value:ResValue, writingConfig:WritingConfig?):Boolean {
        return value.comment != null && (writingConfig == null || writingConfig.isDuplicateComments || value.comment != value.value)
    }
}
