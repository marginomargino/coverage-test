package io.test

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExampleTest {

    @Test
    fun exampleTest() {
        assertTrue { example(true) }
        assertFalse { example(false) }
    }

}