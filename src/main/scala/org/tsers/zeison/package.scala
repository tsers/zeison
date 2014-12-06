package org.tsers

import java.lang

import net.minidev.json.{JSONArray, JSONObject, JSONValue}

import scala.collection.JavaConversions


package object zeison {
  import scala.language.dynamics

  def parse(input: String): JValue = {
    jvalueFromValue(JSONValue.parse(input))
  }

  def render(json: JValue): String = {
    json match {
      case JNull           => "null"
      case JBoolean(value) => JSONValue.toJSONString(value)
      case JInt(value)     => JSONValue.toJSONString(value)
      case JDouble(value)  => JSONValue.toJSONString(value)
      case JString(value)  => JSONValue.toJSONString(value)
      case JObject(value)  => JSONValue.toJSONString(value)
      case JArray(value)   => JSONValue.toJSONString(value)
    }
  }


  private def traverseObject(obj: JSONObject, field: String): JValue = {
    if (obj.containsKey(field)) {
      jvalueFromValue(obj.get(field))
    } else {
      throw new RuntimeException(s"Field $field not found")
    }
  }

  private def traverseArray(arr: JSONArray, index: Int): JValue = {
    val len = arr.size()
    if (index >= 0 && index < len) {
      jvalueFromValue(arr.get(index))
    } else {
      throw new IndexOutOfBoundsException(s"JSON array index ($index) out of bounds: 0..${len-1}")
    }
  }

  private def jvalueFromValue(anyValue: Any): JValue = {
    anyValue match {
      case null              => JNull
      case value: Boolean    => JBoolean(value)
      case value: Int        => JInt(value)
      case value: String     => JString(value)
      case value: Double     => JDouble(value)
      case value: JSONObject => JObject(value)
      case value: JSONArray  => JArray(value)
    }
  }

  implicit class IterableJValue(jValue: JValue) extends Iterable[JValue] {
    def iterator: Iterator[JValue] = jValue match {
      case JArray(value) => JavaConversions.asScalaIterator(value.iterator()).map(jvalueFromValue)
      case _             => throw new RuntimeException(s"Child iterators not supported by clsee: $getClass")
    }
  }

  implicit def iterableToJValue(iterable: Iterable[JValue]) = {
    val arr = new JSONArray()
    iterable.foreach {
      case JNull           => arr.add(null)
      case JBoolean(value) => arr.add(new lang.Boolean(value))
      case JInt(value)     => arr.add(new lang.Integer(value))
      case JDouble(value)  => arr.add(new lang.Double(value))
      case JString(value)  => arr.add(value)
      case JObject(value)  => arr.add(value)
      case JArray(value)   => arr.add(value)
    }
    JArray(arr)
  }

  sealed abstract class JValue extends Dynamic {

    def selectDynamic(field: String): JValue = this match {
      case JObject(value) => traverseObject(value, field)
      case _              => throw new OperationNotSupported(field, getClass)
    }

    def apply(key: Any): JValue = (key, this) match {
      case (field: String, JObject(value)) => traverseObject(value, field)
      case (index: Int, JArray(value))     => traverseArray(value, index)
    }

    def applyDynamic(field: String)(key: Any): JValue = {
      selectDynamic(field).apply(key)
    }

    def asJValue: JValue = this

    def asBoolean: Boolean = this match {
      case JBoolean(value) => value
      case _               => throw new OperationNotSupported("asBoolean", getClass)
    }

    def asString: String = this match {
      case JString(value) => value
      case _              => throw new OperationNotSupported("asString", getClass)
    }

    def asInt: Int = this match {
      case JInt(value) => value
      case _           => throw new OperationNotSupported("asInt", getClass)
    }

    def asDouble: Double = this match {
      case JDouble(value) => value
      case _           => throw new OperationNotSupported("asDouble", getClass)
    }

  }


  case object JNull extends JValue

  case class JBoolean(value: Boolean) extends JValue

  case class JInt(value: Int) extends JValue

  case class JString(value: String) extends JValue

  case class JDouble(value: Double) extends JValue

  case class JObject(value: JSONObject) extends JValue

  case class JArray(value: JSONArray) extends JValue

  class OperationNotSupported(op: String, clz: Class[_]) extends RuntimeException(s"'.$op' is not supported by class: $clz")

}
