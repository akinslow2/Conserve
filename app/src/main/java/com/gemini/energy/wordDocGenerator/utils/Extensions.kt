package com.gemini.energy.wordDocGenerator.utils

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
        fontSize: Int = 12,
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
        bulletedPara.spacingBetween = 1.5
        val run = bulletedPara.createRun()
        run.fontFamily = WordDocumentGenerator.fontAgencyFB
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