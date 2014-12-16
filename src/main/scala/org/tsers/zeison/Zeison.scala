package org.tsers.zeison

import java.io.InputStream
import java.nio.channels.Channels

import jawn.{FContext, Facade, Parser}

import scala.util.{Failure, Success, Try}


object Zeison {
  import scala.language.dynamics
  import scala.collection.immutable
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

  def from(any: Any): JValue = {
    toJValue(any)
  }

  object obj {
    def empty = JObject(immutable.ListMap.empty)

    @deprecated
    def from(fields: Map[String, Any]) = Zeison.from(fields)

    def apply(fields: (String, Any)*): JObject = {
      Zeison.from(immutable.ListMap(fields.toList: _*)).asInstanceOf[JObject]
    }
  }

  object arr {
    def empty = JArray(Vector.empty)

    @deprecated
    def from(elems: Iterable[Any]) = Zeison.from(elems)

    def apply(elems: Any*): JArray = {
      Zeison.from(elems.toIterable).asInstanceOf[JArray]
    }
  }


  /*
   * JSON rendering
   */
  def render(json: JValue): String = Rendering.render(json)


  sealed abstract class JValue extends Dynamic with Traversable[JValue] {
    override def foreach[U](f: (JValue) => U): Unit = this match {
      case JUndefined     =>
      case JNull          =>
      case JArray(values) => values.foreach(f)
      case jValue         => f(jValue)
    }

    // ATTENTION: this must be overridden because otherwise traversable trait
    // will cause StackOverflowError
    override def toString() = {
      def className = getClass.getSimpleName
      this match {
        case JNull      => "JNull"
        case JUndefined => "JUndefined"
        case jValue     => className + "(" + valueOf(jValue).getOrElse("<invalid>") + ")"
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
      case n: JNum if n._isInt => true
      case _                   => false
    }

    def toLong: Long = this match {
      case n: JNum if n._isInt => n._toLong
      case _                   => throw new ZeisonException(s"$this can't be cast to number")
    }

    def toInt: Int = this.toLong.toInt

    def isDouble: Boolean = this match {
      case n: JNum if !n._isInt => true
      case _                    => false
    }

    def toDouble: Double = this match {
      case n: JNum => n._toDbl
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
      case JObject(fields) => fields.toMap
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
  }

  case object JUndefined extends JValue

  case object JNull extends JValue

  case class JBoolean(value: Boolean) extends JValue

  case class JNum(value: String) extends JValue {
    private[zeison] def _isInt = !value.exists(ch => ch == '.' || ch == 'e' || ch == 'E')
    private[zeison] def _toLong = value.toLong
    private[zeison] def _toDbl = value.toDouble
  }

  case class JString(value: String) extends JValue

  case class JObject(fields: immutable.ListMap[String, JValue]) extends JValue

  case class JArray(elems: Vector[JValue]) extends JValue

  abstract class JCustom extends JValue {
    def value: AnyRef
    def valueAsJson: String
    def is(testedType: Class[_]): Boolean = testedType.isAssignableFrom(value.getClass)
  }


  class ZeisonException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)


  private[zeison] object internal {
    import scala.collection.mutable

    class ZeisonFacade extends Facade[JValue] {
      def jarray(arr: mutable.ListBuffer[JValue]) = JArray(arr.toVector)
      def jobject(obj: immutable.ListMap[String, JValue]) = JObject(obj)
      def jnull() = JNull
      def jfalse() = JBoolean(false)
      def jtrue() = JBoolean(true)
      def jnum(s: String) = JNum(s)
      def jint(s: String) = JNum(s)
      def jstring(s: String) = JString(s)

      def singleContext() = new FContext[JValue] {
        var value: JValue = _
        def add(s: String) { value = jstring(s) }
        def add(v: JValue) { value = v }
        def finish = value
        def isObj = false
      }

      def arrayContext() = new FContext[JValue] {
        val vs = mutable.ListBuffer.empty[JValue]
        def add(s: String) { vs += jstring(s) }
        def add(v: JValue) { vs += v }
        def finish = jarray(vs)
        def isObj = false
      }

      def objectContext() = new FContext[JValue] {
        var key: String = null
        var vs = immutable.ListMap.empty[String, JValue]
        def add(s: String) {
          if (key == null) {
            key = s
          } else {
            vs = vs + ((key, jstring(s)))
            key = null
          }
        }
        def add(v: JValue) {
          vs = vs.updated(key, v)
          key = null
        }
        def finish = jobject(vs)
        def isObj = true
      }
    }

    def toJValue(anyValue: Any): JValue = {
      type LMap   = immutable.ListMap[String, Any]
      type ObjMap = scala.collection.Map[String, Any]
      type ObjArr = scala.collection.TraversableOnce[Any]
      anyValue match {
        case null                    => JNull
        case value: JValue           => value
        case valueOpt: Option[_]     => valueOpt.map(toJValue).getOrElse(JUndefined)
        case value: Boolean          => JBoolean(value)
        case value: Number           => JNum(value.toString)
        case value: Char             => JString(value.toString)
        case value: String           => JString(value)
        case fields: LMap            => JObject(fields.flatMap { case (k, v) => toJValue(v).toOption.map((k, _)) })
        case fields: ObjMap          => JObject(immutable.ListMap(fields.flatMap { case (k, v) => toJValue(v).toOption.map((k, _)) }.toList: _*))
        case elems: ObjArr           => JArray(elems.flatMap(e => toJValue(e).toOption).toVector)
        case elems: Array[_]         => JArray(elems.flatMap(e => toJValue(e).toOption).toVector)
        case value                   => throw new ZeisonException(s"Can't parse value ($value) to JValue")
      }
    }

    def valueOf(jValue: JValue): Option[Any] = {
      jValue match {
        case JUndefined      => None
        case JNull           => Some(null)
        case JBoolean(value) => Some(value)
        case JNum(value)     => Some(value)
        case JString(value)  => Some(value)
        case JObject(value)  => Some(value)
        case JArray(value)   => Some(value)
        case custom: JCustom => Some(custom.value)
      }
    }
  }
}
