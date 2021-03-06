package org.tsers.zeison

import java.io.InputStream
import java.nio.channels.Channels
import java.util

import org.typelevel.jawn.{Facade, Parser}

import scala.util.{Failure, Success, Try}


object Zeison {
  import scala.collection.immutable
  import scala.language.dynamics
  import org.tsers.zeison.Zeison.internal._


  /*
   * JSON parsing
   */

  def parse(input: String): JValue = {
    implicit val facade = new ZeisonFacade
    Parser.parseFromString(input) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: ${e.getMessage}", e)
    }
  }

  def parse(in: InputStream): JValue = {
    implicit val facade = new ZeisonFacade
    Parser.parseFromChannel(Channels.newChannel(in)) match {
      case Success(json) => json
      case Failure(e)    => throw new ZeisonException(s"JSON parsing failed: ${e.getMessage}", e)
    }
  }

  /*
   * JSON building
   */

  def toJson(any: Any): JValue = toJValue(any)

  def toJObject(fields: (String, Any)*): JObject = toJValue(fields.toMap).asInstanceOf[JObject]

  def toJArray(elems: Any*): JArray = toJValue(elems).asInstanceOf[JArray]

  /*
   * JSON rendering
   */
  def render(json: JValue): String = Rendering.render(json, pretty = false)

  def renderPretty(json: JValue): String = Rendering.render(json, pretty = true)


  sealed abstract class JValue extends Dynamic with Traversable[JValue] {
    override def foreach[U](f: (JValue) => U): Unit = this match {
      case JUndefined     =>
      case JNull          =>
      case JArray(values) => values.foreach(f)
      case jval           => f(jval)
    }

    override def iterator = this match {
      case JUndefined     => List().iterator
      case JNull          => List().iterator
      case JArray(values) => values.iterator
      case jval           => List(jval).iterator
    }

    // ATTENTION: this must be overridden because otherwise traversable trait
    // will cause StackOverflowError
    override def toString() = {
      def str(value: Any) = s"${getClass.getSimpleName}($value)"
      def extractNum(num: JNum): Any = {
        if (num.hasDecimals) num.valueAsDouble else num.valueAsLong
      }

      this match {
        case JNull           => "JNull"
        case JUndefined      => "JUndefined"
        case num: JNum       => str(extractNum(num))
        case JBoolean(value) => str(value)
        case JString(value)  => str(value)
        case JObject(value)  => str(value)
        case JArray(value)   => str(value)
        case custom: JCustom => str(custom.value)
      }
    }

    def applyDynamic(field: String)(key: Any): JValue = {
      selectDynamic(field).apply(key)
    }

    def apply(key: Any): JValue = (key, this) match {
      case (field: String, _)                                          => selectDynamic(field)
      case (idx: Int, JArray(elems)) if idx >= 0 && elems.length > idx => elems(idx)
      case (index: Int, JUndefined)                                    => throw new ZeisonException(s"Can't get element [$index] from undefined")
      case _                                                           => JUndefined
    }

    def selectDynamic(field: String): JValue = this match {
      case JUndefined      => throw new ZeisonException(s"Can't get field [$field] from undefined")
      case JObject(fields) => fields.getOrElse(field, JUndefined)
      case _               => JUndefined
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
      case n: JNum if !n.hasDecimals => true
      case _                         => false
    }

    def toLong: Long = this match {
      case n: JNum => n.valueAsLong
      case _       => throw new ZeisonException(s"$this can't be cast to integer value")
    }

    def toInt: Int = this.toLong.toInt

    def isDouble: Boolean = this match {
      case n: JNum if n.hasDecimals => true
      case _                        => false
    }

    def toDouble: Double = this match {
      case n: JNum => n.valueAsDouble
      case _       => throw new ZeisonException(s"$this can't be cast to double")
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

    def toMap: Map[String, JValue] = this match {
      case JObject(fields) => fields
      case _               => throw new ZeisonException(s"$this can't be cast to map")
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

    def copy(fields: (String, Any)*): JValue = {
      this match {
        case JObject(old) => JObject(applyFieldChanges(old, fields.map { case (name, value) => (name, toJValue(value)) }))
        case _            => throw new ZeisonException(s"Can't modify fields from $this")
      }
    }
  }

  case object JUndefined extends JValue

  case object JNull extends JValue

  case class JBoolean(value: Boolean) extends JValue

  sealed abstract class JNum extends JValue {
    def hasDecimals: Boolean
    def valueAsLong: Long
    def valueAsDouble: Double
    def valueAsString: String
    override def equals(other: Any) = other match {
      case n: JNum => valueAsString == n.valueAsString
      case _       => false
    }
  }

  case class JParsedNum(value: String) extends JNum {
    def hasDecimals    = value.exists(ch => ch == '.' || ch == 'e' || ch == 'E')
    def valueAsLong    = if (hasDecimals) throw new ZeisonException(s"$this can't be cast to integer value") else value.toLong
    def valueAsDouble  = value.toDouble
    def valueAsString  = value
  }

  case class JInt(value: Long) extends JNum {
    def hasDecimals    = false
    def valueAsLong    = value
    def valueAsDouble  = value.toDouble
    def valueAsString  = value.toString
  }

  case class JDouble(value: Double) extends JNum {
    def hasDecimals    = true
    def valueAsLong    = throw new ZeisonException(s"$this can't be cast to integer value")
    def valueAsDouble  = value
    def valueAsString  = value.toString
  }

  case class JString(value: String) extends JValue

  case class JObject(fields: Map[String, JValue]) extends JValue

  case class JArray(elems: Vector[JValue]) extends JValue

  abstract class JCustom extends JValue {
    def value: AnyRef
    def valueAsJson: String
    def is(testedType: Class[_]): Boolean = testedType.isAssignableFrom(value.getClass)
  }


  class ZeisonException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)


  private[zeison] object internal {

    class ZeisonFacade extends Facade.MutableFacade[JValue] {
      def jarray(arr: scala.collection.mutable.ArrayBuffer[JValue]) = JArray(arr.toVector)
      def jobject(obj: scala.collection.mutable.Map[String, JValue]) = JObject(obj.toMap)
      def jnull = JNull
      def jfalse = JBoolean(false)
      def jtrue = JBoolean(true)
      def jnum(s: CharSequence, decIndex: Int, expIndex: Int) = JParsedNum(s.toString)
      def jstring(s: CharSequence) = JString(s.toString)
    }

    def applyFieldChanges(obj: Map[String, JValue], changes: Iterable[(String, JValue)]): Map[String, JValue] = {
      changes.foldLeft(obj) { (result, change) => 
        change match {
          case (name, JUndefined) => result - name
          case (name, value)      => result + (name -> value)
        }
      }
    }

    def toJValue(anyValue: Any): JValue = {
      import scala.{collection => col}
      (anyValue: @unchecked) match {
        case null                          => JNull
        case value: JValue                 => value
        case valueOpt: Option[_]           => valueOpt.map(toJValue).getOrElse(JUndefined)
        case value: Boolean                => JBoolean(value)
        case value: Float                  => JDouble(value)
        case value: Double                 => JDouble(value)
        case value: BigDecimal             => JDouble(value.toDouble)
        case value: Number                 => JInt(value.longValue())
        case value: Char                   => JString(value.toString)
        case value: String                 => JString(value)
        case f: scala.collection.Map[_,_]  => JObject(f.flatMap { case (k, v) => toOption(toJValue(v)).map((k.toString, _)) }.toMap)
        case elems: col.TraversableOnce[_] => JArray(elems.flatMap(e => toOption(toJValue(e))).toVector)
        case elems: Array[_]               => JArray(elems.flatMap(e => toOption(toJValue(e))).toVector)
        case value                         => throw new ZeisonException(s"Can't parse value ($value) to JValue")
      }
    }

    def toOption(jValue: JValue): Option[JValue] = {
      if (jValue.isDefined) Some(jValue) else None
    }

  }
}
