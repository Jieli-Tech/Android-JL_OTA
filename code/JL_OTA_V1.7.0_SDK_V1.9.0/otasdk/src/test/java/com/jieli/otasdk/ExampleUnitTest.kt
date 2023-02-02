package com.jieli.otasdk

import com.jieli.jl_bt_ota.constant.JL_Constant
import com.jieli.jl_bt_ota.util.CHexConver
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.jl_bt_ota.util.ParseDataUtil
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun main() {
        System.out.printf("%d, %d", Math.round(1.5f), Math.round(-1.5f))
    }

    @Test
    fun testData() {
        JL_Log.setIsTest(true)
        val data =
            "15FFD60541544F4C4A018D0EF8815A0102008200500002010A0B095A5A435F57617463680000000000000000000000000000000000000000000000000000"
        val bleMessage = ParseDataUtil.parseOTAFlagFilterWithBroad(CHexConver.hexStr2Bytes(data), JL_Constant.OTA_IDENTIFY);
        println("ExampleUnitTest.testData : $bleMessage")
    }
}
