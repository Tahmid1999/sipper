package io.github.tahmid1999.sipper.audit

import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

public data class ParsedProfile(
    val declared: Map<String, List<Double>>,
    val lines: Map<String, Int>,
)

public fun parseProfile(xml: String): ParsedProfile {
    val factory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    }
    val reader = factory.createXMLStreamReader(StringReader(xml))
    val declared = LinkedHashMap<String, List<Double>>()
    val lines = LinkedHashMap<String, Int>()
    var key: String? = null
    var line = 0
    var inArray = false
    val values = ArrayList<Double>()
    val text = StringBuilder()
    try {
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                    "item" -> {
                        key = reader.getAttributeValue(null, "name")
                        line = reader.location.lineNumber
                        inArray = false
                        text.setLength(0)
                    }
                    "array" -> {
                        key = reader.getAttributeValue(null, "name")
                        line = reader.location.lineNumber
                        inArray = true
                        values.clear()
                    }
                    "value" -> text.setLength(0)
                }
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> text.append(reader.text)
                XMLStreamConstants.END_ELEMENT -> when (reader.localName) {
                    "item" -> {
                        val k = key
                        if (k != null) {
                            declared[k] = listOf(text.toString().trim().toDouble())
                            lines[k] = line
                        }
                        key = null
                    }
                    "value" -> if (inArray) values.add(text.toString().trim().toDouble())
                    "array" -> {
                        val k = key
                        if (k != null) {
                            declared[k] = values.toList()
                            lines[k] = line
                        }
                        key = null
                        inArray = false
                    }
                }
            }
        }
    } finally {
        reader.close()
    }
    return ParsedProfile(declared, lines)
}
