package com.yohandeku32.nusamusic.data

import org.w3c.dom.Element
import org.w3c.dom.Node

/** Pretty-print indentation is not an authored word separator in syllable TTML. */
internal fun ttmlTextNode(node: Node): String {
    val value = node.nodeValue.orEmpty()
    if (node.nodeType == Node.CDATA_SECTION_NODE) return value
    if (value.isNotBlank() || value.none { it == '\n' || it == '\r' || it == '\t' }) return value
    val parent = node.parentNode as? Element ?: return value
    if ((0 until parent.childNodes.length).none { parent.childNodes.item(it) is Element }) return value
    var ancestor: Node? = parent
    while (ancestor is Element) {
        when (ancestor.getAttributeNS("http://www.w3.org/XML/1998/namespace", "space")) {
            "preserve" -> return value
            "default" -> return ""
        }
        ancestor = ancestor.parentNode
    }
    return ""
}
