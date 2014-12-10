package org.tsers

import java.io.InputStream

import net.minidev.json
import net.minidev.json.{JSONArray, JSONObject, JSONValue}

import scala.collection.{JavaConversions, Map}
import scala.util.{Failure, Success, Try}


package object zeison {
  import scala.language.dynamics

  def parse(input: String): JValue = {
    Try(toJValue(JSONValue.parseWithException(input))) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: ${e.getMessage}", e)
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

  object obj {
    def apply(fields: (String, Any)*): JObject = from(fields)

    def from(fields: Iterable[(String, _)]): JObject = {
      val valueMap = fields
        .flatMap {
          case (f: String, jValue: JValue) => valueOf(jValue).map(value => (f, value))
          case (f: String, value)          => Some(f, toAnyRef(value))
        }
        .toMap

      JObject(new JSONObject(JavaConversions.mapAsJavaMap(valueMap)))
    }

    def empty = JObject(new JSONObject())
  }

  object arr {
    def apply(elems: Any*): JArray = from(elems)

    def from(iterable: Iterable[_]): JArray = {
      val arr = new json.JSONArray()
      iterable
        .flatMap {
          case jValue: JValue => valueOf(jValue)
          case value          => Some(toAnyRef(value))
        }
        .foreach(arr.add)

      JArray(arr)
    }

    def empty = JArray(new json.JSONArray())
  }


  def render(json: JValue): String = {
    valueOf(json) match {
      case Some(value) => JSONValue.toJSONString(value)
      case None        => throw new ZeisonException("Can't render undefined value")
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

    def isDefined: Boolean = this match {
      case JUndefined => false
      case _          => true
    }

    def isNull: Boolean = this match {
      case JNull => true
      case _     => false
    }

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

    def toOption: Option[JValue] = this match {
      case JUndefined => None
      case JNull      => None
      case jValue     => Some(jValue)
    }
  }

  implicit class TraversableJValue(jValue: JValue) extends Traversable[JValue] {
    override def foreach[U](f: (JValue) => U): Unit = {
      jValue match {
        case JArray(value) => JavaConversions.asScalaBuffer(value).map(toJValue).foreach(f)
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

  case class JObject(value: JSONObject) extends JValue {
    override def equals(o: Any) = o match {
      case JObject(otherVal) => value.toString == otherVal.toString  // TODO: more efficient way
      case _                 => false
    }
  }

  case class JArray(value: JSONArray) extends JValue {
    override def equals(o: Any) = o match {
      case JArray(otherVal) => value.toString == otherVal.toString   // TODO: more efficient way
      case _                => false
    }
  }


  class ZeisonException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)


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
      case value: Char       => JString(value.toString)
      case value: String     => JString(value)
      case value: JSONObject => JObject(value)
      case value: JSONArray  => JArray(value)
      case value             => throw new ZeisonException(s"Can't parse value ($value) to JValue")
    }
  }

  private def toAnyRef(anyValue: Any): AnyRef = {
    anyValue match {
      case null              => null
      case value: Boolean    => new java.lang.Boolean(value)
      case value: Byte       => new java.lang.Long(value)
      case value: Short      => new java.lang.Long(value)
      case value: Int        => new java.lang.Long(value)
      case value: Long       => new java.lang.Long(value)
      case value: Float      => new java.lang.Double(value)
      case value: Double     => new java.lang.Double(value)
      case value: Char       => value.toString
      case value: AnyRef     => value
      case value             => throw new ZeisonException(s"Unsupported value type (${value.getClass}): $value")
    }
  }

  private def valueOf(jValue: JValue): Option[AnyRef] = {
    jValue match {
      case JUndefined      => None
      case JNull           => Some(null)
      case JBoolean(value) => Some(new java.lang.Boolean(value))
      case JInt(value)     => Some(new java.lang.Long(value))
      case JDouble(value)  => Some(new java.lang.Double(value))
      case JString(value)  => Some(value)
      case JObject(value)  => Some(value)
      case JArray(value)   => Some(value)
    }
  }
}
