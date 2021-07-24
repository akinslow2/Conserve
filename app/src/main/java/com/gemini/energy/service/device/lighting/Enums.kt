package com.gemini.energy.service.device.lighting

enum class ReplacementType(val type: String) {
    LampReplacement("Lamp Replacement"),
    Retrofit("Retrofit"),
    DirectReplacementFixture("Direct Replacement Fixture")
}

enum class CeilingType(val type: String) {
    DropCeiling("Drop Ceiling"),
    DryWall("Dry Wall")
}

enum class LfFixtureType(val type: String) {
    twoXtwo("2X2"),
    twoXfour("2X4")
}

enum class CeilingHeight(val type: String) {
    GreaterThan10Feet("Yes"),
    LessThan10Feet("No")
}