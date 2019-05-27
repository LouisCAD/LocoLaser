/*
 * Copyright © 2017 Denis Shurygin. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */

package ru.pocketbyte.locolaser.platform.mobile.resource.file

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.pocketbyte.locolaser.config.WritingConfig
import ru.pocketbyte.locolaser.platform.mobile.utils.TemplateStr
import ru.pocketbyte.locolaser.resource.entity.*

import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

import org.junit.Assert.*
import ru.pocketbyte.locolaser.platform.mobile.resource.file.AbsIosStringsResourceFileTest.Companion.PLATFORM_TEST_STRING
import ru.pocketbyte.locolaser.platform.mobile.resource.file.AbsIosStringsResourceFileTest.Companion.TEST_STRING

/**
 * @author Denis Shurygin
 */
class IosResourceFileTest {

    @Rule @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    @Throws(IOException::class)
    fun testRead() {
        val testLocale = "ru"
        val testFile = prepareTestFile(
                "/* Comment */\r\n" +
                        " \"string1\" = \"Value1\";\r\n" +
                        "\"string2\"= \"Value2\";\r\n" +
                        "\r\n" +
                        "\"string3\" = \"Value 3\";")

        val resourceFile = IosResourceFile(testFile, testLocale)
        val resMap = resourceFile.read()

        assertNotNull(resMap)

        val expectedMap = ResMap()
        val resLocale = ResLocale()
        resLocale.put(prepareResItem("string1", arrayOf(ResValue("Value1", "Comment", Quantity.OTHER))))
        resLocale.put(prepareResItem("string2", arrayOf(ResValue("Value2", null, Quantity.OTHER))))
        resLocale.put(prepareResItem("string3", arrayOf(ResValue("Value 3", null, Quantity.OTHER))))
        expectedMap[testLocale] = resLocale

        assertEquals(expectedMap, resMap)
    }

    @Test
    @Throws(IOException::class)
    fun testWrite() {
        val testLocale = "ru"
        val redundantLocale = "base"

        val resMap = ResMap()

        var resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_1", "Comment", Quantity.OTHER))))
        resLocale.put(prepareResItem("key2", arrayOf(ResValue("value2_1", "value2_1", Quantity.OTHER))))
        resMap[testLocale] = resLocale

        // Redundant locale. Shouldn't be written into file.
        resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_2", null, Quantity.OTHER))))
        resLocale.put(prepareResItem("key3", arrayOf(ResValue("value3_2", "value2_1", Quantity.OTHER))))
        resMap[redundantLocale] = resLocale

        val testFile = tempFolder.newFile()
        val resourceFile = IosResourceFile(testFile, testLocale)
        resourceFile.write(resMap, null)

        val expectedResult = (TemplateStr.GENERATED_KEY_VALUE_PAIR_COMMENT + "\r\n\r\n" +
                "/* Comment */\r\n" +
                "\"key1\" = \"value1_1\";\r\n" +
                "\r\n" +
                "/* value2_1 */\r\n" +
                "\"key2\" = \"value2_1\";")

        assertEquals(expectedResult, readFile(testFile))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteWithWritingConfig() {
        val testLocale = "ru"

        val resMap = ResMap()

        val resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_1", "Comment", Quantity.OTHER))))
        resLocale.put(prepareResItem("key2", arrayOf(ResValue("value2_1", "value2_1", Quantity.OTHER))))
        resMap[testLocale] = resLocale

        val writingConfig = WritingConfig()
        writingConfig.isDuplicateComments = false

        val testFile = tempFolder.newFile()
        val resourceFile = IosResourceFile(testFile, testLocale)
        resourceFile.write(resMap, writingConfig)

        val expectedResult = (TemplateStr.GENERATED_KEY_VALUE_PAIR_COMMENT + "\r\n\r\n" +
                "/* Comment */\r\n" +
                "\"key1\" = \"value1_1\";\r\n" +
                "\r\n" +
                "\"key2\" = \"value2_1\";")

        assertEquals(expectedResult, readFile(testFile))
    }

    @Test
    @Throws(IOException::class)
    fun testValueCorrectionWhenRead() {
        val testLocale = "ru"
        val testFile = prepareTestFile(
                (TemplateStr.GENERATED_KEY_VALUE_PAIR_COMMENT + "\r\n\r\n" +
                        "\"string1\" = \"" + PLATFORM_TEST_STRING + "\";"))

        val resourceFile = IosResourceFile(testFile, testLocale)
        val resMap = resourceFile.read()

        assertNotNull(resMap)

        val expectedMap = ResMap()
        val resLocale = ResLocale()
        resLocale.put(prepareResItem("string1", arrayOf(ResValue(TEST_STRING, null, Quantity.OTHER))))
        expectedMap[testLocale] = resLocale

        assertEquals(expectedMap, resMap)
    }

    @Test
    @Throws(IOException::class)
    fun testValueCorrectionWhenWrite() {
        val testLocale = "ru"

        val resMap = ResMap()

        val resLocale = ResLocale()
        resLocale.put(prepareResItem("string1", arrayOf(ResValue(TEST_STRING, null, Quantity.OTHER))))
        resMap[testLocale] = resLocale

        val testFile = tempFolder.newFile()
        val resourceFile = IosResourceFile(testFile, testLocale)
        resourceFile.write(resMap, null)

        val expectedResult = (TemplateStr.GENERATED_KEY_VALUE_PAIR_COMMENT + "\r\n\r\n" +
                "\"string1\" = \"" + PLATFORM_TEST_STRING + "\";")

        assertEquals(expectedResult, readFile(testFile))
    }

    @Throws(IOException::class)
    private fun prepareTestFile(text: String): File {
        val file = tempFolder.newFile()
        val writer = PrintWriter(file)
        writer.write(text)
        writer.flush()
        writer.close()
        return file
    }

    @Throws(IOException::class)
    private fun readFile(file: File): String {
        return String(Files.readAllBytes(Paths.get(file.absolutePath)), Charset.defaultCharset())
    }

    private fun prepareResItem(key: String, values: Array<ResValue>): ResItem {
        val resItem = ResItem(key)
        for (value in values)
            resItem.addValue(value)
        return resItem
    }
}