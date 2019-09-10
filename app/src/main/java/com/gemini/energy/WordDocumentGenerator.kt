package com.gemini.energy

import android.os.Environment
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.UploadErrorException
import com.dropbox.core.v2.files.WriteMode
import com.gemini.energy.presentation.audit.DropBox
import com.gemini.energy.service.device.EBase
import org.apache.poi.wp.usermodel.HeaderFooterType
import org.apache.poi.xwpf.usermodel.*
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.util.*


class WordDocumentGenerator {

    private lateinit var documents: List<XWPFDocument>

    private val greyColor = "595959"
    private val tableGreyColor = "D9D9D9"
    private val greenColor = "70ad47"
    private val tableGreenColor = "469B43"
    private val blackColor = "000000"
    private val whiteColor = "ffffff"

    private val fontAgencyFB = "Agency FB"
    private val fontUnivers = "Univers LT Std 39 Thin UltraCn"

    private val manuallyGeneratedValue = "INSERTME"


    /** Triggers the document generation for each audit and saves docuemnts to appropriate place **/
    fun triggerGeneration(ebases: MutableList<EBase>) {
        setupNeededForApacheToWork()
        // organization & aggregation
        val sorter = SorterForWordDocumentGenerator()

        sorter.prepareAllValues(values)
//        val sortedAudits = sorter.sortEbasesIntoAudits(values)


        Log.i("-----moo", "SORTED")

//        // generation
//        generateFirstPage()
//        generateHvac()
//
//        // lighting
//        if (true) {
//            generateLighting()
//        }
//
//        generateClosingPage()
    }


    // generation
    // TODO: implement generation once ApachePoi is integrated

    private fun generateFirstPage(hvac: HvacValues) {}

    private fun generateEnergySavingPotential() {}

    private fun generateHvacSavings(hvac: HvacValues) {}

    private fun generateLightingSavings(lights: LightingValues) {}

    private fun generateEquipmentSavings() {}

    private fun generateFacilityInformation(hvac: HvacValues) {}


    // utils
    // TODO: implement utils once ApachePoi is integrated

    //    private fun createBullets(items: Array<String>, size: Int, color: String, isBold: Boolean, isItalic: Boolean)
    private fun createBullets() {}

    //    private fun mergeCellHorizontally(table: XWPFTable, row: Int, fromCol: Int, toCol: Int)
    private fun mergeCellHorizontally() {}

    private fun addPageNumbers() {}

    //    private fun centerTable(table: XWPFTable)
    private fun centerTable() {}

    //    private fun fitTable(table: XWPFTable, width: Long)
    private fun fitTable() {}
}

