package ru.pocketbyte.locolaser.platform.kotlinmpp.resource.file

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.pocketbyte.locolaser.resource.PlatformResources
import ru.pocketbyte.locolaser.resource.entity.*

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import ru.pocketbyte.locolaser.config.ExtraParams
import ru.pocketbyte.locolaser.platform.kotlinmpp.resource.AbsKotlinPlatformResources
import ru.pocketbyte.locolaser.platform.kotlinmpp.utils.TemplateStr

class KotlinAndroidResourceFileTest {

    companion object {
        const val StringProviderStr =
            "    interface StringProvider {\n" +
            "        fun getString(key: String, vararg args: Any): String\n" +
            "\n" +
            "        fun getPluralString(\n" +
            "                key: String,\n" +
            "                count: Int,\n" +
            "                vararg args: Any\n" +
            "        ): String\n" +
            "    }\n" +
            "\n" +
            "    private class StringProviderImpl(private val context: Context) : StringProvider {\n" +
            "        private val resIds: MutableMap<String, MutableMap<String, Int>> =\n" +
            "                mutableMapOf<String, MutableMap<String, Int>>()\n" +
            "\n" +
            "        override fun getString(key: String, vararg args: Any): String = this.context.getString(getId(key, \"string\"), *args)\n" +
            "\n" +
            "        override fun getPluralString(\n" +
            "                key: String,\n" +
            "                count: Int,\n" +
            "                vararg args: Any\n" +
            "        ): String = this.context.resources.getQuantityString(getId(key, \"plurals\"), count, *args)\n" +
            "\n" +
            "        private fun getId(resName: String, defType: String): Int {\n" +
            "            var resMap = resIds[defType]\n" +
            "            if (resMap == null) {\n" +
            "                resMap = mutableMapOf()\n" +
            "                resIds[defType] = resMap\n" +
            "            }\n" +
            "            var resId = resMap[resName]\n" +
            "            if (resId == null) {\n" +
            "                resId = context.resources.getIdentifier(\n" +
            "                    resName.trim { it <= ' ' }, defType, context.packageName\n" +
            "                )\n" +
            "                resMap[resName] = resId\n" +
            "            }\n" +
            "            return resId\n" +
            "        }\n" +
            "    }\n"
        const val SecondConstructorsStr =
            "    constructor(context: Context) : this(StringProviderImpl(context))\n"
    }

    @Rule @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    @Throws(IOException::class)
    fun testRead() {
        val resourceFile = KotlinAndroidResourceFile(tempFolder.newFile(),
                "Str", "com.package", null, null)
        assertNull(resourceFile.read(ExtraParams()))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteOneItem() {
        val resMap = ResMap()
        val resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_1", "Comment", Quantity.OTHER))))
        resMap[PlatformResources.BASE_LOCALE] = resLocale

        val testDirectory = tempFolder.newFolder()
        val className = "Str"
        val classPackage = "com.pcg"
        val resourceFile = KotlinAndroidResourceFile(testDirectory, className, classPackage, null, null)
        resourceFile.write(resMap, null)

        val expectedResult = TemplateStr.GENERATED_CLASS_COMMENT + "\n" +
                "package $classPackage\n" +
                "\n" +
                "import android.content.Context\n" +
                "import kotlin.Any\n" +
                "import kotlin.Int\n" +
                "import kotlin.String\n" +
                "import kotlin.collections.MutableMap\n" +
                "\n" +
                "class $className(private val stringProvider: StringProvider) {\n" +
                "    /**\n" +
                "     * value1_1 */\n" +
                "    val key1: String\n" +
                "        get() = this.stringProvider.getString(\"key1\")\n" +
                "\n" +
                SecondConstructorsStr +
                "\n" +
                StringProviderStr +
                "}\n"

        assertEquals(expectedResult, readFile(fileForClass(testDirectory, className, classPackage)))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteOnePluralItem() {
        val resMap = ResMap()
        val resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_1", "Comment 1", Quantity.ONE), ResValue("value1_2", "Comment 2", Quantity.OTHER))))
        resMap[PlatformResources.BASE_LOCALE] = resLocale

        val testDirectory = tempFolder.newFolder()
        val className = "Str"
        val classPackage = "com.pcg"
        val resourceFile = KotlinAndroidResourceFile(testDirectory, className, classPackage, null, null)
        resourceFile.write(resMap, null)

        val expectedResult = TemplateStr.GENERATED_CLASS_COMMENT + "\n" +
                "package $classPackage\n" +
                "\n" +
                "import android.content.Context\n" +
                "import kotlin.Any\n" +
                "import kotlin.Int\n" +
                "import kotlin.String\n" +
                "import kotlin.collections.MutableMap\n" +
                "\n" +
                "class $className(private val stringProvider: StringProvider) {\n" +
                SecondConstructorsStr +
                "\n" +
                "    /**\n" +
                "     * value1_2 */\n" +
                "    fun key1(count: Int): String = this.stringProvider.getPluralString(\"key1\", count)\n" +
                "\n" +
                StringProviderStr +
                "}\n"

        assertEquals(expectedResult, readFile(fileForClass(testDirectory, className, classPackage)))
    }

    @Test
    @Throws(IOException::class)
    fun testWrite() {
        val resMap = ResMap()

        var resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_1", "Comment", Quantity.OTHER))))
        resLocale.put(prepareResItem("key2", arrayOf(ResValue("value2_1", "value2_1", Quantity.OTHER))))
        resMap["ru"] = resLocale

        resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_2", null, Quantity.OTHER))))
        resLocale.put(prepareResItem("key3", arrayOf(ResValue("value3_2", "value2_1", Quantity.OTHER))))
        resMap[PlatformResources.BASE_LOCALE] = resLocale

        val testDirectory = tempFolder.newFolder()
        val className = "StrImpl"
        val classPackage = "com.some.pcg"
        val resourceFile = KotlinAndroidResourceFile(testDirectory, className, classPackage, null, null)
        resourceFile.write(resMap, null)

        val expectedResult = TemplateStr.GENERATED_CLASS_COMMENT + "\n" +
                "package $classPackage\n" +
                "\n" +
                "import android.content.Context\n" +
                "import kotlin.Any\n" +
                "import kotlin.Int\n" +
                "import kotlin.String\n" +
                "import kotlin.collections.MutableMap\n" +
                "\n" +
                "class $className(private val stringProvider: StringProvider) {\n" +
                "    /**\n" +
                "     * value1_2 */\n" +
                "    val key1: String\n" +
                "        get() = this.stringProvider.getString(\"key1\")\n" +
                "\n" +
                "    /**\n" +
                "     * value3_2 */\n" +
                "    val key3: String\n" +
                "        get() = this.stringProvider.getString(\"key3\")\n" +
                "\n" +
                SecondConstructorsStr +
                "\n" +
                StringProviderStr +
                "}\n"

        assertEquals(expectedResult, readFile(fileForClass(testDirectory, className, classPackage)))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteWithInterface() {
        val resMap = ResMap()

        var resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_1", "Comment", Quantity.OTHER))))
        resLocale.put(prepareResItem("key2", arrayOf(ResValue("value2_1", "value2_1", Quantity.OTHER))))
        resMap["ru"] = resLocale

        resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue("value1_2", null, Quantity.OTHER))))
        resLocale.put(prepareResItem("key3", arrayOf(ResValue("value3_2", "value2_1", Quantity.OTHER))))
        resMap[PlatformResources.BASE_LOCALE] = resLocale

        val testDirectory = tempFolder.newFolder()
        val className = "StrImpl"
        val classPackage = "com.some.pcg"
        val resourceFile = KotlinAndroidResourceFile(testDirectory, className, classPackage, "StrInterface", "com.some.package")
        resourceFile.write(resMap, null)

        val expectedResult = TemplateStr.GENERATED_CLASS_COMMENT + "\n" +
                "package $classPackage\n" +
                "\n" +
                "import android.content.Context\n" +
                "import com.some.package.StrInterface\n" +
                "import kotlin.Any\n" +
                "import kotlin.Int\n" +
                "import kotlin.String\n" +
                "import kotlin.collections.MutableMap\n" +
                "\n" +
                "class $className(private val stringProvider: StringProvider) : StrInterface {\n" +
                "    override val key1: String\n" +
                "        get() = this.stringProvider.getString(\"key1\")\n" +
                "\n" +
                "    override val key3: String\n" +
                "        get() = this.stringProvider.getString(\"key3\")\n" +
                "\n" +
                SecondConstructorsStr +
                "\n" +
                StringProviderStr +
                "}\n"

        assertEquals(expectedResult, readFile(fileForClass(testDirectory, className, classPackage)))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteLongPropertyComment() {
        val resMap = ResMap()
        val resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue(
                "Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery " + "Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Long Comment", null,
                Quantity.OTHER))))
        resMap[PlatformResources.BASE_LOCALE] = resLocale

        val testDirectory = tempFolder.newFolder()
        val className = "Strings"
        val classPackage = "ru.pocketbyte"
        val resourceFile = KotlinAndroidResourceFile(testDirectory, className, classPackage, null, null)

        resourceFile.write(resMap, null)

        val expectedResult = TemplateStr.GENERATED_CLASS_COMMENT + "\n" +
                "package $classPackage\n" +
                "\n" +
                "import android.content.Context\n" +
                "import kotlin.Any\n" +
                "import kotlin.Int\n" +
                "import kotlin.String\n" +
                "import kotlin.collections.MutableMap\n" +
                "\n" +
                "class $className(private val stringProvider: StringProvider) {\n" +
                "    /**\n" +
                "     * Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery" +
                " Wery Wery Wery Wery Wery Wery\n" +
                "     * Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Wery Long Comment */\n" +
                "    val key1: String\n" +
                "        get() = this.stringProvider.getString(\"key1\")\n" +
                "\n" +
                SecondConstructorsStr +
                "\n" +
                StringProviderStr +
                "}\n"

        assertEquals(expectedResult, readFile(fileForClass(testDirectory, className, classPackage)))
    }

    @Test
    @Throws(IOException::class)
    fun testWriteFormattedComment() {
        val testValue = "Hello %s %s"
        val resMap = ResMap()
        val resLocale = ResLocale()
        resLocale.put(prepareResItem("key1", arrayOf(ResValue(testValue, "", Quantity.OTHER))))
        resLocale.put(prepareResItem("key2", arrayOf(ResValue(testValue, "Comment 2", Quantity.OTHER), ResValue("value1_1", "Comment 1", Quantity.ONE))))
        resMap[PlatformResources.BASE_LOCALE] = resLocale

        val testDirectory = tempFolder.newFolder()
        val className = "Str"
        val classPackage = "com.pcg"
        val resourceFile = KotlinAndroidResourceFile(testDirectory, className, classPackage, null, null)
        resourceFile.write(resMap, null)

        val expectedResult = TemplateStr.GENERATED_CLASS_COMMENT + "\n" +
                "package $classPackage\n" +
                "\n" +
                "import android.content.Context\n" +
                "import kotlin.Any\n" +
                "import kotlin.Int\n" +
                "import kotlin.String\n" +
                "import kotlin.collections.MutableMap\n" +
                "\n" +
                "class $className(private val stringProvider: StringProvider) {\n" +
                "    /**\n" +
                "     * $testValue */\n" +
                "    val key1: String\n" +
                "        get() = this.stringProvider.getString(\"key1\")\n" +
                "\n" +
                SecondConstructorsStr +
                "\n" +
                "    /**\n" +
                "     * $testValue */\n" +
                "    fun key2(count: Int): String = this.stringProvider.getPluralString(\"key2\", count)\n" +
                "\n" +
                StringProviderStr +
                "}\n"

        assertEquals(expectedResult, readFile(fileForClass(testDirectory, className, classPackage)))
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

    private fun fileForClass(directory: File, className: String, classPackage: String): File {
        return File(
            File(directory, classPackage.replace(".", "/")),
            "$className${AbsKotlinPlatformResources.KOTLIN_FILE_EXTENSION}"
        )
    }
}
