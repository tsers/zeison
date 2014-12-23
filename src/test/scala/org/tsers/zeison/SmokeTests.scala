package org.tsers.zeison

import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

import org.scalatest.FunSuite

import scala.util.Try

class SmokeTests extends FunSuite {

  test("smoke tests") {

    // parsing
    // parse: (String) => JValue
    // parse: (InputStream) => JValue
    val data = Zeison.parse("""
                            | {
                            |   "messages": ["tsers", "TSERS"],
                            |   "meta": {
                            |     "numKeys": 2,
                            |     "active": true,
                            |     "score": 0.6
                            |   },
                            |   "response": null
                            | }""".stripMargin)

    // type checking
    // .isInt .isStr .isBool .isDouble .isArray .isObject .isNull .isDefined
    assert(data.meta.numKeys.isInt == true)

    // value extraction
    assert(data.meta.numKeys.toInt == 2)
    assert(data.meta.active.toBool == true)
    assert(data.meta.score.toDouble == 0.6)
    assert(data.meta.toMap.get("numKeys").map(_.toInt) == Some(2)) // .toMap => Map[String, JValue]
    assert(data.messages.toSeq.head.toStr == "tsers")              // .toSeq => Seq[JValue]

    // object/array traversal
    assert(data("meta")("numKeys") == data.meta.numKeys)
    assert(data.messages(0).toStr == "tsers")

    // iterable behaviour (=> Iterable[JValue])
    assert(data.messages.map(_.toStr).toSet == Set("tsers", "TSERS"))
    assert(data.meta.toSet == Set(data.meta)) // all objects and primitives are handled as single value iterable
    assert(data.response.toSet == Set.empty)  // all null values and undefined values are handled as empty iterable

    // undefined values and optional extraction
    assert(data.meta.numKeys.isDefined == true)
    assert(data.non_existing.isDefined == false)
    assert(data.messages(-1).isDefined == false)
    assert(data.messages(10).isDefined == false)
    assert(data.meta.numKeys.toOption.map(_.toInt) == Some(2))  // .toOption => Option[JValue]
    assert(data.non_existing.toOption == None)
    assert(data.response.toOption == None)

    // runtime errors
    assert(Try(data.messages.toInt).isSuccess == false)         // bad type cast
    assert(Try(data.non_existing.toInt).isSuccess == false)     // undefined has no value
    assert(Try(data.non_existing.sub_field).isSuccess == false) // undefined has no member x

    // JSON creation
    // ATTENTION: Zeison is NOT an object mapper!
    // Only JSON primitive values, Scala Iterables/Maps and Zeison's JValue types are
    // accepted - other types cause runtime exception
    val obj = Zeison.toJson(Map(
      "msg"    -> "tsers!",
      "meta"   -> data.meta,
      "primes" -> Seq(1,2,3,5)
    ))

    // immutable field adding
    // .copy(..) works for JSON objects - other types cause runtime exception
    val metaWithNewFields = obj.meta.copy("foo" -> "bar", "lol" -> "bal")
    assert(metaWithNewFields != obj.meta)
    assert(metaWithNewFields.foo.toStr == "bar")
    assert(metaWithNewFields.numKeys.toInt == 2)

    // immutable field modification
    val modifiedScore = obj.meta.copy("score" -> "tsers")
    assert(modifiedScore.score.toStr == "tsers")
    assert(modifiedScore.numKeys.toInt == 2)

    // immutable field removal
    val removedScore = obj.meta.copy("score" -> Zeison.JUndefined)
    assert(removedScore.score.isDefined == false)
    assert(removedScore.numKeys.isDefined == true)

    // object merging (shallow!)
    val merged = Zeison.toJson(obj.toMap ++ data.toMap)
    assert(merged.primes.isDefined)
    assert(merged.messages.isDefined)

    // rendering
    Zeison.render(obj)
    Zeison.renderPretty(obj)

    // custom types building and rendering
    val now = new Date()
    val custom = Zeison.toJson(Map("createdAt" -> JDate(now)))
    assert(Zeison.render(custom) == s"""{"createdAt":"${toISO8601(now)}"}""")

    // custom types type checking and extraction
    assert(custom.createdAt.is[Date])
    assert(custom.createdAt.to[Date] == now)
  }

  def toISO8601(date: Date) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz") { setTimeZone(TimeZone.getTimeZone("UTC")) }
    sdf.format(date).replaceAll("UTC$", "+00:00")
  }
  case class JDate(value: Date) extends Zeison.JCustom {
    override def valueAsJson: String = "\"" + toISO8601(value) + "\""
  }

}
