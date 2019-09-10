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
        val values = SorterForWordDocumentGenerator().prepareAllValues(ebases)

        val allDocuments = mutableListOf<XWPFDocument>()
        for (value in values) {
            val document = XWPFDocument()
            generateDocument(value, document)
            allDocuments.add(document)
        }

        documents = allDocuments.toList()
    }

    // generation
    private fun generateDocument(value: PreparedForDocument, document: XWPFDocument): XWPFDocument {
        generateFirstPage(document, value.hvac)

        generateEnergySavingPotentialPage(
                document,
                value.lighting
                        ?: LightingValues(0.0, 0, 0.0, 0.0, 0.0, 0.0, listOf()),
                value.building)

        if (value.lighting != null) {
            generateLightingSavingsPage(document, value.lighting)
        }

        generateHvacSavingsPage(document, value.hvac, value.building)

        if (value.equipment != null && value.equipment.instances.any()) {
            generateEquipmentSavingsPage(document, value.equipment)
        }

        generateFacilityInformationPage(document, value, value.hvac)

        addPageNumbers(document)

        writeDocument(value, document)

        return document
    }


    // generation
    // TODO: implement generation once ApachePoi is integrated

    private fun generateFirstPage(hvac: HvacValues) {}

    private fun generateEnergySavingPotential() {}

    private fun generateHvacSavings(hvac: HvacValues) {}

    private fun generateLightingSavings(lights: LightingValues) {}

    private fun generateEquipmentSavings() {}

    // table utils
    private fun createBullets(document: XWPFDocument, items: Array<String>, size: Int, color: String, isBold: Boolean, isItalic: Boolean) {
        val cTAbstractNum = CTAbstractNum.Factory.newInstance()
        cTAbstractNum.abstractNumId = BigInteger.valueOf(0)
        val cTLvl = cTAbstractNum.addNewLvl()
        cTLvl.addNewLvlText().setVal("•")

        val abstractNum = XWPFAbstractNum(cTAbstractNum)
        val numbering = document.createNumbering()
        val abstractNumID = numbering.addAbstractNum(abstractNum)
        val numID = numbering.addNum(abstractNumID)

        for (item in items) {
            val bulletedPara = document.createParagraph()
            bulletedPara.numID = numID
            bulletedPara.spacingBetween = 1.5
            val run = bulletedPara.createRun()
            run.fontFamily = fontAgencyFB
            run.fontSize = size
            run.color = color
            if (isBold) {
                run.isBold = true
            }
            if (isItalic) {
                run.isItalic = true
            }
            run.setText(item)
        }
    }

    private fun addPageNumbers(document: XWPFDocument) {
        val footer = document.createFooter(HeaderFooterType.DEFAULT);

        var paragraph = footer.getParagraphArray(0)
        if (paragraph == null) paragraph = footer.createParagraph()
        paragraph.alignment = ParagraphAlignment.RIGHT

        val run = paragraph.createRun()
        run.setText("Page ")
        paragraph.ctp.addNewFldSimple().instr = "PAGE |\\* ARABIC MERGEFORMAT"
    }

    private fun mergeCellHorizontally(table: XWPFTable, row: Int, fromCol: Int, toCol: Int) {
        for (colIndex in fromCol..toCol) {
            val cell = table.getRow(row).getCell(colIndex)
            val hmerge = CTHMerge.Factory.newInstance()
            if (colIndex == fromCol) {
                // The first merged cell is set with RESTART merge value
                hmerge.setVal(STMerge.RESTART)
            } else {
                // Cells which join (merge) the first one, are set with CONTINUE
                hmerge.setVal(STMerge.CONTINUE)
                // and the content should be removed
                for (i in cell.paragraphs.size downTo 1) {
                    cell.removeParagraph(0)
                }
                cell.addParagraph()
            }
            // Try getting the TcPr. Not simply setting an new one every time.
            var tcPr: CTTcPr? = cell.ctTc.tcPr
            if (tcPr === null) {
                // only set an new TcPr if there is not one already
                tcPr = CTTcPr.Factory.newInstance()
                tcPr!!.hMerge = hmerge
                cell.ctTc.tcPr = tcPr
            } else {
                tcPr.hMerge = hmerge
            }
        }
    }

    private fun centerTable(table: XWPFTable) {
        val t = table.ctTbl
        val pr = t.tblPr
        val jc = pr.addNewJc()
        jc.setVal(STJc.CENTER)
        pr.jc = jc
    }

    private fun fitTable(table: XWPFTable, width: Long) {
        val ctTbl = table.ctTbl
        val properties = ctTbl.tblPr
        val tblW = properties.tblW
        tblW.type = STTblWidth.PCT
        tblW.w = BigInteger.valueOf(width)
        properties.tblW = tblW
        ctTbl.tblPr = properties
    }

    // ApachePOI setup
    private fun setupNeededForApacheToWork() {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
    }

    // document writing
    /** writes document to dropbox and local **/
    private fun writeDocument(value: PreparedForDocument, document: XWPFDocument) {
        val calendar = Calendar.getInstance()
        val dateTime = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}T${calendar.get(Calendar.HOUR)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}.${calendar.get(Calendar.MILLISECOND)}"
        val docName = "${value.hvac.bldgtype}_${value.hvac.businessname}_$dateTime"

        writeDocumentWithName(document, docName)
        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/${docName}.docx")
        uploadFile(DropBox.getClient(), file, "/Gemini/Energy/Reports/$docName.docx")
    }

    /** writes docuemnt to local **/
    private fun writeDocumentWithName(document: XWPFDocument, name: String) {
        val out = FileOutputStream(File(Environment.getExternalStorageDirectory().absolutePath + "/" + name + ".docx"))
        document.write(out)
        out.close()
    }

    /**
     * Uploads a file in a single request. This approach is preferred for small files since it
     * eliminates unnecessary round-trips to the servers.
     *
     * @param dbxClient Dropbox user authenticated client
     * @param localFIle local file to upload
     * @param dropboxPath Where to upload the file to within Dropbox
     */
    private fun uploadFile(dbxClient: DbxClientV2, localFile: File, dropboxPath: String) {
        try {
            FileInputStream(localFile).use { `in` ->

                val metadata = dbxClient.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.ADD)
                        .withClientModified(Date(localFile.lastModified()))
                        .uploadAndFinish(`in`)

                println(metadata.toStringMultiline())
            }
        } catch (ex: UploadErrorException) {
            System.err.println("Error uploading to Dropbox: " + ex.message)
            System.exit(1)
        } catch (ex: DbxException) {
            System.err.println("Error uploading to Dropbox: " + ex.message)
            System.exit(1)
        } catch (ex: IOException) {
            System.err.println("Error reading from file \"" + localFile + "\": " + ex.message)
            System.exit(1)
        }
    }
}

