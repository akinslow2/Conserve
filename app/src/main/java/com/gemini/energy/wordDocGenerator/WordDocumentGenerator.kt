package com.gemini.energy.wordDocGenerator

import android.os.Environment
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.UploadErrorException
import com.dropbox.core.v2.files.WriteMode
import com.gemini.energy.branch
import com.gemini.energy.format
import com.gemini.energy.presentation.audit.DropBox
import com.gemini.energy.service.device.EBase
import com.gemini.energy.wordDocGenerator.models.*
import com.gemini.energy.wordDocGenerator.utils.*
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.VerticalAlign
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.absoluteValue


class WordDocumentGenerator {

    private lateinit var documents: List<XWPFDocument>

    companion object {
        //Hex #'s used for the colors
        private const val greyColor = "595959"
        private const val tableGreyColor = "D9D9D9"
        const val greenColor = "70ad47"
        const val tableGreenColor = "469B43"
        const val blackColor = "000000"
        const val whiteColor = "ffffff"
        const val redColor = "ff0000"

        const val fontAgencyFB = "Agency FB"
        private const val fontUnivers = "Univers LT Std 39 Thin UltraCn"
        const val fontTeko = "Teko"

        const val fontGeorgia = "Georgia"

        private const val manuallyGeneratedValue = "INSERTME"
    }


    /** Triggers the document generation for each audit and saves documents to appropriate place **/
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
        generateFirstPage(document, value.preAudit)

        generateEnergySavingPotentialPage(document, value)

        generateElectricityCalculationPage(document, value)

        if (value.lighting != null) {
            generateLightingSavingsPage(document, value.lighting)
        }

        if (value.hvac != null) {
            generateHvacSavingsPage(document, value.hvac, value.building)
        }

        if (value.waterHeater != null) {
            generateWaterHeaterSavingsPage(document, value.waterHeater)
        }

        if (value.equipment != null && value.equipment.instances.any()) {
            generateEquipmentSavingsPage(document, value.equipment)
        }

        if (value.refrigeration != null) {
            generateRefrigerationSavingsPage(document, value.refrigeration)
        }

        generateFacilityInformationPage(document, value, value.preAudit)

        //////////////////////
        /// APPENDIX START ///
        val appendix = document.createParagraph()
        appendix.spacingBetween = 1.5
        appendix.isPageBreak = true
        appendix.alignment = ParagraphAlignment.CENTER
        val appendixRun = appendix.createRun()
        appendixRun.fontFamily = fontGeorgia
        appendixRun.isBold = true
        appendixRun.isItalic = true
        appendixRun.fontSize = 20
        appendixRun.setText("Appendix")
        appendixRun.addBreak()

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5

        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontGeorgia
        r1p2.fontSize = 13
        r1p2.setText("Electric rates from https://www.tep.com/rates/")
        r1p2.addBreak()
        r1p2.setText("Building is using electric schedule ${value.preAudit.electricStructure}")
        r1p2.addCarriageReturn()


        if (value.preAudit.gasApplicableToBuilding) {
            val r2p2 = p2.createRun()
            r2p2.fontFamily = fontGeorgia
            r2p2.fontSize = 13
            r2p2.setText("Gas rates from https://www.swgas.com/en/arizona-rates-and-regulation")
            r1p2.addBreak()
            r2p2.setText("Building is using gas schedule ${value.preAudit.gasStructure}")
            r2p2.addCarriageReturn()
        }


        if (value.lighting != null) {
            document.createNormalParagraph(

                "The savings represented in the table below reflect the bulb replacement costs abated because LED bulbs will last 2 to 3 times longer. "
                        + "In addition, the energy savings account for the reduction in cooling load due to LED bulbs releasing less heat.",
                isStartOfNewPage = true)

            createLightingTable3(document, value.lighting)

            generateLightingContractorPage(document, value.lighting)
        }

        document.addPageNumbers()

        writeDocument(value, document)

        return document
    }

    // generate pages
    private fun generateFirstPage(document: XWPFDocument, preAudit: PreAuditValues) {
        val p1 = document.createParagraph()
        p1.alignment = ParagraphAlignment.CENTER
        p1.spacingBetween = 1.5
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontGeorgia
        r1p1.fontSize = 28
        r1p1.color = greyColor
        r1p1.isBold = true
        r1p1.setText("Energy Savings Opportunity Report")
        r1p1.addBreak()
        r1p1.setText(preAudit.businessname)
        r1p1.addBreak()
        r1p1.setText("${preAudit.auditmonth} ${preAudit.audityear}")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.08
        p2.alignment = ParagraphAlignment.CENTER
        val r1p2 = p2.createRun()

        r1p2.fontFamily = fontGeorgia
        r1p2.fontSize = 13
        r1p2.color = greyColor
        r1p2.setText("This report presents the results of an ASHRAE Level 2 energy assessment conducted ${preAudit.auditmonth} ${preAudit.startday}")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontGeorgia
        r2p2.fontSize = 13
        r2p2.color = greyColor
        r2p2.subscript = VerticalAlign.SUPERSCRIPT
        r2p2.setText("th")

        val r3p2 = p2.createRun()
        r3p2.fontFamily = fontGeorgia
        r3p2.fontSize = 13
        r3p2.color = greyColor
        r3p2.setText(" through ${preAudit.auditmonth} ${preAudit.endday}")
        val r4p2 = p2.createRun()
        r4p2.fontFamily = fontGeorgia
        r4p2.fontSize = 13
        r4p2.color = greyColor
        r4p2.subscript = VerticalAlign.SUPERSCRIPT
        r4p2.setText("th")

        val r5p2 = p2.createRun()
        r5p2.fontFamily = fontGeorgia
        r5p2.fontSize = 13
        r5p2.color = greyColor
        r5p2.setText(" and evaluation of potential improvements to reduce energy consumption and related operating cost at ")
        r5p2.addBreak()
        r5p2.addBreak()
        r5p2.setText(preAudit.businessname)
        r5p2.addBreak()
        r5p2.setText(preAudit.clientaddress)
    }

    private fun generateEnergySavingPotentialPage(document: XWPFDocument, values: PreparedForDocument) {
        document.createHeaderWithSavings(
                "Energy Savings Opportunity Summary",
                "Implementation of all recommended measures will save you at least:",
                values.building.buildingTotalSavings)

        val p2 = document.createParagraph()
        val r3p2 = p2.createRun()
        r3p2.fontFamily = fontGeorgia
        r3p2.fontSize = 20
        r3p2.setText("with a Net Present Value (NPV) of ${values.building.buildingNetPresentValue.format(0)}")
        r3p2.addBreak()

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontGeorgia
        r1p3.fontSize = 12
        r1p3.color = greyColor
        r1p3.setText("The analysis shows a high potential for energy savings which will:")

        val bullets1 = arrayOf(
                "reduced energy costs,",
                "improved profit margins,",
                "increased thermal comfort in your building, and",
                "more control over how energy is consumed.")
        document.createBullets(bullets1, fontColor = greyColor)

        if (values.building.buildingTotalCost > 5000) {
            val qualifyP = document.createParagraph()
            qualifyP.spacingBetween = 1.5
            val qualifyR = qualifyP.createRun()
            qualifyR.fontFamily = fontGeorgia
            qualifyR.fontSize = 12
            qualifyR.color = greyColor
            qualifyR.setText("The implementation costs of all the measures qualify you for a 0% interest loan. By implementing these energy efficiency measures, you will join the courageous few energy heroes who are making the effort to make your community cleaner.")
        }

        // potentially add section on zero- to low-interest financing - will wait on response from Tucson 2030 folks

        createEnergySavingsTable(document, values)
    }

    private fun generateLightingSavingsPage(document: XWPFDocument, lights: LightingValues) {
        document.createHeaderWithSavings(
                "Lighting Savings",
                "Implementation of recommended lighting measures will save you at least: ",
                lights.totalcostsavings)

        document.createNormalParagraph("Benefits of LED bulbs include:")

        val bullets = arrayOf("Lower maintenance costs", "Better quality light", "Less air-conditioning required", "Happier customers")
        document.createBullets(bullets)

        val paybackString =
                if (lights.paybackMonth > 11)
                    (lights.paybackMonth / 12).format(1) + " years"
                else lights.paybackMonth.format(0) + " months"
        document.createNormalParagraph(
                "Replacing all of your non-LED bulbs and adding controls will cost approximately "
                        + "$${lights.totalCost.format(0)} but will result in an estimated "
                        + "total savings of $${lights.totalcostsavings.format(0)} over the next 10 years. "
                        + "This equates to a payback period of approximately ${paybackString}."
                        + "Overall, accounting for maintenance savings, implementing the LED retrofit translates to a minimum NPV of $${lights.netPresentValue.format(0)}. "
                        + "We strongly recommend you replace all non-LED bulbs.")

        if (lights.lightingRows.any { it.isCeilingOver10Feet.toLowerCase() == "yes" }) {
            document.createNormalParagraph(
                    // @DY - change the code to ask 15 feet instead of 10 - in jsons, .kt files, and input parameters sheets
                    "There is an additional $500 fee to rent the lift needed to replace lighting located in a ceiling that is higher than 15 feet.")
        }

        document.createNormalParagraph("Please see the appendix for specifics of each lighting measure.")

        createLightingTable1(document, lights)
    }

    private fun generateHvacSavingsPage(document: XWPFDocument, hvac: HvacValues, building: BuildingValues) {

        document.createHeaderWithSavings(
                "Heating, Ventilation, & Air-Conditioning [HVAC] Savings",
                "Implementation of recommended HVAC measures will save you at least: ",
                hvac.totalSavings)

        val p3 = document.createParagraph()
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontGeorgia
        r1p3.fontSize = 13
        r1p3.setText("Make sure an HVAC technician checks your package units on a ")

        val r2p3 = p3.createRun()
        r2p3.fontFamily = fontGeorgia
        r2p3.fontSize = 13
        r2p3.isItalic = true
        r2p3.setText("quarterly")

        val r3p3 = p3.createRun()
        r3p3.fontFamily = fontGeorgia
        r3p3.fontSize = 13
        r3p3.setText(" basis. Failure to properly maintain your HVAC units results in increased energy bills "
                + "and shorter equipment life spans, reducing your savings potential.")


        document.createNormalParagraph(
                "The HVAC technician's maintenance scope of work should include:")

        val hvacBullets = arrayOf(

                BulletItem(arrayOf(BulletItem.Span("Cleaning the condenser"))),
                BulletItem(arrayOf(
                        BulletItem.Span("Checking the economizer ("),
                        BulletItem.Span("if applicable", isItalic = true),
                        BulletItem.Span(")")
                )),
                BulletItem(arrayOf(BulletItem.Span("Checking and replacing filters"))),
                BulletItem(arrayOf(BulletItem.Span("Charging the refrigerant"))),
                BulletItem(arrayOf(
                        BulletItem.Span("Tightening belts ("),
                        BulletItem.Span("if applicable", isItalic = true),
                        BulletItem.Span(")")
                ))
        )
        document.createBulletsWithInlineStyling(hvacBullets)


        document.createNormalParagraph("The details of your HVAC units are below:")

        createHvacDetailsTable(document, hvac)


        document.createNormalParagraph("Benefits of a new HVAC system with smart controls include:")
        val hvacBullets2 = arrayOf(
                "Lower operating costs",
                "Less risk of failure and disruption of business",
                "More control over temperature settings",
                "More consistent temperature throughout the space",
                "Better for the environment"
        )

        document.createBullets(hvacBullets2)


        if (hvac.instances.any { i -> i.age < 11 }) {
            document.createNormalParagraph(
                    "You HVAC system could see additional energy savings and an extended life "
                            + "expectancy by adding controls. Benefits of HVAC controls include:")

            val hvacBullets = arrayOf(
                    "Better HVAC performance",
                    "Reduced energy consumption",
                    "More control over temperature settings",
                    "Better for the environment")
            document.createBullets(hvacBullets)
        }

        createHvacTable(document, hvac, building)
    }

    private fun generateWaterHeaterSavingsPage(document: XWPFDocument, waterheater: WaterHeaterValues) {

        if (waterheater.netPresentValue > 1) {
            document.createHeaderWithSavings(
                    "Water Heating Savings",
                    "Implementation of recommended water heating upgrades will save you at least: ",
                    waterheater.totalSavings)
        } else {
            document.createHeaderWithNoRecommendedMeasures("Water Heating Savings")
        }

        document.createNormalParagraph(
                "Replacing your ${waterheater.unittype} water heater(s) will result in a total cost of $${waterheater.totalCost.format(0)} " +
                        "but will result in a minimum of $${waterheater.totalSavings.format(0)} in annual savings. " +
                        "This equates to a payback period of approximately ${waterheater.paybackMonth.format(0)} months. " +
                        "This investment will result in a NPV of $${waterheater.netPresentValue.format(0)}. ",
                addBreakAfter = true)

        createWaterHeaterTable(document, waterheater)
    }

    private fun generateEquipmentSavingsPage(document: XWPFDocument, equipment: EquipmentValues) {
        if (equipment.totalNetPresentValue > 1) {
            document.createHeaderWithSavings(
                    "Kitchen Equipment Savings",
                    "Implementation of recommended kitchen equipment upgrades will save you at least: ",
                    equipment.totalSavings)
        } else {
            document.createHeaderWithNoRecommendedMeasures("Kitchen Equipment Savings")
        }

        for (equip in equipment.instances) {
            var paybackString =
                    if (equip.paybackMonth > 11)
                        (equip.paybackMonth / 12).format(1) + " years"
                    else equip.paybackMonth.format(0) + " months"

            val p = document.createParagraph()
            val run = p.createRun()
            run.fontFamily = fontGeorgia
            run.fontSize = 13
            run.setText("Your ${equip.make} ${equip.name} is ${equip.age.format(0)} years old and as a result is annually consuming " +
                    "${equip.delta.format(0)} kWh more energy than a newer, higher-efficiency version. " +
                    "We recommend you replace the ${equip.name} with a high-effiency version to save $${equip.costElectricity.format(0)} per year. " +
                    "The implementation cost for a new ${equip.name} is roughly $${equip.materialCost.format(0)} " +
                    "and the expected payback period for this replacement is ${paybackString}. " +
                    "This investment will result in a NPV of \$${equip.netPresentValue.format(0)}")
            run.addBreak()
        }

    }

    private fun generateRefrigerationSavingsPage(document: XWPFDocument, refrigeration: RefrigerationValues) {

        if (refrigeration.netPresentValue > 1) {
            document.createHeaderWithSavings(
                    "Refrigeration Savings",
                    "Implementation of recommended refrigeration upgrades will save you at least: ",
                    refrigeration.totalSavings)
        } else {
            document.createHeaderWithNoRecommendedMeasures("Refrigeration Savings")
        }

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontGeorgia
        r1p3.fontSize = 18
        r1p3.setText("The estimated cost is ${refrigeration.totalCost.format(2)}, resulting in a payback of ${refrigeration.paybackMonth.format(2)} months. " +
                "This investment will result in a NPV of $${refrigeration.netPresentValue.format(0)}")
    }

    private fun generateFacilityInformationPage(document: XWPFDocument, values: PreparedForDocument, preAudit: PreAuditValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        p1.alignment = ParagraphAlignment.CENTER
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontGeorgia
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Facility Information")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontGeorgia
        r1p2.fontSize = 13
        r1p2.setText("${preAudit.businessname} is ${preAudit.bldgarea.format(0)} square feet and includes spaces for ${values.zoneString}. ${preAudit.businessname} is open daily from ${preAudit.operationhours}. This building is classified as ${preAudit.bldgtype}.")
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.setText("${preAudit.businessname} is on the ${preAudit.electricCompany} ${preAudit.electricStructure} electric rate schedule. In the one-year period from $manuallyGeneratedValue through $manuallyGeneratedValue, ${preAudit.businessname} consumed $manuallyGeneratedValue kWh, resulting in an electrical cost of $$manuallyGeneratedValue.")
        r1p2.addBreak()
        r1p2.addBreak()

        if (preAudit.gasApplicableToBuilding) {
            r1p2.setText("${preAudit.businessname} is on the ${preAudit.gasCompany} ${preAudit.gasStructure} gas rate schedule and for above stated time period, ${preAudit.businessname} used $manuallyGeneratedValue therms at a cost of $$manuallyGeneratedValue.")
            r1p2.addBreak()
        }

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontGeorgia
        r1p3.isBold
        r1p3.fontSize = 20
        r1p3.setText("Energy Savings Disclaimer")

        val p4 = document.createParagraph()
        p4.spacingBetween = 1.5
        val r1p4 = p4.createRun()
        r1p4.fontFamily = fontGeorgia
        r1p4.fontSize = 13
        r1p4.setText("While Gemini Energy Solutions relies on industry best-practices, on-site data loggers, and energy savings modeling software to make energy savings predictions as accurate as possible," +
                " it is important to understand that Gemini's Energy Savings Opportunity Report is intended to inform participants of the ")
        val r2p4 = p4.createRun()
        r2p4.fontFamily = fontGeorgia
        r2p4.fontSize = 13
        r2p4.isItalic = true
        r2p4.setText("ESTIMATED")

        val r3p4 = p4.createRun()
        r3p4.fontFamily = fontGeorgia
        r3p4.fontSize = 13
        r3p4.setText(" energy cost savings they should expect to realize - assuming all recommended work is done.")
        r3p4.addBreak()
        r3p4.addBreak()
        r3p4.setText("The estimated annual energy saving numbers reflected in the Energy Savings Opportunity Report are based on the last 12 months of weather and energy costs, as well as the behavior patterns during the time that " +
                "the on-site data loggers were active. Since future weather, behavior, and energy costs cannot be accurately predicted, there will be a variable difference between estimated and actual savings.")

    }

    private fun generateElectricityCalculationPage(document: XWPFDocument, values: PreparedForDocument) {

        document.createNormalParagraph("The summary of the changes in peak demand (kW) and energy (kWh) can be seen below:")

        generateElectrictyCalculationTable(document, values)
    }

    private fun generateLightingContractorPage(document: XWPFDocument, lights: LightingValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        p1.alignment = ParagraphAlignment.CENTER
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontGeorgia
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Specifics for Lighting Contractor")

        generateLightContractorTable(document, lights)
    }


    private fun nullableCostString(cost: Double?): String {
        if (cost == null) return "N/A"

        return "$" + cost.format(0)
    }

    // generate tables
    private fun createEnergySavingsTable(document: XWPFDocument, values: PreparedForDocument) {
        val table = document.createTable(19, 2)
        var currentRow = 0

        table.generateFinancialComponentsHeaderRow()
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Total Savings over Life of Equipment",
                "\$${values.building.buildingTotalSavings.format(0)}",
                currentRow,
                cell2IsBold = true,
                cell2Color = if (values.building.buildingTotalSavings >= 0) blackColor else redColor)
        currentRow += 1

//        if (values.preAudit.electrificationSavings > 0.0) {
//            table.generateFinancialComponentsDetailRow(
//                "Electrification Savings ($/yr)",
//                "\$${values.preAudit.electrificationSavings.format(0)}",
//                currentRow)
//            currentRow += 1
//        }
//        if (values.building > 0.0) {
//            table.generateFinancialComponentsDetailRow(
//                "Electrification Savings ($/yr)",
//                "\$${values.preAudit.electrificationSavings.format(0)}",
//                currentRow)
//            currentRow += 1
//        }

        table.generateFinancialComponentsDetailRow(
                "Implementation Cost (\$)",
                "\$${values.building.buildingTotalCost.format(0)}",
                currentRow,
                cell2Color = if (values.building.buildingTotalCost >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Lighting (\$)",
                nullableCostString(values.lighting?.totalCost),
                currentRow,
                cell2Color = if (values.lighting == null || values.lighting.totalCost >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        HVAC (\$)",
                "\$${values.building.hvacTotalCost.format(0)}",
                currentRow,
                cell2Color = if (values.building.hvacTotalCost >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Water Heater (\$)",
                "\$${values.building.waterHeaterTotalCost.format(0)}",
                currentRow,
                cell2Color = if (values.building.waterHeaterTotalCost >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Refrigeration (\$)",
                "\$${values.building.refrigerationTotalCost.format(0)}",
                currentRow,
                cell2Color = if (values.building.refrigerationTotalCost >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Motors (\$)",
                nullableCostString(values.motor?.totalCost),
                currentRow,
                cell2Color = if (values.motor == null || values.motor.totalCost >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Kitchen Equipment (\$)",
                "\$${values.building.equipmentTotalCost.format(0)}",
                currentRow,
                cell2Color = if (values.building.equipmentTotalCost > 0) blackColor else redColor)
        currentRow += 1

        // seperator
        val separatorRow = table.getRow(currentRow)
        separatorRow.getCell(0).color = tableGreyColor
        separatorRow.getCell(1).color = tableGreyColor
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Net Present Value (NPV)",
                values.building.buildingNetPresentValue.absoluteValue.format(0),
                currentRow,
                cell2IsBold = true,
                cell2Color = if (values.building.buildingNetPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Lighting (NPV)",
                values.lighting?.netPresentValue?.absoluteValue?.format(0) ?: "N/A",
                currentRow,
                cell2Color = if (values.lighting?.netPresentValue == null || values.lighting.netPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        HVAC (NPV)",
                values.hvac?.netPresentValue?.absoluteValue?.format(0) ?: "N/A",
                currentRow,
                cell2Color = if (values.hvac?.netPresentValue == null || values.hvac.netPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Water Heater (NPV)",
                values.waterHeater?.netPresentValue?.absoluteValue?.format(0) ?: "N/A",
                currentRow,
                cell2Color = if (values.waterHeater?.netPresentValue == null || values.waterHeater.netPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Refrigeration (NPV)",
                values.refrigeration?.netPresentValue?.absoluteValue?.format(0) ?: "N/A",
                currentRow,
                cell2Color = if (values.refrigeration?.netPresentValue == null || values.refrigeration.netPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Motors (NPV)",
                values.motor?.netPresentValue?.absoluteValue?.format(0) ?: "N/A",
                currentRow,
                cell2Color = if (values.motor?.netPresentValue == null || values.motor.netPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "        Kitchen Equipment (NPV)",
                values.equipment?.totalNetPresentValue?.absoluteValue?.format(0) ?: "N/A",
                currentRow,
                cell2Color = if (values.equipment?.totalNetPresentValue == null || values.equipment.totalNetPresentValue >= 0) blackColor else redColor)
        currentRow += 1

        table.fitTable(4500)
        table.centerTable()
    }

    private fun createLightingTable1(document: XWPFDocument, lights: LightingValues) {
        val table = document.createTable(9, 2)
        var currentRow = 0

        table.generateFinancialComponentsHeaderRow()
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Energy Savings",
                "${(lights.kWhSavings + lights.hvacAdditionalkWhSavings).format(0)} kWh/year",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Minimum Electrical Cost Savings",
                "$${lights.totalcostsavings.format(0)}/year",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Implementation Cost ($)",
                "$${lights.implementationCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Material Cost",
                "$${lights.materialCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Labor Cost",
                "$${lights.laborCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Available Incentives",
                "$${lights.incentives.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Payback",
                lights.paybackYear.format(1) + " years",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Net Present Value",
                "$${lights.netPresentValue.absoluteValue.format(0)}",
                currentRow,
                cell2Color = if (lights.netPresentValue >= 0) blackColor else redColor)

        table.centerTable()
        table.fitTable(4000)
    }

    private fun createLightingTable3(document: XWPFDocument, lights: LightingValues) {
        val rows = lights.lightingRows

        val table = document.createTable(rows.size + 1, 11)
        table.addBorders()

        for (i in 0..rows.size) {
            // saftey check if no rows exist
            if (rows.isEmpty()) {
                return
            }

            val row = table.getRow(i)
            val isFirst = i == 0
            val isLast = i == rows.size

            when {
                isFirst -> {
                    row.createEquipmentInstanceCell(0, "Measure", true)
                    row.createEquipmentInstanceCell(1, "Location", true)
                    row.createEquipmentInstanceCell(2, "Measure Description", true)
                    row.createEquipmentInstanceCell(3, "Quantity", true)
                    row.createEquipmentInstanceCell(4, "Current Power (kW)", true)
                    row.createEquipmentInstanceCell(5, "Usage (hrs)", true)
                    row.createEquipmentInstanceCell(6, "Post Power (kW)", true)
                    row.createEquipmentInstanceCell(7, "Post Usage (hrs)", true)
                    row.createEquipmentInstanceCell(8, "Energy Savings (kWh)", true)
                    row.createEquipmentInstanceCell(9, "Life Cost Savings", true)
                    row.createEquipmentInstanceCell(10, "Implementation Cost", true)
                }
                isLast -> {
                    row.createEquipmentInstanceCell(3, lights.quantityOfBulbs.toString(), true)
                    row.createEquipmentInstanceCell(4, lights.currentTotalkW.format(2), true)
                    row.createEquipmentInstanceCell(5, lights.preUsageHours.format(0), true)
                    row.createEquipmentInstanceCell(6, lights.postTotalkW.format(2), true)
                    row.createEquipmentInstanceCell(7, lights.postUsageHours.format(0), true)
                    row.createEquipmentInstanceCell(8, lights.kWhSavings.format(0), true)
                    row.createEquipmentInstanceCell(9, lights.netPresentValue.format(0), true)
                    row.createEquipmentInstanceCell(10, lights.totalCost.format(0), true)
                }
                else -> { // lighting item
                    val rowIndex = if (isFirst) 0 else i - 1
                    val rowItem = rows[rowIndex]

                    row.createEquipmentInstanceCell(0, rowItem.measure, false)
                    row.createEquipmentInstanceCell(1, rowItem.location, false)
                    row.createEquipmentInstanceCell(2, rowItem.measureDescription, false)
                    row.createEquipmentInstanceCell(3, rowItem.quantity.format(0), false)
                    row.createEquipmentInstanceCell(4, rowItem.currentPowerkW.format(2), false)
                    row.createEquipmentInstanceCell(5, rowItem.usageHours.format(0), false)
                    row.createEquipmentInstanceCell(6, rowItem.postPowerkW.format(2), false)
                    row.createEquipmentInstanceCell(7, rowItem.postUsageHours.format(0), false)
                    row.createEquipmentInstanceCell(8, rowItem.energySavingskWh.format(0), false)
                    row.createEquipmentInstanceCell(9, "$${rowItem.netPresentValue.format(0)}", false)
                    row.createEquipmentInstanceCell(10, "$${rowItem.implementationCost.format(0)}", false)
                }
            }
        }

        table.centerTable()
    }

    private fun createHvacTable(document: XWPFDocument, hvac: HvacValues, building: BuildingValues) {
        val table = document.createTable(9, 2)
        var currentRow = 0

        table.generateFinancialComponentsHeaderRow()
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Energy Savings",
                "${(hvac.currentTotalkWh - hvac.postTotalkWh).format(0)} kWh/year",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Minimum Electrical Cost Savings",
                "$${hvac.totalSavings.format(0)}/year",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Implementation Cost ($)",
                "$${hvac.totalCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Material Cost",
                "$${hvac.materialCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Labor Cost",
                "$${hvac.laborCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Available Incentives",
                "$${hvac.incentives.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Payback",
                hvac.paybackYear.format(1) + " years",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Net Present Value",
                "$${hvac.netPresentValue.absoluteValue.format(0)}",
                currentRow,
                cell2Color = if (hvac.netPresentValue >= 0) blackColor else redColor)

        table.centerTable()
        table.fitTable(4000)
    }

    private fun createHvacDetailsTable(document: XWPFDocument, hvac: HvacValues) {
        val rows = hvac.instances
        val table = document.createTable(rows.size + 1, 8)
        table.addBorders()

        for (i in 0..rows.size) {
            val row = table.getRow(i)
            val isFirst = i == 0

            if (isFirst) {
                row.createEquipmentInstanceCell(0, "Equipment", true)
                row.createEquipmentInstanceCell(1, "Location", true)
                row.createEquipmentInstanceCell(2, "Quantity", true)
                row.createEquipmentInstanceCell(3, "Size", true)
                row.createEquipmentInstanceCell(4, "Year Installed", true)
                row.createEquipmentInstanceCell(5, "SEER value", true)
                row.createEquipmentInstanceCell(6, "Years Past Expected Life", true)
                row.createEquipmentInstanceCell(7, "Suggested Intervention", true)
            } else {
                val rowIndex = if (isFirst) 0 else i - 1
                val rowItem = rows[rowIndex]

                row.createEquipmentInstanceCell(0, rowItem.type, false)
                row.createEquipmentInstanceCell(1, rowItem.location, false)
                row.createEquipmentInstanceCell(2, rowItem.quantity.toString(), false)
                row.createEquipmentInstanceCell(3, "${rowItem.btu}-kBTU", false)
                row.createEquipmentInstanceCell(4, rowItem.year.toString(), false)
                row.createEquipmentInstanceCell(5, rowItem.seer.toString(), false)
                row.createEquipmentInstanceCell(6, rowItem.overage.toString(), false)
                row.createEquipmentInstanceCell(7, rowItem.suggestedIntervention, false)
            }
        }

        table.centerTable()
    }

    private fun createWaterHeaterTable(document: XWPFDocument, waterheater: WaterHeaterValues) {
        val table = document.createTable(9, 2)
        var currentRow = 0

        table.generateFinancialComponentsHeaderRow()
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Energy Savings",
                "${waterheater.energySavings.format(0)} kWh/year",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Minimum Electrical Cost Savings",
                "$${waterheater.totalSavings.format(0)}/year",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Implementation Cost ($)",
                "$${waterheater.totalCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Material Cost",
                "$${waterheater.materialCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Labor Cost",
                "$${waterheater.laborCost.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "       Available Incentives",
                "$${waterheater.incentives.format(0)}",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Payback",
                waterheater.paybackYear.format(1) + " years",
                currentRow)
        currentRow += 1

        table.generateFinancialComponentsDetailRow(
                "Net Present Value",
                "$${waterheater.netPresentValue.absoluteValue.format(0)}",
                currentRow,
                cell2Color = if (waterheater.netPresentValue >= 0) blackColor else redColor)

        table.centerTable()
        table.fitTable(4000)
    }

    private fun generateElectrictyCalculationTable(document: XWPFDocument, values: PreparedForDocument) {
        val table = document.createTable(8, 5)

        var rowIndex = 0
        fun createRow(cell0Text: String, cell1Text: String, cell2Text: String, cell3Text: String, cell4Text: String) {
            val row = table.getRow(rowIndex)

            val cell0 = row.getCell(0)
            val para0 = cell0.paragraphs[0]
            val run0 = para0.createRun()
            run0.fontFamily = fontGeorgia
            run0.fontSize = 12
            run0.setText(cell0Text)

            val cell1 = row.getCell(1)
            val para1 = cell1.paragraphs[0]
            val run1 = para1.createRun()
            run1.fontFamily = fontGeorgia
            run1.fontSize = 12
            run1.setText(cell1Text)

            val cell2 = row.getCell(2)
            val para2 = cell2.paragraphs[0]
            val run2 = para2.createRun()
            run2.fontFamily = fontGeorgia
            run2.fontSize = 12
            run2.setText(cell2Text)

            val cell3 = row.getCell(3)
            val para3 = cell3.paragraphs[0]
            val run3 = para3.createRun()
            run3.fontFamily = fontGeorgia
            run3.fontSize = 12
            run3.setText(cell3Text)

            val cell4 = row.getCell(4)
            val para4 = cell4.paragraphs[0]
            val run4 = para4.createRun()
            run4.fontFamily = fontGeorgia
            run4.fontSize = 12
            run4.setText(cell4Text)

            rowIndex += 1
        }

        createRow(
                "",
                "Current Total kW",
                "Current Total kWh",
                "Post Total kW",
                "Post Total kWh")

        createRow(
                "HVAC",
                values.hvac?.currentTotalkW?.format(2) ?: "0.00",
                values.hvac?.currentTotalkWh?.format(2) ?: "0.00",
                values.hvac?.postTotalkW?.format(2) ?: "0.00",
                values.hvac?.postTotalkWh?.format(2) ?: "0.00")

        createRow(
                "Lighting",
                values.lighting?.currentTotalkW?.format(2) ?: "0.00",
                values.lighting?.currentTotalkWh?.format(2) ?: "0.00",
                values.lighting?.postTotalkW?.format(2) ?: "0.00",
                values.lighting?.postTotalkWh?.format(2) ?: "0.00")

        createRow(
                "Refrigeration",
                values.refrigeration?.currentTotalkW?.format(2) ?: "0.00",
                values.refrigeration?.currentTotalkWh?.format(2) ?: "0.00",
                values.refrigeration?.postTotalkW?.format(2) ?: "0.00",
                values.refrigeration?.postTotalkWh?.format(2) ?: "0.00")

        createRow(
                "Water Heater",
                values.waterHeater?.currentTotalkW?.format(2) ?: "0.00",
                values.waterHeater?.currentTotalkWh?.format(2) ?: "0.00",
                values.waterHeater?.postTotalkW?.format(2) ?: "0.00",
                values.waterHeater?.postTotalkWh?.format(2) ?: "0.00")

        createRow(
                "Kitchen Equipment",
                values.equipment?.currentTotalkW?.format(2) ?: "0.00",
                values.equipment?.currentTotalkWh?.format(2) ?: "0.00",
                values.equipment?.postTotalkW?.format(2) ?: "0.00",
                values.equipment?.postTotalkWh?.format(2) ?: "0.00")

        createRow(
                "Motors",
                values.motor?.currentTotalkW?.format(2) ?: "0.00",
                values.motor?.currentTotalkWh?.format(2) ?: "0.00",
                values.motor?.postTotalkW?.format(2) ?: "0.00",
                values.motor?.postTotalkWh?.format(2) ?: "0.00")

        createRow(
                "Building",
                values.building.currentTotalkW.format(2),
                values.building.currentTotalkWh.format(2),
                values.building.postTotalkW.format(2),
                values.building.postTotalkWh.format(2))

        table.centerTable()
    }

    private fun generateLightContractorTable(document: XWPFDocument, lights: LightingValues) {
        val rows = lights.lightingRows
        val table = document.createTable(rows.size + 1, 7)

        for (i in 0..rows.size) {
            // safety check if no rows exist
            if (rows.isEmpty()) {
                return
            }
            val row = table.getRow(i)
            val isFirst = i == 0
            val rowIndex = if (isFirst) 0 else i - 1
            val rowItem = rows[rowIndex]

            fun generateCell(pos: Int, text: String) {
                val cell = row.getCell(pos)
                cell.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
                val paragraph = cell.paragraphs[0]
                paragraph.alignment = ParagraphAlignment.CENTER
                val run = paragraph.createRun()
                run.fontFamily = fontGeorgia
                run.fontSize = 12
                run.isBold = isFirst
                run.setText(text)
            }

            generateCell(0, if (isFirst) "Zone" else rowItem.location)
            generateCell(1, if (isFirst) "Name" else rowItem.measureDescription)
            generateCell(2, if (isFirst) "Bulbs" else rowItem.lampsPerFixture.toString())
            generateCell(3, if (isFirst) "Fixtures" else rowItem.numberOfFixtures.toString())
            generateCell(4, if (isFirst) "Ceiling Type" else rowItem.ceilingType)
            generateCell(5, if (isFirst) "Replacement Type" else rowItem.replacementType)
            generateCell(6, if (isFirst) "Ceiling Above 10ft." else rowItem.isCeilingOver10Feet)
        }

        table.centerTable()
        table.fitTable(4500)
    }


    /**  ApachePOI setup */
    private fun setupNeededForApacheToWork() {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
    }

    // document writing
    /** writes document to dropbox and local **/
    private fun writeDocument(value: PreparedForDocument, document: XWPFDocument) {
        val calendar = Calendar.getInstance()
        val dateTime = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}T${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}.${calendar.get(Calendar.MILLISECOND)}"
        val docName = "${value.preAudit.bldgtype}_${value.preAudit.businessname}_$dateTime"

        writeDocumentWithName(document, docName)
        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/${docName}.docx")

        try {
            uploadFile(DropBox.getClient(), file, "/Gemini/Energy/$branch/Reports/$docName.docx")
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

