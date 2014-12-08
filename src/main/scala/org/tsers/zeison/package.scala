package org.tsers

import java.io.InputStream

import net.minidev.json.{JSONArray, JSONObject, JSONValue}

import scala.collection.Map
import scala.util.{Try, Failure, Success}


package object zeison {
  import scala.language.dynamics

  def parse(input: String): JValue = {
    Try(toJValue(JSONValue.parseWithException(input))) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: $e")
    }
  }

  def parse(in: InputStream): JValue = {
    Try(toJValue(JSONValue.parseWithException(in))) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: $e")
    }
  }

  def parse(reader: java.io.Reader): JValue = {
    Try(toJValue(JSONValue.parseWithException(reader))) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: $e")
    }
  }

  def render(json: JValue): String = {
    json match {
      case JUndefined      => throw new ZeisonException("Can't render undefined value")
      case JNull           => "null"
      case JBoolean(value) => JSONValue.toJSONString(value)
      case JInt(value)     => JSONValue.toJSONString(value)
      case JDouble(value)  => JSONValue.toJSONString(value)
      case JString(value)  => JSONValue.toJSONString(value)
      case JObject(value)  => JSONValue.toJSONString(value)
      case JArray(value)   => JSONValue.toJSONString(value)
    }
  }


  sealed abstract class JValue extends Dynamic {
    def applyDynamic(field: String)(key: Any): JValue = {
      selectDynamic(field).apply(key)
    }

    def apply(key: Any): JValue = (key, this) match {
      case (field: String, _)           => selectDynamic(field)
      case (index: Int, JArray(value))  => traverseArray(value, index)
      case (index: Int, JUndefined)     => throw new ZeisonException(s"Can't get element [$index] from undefined")
      case _                            => JUndefined
    }

    def selectDynamic(field: String): JValue = this match {
      case JUndefined     => throw new ZeisonException(s"Can't get field [$field] from undefined")
      case JObject(value) => traverseObject(value, field)
      case _              => JUndefined
    }

    def asJValue: JValue = this

    def isBool: Boolean = this match {
      case JBoolean(_) => true
      case _           => false
    }

    def toBool: Boolean = this match {
      case JBoolean(value) => value
      case _               => throw new ZeisonException(s"$this can't be cast to boolean")
    }

    def isInt: Boolean = this match {
      case JInt(_) => true
      case _       => false
    }

    def toLong: Long = this match {
      case JInt(value) => value
      case _           => throw new ZeisonException(s"$this can't be cast to number")
    }

    def toInt: Int = this.toLong.toInt

    def isDouble: Boolean = this match {
      case JDouble(_) => true
      case _          => false
    }

    def toDouble: Double = this match {
      case JDouble(value) => value
      case _              => throw new ZeisonException(s"$this can't be cast to double")
    }

    def isStr: Boolean = this match {
      case JString(_) => true
      case _          => false
    }

    def toStr: String = this match {
      case JString(value) => value
      case _              => throw new ZeisonException(s"$this can't be cast to string")
    }

    def isArray: Boolean = this match {
      case JArray(_) => true
      case _         => false
    }

    def toSeq: Seq[JValue] = {
      import scala.collection.JavaConversions._
      this match {
        case JArray(value) => asScalaBuffer(value).map(toJValue)
        case _             => throw new ZeisonException(s"$this can't be cast to seq")
      }
    }

    def isObject: Boolean = this match {
      case JObject(_) => true
      case _          => false
    }

    def toMap: Map[String, JValue] = {
      import scala.collection.JavaConversions._
      this match {
        case JObject(value) => mapAsScalaMap(value).mapValues(toJValue)
        case _              => throw new ZeisonException(s"$this can't be cast to map")
      }
    }
  }

  implicit class TraversableJValue(jValue: JValue) extends Traversable[JValue] {
    import scala.collection.JavaConversions._
    override def foreach[U](f: (JValue) => U): Unit = {
      jValue match {
        case JArray(value) => asScalaBuffer(value).map(toJValue).foreach(f)
        case _             =>
      }
    }
  }

  case object JUndefined extends JValue

  case object JNull extends JValue

  case class JBoolean(value: Boolean) extends JValue

  case class JInt(value: Long) extends JValue

  case class JString(value: String) extends JValue

  case class JDouble(value: Double) extends JValue

  case class JObject(value: JSONObject) extends JValue

  case class JArray(value: JSONArray) extends JValue


  class ZeisonException(msg: String) extends RuntimeException(msg)


  private def traverseObject(obj: JSONObject, field: String): JValue = {
    if (obj.containsKey(field)) {
      toJValue(obj.get(field))
    } else {
      JUndefined
    }
  }

  private def traverseArray(arr: JSONArray, index: Int): JValue = {
    val len = arr.size()
    if (index >= 0 && index < len) {
      toJValue(arr.get(index))
    } else {
      JUndefined
    }
  }

  private def toJValue(anyValue: Any): JValue = {
    anyValue match {
      case null              => JNull
      case value: Boolean    => JBoolean(value)
      case value: Int        => JInt(value)
      case value: Long       => JInt(value)
      case value: Float      => JDouble(value)
      case value: Double     => JDouble(value)
      case value: BigDecimal => JDouble(value.doubleValue())
      case value: String     => JString(value)
      case value: JSONObject => JObject(value)
      case value: JSONArray  => JArray(value)
      case value             => throw new ZeisonException(s"Can't parse value ($value) to JValue")
    }
  }

  /*
  private def valueOf(jValue: JValue): Option[AnyRef] = {
    jValue match {
      case JUndefined      => None
      case JNull           => Some(null)
      case JBoolean(value) => Some(new lang.Boolean(value))
      case JInt(value)     => Some(new lang.Long(value))
      case JDouble(value)  => Some(new lang.Double(value))
      case JString(value)  => Some(value)
      case JObject(value)  => Some(value)
      case JArray(value)   => Some(value)
    }
  }
  */
}
