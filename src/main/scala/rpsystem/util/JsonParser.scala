package rpsystem.util

import com.google.gson.Gson

object JsonParser {
  def toJson(obj: AnyRef): String = {
    val gson: Gson = new Gson()
    return gson.toJson(obj)
  }

  def fromJson[T](json: String, classOfT: Class[T]): T = {
    val gson: Gson = new Gson()
    return gson.fromJson(json, classOfT)
  }
}
