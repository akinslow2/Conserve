package com.gemini.energy.wordDocGenerator.utils

import com.gemini.energy.format
import com.gemini.energy.wordDocGenerator.WordDocumentGenerator
import org.apache.poi.wp.usermodel.HeaderFooterType
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*
import java.math.BigInteger

/**
 * Word Document extensions
 */


/**
 * creates page header with savings displayed
 *
 * @param headerText initial header text
 * @param implementationText implementation text
 * @param savingsValue savings value for this section
 */
fun XWPFDocument.createHeaderWithSavings(
        headerText: String,
        implementationText: String,
        savingsValue: Double) {

    val p1 = this.createParagraph()
    p1.spacingBetween = 1.5
    p1.isPageBreak = true
    p1.alignment = ParagraphAlignment.CENTER
    val r1p1 = p1.createRun()
    r1p1.fontFamily = WordDocumentGenerator.fontGeorgia
    r1p1.isBold = true
    r1p1.isItalic = true
    r1p1.fontSize = 20
    r1p1.setText(headerText)
    r1p1.addBreak()

    val p2 = this.createParagraph()
    p2.spacingBetween = 1.5
    p2.alignment = ParagraphAlignment.CENTER
    val r1p2 = p2.createRun()
    r1p2.fontFamily = WordDocumentGenerator.fontGeorgia
    r1p2.fontSize = 14
    r1p2.setText(implementationText)
    r1p2.addBreak()
    val r2p2 = p2.createRun()
    r2p2.fontFamily = WordDocumentGenerator.fontGeorgia
    r2p2.isBold = true
    r2p2.fontSize = 24
    r2p2.color = WordDocumentGenerator.greenColor
    r2p2.setText("$${savingsValue.format(0)}")
    val r3p2 = p2.createRun()
    r3p2.fontFamily = WordDocumentGenerator.fontGeorgia
    r3p2.isBold = true
    r3p2.isItalic = true
    r3p2.fontSize = 24
    r3p2.color = WordDocumentGenerator.greenColor
    r3p2.setText(" every year")
    r3p2.addBreak()
}


/**
 * creates a header stating
 *  At this time there are no recommended measures
 *
 *  @param headerText intial header text
 */
fun XWPFDocument.createHeaderWithNoRecommendedMeasures(headerText: String) {
    val p1 = this.createParagraph()
    p1.spacingBetween = 1.5
    p1.isPageBreak = true
    p1.alignment = ParagraphAlignment.CENTER
    val r1p1 = p1.createRun()
    r1p1.fontFamily = WordDocumentGenerator.fontGeorgia
    r1p1.isBold = true
    r1p1.isItalic = true
    r1p1.fontSize = 20
    r1p1.setText(headerText)
    r1p1.addBreak()

    val p2 = this.createParagraph()
    p2.spacingBetween = 1.5
    p2.alignment = ParagraphAlignment.CENTER
    val r1p2 = p2.createRun()
    r1p2.fontFamily = WordDocumentGenerator.fontGeorgia
    r1p2.fontSize = 14
    r1p2.setText("At this time, there are no recommended measures to take.")
    r1p2.addBreak()
}


/**
 * creates a paragraph in the given document [this]
 * with normal formatting
 *
 * @param text paragraph text
 * @param fontSize size of text, defaults to 12
 * @param fontColor color of text, defaults to black
 * @param isStartOfNewPage if this paragraph should be the start of a new page
 * @param addBreakAfter if a line break should be added after paragraph text, defaults to false
 */
fun XWPFDocument.createNormalParagraph(
        text: String,
        fontSize: Int = 13,
        fontColor: String = WordDocumentGenerator.blackColor,
        isStartOfNewPage: Boolean = false,
        addBreakAfter: Boolean = false) {

    val paragraph = this.createParagraph()
    paragraph.spacingBetween = 1.5
    paragraph.isPageBreak = isStartOfNewPage
    val run = paragraph.createRun()
    run.fontFamily = WordDocumentGenerator.fontGeorgia
    run.fontSize = fontSize
    run.color = fontColor
    run.setText(text)

    if (addBreakAfter)
        run.addBreak()
}


/**
 * create bulleted list
 *
 * @param items array of string to bullet
 * @param fontColor color of text
 * @param fontSize size of text
 * @param isBold should text be bold
 * @param isItalic should text be italic
 */
fun XWPFDocument.createBullets(
        items: Array<String>,
        fontColor: String = WordDocumentGenerator.blackColor,
        fontSize: Int = 13,
        isBold: Boolean = false,
        isItalic: Boolean = false) {

    val cTAbstractNum = CTAbstractNum.Factory.newInstance()
    cTAbstractNum.abstractNumId = BigInteger.valueOf(0)
    val cTLvl = cTAbstractNum.addNewLvl()
    cTLvl.addNewLvlText().setVal("•")

    val abstractNum = XWPFAbstractNum(cTAbstractNum)
    val numbering = this.createNumbering()
    val abstractNumID = numbering.addAbstractNum(abstractNum)
    val numID = numbering.addNum(abstractNumID)

    for (item in items) {
        val bulletedPara = this.createParagraph()
        bulletedPara.numID = numID
        bulletedPara.spacingBetween = 1.15
        val run = bulletedPara.createRun()
        run.fontFamily = WordDocumentGenerator.fontGeorgia
        run.fontSize = fontSize
        run.color = fontColor
        if (isBold) {
            run.isBold = true
        }
        if (isItalic) {
            run.isItalic = true
        }
        run.setText(item)
    }
}


/**
 * add page numbers to document
 * page numbers are added to footer
 */
fun XWPFDocument.addPageNumbers() {
    val footer = this.createFooter(HeaderFooterType.DEFAULT);

    var paragraph = footer.getParagraphArray(0)
    if (paragraph == null) paragraph = footer.createParagraph()
    paragraph.alignment = ParagraphAlignment.RIGHT

    val run = paragraph.createRun()
    run.setText("Page ")
    paragraph.ctp.addNewFldSimple().instr = "PAGE |\\* ARABIC MERGEFORMAT"
}


/**
 * Word document table extensions
 */


/**
 * create header row in table for financial components
 *
 * @param cell1 value shown in cell one, defaults to "Financial Components"
 * @param cell2 value shown in cell two, defaults to "Financial Overview"
 * @param rowIndex row index, defaults to 0 (initial row)
 */
fun XWPFTable.generateFinancialComponentsHeaderRow(
        cell1: String = "Financial Components",
        cell2: String = "Financial Overview",
        rowIndex: Int = 0) {
    val row0 = this.getRow(rowIndex)
    val c0r0 = row0.getCell(0)
    c0r0.color = WordDocumentGenerator.tableGreenColor
    val pc0r0 = c0r0.paragraphs[0]
    pc0r0.alignment = ParagraphAlignment.CENTER
    val rc0r0 = pc0r0.createRun()
    rc0r0.fontFamily = WordDocumentGenerator.fontGeorgia
    rc0r0.fontSize = 12
    rc0r0.isBold = true
    rc0r0.color = WordDocumentGenerator.whiteColor
    rc0r0.setText(cell1)

    val c1r0 = row0.getCell(1)
    c1r0.color = WordDocumentGenerator.tableGreenColor
    val pc1r0 = c1r0.paragraphs[0]
    pc1r0.alignment = ParagraphAlignment.CENTER
    val rc1r0 = pc1r0.createRun()
    rc1r0.fontFamily = WordDocumentGenerator.fontGeorgia
    rc1r0.fontSize = 12
    rc1r0.isBold = true
    rc1r0.color = WordDocumentGenerator.whiteColor
    rc1r0.setText(cell2)
}


/**
 * create detail row in table for financial components
 *
 * @param cell1 value shown in cell one
 * @param cell2 value shown in cell two
 * @param rowIndex row index
 */
fun XWPFTable.generateFinancialComponentsDetailRow(cell1: String, cell2: String, rowIndex: Int) {
    val row = this.getRow(rowIndex)
    val cell0 = row.getCell(0)
    val para0 = cell0.paragraphs[0]
    val para0run = para0.createRun()
    para0run.fontFamily = WordDocumentGenerator.fontGeorgia
    para0run.fontSize = 12
    para0run.isBold = true
    para0run.setText(cell1)

    val cellOne = row.getCell(1)
    val para1 = cellOne.paragraphs[0]
    para1.alignment = ParagraphAlignment.CENTER
    val para1run = para1.createRun()
    para1run.fontSize = 12
    para1run.fontFamily = WordDocumentGenerator.fontGeorgia
    para1run.setText(cell2)
}


/**
 * merge cell(s) horizontally
 *
 * @param row affected row
 * @param fromCol starting collumn of merge
 * @param toCol end collumn of merge
 */
fun XWPFTable.mergeCellHorizontally(row: Int, fromCol: Int, toCol: Int) {
    for (colIndex in fromCol..toCol) {
        val cell = this.getRow(row).getCell(colIndex)
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


/**
 * center table horizontally
 */
fun XWPFTable.centerTable() {
    val t = this.ctTbl
    val pr = t.tblPr
    val jc = pr.addNewJc()
    jc.setVal(STJc.CENTER)
    pr.jc = jc
}


/**
 * fit table to width
 *
 * @param width table width
 */
fun XWPFTable.fitTable(width: Long) {
    val ctTbl = this.ctTbl
    val properties = ctTbl.tblPr
    val tblW = properties.tblW
    tblW.type = STTblWidth.PCT
    tblW.w = BigInteger.valueOf(width)
    properties.tblW = tblW
    ctTbl.tblPr = properties
}