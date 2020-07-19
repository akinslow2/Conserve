package com.gemini.energy.CompanyCamAPI.Requests

data class CreateTagRequest (
    val tag: DisplayValue
) {
    constructor(tagName: String) : this(
        tag = DisplayValue(tagName)
    )
}

data class DisplayValue (
        val display_value: String)