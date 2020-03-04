package com.gemini.energy

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.gemini.energy.presentation.util.ERateKey
import com.gemini.energy.service.type.Electricity
import com.gemini.energy.service.type.UtilityRate
import com.gemini.energy.service.type.Gas
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilityRateTest {

    @Test
    fun readUtilityDataElectric() {
        val context = InstrumentationRegistry.getTargetContext()
        val utility = UtilityRate(context)
                .initUtility(Electricity("A-1 TOU", "pge"))
                .build()

        val structure = utility.structure
        Log.d(this.javaClass.simpleName, structure.toString())

        assertEquals(5, structure.count())
        assertEquals(listOf("0.19956", "0.23020", "0"), structure["winter-off-peak"])
        assertEquals(listOf("0.22047", "0.23020", "0"), structure["winter-part-peak"])
        assertEquals(listOf("0.21197", "0.23020", "0"), structure["summer-off-peak"])
        assertEquals(listOf("0.23933", "0.23020", "0"), structure["summer-part-peak"])
        assertEquals(listOf("0.26298", "0.23020", "0"), structure["summer-on-peak"])
    }

    @Test
    fun readUtilityDataGas() {
        val context = InstrumentationRegistry.getTargetContext()
        val utility = UtilityRate(context)
                .initUtility(Gas())
                .build()

        val structure = utility.structure

        assertEquals(8, structure.count())
        assertEquals(listOf("0.27048"), structure[ERateKey.Slab1.value])
        assertEquals(listOf("0.52106"), structure[ERateKey.Slab2.value])
        assertEquals(listOf("0.95482"), structure[ERateKey.Slab3.value])
        assertEquals(listOf("1.66489"), structure[ERateKey.Slab4.value])
        assertEquals(listOf("2.14936"), structure[ERateKey.Slab5.value])
        assertEquals(listOf("0.91132"), structure[ERateKey.SummerTransport.value])
        assertEquals(listOf("1.0215"), structure[ERateKey.WinterTransport.value])
        assertEquals(listOf("0.04672"), structure[ERateKey.Surcharge.value])

    }

}