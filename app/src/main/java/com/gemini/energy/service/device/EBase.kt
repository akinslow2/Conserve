package com.gemini.energy.service.device

import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.internal.AppSchedulers
import com.gemini.energy.presentation.util.EDay
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.EZoneType
import com.gemini.energy.service.CostElectric
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.ParseAPI
import com.gemini.energy.service.crunch.*
import com.gemini.energy.service.type.Electricity
import com.gemini.energy.service.type.Gas
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import org.json.JSONObject
import timber.log.Timber

abstract class EBase(private val computable: Computable<*>,
                     private val utilityRateGas: UtilityRate,
                     private val utilityRateElectricity: UtilityRate,
                     val operatingHours: UsageHours,
                     val outgoingRows: OutgoingRows)  {

    lateinit var schedulers: Schedulers
    lateinit var gasRate: UtilityRate
    lateinit var electricityRate: UtilityRate

    private lateinit var powerTimeChange: PowerTimeChange

    var preAudit: Map<String, Any> = mapOf()
    var featureData: Map<String, Any> = mapOf()
    var electricRateStructure: String = RATE
    var electricCompanyCode: String = COMPANY_CODE

    val usageHoursBusiness = UsageHours()
    val usageHoursSpecific = UsageHours()

    private fun initialize() {
        val base = this

        Timber.d("<< COMPUTE :: ${identifier()} >> [Start] - (${thread()})")
        Timber.d(computable.toString())

        base.schedulers = AppSchedulers()
        base.featureData = computable.mappedFeatureAuditScope()
        base.preAudit = computable.mappedFeaturePreAudit()

        setupUtility(base)
        setupUsage(base)
        setupOutgoingRows(base)
        setupPowerTimeChange(base)

        setup()
    }

    /**
     * Electricity | Gas Utility Rate Setup
     * */
    private fun setupUtility(base: EBase) {
        base.gasRate = utilityRateGas.initUtility(Gas()).build()
        //Others has been added to the Electric Rate Structure as a fix cause the Section Name
        //is added to the Element Key
        val fix = "Others"
        base.electricRateStructure = preAudit["$fix Electric Rate Structure"] as String
        base.electricCompanyCode = preAudit["$fix Utility Company"] as String

        Timber.d("####### RATE STRUCTURE CHECKER #######")
        Timber.d(electricRateStructure)
        Timber.d("####### COMPANY CODE CHECKER #######")
        Timber.d(electricCompanyCode)

        //This is where we are initializing the Electricity Rate Class
        //Now we are only passing the Rate Structure but we also need to pass the Company Code
        base.electricityRate = utilityRateElectricity.initUtility(
                Electricity(electricRateStructure, electricCompanyCode)).build()

        Timber.d("####### OBJECT CHECKER #######")
        Timber.d(gasRate.toString())
        Timber.d(electricityRate.toString())

    }

    /**
     * Operation Hours
     * 1. PreAudit - Energy Usage Business (pre)
     * 2. FeatureData - Energy Usage Specific (post)
     * */
    private fun setupUsage(base: EBase) {
        base.usageHoursBusiness.initUsage(mappedBusinessHours()).build()
        base.usageHoursSpecific.initUsage(mappedSpecificHours()).build()
    }

    /**
     * Holds the data to be written of off to a CSV File - Generally the Energy Crunch Reports
     * */
    private fun setupOutgoingRows(base: EBase) {
        base.outgoingRows.computable = computable
        base.outgoingRows.dataHolder = mutableListOf()
    }

    /**
     * To calculate Energy Efficiency and eventually Savings - It's important to figure out the change of
     * the below mentioned 3 variables. Energy Calculations are based off of these.
     * 1. Power
     * 2. Time
     * 3. Both Power and Time
     * */
    private fun setupPowerTimeChange(base: EBase) {
        base.powerTimeChange = PowerTimeChange()
        base.powerTimeChange.energyPowerChange = { base.energyPowerChange() }
        base.powerTimeChange.energyTimeChange = { base.energyTimeChange() }
        base.powerTimeChange.energyPowerTimeChange = { base.energyPowerTimeChange() }
    }

    private fun thread() = Thread.currentThread().name
    private fun identifier() = "${computable.auditScopeType} - ${computable.auditScopeSubType}"

    /**
     * Collect the Various Energy Calculation - Concat them
     * Write the result to the CSV - Emit back Computable
     * */
    fun compute(extra: (param: String) -> Unit): Observable<Computable<*>> {
        initialize()

        class Mapper : Function<DataHolder, Computable<*>> {
            override fun apply(dataHolder: DataHolder): Computable<*> {
                synchronized(outgoingRows.dataHolder) {
                    if (dataHolder.path.isNotEmpty()) {
                        outgoingRows.dataHolder.add(dataHolder)
                    }
                }
                computable.outgoingRows = outgoingRows
                return computable
            }
        }

        return Observable.concat(calculateEnergyPreState(extra), calculateEnergyPostState(extra),
                calculateEnergySavings(extra), calculateCostSavings(extra))
                .map(Mapper()).doOnComplete {
                    Timber.d("$$$$$$$ SUPER.COMPUTE.CONCAT.COMPLETE $$$$$$$")
                    outgoingRows.save()
                }

    }

    /**
     * Pre State - Energy Calculation
     * Gives an Observable with the Data Holder
     * */
    private fun calculateEnergyPreState(extra: (param: String) -> Unit): Observable<DataHolder> {
        Timber.d(identifier())
        val energyPreState = EnergyPreState()
        energyPreState.featureData = featureData
        energyPreState.featureDataFields = featureDataFields()
        energyPreState.preAuditData = preAudit
        energyPreState.preAuditDataFields = preAuditFields()
        energyPreState.computable = computable

        // ** Prepare a list of Observable - Extractor that is required by each of the Zone Type **
        val extractorHVAC = listOf(dataExtractHVAC(queryHVACCoolingHours()),
                dataExtractHVAC(queryHVACEer()))

        val extractorMotor = listOf(dataExtractMotors(queryMotorEfficiency()))
        val extractorNone = listOf(Observable.just(JsonArray()))

        // ** Extractor List gets called depending on the Zone Type **
        val remoteExtract = when (computable.auditScopeType) {
            EZoneType.HVAC      -> extractorHVAC
            EZoneType.Motors    -> extractorMotor
            else                -> extractorNone
        }

        return energyPreState.getObservable(remoteExtract) {
            costPreState(it)
        }
    }

    /**
     * Post State - Energy Calculation
     * Gives an Observable with the Data Holder
     * */
    private fun calculateEnergyPostState(extra: (param: String) -> Unit): Observable<DataHolder> {
        Timber.d(identifier())
        val mapper = EnergyPostState.Mapper()
        mapper.computable = computable
        mapper.postStateFields = postStateFields()
        mapper.cost = { element, dataHolder ->
            costPostState(element, dataHolder)
        }

        return starValidator(queryEnergyStar())
                .flatMap { efficientAlternative(!it).map(mapper) }
    }

    /**
     * Energy Saving - Calculates the Energy Saved via examining the 3 cases
     * Power Change | Time Change | Both Power Time Change
     * Via PowerTimeChange (helper class)
     * */
    private fun calculateEnergySavings(extra: (param: String) -> Unit): Observable<DataHolder> {
        Timber.d(identifier())
        val mapper = EnergySavings.Mapper()
        mapper.computable = computable
        mapper.powerTimeChange = powerTimeChange

        return Observable.just(Unit).map(mapper)
    }

    /**
     * Energy Cost Saving
     * */
    private fun calculateCostSavings(extra: (param: String) -> Unit): Observable<DataHolder> {
        Timber.d(identifier())
        val mapper = CostSavings.Mapper()
        mapper.computable = computable

        //ToDo : @Johnny - Verify this - What should be the new Usage Hours be ??
        mapper.usageHoursSpecific = usageHoursSpecific
        mapper.usageHoursBusiness = usageHoursBusiness

        mapper.schedule = electricRateStructure
        mapper.rateElectric = electricityRate
        mapper.rateGas = gasRate
        mapper.featureData = featureData
        mapper.powerTimeChange = powerTimeChange

        mapper.dailyEnergyUsagePre = { hourlyEnergyUsagePre()[0] }
        mapper.materialCost = { materialCost() }
        mapper.laborCost = { laborCost() }
        mapper.incentives = { incentives() }

        mapper.isGas = { isGas() }

        //@Johnny - This is where the Material Cost is going to be fetched
        fun prerequisite() = laborCost(queryLaborCost())
                .flatMap { response ->
                    val jsonElements = response.map { it.asJsonObject.get("data") }
                    if (jsonElements.count() > 0) {
                        val cost = jsonElements[0].asJsonObject.get("cost").asDouble
                        computable.laborCost = cost
                    }
                    Observable.just(Unit)
                }

        return prerequisite().map(mapper)
    }

    companion object {
        private const val RATE = "A-1 TOU"
        private const val COMPANY_CODE = "pge"
        private val regex = "^.*TOU$".toRegex()
    }

    fun isTOU(rate: String = electricRateStructure) = rate.matches(regex)

    abstract fun queryEfficientFilter(): String
    abstract fun efficientLookup(): Boolean

    //ToDo - Where is this used ?? Cleanup
    abstract fun usageHoursSpecific(): Boolean

    /**
     * The equipment class is supposed to override this method
     * if in-case of a Suggested Alternative being present as Feature Data
     * */
    open fun buildPostState(): Single<JsonObject> {
        val wrapper = JsonObject()
        wrapper.add("results", JsonArray())
        return Single.just(wrapper)
    }

    abstract fun preAuditFields(): MutableList<String>
    abstract fun featureDataFields(): MutableList<String>

    //ToDo - Where is this used ?? Cleanup
    abstract fun preStateFields(): MutableList<String>
    abstract fun postStateFields(): MutableList<String>

    //ToDo - Where is this used ?? Cleanup
    abstract fun computedFields(): MutableList<String>

    /**
     * Energy Cost Functions
     * These are Consumed by the Energy Pre State - Post State | Energy - Cost Savings
     * The Equipment Classes define how the calculations are supposed to be done
     * */
    abstract fun costPreState(elements: List<JsonElement?>): Double
    abstract fun costPostState(element: JsonElement, dataHolder: DataHolder): Double

    /**
     * Power Time Change
     * */
    abstract fun hourlyEnergyUsagePre(): List<Double>
    abstract fun hourlyEnergyUsagePost(element: JsonElement): List<Double>

    /**
     * Other Costs
     * */
    open fun materialCost() = 0.0
    open fun laborCost() = 0.0
    open fun incentives() = 0.0

    abstract fun usageHoursPre(): Double
    abstract fun usageHoursPost(): Double

    /**
     * Gas Based Equipment Checker
     * */
    open fun isGas() = false

    /**
     * Energy Efficiency Calculations
     * */
    abstract fun energyPowerChange(): Double
    abstract fun energyTimeChange(): Double
    abstract fun energyPowerTimeChange(): Double

    /**
     * Setup the Device
     * */
    abstract fun setup()

    /**
     * Energy Star Queries
     * To figure out if the Equipment is already in the Energy Efficient List
     * */
    private fun queryEnergyStar() = JSONObject()
            .put("data.company", featureData["Company"] ?: "none")
            .put("data.model_number", featureData["Model Number"] ?: "none")
            .toString()

    /**
     * Labor Cost Query
     * */
    private fun queryLaborCost() = JSONObject()
            .put("data.zipcode", featureData["ZipCode"] ?: 0)
            .put("data.profession", featureData["Profession"] ?: "none")
            .toString()

    /**
     * HVAC Query - Should be overridden by the HVAC Equipment Implementor
     *
     * Note: These queries are currently being used by the Extractor - Pre State.
     * Both the Post and Pre State Share this data via UDF defined within the Computable.
     * This is only possible as the values being used to query are independent of the States.
     * Need a better alternative that covers the Use-Case where the query parameters can be specific to the
     * States.
     * */
    open fun queryHVACCoolingHours() = ""
    open fun queryHVACEer() = ""

    /**
     * Motors Query - Fetch Efficiency
     * */
    open fun queryMotorEfficiency() = ""

    /**
     * Get the Specific Query Result from the Parse API
     * */
    private fun starValidator(query: String): Observable<Boolean> {
        return parseAPIService.fetchPlugload(query)
                .map { it.getAsJsonArray("results").size() == 0 }
                .toObservable()
    }

    /**
     * Generic Query to retrieve the Energy Efficient
     * */
    private fun efficientAlternative(isEnergyStar: Boolean): Observable<JsonArray> {

        // ** Load the Efficient Query for each of the Zone Type **
        Timber.d("Energy Star -- [$isEnergyStar]")
        Timber.d("Efficient Alternative for Type -- [${computable.auditScopeType}]")
        Timber.d("Efficient Lookup -- [${efficientLookup()}]")
        val query = queryEfficientFilter()

        fun switcherHVAC() = if (efficientLookup()) parseAPIService.fetchHVAC(query) else buildPostState()
        fun switcherPlugload() = if (efficientLookup() && !isEnergyStar) parseAPIService.fetchPlugload(query) else buildPostState()

        val result = when (computable.auditScopeType) {
            EZoneType.HVAC          -> switcherHVAC()
            EZoneType.Plugload      -> switcherPlugload()
            else                    -> buildPostState() // This gives an empty JSON !!
        }

        return result.map { it.getAsJsonArray("results") }.toObservable()
    }

    private fun laborCost(query: String): Observable<JsonArray> {
        return parseAPIService.fetchLaborCost(query)
                .map { it.getAsJsonArray("results") }
                .toObservable()
    }

    private fun dataExtractHVAC(query: String): Observable<JsonArray> {
        return parseAPIService.fetchHVAC(query)
                .map { it.getAsJsonArray("results") }
                .toObservable()
    }

    private fun dataExtractMotors(query: String): Observable<JsonArray> {
        return parseAPIService.fetchMotors(query)
                .map { it.getAsJsonArray("results") }
                .toObservable()
    }

    /**
     * UsageHours Hours
     * 1. Pre - Business Hours (Found at PreAudit)
     * 2. Post - Specific Hours (Applicable for the individual Appliances)
     * */
    private fun mappedBusinessHours(): Map<EDay, String?> {
        val usage = mutableListOf<String>()

        for (eDay in EDay.values()) {
            //This key is fixed as the Section Name is being appended to Element Key
            val fixed = "Operation Hours " + eDay.value
            if (preAudit.containsKey(fixed)) {
                usage.add(preAudit[fixed] as String)
            } else {
                usage.add("")
            }
        }

        Timber.d(usage.toString())

        return EDay.values().associateBy({ it }, {
            usage[EDay.values().indexOf(it)]
        })
    }

    private fun mappedSpecificHours(): Map<EDay, String?> {
        val usage = mutableListOf<String>()

        for (eDay in EDay.values()) {
            if (featureData.containsKey(eDay.value)) {
                usage.add(featureData[eDay.value] as String)
            } else {
                usage.add("")
            }
        }

        Timber.d(usage.toString())

        return EDay.values().associateBy({ it }, {
            usage[EDay.values().indexOf(it)]
        })
    }

    /**
     * Computes the Electric Cost
     * */
    fun costElectricity(powerUsed: Double, usageHours: UsageHours, utilityRate: UtilityRate): Double {
        val costElectric = CostElectric(usageHours, utilityRate, true)
        costElectric.structure = electricRateStructure
        costElectric.power = powerUsed

        return costElectric.cost()
    }

    /**
     * Computes the Gas Cost
     * ToDo: The Gas Rate should be an Average calculated from all the Rates for that Year.
     * */
    fun costGas(energyUsed: Double): Double {
        val gas= gasRate.nonTimeOfUse()
        val rateFirst = (gas.summerNone() + gas.winterNone()) / 2
        val rateExcess = (gas.summerExcess() + gas.winterExcess()) / 2

        Timber.d("RATE FIRST :: $rateFirst")
        Timber.d("RATE SECOND :: $rateExcess")

        return (energyUsed) * (if (energyUsed > Gas.FIRST_SLAB) rateExcess else rateFirst)
    }

    /**
     * Parse API Service Call
     * */
    private val parseAPIService by lazy { ParseAPI.create() }

    /**
     * Lighting Config - The enum provides the index to the Map
     * ## Instead of PercentHourReduced this is PercentPowerReduced @ Johnny ##
     * */
    enum class ELightingIndex(val value: Int) {
        LifeHours(0),
        PercentPowerReduced(1),
        Cooling(2)
    }

    fun lightingConfig(type: ELightingType) = when (type) {
                ELightingType.CFL -> listOf(15000.0, 0.25, 0.8)
                ELightingType.Halogen -> listOf(5000.0, 0.75, 0.95)
                ELightingType.Incandescent -> listOf(2500.0, 0.9, 0, 9)
                ELightingType.LinearFluorescent -> listOf(10000.0, 0.85, 0.85) }
}
