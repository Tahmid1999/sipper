package io.github.tahmid1999.sipper.audit

import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * `tokens` carries each key's literal text as written in the file. AUDIT's value column renders
 * the token, not a formatted Double (CONTRACT §4), and the route digest normalises over tokens
 * rather than parsed values precisely so `0` and `0.0` do not collide (the digest rule,
 * CONTRACT §1.2). The parse still happens; this keeps the input.
 */
public data class ParsedProfile(
    val declared: Map<String, List<Double>>,
    val lines: Map<String, Int>,
    val tokens: Map<String, List<String>> = emptyMap(),
)

public fun parseProfile(xml: String): ParsedProfile {
    val factory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    }
    val reader = factory.createXMLStreamReader(StringReader(xml))
    val declared = LinkedHashMap<String, List<Double>>()
    val lines = LinkedHashMap<String, Int>()
    val tokens = LinkedHashMap<String, List<String>>()
    var key: String? = null
    var line = 0
    var inArray = false
    val values = ArrayList<Double>()
    val tokenList = ArrayList<String>()
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
                            val token = text.toString().trim()
                            declared[k] = listOf(token.toDouble())
                            tokens[k] = listOf(token)
                            lines[k] = line
                        }
                        key = null
                    }
                    "value" -> if (inArray) {
                        val token = text.toString().trim()
                        values.add(token.toDouble())
                        tokenList.add(token)
                    }
                    "array" -> {
                        val k = key
                        if (k != null) {
                            declared[k] = values.toList()
                            tokens[k] = tokenList.toList()
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
    return ParsedProfile(declared, lines, tokens)
}
