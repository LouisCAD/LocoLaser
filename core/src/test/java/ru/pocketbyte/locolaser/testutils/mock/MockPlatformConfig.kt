/*
 * Copyright © 2017 Denis Shurygin. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */

package ru.pocketbyte.locolaser.testutils.mock

import ru.pocketbyte.locolaser.config.platform.PlatformConfig

import java.io.File

/**
 * @author Denis Shurygin
 */
open class MockPlatformConfig : PlatformConfig {

    override val type = "mock"

    override val resources = MockPlatformResources(File("./"), "mock")

    override val defaultTempDir = File("./temp/mock/")
}