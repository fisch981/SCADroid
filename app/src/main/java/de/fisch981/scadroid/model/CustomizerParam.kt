package de.fisch981.scadroid.model

sealed class CustomizerParam(
    open val name: String,
    open val label: String,
    open val group: String = "Allgemein"
) {
    data class NumberParam(
        override val name: String,
        override val label: String,
        override val group: String = "Allgemein",
        val value: Double,
        val min: Double? = null,
        val max: Double? = null,
        val step: Double? = null
    ) : CustomizerParam(name, label, group)

    data class BooleanParam(
        override val name: String,
        override val label: String,
        override val group: String = "Allgemein",
        val value: Boolean
    ) : CustomizerParam(name, label, group)

    data class StringParam(
        override val name: String,
        override val label: String,
        override val group: String = "Allgemein",
        val value: String,
        val allowedValues: List<String> = emptyList()
    ) : CustomizerParam(name, label, group)
}
