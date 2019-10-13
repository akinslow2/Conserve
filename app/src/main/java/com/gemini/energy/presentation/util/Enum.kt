package com.gemini.energy.presentation.util

/*
* Zone - Type
* */
enum class EZoneType(val value: String) {

    Plugload("Plugload"),
    HVAC("HVAC"),
    WaterHeater("WaterHeater"),
    Lighting("Lighting"),
    Motors("Motors"),
    Others("Others");

    companion object {
        private val valueMap = EZoneType.values().associateBy(EZoneType::value)
        private val ordinalMap = EZoneType.values().associateBy(EZoneType::ordinal)

        fun get(type: String) = valueMap[type]
        fun get(id: Int) = ordinalMap[id]
        fun count() = valueMap.size
    }

}


/*
* Stack Operation for Type Counter
* */
enum class EAction(val value: String) {

    Push("push"),
    Pop("pop");

}


/*
* Plugload - Appliance Type
* */
enum class EApplianceType(val value: String) {

    CombinationOven("Combination Oven"),
    ConvectionOven("Convection Oven"),
    ConveyorOven("Conveyor Oven"),
    Fryer("Fryer"),
    IceMaker("Ice Maker"),
    RackOven("Rack Oven"),
    Refrigerator("Refrigerator"),
    SteamCooker("Steam Cooker"),

    Griddle("Griddle"),
    HotFoodCabinet("Hot Food Cabinet"),
    ConveyorBroiler("Conveyor Broiler"),
    DishWasher("Dish Washer"),
    PreRinseSpray("Pre Rinse Spray"),

    //**Add to enum class EApplianceType **//
    SampleAppliance("Sample Appliance");

    companion object {
        private val map = EApplianceType.values()
                .associateBy(EApplianceType::value)

        fun get(type: String) = map[type]
        fun options() = EApplianceType.values().map { it.value }.sorted()
    }

}


/*
* Plugload - Lighting Type
* */
enum class ELightingType(val value: String) {

    Halogen("Halogen"),
    CFL("CFL"),
    LinearFluorescent("Linear Fluorescent"),
    Incandescent("Incandescent"),
    HPSodium("High Pressure Sodium"),
    LPSodium("Low Pressure Sodium");

    companion object {
        private val map = ELightingType.values()
                .associateBy(ELightingType::value)

        fun get(type: String) = map[type]
        fun options() = ELightingType.values().map { it.value }.sorted()
    }

}


/*
* Base Row Type
* */
enum class BaseRowType(val value: String) {

    TextRow("textrow"),
    IntRow("introw"),
    DecimalRow("decimalrow"),
    PickerInputRow("pickerinputrow"),
    PhoneRow("phonerow"),
    EmailRow("emailrow"),
    TextAreaRow("textarearow");

    companion object {
        private val map = BaseRowType.values().associateBy(BaseRowType::value)
        fun get(type: String) = map[type]
    }

}


/**
 * Operating Hours
 * */
enum class EDay(val value: String) {

    Mon("Monday Operating Hours"),
    Tue("Tuesday Operating Hours"),
    Wed("Wednesday Operating Hours"),
    Thu("Thursday Operating Hours"),
    Fri("Friday Operating Hours"),
    Sat("Saturday Operating Hours"),
    Sun("Sunday Operating Hours");

    companion object {
        private val map = EDay.values().associateBy(EDay::value)
        fun get(day: String) = map[day]
    }

}


/**
 * UtilityRate Rate | Slabs
 * */
enum class ERateKey(val value: String) {
    SummerOff("summer-off-peak"),
    SummerPart("summer-part-peak"),
    SummerOn("summer-on-peak"),
    WinterOff("winter-off-peak"),
    WinterPart("winter-part-peak"),

    SummerNone("summer-none"),
    WinterNone("winter-none"),

    AverageElectric("average"),

    Slab1("0_5.0"),
    Slab2("5.1_16.0"),
    Slab3("16.1_41.0"),
    Slab4("41.1_123.0"),
    Slab5("123.1_n_up"),
    SummerTransport("summer_first_4000_therms"),
    SummerExcess("summer_excess"),
    WinterTransport("winter_first_4000_therms"),
    WinterExcess("winter_excess"),
    Surcharge("surcharge"),

    GasWinter("winter_first_4000_therms"),
    GasSummer("summer_first_4000_therms"),
    GasFirstSlab("4000"),

    None("none");

    companion object {

        private val map = ERateKey.values().associateBy(ERateKey::value)
        fun get(rateKey: String) = map[rateKey]

        fun getAllElectric() = listOf(SummerOff, SummerPart, SummerOn, WinterOff,
                WinterPart, SummerNone, WinterNone)

    }

}
