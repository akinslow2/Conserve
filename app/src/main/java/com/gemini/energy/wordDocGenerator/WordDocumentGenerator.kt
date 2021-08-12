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
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class WordDocumentGenerator {

    private lateinit var documents: List<XWPFDocument>

    companion object {
        //Hex #'s used for the colors
        private const val greyColor = "595959"
        private const val tableGreyColor = "D9D9D9"
        const val greenColor = "70ad47"
        private const val tableGreenColor = "469B43"
        const val blackColor = "000000"
        private const val whiteColor = "ffffff"

        const val fontAgencyFB = "Agency FB"
        private const val fontUnivers = "Univers LT Std 39 Thin UltraCn"

        const val fontTeko = "Teko"

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

        generateElectrictyCalculationPage(document, value)

        generateEnergySavingPotentialPage(document, value)

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

        if (value.lighting != null) {
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
        r1p1.fontFamily = fontTeko
        r1p1.fontSize = 28
        r1p1.color = greyColor
        r1p1.isBold = true
        r1p1.setText("Energy Audit Report")
        r1p1.addBreak()
        r1p1.setText(preAudit.businessname)
        r1p1.addBreak()
        r1p1.setText("${preAudit.auditmonth} ${preAudit.audityear}")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.08
        p2.alignment = ParagraphAlignment.CENTER
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontTeko
        r1p2.fontSize = 13
        r1p2.color = greyColor
        r1p2.setText("This report presents the results of an energy assessment conducted ${preAudit.auditmonth} ${preAudit.startday}")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontTeko
        r2p2.fontSize = 13
        r2p2.color = greyColor
        r2p2.subscript = VerticalAlign.SUPERSCRIPT
        r2p2.setText("th")
        val r3p2 = p2.createRun()
        r3p2.fontFamily = fontTeko
        r3p2.fontSize = 13
        r3p2.color = greyColor
        r3p2.setText(" through ${preAudit.auditmonth} ${preAudit.endday}")
        val r4p2 = p2.createRun()
        r4p2.fontFamily = fontTeko
        r4p2.fontSize = 13
        r4p2.color = greyColor
        r4p2.subscript = VerticalAlign.SUPERSCRIPT
        r4p2.setText("th")
        val r5p2 = p2.createRun()
        r5p2.fontFamily = fontTeko
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
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontTeko
        r1p1.isBold = true
        r1p1.fontSize = 16
        r1p1.setText("Summary of the Building’s Energy Savings Potential")
        r1p1.addBreak()

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontTeko
        r1p2.fontSize = 20
        r1p2.setText("Implementation of all recommended measures will provide an estimated annual savings of")
        r1p2.addBreak()
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontTeko
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${values.building.buildingTotalSavings.format(0)}")
        r2p2.addBreak()
        val r3p2 = p2.createRun()
        r3p2.fontFamily = fontTeko
        r3p2.fontSize = 20
        r3p2.setText("with a co-payment of $0")
        r3p2.addBreak()

        // TODO: calculate this!
        val differenceBetweenEnergySavingsAndMaintenanceSavings = "TODO"
        document.createNormalParagraph(
                "The Pay as You Save program means you will receive all the recommended upgrades at "
                        + "no cost AND see your monthly energy bill greatly reduced from pre-Covid levels. "
                        + "The utility will recover the costs by adding a charge to your bill that equates "
                        + "to the project cost paid out monthly over 12 years. This charge will last for 12 years. "
                        + "This is not a loan, the charge is only tied to the meter, and will stay on "
                        + "the meter even if you relocate. In addition, we expect business cost savings due "
                        + "to these upgrades to total $$differenceBetweenEnergySavingsAndMaintenanceSavings. "
                        + "Overall, you can expect:",
                fontSize = 14,
                fontColor = greyColor)

        val bullets1 = arrayOf(
                "Reduced energy costs,",
                "Improved profit margins,",
                "Increased thermal comfort in your building, and",
                "More control over how energy is consumed.")
        document.createBullets(bullets1, greyColor, fontSize = 14)
/*
        if (buildingValues.buildingTotalCost > 5000) {
            val qualifyP = document.createParagraph()
            qualifyP.spacingBetween = 1.5
            val qualifyR = qualifyP.createRun()
            qualifyR.fontFamily = fontTeko
            qualifyR.fontSize = 12
            qualifyR.color = greyColor
            qualifyR.setText("The implementation costs of all the measures qualify you for a 0% interest loan. By implementing these energy efficiency measures, you will join the courageous few energy heroes who are making the effort to make your community cleaner.")

        }
*/

        createEnergySavingsTable(document, values)
    }

    private fun generateHvacSavingsPage(document: XWPFDocument, hvac: HvacValues, building: BuildingValues) {
        document.createHeaderWithSavings(
                "Heating, Ventilation, and Air-Conditioning Savings",
                "Implementation of recommended HVAC measures will result in minimum annual savings of ",
                hvac.totalSavings)

        val p3 = document.createParagraph()
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontTeko
        r1p3.isBold = true
        r1p3.isItalic = true
        r1p3.fontSize = 16
        r1p3.setText("Package Unit")

        document.createNormalParagraph(
                " Make sure you are having your package units checked by a HVAC technician quarterly. "
                        + "Review you HVAC technician's maintenance scope of work it should include:")

        val hvacBullets = arrayOf(
                "Cleaning the condenser",
                "Checking the economizer (if applicable)",
                "Checking and replacing filters",
                "Charging the refrigerant",
                "Tightening belts (if applicable)")
        document.createBullets(hvacBullets)

        document.createNormalParagraph("If your HVAC technician is not checking all of these components "
                + "or you do not have an HVAC technician maintaining your units, this will result in increased "
                + "energy bills and reduce the overall life of your equipment.")

        for (instance in hvac.instances) {

            if (instance.age > 15) {
                document.createNormalParagraph(
                        "Your ${instance.quantity} ${instance.btu}-BTU HVAC package unit(s) are from ${instance.year} "
                                + "with a SEER value of ${instance.seer.format(0)}. "
                                + "They are ${instance.overage} years past their expected end of life use and are at immediate risk of failure. "
                                + "We strongly recommend replacing your package unit(s) as soon as possible. "
                                + "This investment will result in a NPV of \$${hvac.netPresentValue.format(0)}.")

                document.createNormalParagraph("Benefits of a new HVAC include:")

                val hvacBullets = arrayOf(
                        "Lower operating costs",
                        "Less risk of failure and disruption of business",
                        "More control over temperature settings",
                        "More consistent temperature throughout the space",
                        "Better for the environment"
                )
                document.createBullets(hvacBullets)
            }

            if (instance.age < 15 && instance.quantity > 1) {
                document.createNormalParagraph(
                        "Your ${instance.quantity} ${instance.btu}-BTU HVAC package units are from "
                                + "${instance.year} with a SEER value of ${instance.seer.format(0)}, "
                                + "current federal minimum is 13.")
            }

            if (instance.age < 15 && instance.quantity < 2) {
                document.createNormalParagraph(
                        "Your ${instance.quantity} ${instance.btu}-BTU HVAC package unit is from "
                                + "${instance.year} with a SEER value of ${instance.seer.format(0)}, "
                                + "current federal minimum is 13.")
            }

            if (instance.thermotype == "Analog") {
                document.createNormalParagraph(
                        "We strongly recommend installing a programmable thermostat with a remote control app. "
                                + "Installation of a programmable thermostat with remote programming will save you "
                                + "thousands of dollars over its life. Having a remote control app is important because "
                                + "the vast majority of people have their programmable thermostat programmed incorrectly. "
                                + "With a remote programmable thermostats you have the ability to easily see what your "
                                + "temperature set-points are and simply adjust them at need from your phone."
                                + "Imagine having your space comfortable when you enter without having to have the HVAC "
                                + "running the entire time you are away.")
            }
        }

        if (hvac.instances.any { i -> i.age < 15 }) {
            document.createNormalParagraph(
                    "Your HVAC system could see additional energy savings and an extended life expectancy by "
                            + "adding controls. Benefits of HVAC controls include:")

            val hvacBullets = arrayOf(
                    "Better HVAC performance",
                    "Reduced energy consumption",
                    "Reduced maintenance costs",
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
                    "Implementation of recommended water heater measures will result in minimum annual savings of ",
                    waterheater.totalSavings)
        } else {
            document.createHeaderWithNoRecommendedMeasures("Water Heating Savings")
        }

        val p3 = document.createParagraph()
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontTeko
        r1p3.isBold = true
        r1p3.isItalic = true
        r1p3.fontSize = 16
        r1p3.setText(waterheater.unittype)

        document.createNormalParagraph(
                "Replacing your water heater(s) will result in a total cost of $${waterheater.totalCost.format(0)} but will result in a minimum of $${waterheater.totalSavings.format(0)} in annual savings. " +
                        "This equates to a payback period of approximately ${waterheater.paybackMonth.format(0)} months. " +
                        "This investment will result in a NPV of $${waterheater.netPresentValue.format(0)}. ",
                addBreakAfter = true)

        createWaterHeaterTable(document, waterheater)
    }

    private fun generateLightingSavingsPage(document: XWPFDocument, lights: LightingValues) {
        document.createHeaderWithSavings(
                "Lighting Savings",
                "Implementation of recommended lighting measures will result in an estimated annual savings of",
                lights.totalcostsavings)

        document.createNormalParagraph("Benefits of LED bulbs include:")

        val bullets = arrayOf("Lower maintenance costs", "Less air-conditioning required", "Happier customers")
        document.createBullets(bullets)

        document.createNormalParagraph(
                "Replacing all of your non-LED bulbs and adding controls will cost approximately "
                        + "$${lights.totalCost.format(0)} but will result in an estimated "
                        + "minimum of $${lights.totalcostsavings.format(0)} in annual savings. "
                        + "This equates to a payback period of approximately ${lights.paybackMonth.format(0)} "
                        + "months. Alternatively, to replace all non-LED bulbs will result in a minimum of "
                        + "$${lights.totalcostsavings.format(0)} in annual savings and cost approximately $${lights.totalCost.format(0)}. "
                        + "This equates to a payback period of approximately ${lights.paybackMonth.format(0)} months. "
                        + "Overall, implementing the LED retrofit translates to a minimum NPV of $${lights.netPresentValue.format(0)}. "
                        + "We strongly recommend you replace all non-LED bulbs and ensure the bathroom lights are off at the end of the day.",
                addBreakAfter = true)

        if (lights.lightingRows.any { it.isCeilingOver10Feet.toLowerCase() == "yes" }) {
            document.createNormalParagraph(
                    "There is an additional $500 fee to replace lighting with a ceiling over 10 feet to rent the lift needed.",
                    addBreakAfter = true)
        }

        document.createNormalParagraph("Please see the next page for specifics of each lighting measure.")

        createLightingTable1(document, lights)

        document.createNormalParagraph(
                "The savings represented in the table below reflect the maintenance and cooling costs abated "
                        + "because LED bulbs have a longer life and produce considerably less heat.",
                isStartOfNewPage = true)

        createLightingTable3(document, lights)
    }

    private fun generateEquipmentSavingsPage(document: XWPFDocument, equipment: EquipmentValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontTeko
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Equipment Savings")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontTeko
        r1p2.fontSize = 18
        r1p2.setText("Implementation of recommended Equipment Savings will result in a minimum savings of ")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontTeko
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${equipment.totalSavings.format(0)}/year")

        for (equip in equipment.instances) {
            val p3 = document.createParagraph()
            val r1p3 = p3.createRun()
            r1p3.fontFamily = fontTeko
            r1p3.fontSize = 13
            r1p3.setText("Your ${equip.name} is ${equip.age.format(0)} years old and as a result is annually consuming ${equip.delta.format(0)} kWh more energy than a newer version. " +
                    "We recommend you replace the ${equip.name} with a newer version to save $${equip.costElectricity.format(0)} of dollars per year. " +
                    "The cost for a new ${equip.name} is roughly $${equip.materialCost.format(0)} with an expected payback period for this replacement is ${equip.paybackMonth.format(0)} months." +
                    "This investment will result in a NPV of \$${equip.netPresentValue.format(0)}")
            r1p3.addBreak()
        }
    }

    private fun generateRefrigerationSavingsPage(document: XWPFDocument, refrigeration: RefrigerationValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontTeko
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Refrigeration Savings")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontTeko
        r1p2.fontSize = 18
        r1p2.setText("Implementing all refrigeration measures will result in a minimum savings of  ")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontTeko
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${refrigeration.totalSavings.format(0)}.")

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontTeko
        r1p3.fontSize = 18
        r1p3.setText("The estimated cost is ${refrigeration.totalCost.format(2)}, resulting in a payback of ${refrigeration.paybackMonth.format(2)} months. " +
                "This investment will result in a NPV of $${refrigeration.netPresentValue.format(0)}")
    }

    private fun generateFacilityInformationPage(document: XWPFDocument, values: PreparedForDocument, preAudit: PreAuditValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontTeko
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Facility Information")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontTeko
        r1p2.fontSize = 13
        r1p2.setText("${preAudit.businessname}is ${preAudit.bldgarea.format(0)} square feet and includes a ${values.zoneString}. ${preAudit.businessname} is open daily from ${preAudit.operationhours}. This building is classified as ${preAudit.bldgtype}.")
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.setText("${preAudit.businessname}is on the ${preAudit.electricstructure} ${preAudit.utilitycompany} electric rate schedule. In the one-year period from $manuallyGeneratedValue through $manuallyGeneratedValue, ${preAudit.businessname} consumed $manuallyGeneratedValue kWh, resulting in an electrical cost of $$manuallyGeneratedValue.")
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.setText("${preAudit.businessname}is on the ${preAudit.gasstructure} ${preAudit.utilitycompany} gas rate schedule and for above stated time period, ${preAudit.businessname} used $manuallyGeneratedValue therms at a cost of $$manuallyGeneratedValue.")
        r1p2.addBreak()

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontTeko
        r1p3.isBold
        r1p3.fontSize = 13
        r1p3.setText("Energy Savings Disclaimer")

        val p4 = document.createParagraph()
        p4.spacingBetween = 1.5
        val r1p4 = p4.createRun()
        r1p4.fontFamily = fontTeko
        r1p4.fontSize = 13
        r1p4.setText("While electric companies and affiliates offering this program rely on industry best-practices, on-site data loggers, and energy savings modeling software to make energy savings predictions as accurate as possible," +
                " it is important to understand that the Easy Plan report is intended to inform participants of the ESTIMATED energy cost savings they should expect to realize - assuming all recommended work is done." +
                " The estimated savings reflected in the Easy Plan therefore are in no way a guarantee of the actual savings.")
        r1p4.addBreak()
        r1p4.addBreak()
        r1p4.setText("The estimated annual energy saving numbers reflected in the Easy Plan are based on the last 12 months weather and energy costs, as well as the behavior patterns during the time that " +
                "the on-site data loggers were active. Since future weather, behavior, and energy costs cannot be accurately predicted, there will be a variable difference between estimated and actual savings.")

    }

    private fun generateElectrictyCalculationPage(document: XWPFDocument, values: PreparedForDocument) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontTeko
        r1p1.isBold = true
        r1p1.fontSize = 16
        r1p1.setText("Electricity Calculations")
        r1p1.addBreak()

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontTeko
        r1p2.fontSize = 13
        r1p2.setText("Rates from https://www.roanokeelectric.com/about-us/resources/rates/")
        r1p2.addCarriageReturn()
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontTeko
        r2p2.fontSize = 13
        r2p2.setText("Building is using schedule ${values.preAudit.electricstructure}")

        generateElectrictyCalculationTable(document, values)

        document.createNormalParagraph(
                "Given current building aggregation, current total charge is "
                        + "$${values.building.currentElectricityCharge.format(2)}")

        document.createNormalParagraph(
                "Given current building aggregation, post total charge is "
                        + "$${values.building.postElectricityCharge.format(2)}")

        document.createNormalParagraph(
                "By allowing Roanoke Electric to implement these proposed changes you will see an "
                        + "estimated electricity bill savings of $${values.building.customerAnnualSavingsDuringPaybackPeriod.format(2)} "
                        + "a year. If there is a question or concern about your bill, please email us at "
                        + "roanoke@geminiEsolutions.com or call us at (252) 209-2236 or (800) 433-2236.")
    }

    private fun generateLightingContractorPage(document: XWPFDocument, lights: LightingValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontTeko
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Specifics for Lighting Contractor")

        generateLightContractorTable(document, lights)
    }

    // generate tables
    private fun createEnergySavingsTable(document: XWPFDocument, values: PreparedForDocument) {
        val table = document.createTable(15, 2)

        var currentRow = 0

        fun generateHeaderRow(cell1: String, cell2: String) {
            val row0 = table.getRow(currentRow)
            val c0r0 = row0.getCell(0)
            c0r0.color = tableGreenColor
            val pc0r0 = c0r0.paragraphs[0]
            pc0r0.alignment = ParagraphAlignment.CENTER
            val rc0r0 = pc0r0.createRun()
            rc0r0.fontFamily = fontTeko
            rc0r0.fontSize = 12
            rc0r0.isBold = true
            rc0r0.color = whiteColor
            rc0r0.setText(cell1)

            val c1r0 = row0.getCell(1)
            c1r0.color = tableGreenColor
            val pc1r0 = c1r0.paragraphs[0]
            pc1r0.alignment = ParagraphAlignment.CENTER
            val rc1r0 = pc1r0.createRun()
            rc1r0.fontFamily = fontTeko
            rc1r0.fontSize = 12
            rc1r0.isBold = true
            rc1r0.color = whiteColor
            rc1r0.setText(cell2)

            currentRow += 1
        }

        fun generateInfoRow(cell1: String, cell2: String) {
            val row = table.getRow(currentRow)
            val cell0 = row.getCell(0)
            val para0 = cell0.paragraphs[0]
            val para0run = para0.createRun()
            para0run.fontFamily = fontTeko
            para0run.fontSize = 12
            para0run.isBold = true
            para0run.setText(cell1)

            val cell1 = row.getCell(1)
            val para1 = cell1.paragraphs[0]
            para1.alignment = ParagraphAlignment.CENTER
            val para1run = para1.createRun()
            para1run.fontSize = 12
            para1run.fontFamily = fontTeko
            para1run.setText(cell2)

            currentRow += 1
        }

        // Header row
        generateHeaderRow("Financial Components", "Financial Overview")

        generateInfoRow(
                "Financial Overview",
                "\$${values.building.buildingTotalSavings.format(0)}")

        generateInfoRow(
                "Implementation Cost (\$)",
                "\$${values.building.buildingTotalCost.format(0)}")

        generateInfoRow(
                "    Lighting (\$)",
                "\$${values.lighting?.totalCost?.format(0) ?: "N/A"}")

        generateInfoRow(
                "    HVAC (\$)",
                "\$${values.building.hvacTotalCost.format(0)}")

        generateInfoRow(
                "    Plug Load (\$)",
                "\$${values.building.equipmentTotalCost.format(0)}")

        // seperator
        val separatorRow = table.getRow(currentRow)
        separatorRow.getCell(0).color = tableGreyColor
        separatorRow.getCell(1).color = tableGreyColor
        currentRow += 1

        generateInfoRow(
                "Net Present Value (NPV)",
                values.building.buildingNetPresentValue.format(0))

        generateInfoRow(
                "    Lighting (NVP)",
                values.lighting?.netPresentValue?.format(0) ?: "N/A")

        generateInfoRow(
                "    HVAC (NPV)",
                values.hvac?.netPresentValue?.format(0) ?: "N/A")

        generateInfoRow(
                "    Water Heater (NPV)",
                values.waterHeater?.netPresentValue?.format(0) ?: "N/A")

        generateInfoRow(
                "    Refrigeration (NPV)",
                values.refrigeration?.netPresentValue?.format(0) ?: "N/A")

        generateInfoRow(
                "    Motors (NPV)",
                values.motor?.netPresentValue?.format(0) ?: "N/A")

        generateInfoRow(
                "    Other Equipment (NPV)",
                values.equipment?.netPresentValue?.format(0) ?: "N/A")

        table.fitTable(4500)
        table.centerTable()
    }

    private fun createLightingTable1(document: XWPFDocument, lights: LightingValues) {
        val table = document.createTable(6, 2)
        val row0 = table.getRow(0)

        val cell0r0 = row0.getCell(0)
        cell0r0.color = tableGreenColor
        val pcell0r0 = cell0r0.paragraphs[0]
        pcell0r0.alignment = ParagraphAlignment.CENTER
        val rcell0r0 = cell0r0.paragraphs[0].createRun()
        rcell0r0.fontFamily = fontTeko
        rcell0r0.fontSize = 12
        rcell0r0.color = whiteColor
        rcell0r0.isBold = true
        rcell0r0.setText("Financial Components")

        val cell1r0 = row0.getCell(1)
        cell1r0.color = tableGreenColor
        val pcell1ro = cell1r0.paragraphs[0]
        pcell1ro.alignment = ParagraphAlignment.CENTER
        val rcell1r0 = cell1r0.paragraphs[0].createRun()
        rcell1r0.fontFamily = fontTeko
        rcell1r0.fontSize = 12
        rcell1r0.color = whiteColor
        rcell1r0.isBold = true
        rcell1r0.setText("Financial Overview")

        val row1 = table.getRow(1)

        val c0r1 = row1.getCell(0)
        val rc0r1 = c0r1.paragraphs[0].createRun()
        rc0r1.fontFamily = fontTeko
        rc0r1.isBold = true
        rc0r1.fontSize = 12
        rc0r1.setText("Minimum Electrical Cost Savings ($/yr)")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = c1r1.paragraphs[0].createRun()
        rc1r1.fontFamily = fontTeko
        rc1r1.fontSize = 12
        rc1r1.setText("\$${lights.totalcostsavings.format(0)}")

        val row2 = table.getRow(2)

        val c0r2 = row2.getCell(0)
        val rc0r2 = c0r2.paragraphs[0].createRun()
        rc0r2.fontFamily = fontTeko
        rc0r2.fontSize = 12
        rc0r2.isBold = true
        rc0r2.setText("Implementation Cost ($)")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontTeko
        rc1r2.fontSize = 12
        rc1r2.setText("\$${lights.totalCost}")

        val row3 = table.getRow(3)

        val c0r3 = row3.getCell(0)
        val rc0r3 = c0r3.paragraphs[0].createRun()
        rc0r3.fontFamily = fontTeko
        rc0r3.fontSize = 12
        rc0r3.isBold = true
        rc0r3.setText("Payback")

        val row4 = table.getRow(4)

        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        pc0r4.alignment = ParagraphAlignment.RIGHT
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontSize = 12
        rc0r4.fontFamily = fontTeko
        rc0r4.setText("Year")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontSize = 12
        rc1r4.fontFamily = fontTeko
        rc1r4.setText(lights.paybackYear.format(2))

        val row5 = table.getRow(5)

        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        pc0r5.alignment = ParagraphAlignment.RIGHT
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontTeko
        rc0r5.fontSize = 12
        rc0r5.setText("Month")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontTeko
        rc1r5.fontSize = 12
        rc1r5.setText(lights.paybackMonth.format(0))

        table.centerTable()
        table.fitTable(4000)
    }

    private fun createLightingTable3(document: XWPFDocument, lights: LightingValues) {
        val rows = lights.lightingRows
        val table = document.createTable(rows.size + 1, 11)

        for (i in 0..rows.size) {
            // safety check if no rows exist
            if (rows.isEmpty()) {
                return
            }

            val row = table.getRow(i)
            val isFirst = i == 0
            val rowIndex = if (isFirst) 0 else i - 1
            val rowItem = rows[rowIndex]

            val cell0 = row.getCell(0)
            cell0.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell0 = cell0.paragraphs[0]
            pcell0.alignment = ParagraphAlignment.CENTER
            val rcell0 = pcell0.createRun()
            rcell0.fontFamily = fontTeko
            rcell0.fontSize = 12
            rcell0.setText(if (isFirst) "Measure" else rowItem.measure)

            val cell1 = row.getCell(1)
            cell1.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell1 = cell1.paragraphs[0]
            pcell1.alignment = ParagraphAlignment.CENTER
            val rcell1 = pcell1.createRun()
            rcell1.fontFamily = fontTeko
            rcell1.fontSize = 12
            rcell1.setText(if (isFirst) "Location" else rowItem.location)

            val cell2 = row.getCell(2)
            cell2.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell2 = cell2.paragraphs[0]
            pcell2.alignment = ParagraphAlignment.CENTER
            val rcell2 = pcell2.createRun()
            rcell2.fontFamily = fontTeko
            rcell2.fontSize = 12
            rcell2.setText(if (isFirst) "Measure Description" else rowItem.measureDescription)

            val cell3 = row.getCell(3)
            cell3.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell3 = cell3.paragraphs[0]
            pcell3.alignment = ParagraphAlignment.CENTER
            val rcell3 = pcell3.createRun()
            rcell3.fontFamily = fontTeko
            rcell3.fontSize = 12
            rcell3.setText(if (isFirst) "Quantity" else rowItem.quantity.format(0))

            val cell4 = row.getCell(4)
            cell4.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell4 = cell4.paragraphs[0]
            pcell4.alignment = ParagraphAlignment.CENTER
            val rcell4 = pcell4.createRun()
            rcell4.fontFamily = fontTeko
            rcell4.fontSize = 12
            rcell4.setText((if (isFirst) "Current Power (kW)" else rowItem.currentPowerkW.format(2)))

            val cell5 = row.getCell(5)
            cell5.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell5 = cell5.paragraphs[0]
            pcell5.alignment = ParagraphAlignment.CENTER
            val rcell5 = pcell5.createRun()
            rcell5.fontFamily = fontTeko
            rcell5.fontSize = 12
            rcell5.setText(if (isFirst) "Usage (hrs)" else rowItem.usageHours.format(0))

            val cell6 = row.getCell(6)
            cell6.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell6 = cell6.paragraphs[0]
            pcell6.alignment = ParagraphAlignment.CENTER
            val rcell6 = pcell6.createRun()
            rcell6.fontFamily = fontTeko
            rcell6.fontSize = 12
            rcell6.setText(if (isFirst) "Post Power (kW)" else rowItem.postPowerkW.format(2))

            val cell7 = row.getCell(7)
            cell7.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell7 = cell7.paragraphs[0]
            pcell7.alignment = ParagraphAlignment.CENTER
            val rcell7 = pcell7.createRun()
            rcell7.fontFamily = fontTeko
            rcell7.fontSize = 12
            rcell7.setText(if (isFirst) "Post Usage (hrs)" else rowItem.postUsageHours.format(0))

            val cell8 = row.getCell(8)
            cell8.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell8 = cell8.paragraphs[0]
            pcell8.alignment = ParagraphAlignment.CENTER
            val rcell8 = pcell8.createRun()
            rcell8.fontFamily = fontTeko
            rcell8.fontSize = 12
            rcell8.setText(if (isFirst) "Energy Savings (kWh)" else rowItem.energySavingskWh.format(0))

            val cell9 = row.getCell(9)
            cell9.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell9 = cell9.paragraphs[0]
            pcell9.alignment = ParagraphAlignment.CENTER
            val rcell9 = pcell9.createRun()
            rcell9.fontFamily = fontTeko
            rcell9.fontSize = 12
            rcell9.setText(if (isFirst) "Life Cost Savings" else "$${rowItem.netPresentValue.format(0)}")

            val cell10 = row.getCell(10)
            cell10.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell10 = cell10.paragraphs[0]
            pcell10.alignment = ParagraphAlignment.CENTER
            val rcell10 = pcell10.createRun()
            rcell10.fontFamily = fontTeko
            rcell10.fontSize = 12
            rcell10.setText(if (isFirst) "Implementation Cost" else "$${rowItem.implementationCost.format(0)}")
        }

        table.centerTable()
    }

    private fun createHvacTable(document: XWPFDocument, hvac: HvacValues, building: BuildingValues) {
        val table = document.createTable(6, 2)
        val row0 = table.getRow(0)

        val cell0r0 = row0.getCell(0)
        cell0r0.color = tableGreenColor
        val pcellr0 = cell0r0.paragraphs[0]
        pcellr0.alignment = ParagraphAlignment.CENTER
        val rcell0r0 = cell0r0.paragraphs[0].createRun()
        rcell0r0.fontFamily = fontTeko
        rcell0r0.fontSize = 12
        rcell0r0.color = whiteColor
        rcell0r0.isBold = true
        rcell0r0.setText("Financial Components")

        val cell1r0 = row0.getCell(1)
        cell1r0.color = tableGreenColor
        val pcell1r0 = cell1r0.paragraphs[0]
        pcell1r0.alignment = ParagraphAlignment.CENTER
        val rcell1r0 = cell1r0.paragraphs[0].createRun()
        rcell1r0.fontFamily = fontTeko
        rcell1r0.fontSize = 12
        rcell1r0.color = whiteColor
        rcell1r0.isBold = true
        rcell1r0.setText("Financial Overview")

        // row 1
        val row1 = table.getRow(1)

        val c0r1 = row1.getCell(0)
        val rc0r1 = c0r1.paragraphs[0].createRun()
        rc0r1.fontFamily = fontTeko
        rc0r1.fontSize = 12
        rc0r1.isBold = true
        rc0r1.setText("Total Electrical Cost Savings ($/yr)")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = c1r1.paragraphs[0].createRun()
        rc1r1.fontFamily = fontTeko
        rc1r1.fontSize = 12
        rc1r1.setText("\$${hvac.totalSavings.format(0)}")

        val row2 = table.getRow(2)

        val c0r2 = row2.getCell(0)
        val rc0r2 = c0r2.paragraphs[0].createRun()
        rc0r2.fontFamily = fontTeko
        rc0r2.isBold = true
        rc0r2.fontSize = 12
        rc0r2.setText("Implementation Cost ($)")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontTeko
        rc1r2.fontSize = 12
        rc1r2.setText("\$${hvac.totalCost.format(0)}")

        val row3 = table.getRow(3)

        val c0r3 = row3.getCell(0)
        val rc0r3 = c0r3.paragraphs[0].createRun()
        rc0r3.fontFamily = fontTeko
        rc0r3.fontSize = 12
        rc0r3.isBold = true
        rc0r3.setText("Payback")

        val row4 = table.getRow(4)

        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        pc0r4.alignment = ParagraphAlignment.RIGHT
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontFamily = fontTeko
        rc0r4.fontSize = 12
        rc0r4.setText("Year")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontFamily = fontTeko
        rc1r4.fontSize = 12
        rc1r4.setText(building.hvacPaybackYear.format(2))

        val row5 = table.getRow(5)

        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        pc0r5.alignment = ParagraphAlignment.RIGHT
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontTeko
        rc0r5.fontSize = 12
        rc0r5.setText("Month")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontTeko
        rc1r5.fontSize = 12
        rc1r5.setText(building.hvacPaybackMonth.format(0))

        table.centerTable()
        table.fitTable(4000)
    }

    private fun createWaterHeaterTable(document: XWPFDocument, waterheater: WaterHeaterValues) {
        val table = document.createTable(6, 2)
        val row0 = table.getRow(0)

        val cell0r0 = row0.getCell(0)
        cell0r0.color = tableGreenColor
        val pcellr0 = cell0r0.paragraphs[0]
        pcellr0.alignment = ParagraphAlignment.CENTER
        val rcell0r0 = cell0r0.paragraphs[0].createRun()
        rcell0r0.fontFamily = fontTeko
        rcell0r0.color = whiteColor
        rcell0r0.fontSize = 12
        rcell0r0.isBold = true
        rcell0r0.setText("Financial Components")

        val cell1r0 = row0.getCell(1)
        cell1r0.color = tableGreenColor
        val pcell1r0 = cell1r0.paragraphs[0]
        pcell1r0.alignment = ParagraphAlignment.CENTER
        val rcell1r0 = cell1r0.paragraphs[0].createRun()
        rcell1r0.fontFamily = fontTeko
        rcell1r0.fontSize = 12
        rcell1r0.color = whiteColor
        rcell1r0.isBold = true
        rcell1r0.setText("Financial Overview")

        // row 1
        val row1 = table.getRow(1)

        val c0r1 = row1.getCell(0)
        val rc0r1 = c0r1.paragraphs[0].createRun()
        rc0r1.fontFamily = fontTeko
        rc0r1.fontSize = 12
        rc0r1.isBold = true
        rc0r1.setText("Total Electrical Cost Savings ($/yr)")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = c1r1.paragraphs[0].createRun()
        rc1r1.fontFamily = fontTeko
        rc1r1.fontSize = 12
        rc1r1.setText("\$${waterheater.totalSavings.format(0)}")

        val row2 = table.getRow(2)

        val c0r2 = row2.getCell(0)
        val rc0r2 = c0r2.paragraphs[0].createRun()
        rc0r2.fontFamily = fontTeko
        rc0r2.fontSize = 12
        rc0r2.isBold = true
        rc0r2.setText("Implementation Cost ($)")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontTeko
        rc1r2.fontSize = 12
        rc1r2.setText("\$${waterheater.totalCost.format(0)}")

        val row3 = table.getRow(3)

        val c0r3 = row3.getCell(0)
        val rc0r3 = c0r3.paragraphs[0].createRun()
        rc0r3.fontFamily = fontTeko
        rc0r3.fontSize = 12
        rc0r3.isBold = true
        rc0r3.setText("Payback")

        val row4 = table.getRow(4)

        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        pc0r4.alignment = ParagraphAlignment.RIGHT
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontFamily = fontTeko
        rc0r4.fontSize = 12
        rc0r4.setText("Year")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontFamily = fontTeko
        rc1r4.fontSize = 12
        rc1r4.setText(waterheater.paybackYear.format(2))

        val row5 = table.getRow(5)

        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        pc0r5.alignment = ParagraphAlignment.RIGHT
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontTeko
        rc0r5.fontSize = 12
        rc0r5.setText("Month")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontTeko
        rc1r5.fontSize = 12
        rc1r5.setText(waterheater.paybackMonth.format(0))

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
            run0.fontFamily = fontTeko
            run0.fontSize = 12
            run0.setText(cell0Text)

            val cell1 = row.getCell(1)
            val para1 = cell1.paragraphs[0]
            val run1 = para1.createRun()
            run1.fontFamily = fontTeko
            run1.fontSize = 12
            run1.setText(cell1Text)

            val cell2 = row.getCell(2)
            val para2 = cell2.paragraphs[0]
            val run2 = para2.createRun()
            run2.fontFamily = fontTeko
            run2.fontSize = 12
            run2.setText(cell2Text)

            val cell3 = row.getCell(3)
            val para3 = cell3.paragraphs[0]
            val run3 = para3.createRun()
            run3.fontFamily = fontTeko
            run3.fontSize = 12
            run3.setText(cell3Text)

            val cell4 = row.getCell(4)
            val para4 = cell4.paragraphs[0]
            val run4 = para4.createRun()
            run4.fontFamily = fontTeko
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
                "Other Equipment",
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
        table.fitTable(4500)
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
                run.fontFamily = fontTeko
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

