package org.tsers.zeison

import org.tsers.zeison.Zeison._

import scala.annotation.switch

private [zeison] object Rendering {

  private final val Hex = "0123456789ABCDEF".intern()
  private final val IndentWidth = 2

  def render(value: JValue, pretty: Boolean): String = {
    val sb = new StringBuilder
    renderRecursive(value, sb, pretty, 0)
    sb.toString()
  }

  private def renderRecursive(value: JValue, sb: StringBuilder, pretty: Boolean, indent: Int): Unit =  {
    def renderStr(str: String, sb: StringBuilder): Unit = {
      sb.append('"')
      str.foreach(c => (c: @switch) match {
        case '"'  => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case '\b' => sb.append("\\b")
        case '\f' => sb.append("\\f")
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

    @inline def compactObject(fields: Map[String, JValue]): Unit = {
      sb.append('{')
      var isFirst = true
      fields.foreach { f =>
        val (key, value) = f
        if (value.isDefined) {
          if (!isFirst) sb.append(',') else isFirst = false
          renderStr(key, sb)
          sb.append(':')
          renderRecursive(value, sb, pretty = false, 0)
        }
      }
      sb.append('}')
    }

    @inline def compactArray(elems: Vector[JValue]): Unit = {
      sb.append('[')
      var isFirst = true
      elems.foreach { e =>
        if (e.isDefined) {
          if (!isFirst) sb.append(',') else isFirst = false
          renderRecursive(e, sb, pretty = false, 0)
        }
      }
      sb.append(']')
    }

    def prettyArray(elems: Vector[JValue]): Unit = {
      sb.append('[')
      if (elems.size == 1) {
        val e = elems.head
        if (e.isDefined) {
          renderRecursive(elems.head, sb, pretty = true, indent)
        }
        sb.append(']')
      } else {
        val spaces = " " * (indent + IndentWidth)
        var isFirst = true
        elems.foreach { e =>
          if (e.isDefined) {
            if(!isFirst) sb.append(",\n") else { sb.append('\n'); isFirst = false }
            sb.append(spaces)
            renderRecursive(e, sb, pretty = true, indent + IndentWidth)
          }
        }
        if (!isFirst) {
          sb.append('\n')
          sb.append(" " * indent)
        }
        sb.append(']')
      }
    }

    def prettyObject(fields: Map[String, JValue]): Unit = {
      sb.append('{')
      val spaces = " " * (indent + IndentWidth)
      var isFirst = true
      fields.foreach { f =>
        val (key, value) = f
        if (value.isDefined) {
          if(!isFirst) sb.append(",\n") else { sb.append('\n'); isFirst = false }
          sb.append(spaces)
          renderStr(key, sb)
          sb.append(": ")
          renderRecursive(value, sb, pretty = true, indent + IndentWidth)
        }
      }
      if (!isFirst) {
        sb.append('\n')
        sb.append(" " * indent)
      }
      sb.append('}')
    }

    value match {
      case JUndefined       => throw new ZeisonException("Can't render JUndefined")
      case JNull            => sb.append("null")
      case JBoolean(v)      => sb.append(v)
      case num: JNum        => sb.append(num.valueAsString)
      case JString(v)       => renderStr(v, sb)
      case JObject(fields)  => if (pretty) prettyObject(fields) else compactObject(fields)
      case JArray(elems)    => if (pretty) prettyArray(elems) else compactArray(elems)
      case custom: JCustom  => sb.append(custom.valueAsJson)
    }
  }

}
