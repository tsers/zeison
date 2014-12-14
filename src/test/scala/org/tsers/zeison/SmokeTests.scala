package org.tsers.zeison

import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

import org.scalatest.FunSuite

import scala.util.Try

class SmokeTests extends FunSuite {
  import Zeison._

  test("smoke tests") {

    val json = parse( """
                        | {
                        |   "messages": ["tsers", {"msg": "tsers!"}],
                        |   "meta": {
                        |     "numKeys": 2,
                        |     "active": true,
                        |     "score": 0.6
                        |   },
                        |   "response": null
                        | }""".stripMargin)

    // conversions
    assert(json.meta.numKeys.toInt == 2)
    assert(json.meta.active.toBool == true)
    assert(json.meta.score.toDouble == 0.6)
    assert(json.meta.toMap.get("numKeys").map(_.toInt) == Some(2))
    assert(json.messages.toSeq.head.toStr == "tsers")

    // type checking
    assert(json.meta.numKeys.isInt == true) // also .isStr .isBool .isDouble .isArray .isObject .isNull .isDefined

    // traversing
    assert(json("meta")("numKeys").toInt == 2)
    assert(json.messages(0).toStr == "tsers")
    assert(json.messages(1).msg.toStr == "tsers!")
    assert(json.messages.filter(_.isObject).map(_.msg.toStr).toSeq == Seq("tsers!"))

    // undefined values
    assert(json.meta.numKeys.isDefined == true)
    assert(json.non_existing.isDefined == false)
    assert(json.messages(-1).isDefined == false)
    assert(json.messages(10).isDefined == false)
    assert(json.meta.numKeys.toOption.map(_.toInt) == Some(2))
    assert(json.non_existing.toOption == None)
    assert(json.response.toOption == None)

    // exceptions
    assert(Try(json.messages.toInt).isSuccess == false) // bad type cast
    assert(Try(json.non_existing.toInt).isSuccess == false) // undefined has no value
    assert(Try(json.non_existing.sub_field).isSuccess == false)
    // undefined has no member x


    val src = parse( """
                       | {
                       |   "meta": {
                       |     "numKeys": 2
                       |   },
                       |   "response": null
                       | }""".stripMargin)

    // building objects with obj/arr
    assert(render(obj("msg" -> "tsers!", "meta" -> src.meta)) == """{"msg":"tsers!","meta":{"numKeys":2}}""")
    assert(render(arr(1, obj("msg" -> "tsers!"))) == """[1,{"msg":"tsers!"}]""")

    // building objects from Scala collections
    val primes = Seq(1, 2, 3, 5)
    assert(render(arr.from(primes)) == "[1,2,3,5]")
    val config = Map("version" -> 2)
    assert(render(obj.from(config)) == """{"version":2}""")

    // custom types building and rendering
    val now = new Date()
    val customJson = obj("createdAt" -> JDate(now))
    assert(render(customJson) == s"""{"createdAt":"${toISO8601(now)}"}""")

    // custom types type checking and extraction
    assert(customJson.createdAt.is[Date])
    assert(customJson.createdAt.to[Date] == now)
    assert(!customJson.createdAt.isInt)
    assert(Try(customJson.createdAt.toInt).isFailure)
  }

  def toISO8601(date: Date) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz") { setTimeZone(TimeZone.getTimeZone("UTC")) }
    sdf.format(date).replaceAll("UTC$", "+00:00")
  }
  case class JDate(value: Date) extends JCustom {
    override def valueAsJson: String = "\"" + toISO8601(value) + "\""
  }

}
