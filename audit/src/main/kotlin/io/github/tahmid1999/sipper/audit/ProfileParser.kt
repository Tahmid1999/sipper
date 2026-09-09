package io.github.tahmid1999.sipper.audit

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

public fun parseProfile(xml: String): Map<String, List<Double>> {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    val declared = LinkedHashMap<String, List<Double>>()
    val children = doc.documentElement.childNodes
    for (i in 0 until children.length) {
        val node = children.item(i)
        if (node !is Element) continue
        val key = node.getAttribute("name")
        when (node.tagName) {
            "item" -> declared[key] = listOf(node.textContent.trim().toDouble())
            "array" -> {
                val values = ArrayList<Double>()
                val valueNodes = node.getElementsByTagName("value")
                for (j in 0 until valueNodes.length) {
                    values.add(valueNodes.item(j).textContent.trim().toDouble())
                }
                declared[key] = values
            }
        }
    }
    return declared
}
