package ru.pocketbyte.locolaser.platform.kotlinmobile.resource

import java.io.File

abstract class AbsKotlinImplementationPlatformResources(
        dir: File,
        name: String,
        interfaceName: String?
) : AbsKotlinPlatformResources(dir, name) {

    val interfaceName: String?
    val interfacePackage: String?

    init {

        if (interfaceName != null) {
            val interfaceNameParts = interfaceName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (interfaceNameParts.size < 2)
                throw IllegalArgumentException("Invalid interface name")

            this.interfaceName = interfaceNameParts[interfaceNameParts.size - 1]
            this.interfacePackage = interfaceName.substring(0, interfaceName.length - this.interfaceName.length - 1)
        } else {
            this.interfaceName = null
            this.interfacePackage = null
        }
    }
}