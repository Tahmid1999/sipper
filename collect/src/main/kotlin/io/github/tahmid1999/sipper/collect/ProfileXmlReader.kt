package io.github.tahmid1999.sipper.collect

import io.github.tahmid1999.sipper.audit.ParsedProfile
import org.xmlpull.v1.XmlPullParser

/**
 * The on-device twin of `parseProfile`. It has to keep the literal tokens too: AUDIT's value column
 * and the route digest both read them, and without them every digest is sha256 of nothing.
 */
public fun readProfile(parser: XmlPullParser): ParsedProfile {
    val declared = LinkedHashMap<String, List<Double>>()
    val lines = LinkedHashMap<String, Int>()
    val tokens = LinkedHashMap<String, List<String>>()
    var key: String? = null
    var line = 0
    var inArray = false
    val values = ArrayList<Double>()
    val tokenList = ArrayList<String>()
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
                    tokenList.clear()
                }
                "value" -> text.setLength(0)
            }
            XmlPullParser.TEXT -> text.append(parser.text)
            XmlPullParser.END_TAG -> when (parser.name) {
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
        parser.next()
    }

    return ParsedProfile(declared, lines, tokens)
}
