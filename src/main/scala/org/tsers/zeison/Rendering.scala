package org.tsers.zeison

import org.tsers.zeison.Zeison._

import scala.annotation.switch

private [zeison] object Rendering {
  import scala.collection.immutable

  private final val Hex = "0123456789ABCDEF".intern()

  def render(value: JValue): String = {
    val sb = new StringBuilder
    rrender(value, sb)
    sb.toString()
  }

  private def rrender(value: JValue, sb: StringBuilder): Unit =  {
    def renderStr(str: String): Unit = {
      sb.append('"')
      str.foreach(c => (c: @switch) match {
        case '"'  => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\b' => sb.append("\\b")
        case '\f' => sb.append("\\f")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case '/'  => sb.append("\\/")
        case ch   => {
          if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
            sb.append("\\u")
            sb.append(Hex.charAt(ch >> 12 & 0x0F))
            sb.append(Hex.charAt(ch >> 8 & 0x0F))
            sb.append(Hex.charAt(ch >> 4 & 0x0F))
            sb.append(Hex.charAt(ch >> 0 & 0x0F))
          } else {
            sb.append(ch)
          }
        }
      })
      sb.append('"')
    }
    @inline def renderField(field: (String, JValue)): Unit = {
      renderStr(field._1)
      sb.append(':')
      rrender(field._2, sb)
    }
    @inline def renderObject(fields: immutable.ListMap[String, JValue]): Unit = {
      sb.append('{')
      var isFirst = true
      fields.foreach { f =>
        if (f._2.isDefined) {
          if (!isFirst) sb.append(',') else isFirst = false
          renderField(f)
        }
      }
      sb.append('}')
    }
    @inline def renderArray(elems: Vector[JValue]): Unit = {
      sb.append('[')
      var isFirst = true
      elems.foreach { e =>
        if (e.isDefined) {
          if (!isFirst) sb.append(',') else isFirst = false
          rrender(e, sb)
        }
      }
      sb.append(']')
    }

    value match {
      case JUndefined       => new ZeisonException("Can't render JUndefined")
      case JNull            => sb.append("null")
      case JBoolean(v)      => sb.append(v)
      case JNum(v)          => sb.append(v)
      case JString(v)       => renderStr(v)
      case JObject(fields)  => renderObject(fields)
      case JArray(elems)    => renderArray(elems)
      case custom: JCustom  => sb.append(custom.valueAsJson)
    }
  }


}
