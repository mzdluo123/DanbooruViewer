package win.rainchan.win

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun splitTest() {
        val stream = SplitInputStream(100)
        assertEquals(0, stream.available())
        stream.putData(ByteArray(10) { 0 }, 1)
        assertEquals(0, stream.available())
        stream.putData(ByteArray(10) { 0 }, 30)
        assertEquals(0, stream.available())
        stream.putData(ByteArray(1) { 0 }, 0)
        assertEquals(11, stream.available())
    }
}