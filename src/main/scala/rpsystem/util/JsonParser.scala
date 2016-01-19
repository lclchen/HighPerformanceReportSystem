/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.util

import com.google.gson.Gson

/** Transform between Json and Scala Object. */
object JsonParser {
  /** Transform a Scala Object into a json String.
    * @param obj Scala object.
    * @return String of the json.
    */
  def toJson(obj: AnyRef): String = {
    val gson: Gson = new Gson()
    return gson.toJson(obj)
  }

  /** Transform a json String into a Scala Object.
    * @param json String of a json.
    * @param classOfT The Class type of the Scala Object.
    * @tparam T The Class type.
    * @return Scala Object.
    */
  def fromJson[T](json: String, classOfT: Class[T]): T = {
    val gson: Gson = new Gson()
    return gson.fromJson(json, classOfT)
  }
}
