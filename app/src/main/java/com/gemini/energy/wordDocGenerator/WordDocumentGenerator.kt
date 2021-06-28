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

    //Hex #'s used for the colors
    private val greyColor = "595959"
    private val tableGreyColor = "D9D9D9"
    private val greenColor = "70ad47"
    private val tableGreenColor = "469B43"
    private val blackColor = "000000"
    private val whiteColor = "ffffff"

    private val fontAgencyFB = "Agency FB"
    private val fontUnivers = "Univers LT Std 39 Thin UltraCn"

    private val manuallyGeneratedValue = "INSERTME"


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

        generateEnergySavingPotentialPage(
                document,
                value.lighting
                        ?: LightingValues(
                                0.0,
                                0.0,
                                0,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                0.0, listOf(),
                                0.0,
                                0.0,
                                0.0,
                                0.0),
                value.building)

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

        addPageNumbers(document)

        writeDocument(value, document)

        return document
    }

    // generate pages
    private fun generateFirstPage(document: XWPFDocument, preAudit: PreAuditValues) {
        val p1 = document.createParagraph()
        p1.alignment = ParagraphAlignment.CENTER
        p1.spacingBetween = 1.5
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontUnivers
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
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 12
        r1p2.color = greyColor
        r1p2.setText("This report presents the results of an energy assessment conducted ${preAudit.auditmonth} ${preAudit.startday}")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.fontSize = 12
        r2p2.color = greyColor
        r2p2.subscript = VerticalAlign.SUPERSCRIPT
        r2p2.setText("th")
        val r3p2 = p2.createRun()
        r3p2.fontFamily = fontAgencyFB
        r3p2.fontSize = 12
        r3p2.color = greyColor
        r3p2.setText(" through ${preAudit.auditmonth} ${preAudit.endday}")
        val r4p2 = p2.createRun()
        r4p2.fontFamily = fontAgencyFB
        r4p2.fontSize = 12
        r4p2.color = greyColor
        r4p2.subscript = VerticalAlign.SUPERSCRIPT
        r4p2.setText("th")
        val r5p2 = p2.createRun()
        r5p2.fontFamily = fontAgencyFB
        r5p2.fontSize = 12
        r5p2.color = greyColor
        r5p2.setText(" and evaluation of potential improvements to reduce energy consumption and related operating cost at ")
        r5p2.addBreak()
        r5p2.addBreak()
        r5p2.setText(preAudit.businessname)
        r5p2.addBreak()
        r5p2.setText(preAudit.clientaddress)
    }

    private fun generateEnergySavingPotentialPage(document: XWPFDocument, lightingData: LightingValues, buildingValues: BuildingValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.fontSize = 16
        r1p1.setText("Summary of the Building’s Energy Savings Potential")
        r1p1.addBreak()

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 20
        r1p2.setText("Implementation of all recommended measures will provide annual savings of")
        r1p2.addBreak()
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${buildingValues.buildingTotalSavings.format(0)}")
        r2p2.addBreak()
        val r3p2 = p2.createRun()
        r3p2.fontFamily = fontAgencyFB
        r3p2.fontSize = 20
        r3p2.setText("with a co-payment of $0")
        r3p2.addBreak()

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.fontSize = 12
        r1p3.color = greyColor
        r1p3.setText("The Pay as You Save program means you will receive all of the recommended upgrades at no cost AND see your monthly energy bill reduce at least 20% ($manuallyGeneratedValue)." +
                " The utility will recover the costs by adding a charge to your bill that equates to 80% of the estimated monthly energy savings. This charge will last for 12 years." +
                " This is not a loan, the charge is only tied to the meter, and will stay on the meter even if you relocate." +
                " In addition, we expect business cost savings due to these upgrades to total ($manuallyGeneratedValue)." +
                " Overall, you can expect:")

        val bullets1 = arrayOf("reduced energy costs,", "improved profit margins,", "increased thermal comfort in your building, and", "more control over how energy is consumed.")
        createBullets(document, bullets1, 12, greyColor, false, false)
/*
        if (buildingValues.buildingTotalCost > 5000) {
            val qualifyP = document.createParagraph()
            qualifyP.spacingBetween = 1.5
            val qualifyR = qualifyP.createRun()
            qualifyR.fontFamily = fontAgencyFB
            qualifyR.fontSize = 12
            qualifyR.color = greyColor
            qualifyR.setText("The implementation costs of all the measures qualify you for a 0% interest loan. By implementing these energy efficiency measures, you will join the courageous few energy heroes who are making the effort to make your community cleaner.")

        }
*/
        createEnergySavingsTable(document, lightingData, buildingValues)
    }

    private fun generateHvacSavingsPage(document: XWPFDocument, hvac: HvacValues, building: BuildingValues) {
        val p1 = document.createParagraph()
        p1.isPageBreak = true
        p1.spacingBetween = 1.5
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Heating, Ventilation, and Air-Conditioning Savings")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 18
        r1p2.setText("Implementation of recommended HVAC measures will result in minimum annual savings of ")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${hvac.totalSavings.format(0)}")

        val p3 = document.createParagraph()
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.isBold = true
        r1p3.isItalic = true
        r1p3.fontSize = 16
        r1p3.setText("Package Unit")

        for (instance in hvac.instances) {

            if (instance.age > 15) {
                val hvacP1 = document.createParagraph()
                hvacP1.spacingBetween = 1.5
                val hvacR1P1 = hvacP1.createRun()
                hvacR1P1.fontFamily = fontAgencyFB
                hvacR1P1.fontSize = 12
                hvacR1P1.setText("Your ${instance.quantity} ${instance.btu}-BTU HVAC package unit(s) are from ${instance.year} with a SEER value of ${instance.seer.format(0)}. " +
                        "They are ${instance.overage} years past their expected end of life use and are at immediate risk of failure. " +
                        "We strongly recommend replacing your package unit(s) as soon as possible. " +
                        "This investment will result in a NPV of \$${hvac.netPresentValue.format(0)}.")

                val hvacP2 = document.createParagraph()
                hvacP2.spacingBetween = 1.5
                val hvacR1P2 = hvacP2.createRun()
                hvacR1P2.fontFamily = fontAgencyFB
                hvacR1P2.fontSize = 12
                hvacR1P2.setText("Benefits of a new HVAC include:")

                val hvacBullets = arrayOf(
                        "Lower operating costs",
                        "Less risk of failure and disruption of business",
                        "More control over temperature settings",
                        "More consistent temperature throughout the space",
                        "Better for the environment"
                )
                createBullets(document, hvacBullets, 12, blackColor, false, false)
            }
            if (instance.age < 15 && instance.quantity > 1) {
                val hvacP1 = document.createParagraph()
                hvacP1.spacingBetween = 1.5
                val hvacR1P1 = hvacP1.createRun()
                hvacR1P1.fontFamily = fontAgencyFB
                hvacR1P1.fontSize = 12
                hvacR1P1.setText("Your ${instance.quantity} ${instance.btu}-BTU HVAC package units are from ${instance.year} with a SEER value of ${instance.seer.format(0)}, current federal minimum is 13." +
                        " Make sure you are having your package units checked by a HVAC technician quarterly. Review you HVAC technician's maintenance scope of work it should include:")

                val hvacBullets = arrayOf(
                        "Cleaning the condenser",
                        "Checking the economizer (if applicable)",
                        "Checking and replacing filters",
                        "Charging the refrigerant",
                        "Tightening belts (if applicable)"
                )
                createBullets(document, hvacBullets, 12, blackColor, false, false)

                val hvacP2 = document.createParagraph()
                hvacP2.spacingBetween = 1.5
                val hvacR1P2 = hvacP2.createRun()
                hvacR1P2.fontFamily = fontAgencyFB
                hvacR1P2.fontSize = 12
                hvacR1P2.setText("If your HVAC technician is not checking all of these components or you do not have an HVAC technician maintaining your units, this will result in increased energy bills and reduce the overall life of your equipment.")
            }
            if (instance.age < 15 && instance.quantity < 2) {
                val hvacP1 = document.createParagraph()
                hvacP1.spacingBetween = 1.5
                val hvacR1P1 = hvacP1.createRun()
                hvacR1P1.fontFamily = fontAgencyFB
                hvacR1P1.fontSize = 12
                hvacR1P1.setText("Your ${instance.quantity} ${instance.btu}-BTU HVAC package unit is from ${instance.year} with a SEER value of ${instance.seer.format(0)}, current federal minimum is 13." +
                        " Make sure you are having the package unit checked by a HVAC technician quarterly. Review your HVAC technician's maintenance scope of work it should include:")

                val hvacBullets = arrayOf(
                        "Cleaning the condenser",
                        "Checking the economizer (if applicable)",
                        "Checking and replacing filters",
                        "Charging the refrigerant",
                        "Tightening belts (if applicable)"
                )
                createBullets(document, hvacBullets, 12, blackColor, false, false)

                val hvacP2 = document.createParagraph()
                hvacP2.spacingBetween = 1.5
                val hvacR1P2 = hvacP2.createRun()
                hvacR1P2.fontFamily = fontAgencyFB
                hvacR1P2.fontSize = 12
                hvacR1P2.setText("If your HVAC technician is not checking all of these components or you do not have an HVAC technician maintaining your unit, this will result in increased energy bills and reduce the overall life of your equipment.")
            }
            if (instance.thermotype == "Analog"){
                val hvacP3 = document.createParagraph()
                hvacP3.spacingBetween = 1.5
                val hvacR1P3 = hvacP3.createRun()
                hvacR1P3.fontFamily = fontAgencyFB
                hvacR1P3.fontSize = 12
                hvacR1P3.setText("We strongly recommend installing a programmable thermostat with a remote control app." +
                        " Installation of a programmable thermostat with remote programming will save you thousands of dollars over its life." +
                        " Having a remote control app is important because the vast majority of people have their programmable thermostat programmed incorrectly." +
                        " With a remote programmable thermostats you have the ability to easily see what your temperature set-points are and simply adjust them at need from your phone." +
                        "Imagine having your space comfortable when you enter without having to have the HVAC running the entire time you are away.")
            }
        }

        if (hvac.instances.any { i -> i.age < 15 }) {
            val hvacP3 = document.createParagraph()
            hvacP3.spacingBetween = 1.5
            val hvacR1P3 = hvacP3.createRun()
            hvacR1P3.fontFamily = fontAgencyFB
            hvacR1P3.fontSize = 12
            hvacR1P3.setText("Your HVAC system could see additional energy savings and an extended life expectancy by adding controls. Benefits of HVAC controls include:")

            val hvacBullets = arrayOf(
                    "Better HVAC performance",
                    "Reduced energy consumption",
                    "Reduced maintenance costs",
                    "More control over temperature settings",
                    "Better for the environment")
            createBullets(document, hvacBullets, 12, blackColor, false, false)
        }

        createHvacTable(document, hvac, building)
    }

    private fun generateWaterHeaterSavingsPage(document: XWPFDocument, waterheater: WaterHeaterValues) {
        val p1 = document.createParagraph()
        p1.isPageBreak = true
        p1.spacingBetween = 1.5
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Water Heating Savings")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 18
        r1p2.setText("Implementation of recommended water heater measures will result in minimum annual savings of ")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${waterheater.totalSavings.format(0)}")

        val p3 = document.createParagraph()
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.isBold = true
        r1p3.isItalic = true
        r1p3.fontSize = 16
        r1p3.setText("${waterheater.unittype}")

        val p4 = document.createParagraph()
        p4.spacingBetween = 1.5
        val r1p4 = p4.createRun()
        r1p4.fontFamily = fontAgencyFB
        r1p4.fontSize = 12
        r1p4.setText("Replacing your water heater(s) will result in a total cost of $${waterheater.totalCost.format(0)} but will result in a minimum of $${waterheater.totalSavings.format(0)} in annual savings. " +
                "This equates to a payback period of approximately ${waterheater.paybackMonth.format(0)} months. " +
                "This investment will result in a NPV of $${waterheater.netPresentValue.format(0)}. ")
        r1p4.addBreak()

        createWaterHeaterTable(document, waterheater)
    }

    private fun generateLightingSavingsPage(document: XWPFDocument, lights: LightingValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 16
        r1p1.setText("Lighting Savings")
        r1p1.addBreak()

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 18
        r1p2.setText("Implementation of recommended lighting measures will result in annual savings of")
        r1p2.addBreak()
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${lights.totalcostsavings.format(0)}")
        r2p2.addBreak()

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.fontSize = 12
        r1p3.setText("Benefits of LED bulbs include:")

        val bullets = arrayOf("Lower maintenance costs", "Less air-conditioning required", "Happier customers")
        createBullets(document, bullets, 12, blackColor, false, false)

        val p4 = document.createParagraph()
        p4.spacingBetween = 1.5
        val r1p4 = p4.createRun()
        r1p4.fontFamily = fontAgencyFB
        r1p4.fontSize = 12
        r1p4.setText("Replacing all of your non-LED bulbs and adding controls will cost approximately $${lights.totalCost.format(0)} but will result in a minimum of $${lights.totalcostsavings.format(0)} in annual savings. " +
                "This equates to a payback period of approximately ${lights.paybackMonth.format(0)} months. The cost can be reduced to $${lights.selfinstallcost} if you self-install the occupancy sensors. " +
                "Alternatively, to replace all non-LED bulbs will result in a minimum of $${lights.totalcostsavings.format(0)} in annual savings and cost approximately $${lights.totalCost.format(0)}. " +
                "This equates to a payback period of approximately ${lights.paybackMonth.format(0)} months. " +
                "Overall, implementing the LED retrofit translates to a minimum NPV of $${lights.netPresentValue.format(0)}. We strongly recommend you replace all non-LED bulbs and ensure the bathroom lights are off at the end of the day.")
        r1p4.addBreak()

        val p5 = document.createParagraph()
        p5.spacingBetween = 1.5
        val r1p5 = p5.createRun()
        r1p5.fontFamily = fontAgencyFB
        r1p5.fontSize = 12
        r1p5.setText("Please see the next page for specifics of each lighting measure.")

        createLightingTable1(document, lights)

        val p6 = document.createParagraph()
        p6.spacingBetween = 1.5
        p6.isPageBreak = true
        val r1p6 = p6.createRun()
        r1p6.fontFamily = fontAgencyFB
        r1p6.fontSize = 12
        r1p6.setText("The savings represented in the table below reflect the maintenance and cooling costs abated because LED bulbs have a longer life and produce considerably less heat.")

        createLightingTable3(document, lights)
    }

    private fun generateEquipmentSavingsPage(document: XWPFDocument, equipment: EquipmentValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Equipment Savings")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 18
        r1p2.setText("Implementation of recommended Equipment Savings will result in a minimum savings of ")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${equipment.totalSavings.format(0)}/year")

        for (equip in equipment.instances) {
            val p3 = document.createParagraph()
            val r1p3 = p3.createRun()
            r1p3.fontFamily = fontAgencyFB
            r1p3.fontSize = 13
            r1p3.setText("Your ${equip.name} is ${equip.age.format(0)} years old and as a result is annually consuming ${equip.delta.format(0)} kWh more energy than a newer version. " +
                    "We recommend you replace the ${equip.name} with a newer version to save $${equip.costElectricity.format(0)} of dollars per year. " +
                    "The cost for a new ${equip.name} is roughly $${equip.materialCost.format(0)} with an expected payback period for this replacement is ${equip.paybackMonth.format(0)} months." +
                    "This investment will result in a NPV of \$${equipment.netPresentValue.format(0)}")
            r1p3.addBreak()
        }
    }

    private fun generateRefrigerationSavingsPage(document: XWPFDocument, refrigeration: RefrigerationValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Refrigeration Savings")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 18
        r1p2.setText("Implementing all refrigeration measures will result in a minimum savings of  ")
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.isBold = true
        r2p2.fontSize = 24
        r2p2.color = greenColor
        r2p2.setText("$${refrigeration.totalSavings.format(0)}.")

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.fontSize = 18
        r1p3.setText("The estimated cost is ${refrigeration.totalCost.format(2)}, resulting in a payback of ${refrigeration.paybackMonth.format(2)} months. " +
                "This investment will result in a NPV of $${refrigeration.netPresentValue.format(0)}")
    }

    private fun generateFacilityInformationPage(document: XWPFDocument, values: PreparedForDocument, preAudit: PreAuditValues) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.isItalic = true
        r1p1.fontSize = 20
        r1p1.setText("Facility Information")

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.fontSize = 12
        r1p2.setText("${preAudit.businessname}is ${preAudit.bldgarea.format(0)} square feet and includes a ${values.zoneString}. ${preAudit.businessname} is open daily from ${preAudit.operationhours}. This building is classified as ${preAudit.bldgtype}.")
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.setText("${preAudit.businessname}is on the ${preAudit.electricstructure} ${preAudit.utilitycompany} electric rate schedule. In the one-year period from $manuallyGeneratedValue through $manuallyGeneratedValue, ${preAudit.businessname} consumed $manuallyGeneratedValue kWh, resulting in an electrical cost of $$manuallyGeneratedValue.")
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.setText("${preAudit.businessname}is on the ${preAudit.gasstructure} ${preAudit.utilitycompany} gas rate schedule and for above stated time period, ${preAudit.businessname} used $manuallyGeneratedValue therms at a cost of $$manuallyGeneratedValue.")
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.addBreak()
        r1p2.addBreak()

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.isBold
        r1p3.fontSize = 12
        r1p3.setText("Energy Savings Disclaimer")

        val p4 = document.createParagraph()
        p4.spacingBetween = 1.5
        val r1p4 = p4.createRun()
        r1p4.fontFamily = fontAgencyFB
        r1p4.fontSize = 12
        r1p4.setText("While electric companies and affiliates offering this program rely on industry best-practices, on-site data loggers, and energy savings modeling software to make energy savings predictions as accurate as possible," +
                " it is important to understand that the Easy Plan report is intended to inform participants of the ESTIMATED energy cost savings they should expect to realize - assuming all recommended work is done." +
                " The estimated savings reflected in the Easy Plan therefore are in no way a guarantee of the actual savings.")
        r1p4.addBreak()
        r1p4.addBreak()
        r1p4.setText("The estimated annual energy saving numbers reflected in the Easy Plan are based on the last 12 months weather and energy costs, as well as the behavior patterns during the time that " +
                "the on-site data loggers were active. Since future weather, behavior, and energy costs cannot be accurately predicted, there will be a variable difference between estimated and actual savings.")

    }

    // @Anthony this function generates a page in the word doc that displays the latest calculations you requested
    private fun generateElectrictyCalculationPage(document: XWPFDocument, values: PreparedForDocument) {
        val p1 = document.createParagraph()
        p1.spacingBetween = 1.5
        p1.isPageBreak = true
        val r1p1 = p1.createRun()
        r1p1.fontFamily = fontAgencyFB
        r1p1.isBold = true
        r1p1.fontSize = 16
        r1p1.setText("Electricity Calculations")
        r1p1.addBreak()

        val p2 = document.createParagraph()
        p2.spacingBetween = 1.5
        val r1p2 = p2.createRun()
        r1p2.fontFamily = fontAgencyFB
        r1p2.setText("Rates from https://www.roanokeelectric.com/about-us/resources/rates/")
        r1p2.addCarriageReturn()
        val r2p2 = p2.createRun()
        r2p2.fontFamily = fontAgencyFB
        r2p2.setText("Building is using schedule ${values.preAudit.electricstructure}")

        generateElectrictyCalculationTable(document, values)

        val p3 = document.createParagraph()
        p3.spacingBetween = 1.5
        val r1p3 = p3.createRun()
        r1p3.fontFamily = fontAgencyFB
        r1p3.setText("Given current building aggregation, current total charge is $${electrictyCharge(values.preAudit.electricstructure, values.building.currentTotalkW, values.building.currentTotalkWh).format(2)}")

        val p4 = document.createParagraph()
        p4.spacingBetween = 1.5
        val r1p4 = p4.createRun()
        r1p4.fontFamily = fontAgencyFB
        r1p4.setText("Given current building aggregation, current total charge is $${electrictyCharge(values.preAudit.electricstructure, values.building.postTotalkW, values.building.postTotalkW).format(2)}")
    }


    // generate tables
    private fun createEnergySavingsTable(document: XWPFDocument, lights: LightingValues, building: BuildingValues) {
        val table = document.createTable(11, 3)

        val row0 = table.getRow(0)
        val c0r0 = row0.getCell(0)
        c0r0.color = tableGreenColor
        val pc0r0 = c0r0.paragraphs[0]
        pc0r0.alignment = ParagraphAlignment.CENTER
        val rc0r0 = pc0r0.createRun()
        rc0r0.fontFamily = fontAgencyFB
        rc0r0.isBold = true
        rc0r0.color = whiteColor
        rc0r0.setText("Financial Components")

        mergeCellHorizontally(table, 0, 1, 2)
        val c1r0 = row0.getCell(1)
        c1r0.color = tableGreenColor
        val pc1r0 = c1r0.paragraphs[0]
        pc1r0.alignment = ParagraphAlignment.CENTER
        val rc1r0 = pc1r0.createRun()
        rc1r0.fontFamily = fontAgencyFB
        rc1r0.isBold = true
        rc1r0.color = whiteColor
        rc1r0.setText("Financial Overview")

        val row1 = table.getRow(1)
        val c0r1 = row1.getCell(0)
        val pc0r1 = c0r1.paragraphs[0]
        val rc0r1 = pc0r1.createRun()
        rc0r1.fontFamily = fontAgencyFB
        rc0r1.isBold = true
        rc0r1.setText("Electric Cost Savings (\$/yr)")

        mergeCellHorizontally(table, 1, 1, 2)
        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = pc1r1.createRun()
        rc1r1.fontFamily = fontAgencyFB
        rc1r1.setText("\$${building.buildingTotalSavings.format(0)}")

        val row2 = table.getRow(2)
        val c0r2 = row2.getCell(0)
        val pc0r2 = c0r2.paragraphs[0]
        val rc0r2 = pc0r2.createRun()
        rc0r2.fontFamily = fontAgencyFB
        rc0r2.isBold = true
        rc0r2.setText("Implementation Cost (\$)")

        mergeCellHorizontally(table, 2, 1, 2)
        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontAgencyFB
        rc1r2.setText("\$${building.buildingTotalCost.format(0)}")

        val row3 = table.getRow(3)
        val c0r3 = row3.getCell(0)
        val pc0r3 = c0r3.paragraphs[0]
        val rc0r3 = pc0r3.createRun()
        rc0r3.fontFamily = fontAgencyFB
        rc0r3.isBold = true
        rc0r3.setText("    Lighting (\$)")

        mergeCellHorizontally(table, 3, 1, 2)
        val c1r3 = row3.getCell(1)
        val pc1r3 = c1r3.paragraphs[0]
        pc1r3.alignment = ParagraphAlignment.CENTER
        val rc1r3 = pc1r3.createRun()
        rc1r3.fontFamily = fontAgencyFB
        rc1r3.setText("\$${lights.totalCost.format(0)}")

        val row4 = table.getRow(4)
        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontFamily = fontAgencyFB
        rc0r4.isBold = true
        rc0r4.setText("    HVAC (\$)")

        mergeCellHorizontally(table, 4, 1, 2)
        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontFamily = fontAgencyFB
        rc1r4.setText("\$${building.hvacTotalCost.format(0)}")

        val row5 = table.getRow(5)
        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontAgencyFB
        rc0r5.isBold = true
        rc0r5.setText("    Plug Load (\$)")

        mergeCellHorizontally(table, 5, 1, 2)
        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontAgencyFB
        rc1r5.setText("\$${building.equipmentTotalCost.format(0)}")

        // MARK: payback period

        val row6 = table.getRow(6)
        val c0r6 = row6.getCell(0)
        c0r6.color = tableGreyColor

        val c1r6 = row6.getCell(1)
        val pc1r6 = c1r6.paragraphs[0]
        pc1r6.alignment = ParagraphAlignment.CENTER
        val rc1r6 = pc1r6.createRun()
        rc1r6.fontFamily = fontAgencyFB
        rc1r6.isBold = true
        rc1r6.setText("Year")

        val c2r6 = row6.getCell(2)
        val pc2r6 = c2r6.paragraphs[0]
        pc2r6.alignment = ParagraphAlignment.CENTER
        val rc2r6 = pc2r6.createRun()
        rc2r6.fontFamily = fontAgencyFB
        rc2r6.isBold = true
        rc2r6.setText("Month")

        val row7 = table.getRow(7)
        val c0r7 = row7.getCell(0)
        val pc0r7 = c0r7.paragraphs[0]
        val rc0r7 = pc0r7.createRun()
        rc0r7.fontFamily = fontAgencyFB
        rc0r7.isBold = true
        rc0r7.setText("Payback Period")

        val c1r7 = row7.getCell(1)
        val pc1r7 = c1r7.paragraphs[0]
        pc1r7.alignment = ParagraphAlignment.CENTER
        val rc1r7 = pc1r7.createRun()
        rc1r7.fontFamily = fontAgencyFB
        rc1r7.setText(building.buildingPayback.format(2))

        val c2r7 = row7.getCell(2)
        val pc2r7 = c2r7.paragraphs[0]
        pc2r7.alignment = ParagraphAlignment.CENTER
        val rc2r7 = pc2r7.createRun()
        rc2r7.fontFamily = fontAgencyFB
        rc2r7.setText(building.buildingPaybackMonth.format(0))

        val row8 = table.getRow(8)
        val c0r8 = row8.getCell(0)
        val pc0r8 = c0r8.paragraphs[0]
        val rc0r8 = pc0r8.createRun()
        rc0r8.fontFamily = fontAgencyFB
        rc0r8.isBold = true
        rc0r8.setText("    Lighting Payback Period")

        val c1r8 = row8.getCell(1)
        val pc1r8 = c1r8.paragraphs[0]
        pc1r8.alignment = ParagraphAlignment.CENTER
        val rc1r8 = pc1r8.createRun()
        rc1r8.fontFamily = fontAgencyFB
        rc1r8.setText(lights.paybackYear.format(2))

        val c2r8 = row8.getCell(2)
        val pc2r8 = c2r8.paragraphs[0]
        pc2r8.alignment = ParagraphAlignment.CENTER
        val rc2r8 = pc2r8.createRun()
        rc2r8.fontFamily = fontAgencyFB
        rc2r8.setText(lights.paybackMonth.format(0))

        val row9 = table.getRow(9)
        val c0r9 = row9.getCell(0)
        val pc0r9 = c0r9.paragraphs[0]
        val rc0r9 = pc0r9.createRun()
        rc0r9.fontFamily = fontAgencyFB
        rc0r9.isBold = true
        rc0r9.setText("    HVAC Payback Period")

        val c1r9 = row9.getCell(1)
        val pc1r9 = c1r9.paragraphs[0]
        pc1r9.alignment = ParagraphAlignment.CENTER
        val rc1r9 = pc1r9.createRun()
        rc1r9.fontFamily = fontAgencyFB
        rc1r9.setText(building.hvacPaybackYear.format(2))

        val c2r9 = row9.getCell(2)
        val pc2r9 = c2r9.paragraphs[0]
        pc2r9.alignment = ParagraphAlignment.CENTER
        val rc2r9 = pc2r9.createRun()
        rc2r9.fontFamily = fontAgencyFB
        rc2r9.setText(building.hvacPaybackMonth.format(0))

        val row10 = table.getRow(10)
        val c0r10 = row10.getCell(0)
        val pc0r10 = c0r10.paragraphs[0]
        val rc0r10 = pc0r10.createRun()
        rc0r10.fontFamily = fontAgencyFB
        rc0r10.isBold = true
        rc0r10.setText("    Plug Load Payback Period")

        val c1r10 = row10.getCell(1)
        val pc1r10 = c1r10.paragraphs[0]
        pc1r10.alignment = ParagraphAlignment.CENTER
        val rc1r10 = pc1r10.createRun()
        rc1r10.fontFamily = fontAgencyFB
        rc1r10.setText(building.equipmentPaybackYear.format(2))

        val c2r10 = row10.getCell(2)
        val pc2r10 = c2r10.paragraphs[0]
        pc2r10.alignment = ParagraphAlignment.CENTER
        val rc2r10 = pc2r10.createRun()
        rc2r10.fontFamily = fontAgencyFB
        rc2r10.setText(building.equipmentPaybackMonth.format(0))


        fitTable(table, 4500)
        centerTable(table)
    }

    private fun createLightingTable1(document: XWPFDocument, lights: LightingValues) {
        val table = document.createTable(6, 2)
        val row0 = table.getRow(0)

        val cell0r0 = row0.getCell(0)
        cell0r0.color = tableGreenColor
        val pcell0r0 = cell0r0.paragraphs[0]
        pcell0r0.alignment = ParagraphAlignment.CENTER
        val rcell0r0 = cell0r0.paragraphs[0].createRun()
        rcell0r0.fontFamily = fontAgencyFB
        rcell0r0.color = whiteColor
        rcell0r0.isBold = true
        rcell0r0.setText("Financial Components")

        val cell1r0 = row0.getCell(1)
        cell1r0.color = tableGreenColor
        val pcell1ro = cell1r0.paragraphs[0]
        pcell1ro.alignment = ParagraphAlignment.CENTER
        val rcell1r0 = cell1r0.paragraphs[0].createRun()
        rcell1r0.fontFamily = fontAgencyFB
        rcell1r0.color = whiteColor
        rcell1r0.isBold = true
        rcell1r0.setText("Financial Overview")

        val row1 = table.getRow(1)

        val c0r1 = row1.getCell(0)
        val rc0r1 = c0r1.paragraphs[0].createRun()
        rc0r1.fontFamily = fontAgencyFB
        rc0r1.isBold = true
        rc0r1.setText("Minimum Electrical Cost Savings ($/yr)")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = c1r1.paragraphs[0].createRun()
        rc1r1.fontFamily = fontAgencyFB
        rc1r1.setText("\$${lights.totalcostsavings.format(0)}")

        val row2 = table.getRow(2)

        val c0r2 = row2.getCell(0)
        val rc0r2 = c0r2.paragraphs[0].createRun()
        rc0r2.fontFamily = fontAgencyFB
        rc0r2.isBold = true
        rc0r2.setText("Implementation Cost ($)")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontAgencyFB
        rc1r2.setText("\$${lights.totalCost}")

        val row3 = table.getRow(3)

        val c0r3 = row3.getCell(0)
        val rc0r3 = c0r3.paragraphs[0].createRun()
        rc0r3.fontFamily = fontAgencyFB
        rc0r3.isBold = true
        rc0r3.setText("Payback")

        val row4 = table.getRow(4)

        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        pc0r4.alignment = ParagraphAlignment.RIGHT
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontFamily = fontAgencyFB
        rc0r4.setText("Year")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontFamily = fontAgencyFB
        rc1r4.setText(lights.paybackYear.format(2))

        val row5 = table.getRow(5)

        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        pc0r5.alignment = ParagraphAlignment.RIGHT
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontAgencyFB
        rc0r5.setText("Month")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontAgencyFB
        rc1r5.setText(lights.paybackMonth.format(0))

        centerTable(table)
        fitTable(table, 4000)
    }

    private fun createLightingTable3(document: XWPFDocument, lights: LightingValues) {
        val rows = lights.lightingRows
        val table = document.createTable(rows.size + 1, 13)

        for (i in 0..rows.size) {
            // saftey check if no rows exist
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
            rcell0.setText(if (isFirst) "Measure" else rowItem.measure)

            val cell1 = row.getCell(1)
            cell1.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell1 = cell1.paragraphs[0]
            pcell1.alignment = ParagraphAlignment.CENTER
            val rcell1 = pcell1.createRun()
            rcell1.setText(if (isFirst) "Location" else rowItem.location)

            val cell2 = row.getCell(2)
            cell2.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell2 = cell2.paragraphs[0]
            pcell2.alignment = ParagraphAlignment.CENTER
            val rcell2 = pcell2.createRun()
            rcell2.setText(if (isFirst) "Measure Description" else rowItem.measureDescription)

            val cell3 = row.getCell(3)
            cell3.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell3 = cell3.paragraphs[0]
            pcell3.alignment = ParagraphAlignment.CENTER
            val rcell3 = pcell3.createRun()
            rcell3.setText(if (isFirst) "Quantity" else rowItem.quantity.format(0))

            val cell4 = row.getCell(4)
            cell4.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell4 = cell4.paragraphs[0]
            pcell4.alignment = ParagraphAlignment.CENTER
            val rcell4 = pcell4.createRun()
            rcell4.setText((if (isFirst) "Current Power (kW)" else rowItem.currentPowerkW.format(2)))

                        val cell5 = row.getCell(5)
            cell5.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell5 = cell5.paragraphs[0]
            pcell5.alignment = ParagraphAlignment.CENTER
            val rcell5 = pcell5.createRun()
            rcell5.setText(if (isFirst) "Usage (hrs)" else rowItem.usageHours.format(0))

            val cell6 = row.getCell(6)
            cell6.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell6 = cell6.paragraphs[0]
            pcell6.alignment = ParagraphAlignment.CENTER
            val rcell6 = pcell6.createRun()
            rcell6.setText(if (isFirst) "Post Power (kW)" else rowItem.postPowerkW.format(2))

            val cell7 = row.getCell(7)
            cell7.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell7 = cell7.paragraphs[0]
            pcell7.alignment = ParagraphAlignment.CENTER
            val rcell7 = pcell7.createRun()
            rcell7.setText(if (isFirst) "Post Usage (hrs)" else rowItem.postUsageHours.format(0))

            val cell8 = row.getCell(8)
            cell8.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell8 = cell8.paragraphs[0]
            pcell8.alignment = ParagraphAlignment.CENTER
            val rcell8 = pcell8.createRun()
            rcell8.setText(if (isFirst) "Energy Savings (kWh)" else rowItem.energySavingskWh.format(0))

            val cell9 = row.getCell(9)
            cell9.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell9 = cell9.paragraphs[0]
            pcell9.alignment = ParagraphAlignment.CENTER
            val rcell9 = pcell9.createRun()
            rcell9.setText(if (isFirst) "Life Cost Savings" else "$${rowItem.costSavings.format(0)}")

            val cell10 = row.getCell(10)
            cell10.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell10 = cell10.paragraphs[0]
            pcell10.alignment = ParagraphAlignment.CENTER
            val rcell10 = pcell10.createRun()
            rcell10.setText(if (isFirst) "Implementation Cost" else "$${rowItem.implementationCost.format(0)}")

            val cell11 = row.getCell(11)
            cell11.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell11 = cell11.paragraphs[0]
            pcell11.alignment = ParagraphAlignment.CENTER
            val rcell11 = pcell11.createRun()
            rcell11.setText(if (isFirst) "Payback Period (months)" else rowItem.paybackPeriodMonths.format(0))

            val cell12 = row.getCell(12)
            cell12.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
            val pcell12 = cell12.paragraphs[0]
            pcell12.alignment = ParagraphAlignment.CENTER
            val rcell12 = pcell12.createRun()
            rcell12.setText(if (isFirst) "Payback Period (years)" else rowItem.paybackPeriodYears.format(1))
        }

        centerTable(table)
    }

    private fun createHvacTable(document: XWPFDocument, hvac: HvacValues, building: BuildingValues) {
        val table = document.createTable(6, 2)
        val row0 = table.getRow(0)

        val cell0r0 = row0.getCell(0)
        cell0r0.color = tableGreenColor
        val pcellr0 = cell0r0.paragraphs[0]
        pcellr0.alignment = ParagraphAlignment.CENTER
        val rcell0r0 = cell0r0.paragraphs[0].createRun()
        rcell0r0.fontFamily = fontAgencyFB
        rcell0r0.color = whiteColor
        rcell0r0.isBold = true
        rcell0r0.setText("Financial Components")

        val cell1r0 = row0.getCell(1)
        cell1r0.color = tableGreenColor
        val pcell1r0 = cell1r0.paragraphs[0]
        pcell1r0.alignment = ParagraphAlignment.CENTER
        val rcell1r0 = cell1r0.paragraphs[0].createRun()
        rcell1r0.fontFamily = fontAgencyFB
        rcell1r0.color = whiteColor
        rcell1r0.isBold = true
        rcell1r0.setText("Financial Overview")

        // row 1
        val row1 = table.getRow(1)

        val c0r1 = row1.getCell(0)
        val rc0r1 = c0r1.paragraphs[0].createRun()
        rc0r1.fontFamily = fontAgencyFB
        rc0r1.isBold = true
        rc0r1.setText("Total Electrical Cost Savings ($/yr)")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = c1r1.paragraphs[0].createRun()
        rc1r1.fontFamily = fontAgencyFB
        rc1r1.setText("\$${hvac.totalSavings.format(0)}")

        val row2 = table.getRow(2)

        val c0r2 = row2.getCell(0)
        val rc0r2 = c0r2.paragraphs[0].createRun()
        rc0r2.fontFamily = fontAgencyFB
        rc0r2.isBold = true
        rc0r2.setText("Implementation Cost ($)")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontAgencyFB
        rc1r2.setText("\$${hvac.totalCost.format(0)}")

        val row3 = table.getRow(3)

        val c0r3 = row3.getCell(0)
        val rc0r3 = c0r3.paragraphs[0].createRun()
        rc0r3.fontFamily = fontAgencyFB
        rc0r3.isBold = true
        rc0r3.setText("Payback")

        val row4 = table.getRow(4)

        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        pc0r4.alignment = ParagraphAlignment.RIGHT
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontFamily = fontAgencyFB
        rc0r4.setText("Year")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontFamily = fontAgencyFB
        rc1r4.setText(building.hvacPaybackYear.format(2))

        val row5 = table.getRow(5)

        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        pc0r5.alignment = ParagraphAlignment.RIGHT
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontAgencyFB
        rc0r5.setText("Month")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontAgencyFB
        rc1r5.setText(building.hvacPaybackMonth.format(0))

        centerTable(table)
        fitTable(table, 4000)
    }

    private fun createWaterHeaterTable(document: XWPFDocument, waterheater: WaterHeaterValues) {
        val table = document.createTable(6, 2)
        val row0 = table.getRow(0)

        val cell0r0 = row0.getCell(0)
        cell0r0.color = tableGreenColor
        val pcellr0 = cell0r0.paragraphs[0]
        pcellr0.alignment = ParagraphAlignment.CENTER
        val rcell0r0 = cell0r0.paragraphs[0].createRun()
        rcell0r0.fontFamily = fontAgencyFB
        rcell0r0.color = whiteColor
        rcell0r0.isBold = true
        rcell0r0.setText("Financial Components")

        val cell1r0 = row0.getCell(1)
        cell1r0.color = tableGreenColor
        val pcell1r0 = cell1r0.paragraphs[0]
        pcell1r0.alignment = ParagraphAlignment.CENTER
        val rcell1r0 = cell1r0.paragraphs[0].createRun()
        rcell1r0.fontFamily = fontAgencyFB
        rcell1r0.color = whiteColor
        rcell1r0.isBold = true
        rcell1r0.setText("Financial Overview")

        // row 1
        val row1 = table.getRow(1)

        val c0r1 = row1.getCell(0)
        val rc0r1 = c0r1.paragraphs[0].createRun()
        rc0r1.fontFamily = fontAgencyFB
        rc0r1.isBold = true
        rc0r1.setText("Total Electrical Cost Savings ($/yr)")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        pc1r1.alignment = ParagraphAlignment.CENTER
        val rc1r1 = c1r1.paragraphs[0].createRun()
        rc1r1.fontFamily = fontAgencyFB
        rc1r1.setText("\$${waterheater.totalSavings.format(0)}")

        val row2 = table.getRow(2)

        val c0r2 = row2.getCell(0)
        val rc0r2 = c0r2.paragraphs[0].createRun()
        rc0r2.fontFamily = fontAgencyFB
        rc0r2.isBold = true
        rc0r2.setText("Implementation Cost ($)")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        pc1r2.alignment = ParagraphAlignment.CENTER
        val rc1r2 = pc1r2.createRun()
        rc1r2.fontFamily = fontAgencyFB
        rc1r2.setText("\$${waterheater.totalCost.format(0)}")

        val row3 = table.getRow(3)

        val c0r3 = row3.getCell(0)
        val rc0r3 = c0r3.paragraphs[0].createRun()
        rc0r3.fontFamily = fontAgencyFB
        rc0r3.isBold = true
        rc0r3.setText("Payback")

        val row4 = table.getRow(4)

        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        pc0r4.alignment = ParagraphAlignment.RIGHT
        val rc0r4 = pc0r4.createRun()
        rc0r4.fontFamily = fontAgencyFB
        rc0r4.setText("Year")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        pc1r4.alignment = ParagraphAlignment.CENTER
        val rc1r4 = pc1r4.createRun()
        rc1r4.fontFamily = fontAgencyFB
        rc1r4.setText(waterheater.paybackYear.format(2))

        val row5 = table.getRow(5)

        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        pc0r5.alignment = ParagraphAlignment.RIGHT
        val rc0r5 = pc0r5.createRun()
        rc0r5.fontFamily = fontAgencyFB
        rc0r5.setText("Month")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        pc1r5.alignment = ParagraphAlignment.CENTER
        val rc1r5 = pc1r5.createRun()
        rc1r5.fontFamily = fontAgencyFB
        rc1r5.setText(waterheater.paybackMonth.format(0))

        centerTable(table)
        fitTable(table, 4000)
    }

    private fun generateElectrictyCalculationTable(document: XWPFDocument, values: PreparedForDocument) {
        val table = document.createTable(7,5)

        // header row
        val row0 = table.getRow(0)
        val c0r0 = row0.getCell(0)

        val c1r0 = row0.getCell(1)
        val pc1r0 = c1r0.paragraphs[0]
        var rc1r0 = pc1r0.createRun()
        rc1r0.setText("Current Total kW")

        val c2r0 = row0.getCell(2)
        val pc2r0 = c2r0.paragraphs[0]
        var rc2r0 = pc2r0.createRun()
        rc2r0.setText("Current Total kWh")

        val c3r0 = row0.getCell(3)
        val pc3r0 = c3r0.paragraphs[0]
        var rc3r0 = pc3r0.createRun()
        rc3r0.setText("Post Total kW")

        val c4r0 = row0.getCell(4)
        val pc4r0 = c4r0.paragraphs[0]
        var rc4r0 = pc4r0.createRun()
        rc4r0.setText("Post Total kWh")

        // hvac row
        val row1 = table.getRow(1)
        val c0r1 = row1.getCell(0)
        val pc0r1 = c0r1.paragraphs[0]
        val rc0r1 = pc0r1.createRun()
        rc0r1.setText("HVAC")

        val c1r1 = row1.getCell(1)
        val pc1r1 = c1r1.paragraphs[0]
        var rc1r1 = pc1r1.createRun()
        rc1r1.setText("${values.hvac?.currentTotalkW?.format(2) ?: "0.00"}")

        val c2r1 = row1.getCell(2)
        val pc2r1 = c2r1.paragraphs[0]
        var rc2r1 = pc2r1.createRun()
        rc2r1.setText("${values.hvac?.currentTotalkWh?.format(2) ?: "0.00"}")

        val c3r1 = row1.getCell(3)
        val pc3r1 = c3r1.paragraphs[0]
        var rc3r1 = pc3r1.createRun()
        rc3r1.setText("${values.hvac?.postTotalkW?.format(2) ?: "0.00"}")

        val c4r1 = row1.getCell(4)
        val pc4r1 = c4r1.paragraphs[0]
        var rc4r1 = pc4r1.createRun()
        rc4r1.setText("${values.hvac?.postTotalkWh?.format(2) ?: "0.00"}")

        // lighting row
        val row2 = table.getRow(2)
        val c0r2 = row2.getCell(0)
        val pc0r2 = c0r2.paragraphs[0]
        val rc0r2 = pc0r2.createRun()
        rc0r2.setText("Lighting")

        val c1r2 = row2.getCell(1)
        val pc1r2 = c1r2.paragraphs[0]
        var rc1r2 = pc1r2.createRun()
        rc1r2.setText("${values.lighting?.currentTotalkW?.format(2) ?: "0.00"}")

        val c2r2 = row2.getCell(2)
        val pc2r2 = c2r2.paragraphs[0]
        var rc2r2 = pc2r2.createRun()
        rc2r2.setText("${values.lighting?.currentTotalkWh?.format(2) ?: "0.00"}")

        val c3r2 = row2.getCell(3)
        val pc3r2 = c3r2.paragraphs[0]
        var rc3r2 = pc3r2.createRun()
        rc3r2.setText("${values.lighting?.postTotalkW?.format(2) ?: "0.00"}")

        val c4r2 = row2.getCell(4)
        val pc4r2 = c4r2.paragraphs[0]
        var rc4r2 = pc4r2.createRun()
        rc4r2.setText("${values.lighting?.postTotalkWh?.format(2) ?: "0.00"}")

        // refirgeration row
        val row3 = table.getRow(3)
        val c0r3 = row3.getCell(0)
        val pc0r3 = c0r3.paragraphs[0]
        val rc0r3 = pc0r3.createRun()
        rc0r3.setText("Refrigeration")

        val c1r3 = row3.getCell(1)
        val pc1r3 = c1r3.paragraphs[0]
        var rc1r3 = pc1r3.createRun()
        rc1r3.setText("${values.refrigeration?.currentTotalkW?.format(2) ?: "0.00"}")

        val c2r3 = row3.getCell(2)
        val pc2r3 = c2r3.paragraphs[0]
        var rc2r3 = pc2r3.createRun()
        rc2r3.setText("${values.refrigeration?.currentTotalkWh?.format(2) ?: "0.00"}")

        val c3r3 = row3.getCell(3)
        val pc3r3 = c3r3.paragraphs[0]
        var rc3r3 = pc3r3.createRun()
        rc3r3.setText("${values.refrigeration?.postTotalkW?.format(2) ?: "0.00"}")

        val c4r3 = row3.getCell(4)
        val pc4r3 = c4r3.paragraphs[0]
        var rc4r3 = pc4r3.createRun()
        rc4r3.setText("${values.refrigeration?.postTotalkWh?.format(2) ?: "0.00"}")

        // water heater row
        val row4 = table.getRow(4)
        val c0r4 = row4.getCell(0)
        val pc0r4 = c0r4.paragraphs[0]
        val rc0r4 = pc0r4.createRun()
        rc0r4.setText("Water Heater")

        val c1r4 = row4.getCell(1)
        val pc1r4 = c1r4.paragraphs[0]
        var rc1r4 = pc1r4.createRun()
        rc1r4.setText("${values.waterHeater?.currentTotalkW?.format(2) ?: "0.00"}")

        val c2r4 = row4.getCell(2)
        val pc2r4 = c2r4.paragraphs[0]
        var rc2r4 = pc2r4.createRun()
        rc2r4.setText("${values.waterHeater?.currentTotalkWh?.format(2) ?: "0.00"}")

        val c3r4 = row4.getCell(3)
        val pc3r4 = c3r4.paragraphs[0]
        var rc3r4 = pc3r4.createRun()
        rc3r4.setText("${values.waterHeater?.postTotalkW?.format(2) ?: "0.00"}")

        val c4r4 = row4.getCell(4)
        val pc4r4 = c4r4.paragraphs[0]
        var rc4r4 = pc4r4.createRun()
        rc4r4.setText("${values.waterHeater?.postTotalkWh?.format(2) ?: "0.00"}")

        // equipment row
        val row5 = table.getRow(5)
        val c0r5 = row5.getCell(0)
        val pc0r5 = c0r5.paragraphs[0]
        val rc0r5 = pc0r5.createRun()
        rc0r5.setText("Other Equipment")

        val c1r5 = row5.getCell(1)
        val pc1r5 = c1r5.paragraphs[0]
        var rc1r5 = pc1r5.createRun()
        rc1r5.setText("${values.equipment?.currentTotalkW?.format(2) ?: "0.00"}")

        val c2r5 = row5.getCell(2)
        val pc2r5 = c2r5.paragraphs[0]
        var rc2r5 = pc2r5.createRun()
        rc2r5.setText("${values.equipment?.currentTotalkWh?.format(2) ?: "0.00"}")

        val c3r5 = row5.getCell(3)
        val pc3r5 = c3r5.paragraphs[0]
        var rc3r5 = pc3r5.createRun()
        rc3r5.setText("${values.equipment?.postTotalkW?.format(2) ?: "0.00"}")

        val c4r5 = row5.getCell(4)
        val pc4r5 = c4r5.paragraphs[0]
        var rc4r5 = pc4r5.createRun()
        rc4r5.setText("${values.equipment?.postTotalkWh?.format(2) ?: "0.00"}")

        // building row
        val row6 = table.getRow(6)
        val c0r6 = row6.getCell(0)
        val pc0r6 = c0r6.paragraphs[0]
        val rc0r6 = pc0r6.createRun()
        rc0r6.setText("Building")

        val c1r6 = row6.getCell(1)
        val pc1r6 = c1r6.paragraphs[0]
        var rc1r6 = pc1r6.createRun()
        rc1r6.setText("${values.building?.currentTotalkW?.format(2) ?: "0.00"}")

        val c2r6 = row6.getCell(2)
        val pc2r6 = c2r6.paragraphs[0]
        var rc2r6 = pc2r6.createRun()
        rc2r6.setText("${values.building?.currentTotalkWh?.format(2) ?: "0.00"}")

        val c3r6 = row6.getCell(3)
        val pc3r6 = c3r6.paragraphs[0]
        var rc3r6 = pc3r6.createRun()
        rc3r6.setText("${values.building?.postTotalkW?.format(2) ?: "0.00"}")

        val c4r6 = row6.getCell(4)
        val pc4r6 = c4r6.paragraphs[0]
        var rc4r6 = pc4r6.createRun()
        rc4r6.setText("${values.building?.postTotalkWh?.format(2) ?: "0.00"}")

        centerTable(table)
    }


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


    // electricty rate calculations
    fun electrictyCharge(structure: String, kW: Double, kWh: Double): Double {
//        val electrictySchedule = preaudit.electricstructure
        if (structure == "G") {
            println("calculate schedule G")
            return calculateScheduleF(kW, kWh)
        }
        else if (structure == "GS") {
            println("calculate schedule GS")
            return calculateScheduleFTOD(kW, kWh)
        }
        else if (structure == "GL") {
            println("calculate schedule GL")
            return calculateScheduleH(kW, kWh)
        }
        return 0.0
    }

    fun calculateScheduleF(kW: Double, kWh: Double): Double {
        var totalDemand = 0.0
        for (i in 1..kW.toInt()) {
            if (i <= 15) {
                totalDemand += 0.0
            }
            else {
                totalDemand += 9.50
            }
        }
        println("total demand is ${totalDemand}")

        var totalEnergy = 0.0
        for (i in 1..kWh.toInt()) {
            if (i <= 2500) {
                totalEnergy += 0.1288
            }
            else if (i <= 1000) {
                totalEnergy += 0.0918
            }
            else if (i <= 50000) {
                totalEnergy += 0.077
            }
            else {
                totalEnergy += 0.066
            }
        }
        println("total energy is ${totalEnergy}")

        var totalCost = totalDemand + totalEnergy
        println("cost 1 is ${totalCost}")

        if (kW >= 100) {
            var otherPotentialCost = kWh * 0.1697
            println("cost 2 is ${otherPotentialCost}")
            if (totalCost > otherPotentialCost) {
                //        totalCost = otherPotentialCost
                return otherPotentialCost
            }
        }
        return totalCost
    }

    fun calculateScheduleFTOD(kW: Double, kWh: Double): Double {
        // calculating based on assumption that all hours are off peak
        val totalDemand = kW * 4.95
        println("totalDemand $totalDemand")
        val totalEnergy = kWh * 0.05
        println("totalEnergy $totalEnergy")
        val totalCost = totalDemand + totalEnergy
        println("totalCost $totalCost")
        return totalCost
    }

    fun calculateScheduleH(kW: Double, kWh: Double): Double {
        // calculating based on assumption that all hours are off peak
        val totalDemand = kW * 5.20
        println("totalDemand $totalDemand")
        val totalEnergy = kWh * 0.0449
        println("totalEnergy $totalEnergy")
        val totalCost = totalDemand + totalEnergy
        println("totalCost $totalCost")
        return totalCost
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

