package io.github.tahmid1999.sipper.collect

import io.github.tahmid1999.sipper.audit.ParsedProfile
import org.xmlpull.v1.XmlPullParser

public fun readProfile(parser: XmlPullParser): ParsedProfile {
    val declared = LinkedHashMap<String, List<Double>>()
    val lines = LinkedHashMap<String, Int>()
    var key: String? = null
    var line = 0
    var inArray = false
    val values = ArrayList<Double>()
    val text = StringBuilder()

    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "item" -> {
                    key = parser.getAttributeValue(null, "name")
                    line = parser.lineNumber
                    inArray = false
                    text.setLength(0)
                }
                "array" -> {
                    key = parser.getAttributeValue(null, "name")
                    line = parser.lineNumber
                    inArray = true
                    values.clear()
                }
                "value" -> text.setLength(0)
            }
            XmlPullParser.TEXT -> text.append(parser.text)
            XmlPullParser.END_TAG -> when (parser.name) {
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
        parser.next()
    }

    return ParsedProfile(declared, lines)
}
