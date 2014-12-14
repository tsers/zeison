package org.tsers.zeison

import java.io.InputStream

import net.minidev.json
import net.minidev.json.{JSONAware, JSONArray, JSONObject, JSONValue}

import scala.collection.{JavaConversions, Map}
import scala.util.{Failure, Success, Try}


object Zeison {
  import scala.language.dynamics

  def parse(input: String): JValue = {
    Try(toJValue(JSONValue.parseStrict(input))) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: ${e.getMessage}", e)
    }
  }

  def parse(in: InputStream): JValue = {
    Try(toJValue(JSONValue.parseStrict(in))) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: $e")
    }
  }

  def parse(reader: java.io.Reader): JValue = {
    Try(toJValue(JSONValue.parseStrict(reader))) match {
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


  sealed abstract class JValue extends Dynamic with Traversable[JValue] {
    override def foreach[U](f: (JValue) => U): Unit = this match {
      case JUndefined     =>
      case JNull          =>
      case JArray(values) => JavaConversions.asScalaBuffer(values).map(toJValue).foreach(f)
      case jValue         => f(jValue)
    }

    // ATTENTION: this must be overridden because otherwise traversable trait
    // will cause StackOverflowError
    override def toString() = {
      def className = getClass.getSimpleName
      this match {
        case JNull      => "JNull"
        case JUndefined => "JUndefined"
        case c: JCustom => s"JCustom(${c.value})"
        case jValue     => className + "(" + valueOf(jValue).getOrElse("<invalid>") + ")"
      }
    }

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

    def is[CustomType <: AnyRef: Manifest]: Boolean = this match {
      case custom: JCustom => custom.is(manifest.runtimeClass)
      case _               => false
    }

    def to[CustomType <: AnyRef: Manifest]: CustomType = {
      def extractSafely(custom: JCustom) = {
        if (custom.is[CustomType]) {
          Try(custom.value.asInstanceOf[CustomType]) match {
            case Success(value) => value
            case Failure(cause) => throw new ZeisonException(s"Custom type '${manifest.runtimeClass}' can't be extracted", cause)
          }
        } else {
          throw new ZeisonException(s"$this can't be cast to '${manifest.runtimeClass}'")
        }
      }

      this match {
        case c: JCustom => extractSafely(c)
        case _          => throw new ZeisonException(s"$this can't be cast to '${manifest.runtimeClass}'")
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

  abstract class JCustom extends JValue with JSONAware {
    def value: AnyRef
    def valueAsJson: String

    def is(testedType: Class[_]): Boolean =
      testedType.isAssignableFrom(value.getClass)

    protected def toJSONString: String = valueAsJson
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
      case null                    => JNull
      case value: Boolean          => JBoolean(value)
      case value: Byte             => JInt(value)
      case value: Short            => JInt(value)
      case value: Int              => JInt(value)
      case value: Long             => JInt(value)
      case value: Float            => JDouble(value)
      case value: Double           => JDouble(value)
      case value: BigDecimal       => JDouble(value.doubleValue())
      case value: java.lang.Float  => JDouble(value.toDouble)
      case value: java.lang.Double => JDouble(value)
      case value: Number           => JInt(value.longValue())
      case value: Char             => JString(value.toString)
      case value: String           => JString(value)
      case value: JSONObject       => JObject(value)
      case value: JSONArray        => JArray(value)
      case value: JCustom          => value
      case value                   => throw new ZeisonException(s"Can't parse value ($value) to JValue")
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
      case custom: JCustom => Some(custom)
    }
  }
}
