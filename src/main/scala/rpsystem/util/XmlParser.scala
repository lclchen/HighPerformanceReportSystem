/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.util

import com.thoughtworks.xstream._

/** Transform between XML and Scala object */
object XmlParser {

  /** Transform a Scala Object to a XML string.
    * @param obj Scala Object.
    * @return String of XML.
    */
  def toXml(obj: AnyRef): String = {
    val xstream:XStream = new XStream()
    return xstream.toXML(obj)
  }

  /** Transform a XML String to a Scala Object.
    * @param xml String of XML.
    * @return Scala Object.
    */
  def fromXml(xml: String): AnyRef = {
    val xstream: XStream = new XStream()
    return xstream.fromXML(xml)
  }
}
