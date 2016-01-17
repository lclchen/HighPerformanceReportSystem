package rpsystem.util

import com.thoughtworks.xstream._

object XmlParser {
  def toXml(obj: AnyRef): String = {
    val xstream:XStream = new XStream()
    return xstream.toXML(obj)
  }

  def fromXml(xml: String): AnyRef = {
    val xstream: XStream = new XStream()
    return xstream.fromXML(xml)
  }
}
