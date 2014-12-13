package org.tsers.zeison

import java.io.{ByteArrayInputStream, StringReader}

class ParsingSpec extends BaseSpec {
  import Zeison._

  describe("JSON parsing") {
    describe("type support") {
      it("returns JInt if top element is an integer number") {
        parse("123") should equal(JInt(123))
      }

      it("returns JDouble if top element is a floating point value") {
        parse("123.1") should equal(JDouble(123.1))
      }

      it("returns JString if top element is a string") {
        parse("\"foobar\"") should equal(JString("foobar"))
      }

      it("returns JBoolean if top element is a boolean") {
        parse("true") should equal(JBoolean(true))
      }

      it("returns JNull if top element is null") {
        parse("null") should equal(JNull)
      }

      it("returns JArray if top element is an array") {
        parse("[1, 2, 3]").getClass should equal(classOf[JArray])
      }

      it("returns JObject if top element is an object") {
        parse("""{ "foo": 123 }""").getClass should equal(classOf[JObject])
      }
    }

    describe("parsing sources") {
      it("include parsing from InputStream") {
        val is = new ByteArrayInputStream("[1, 2, 3]".getBytes)
        parse(is) should be ('defined)
      }

      it("include parsing from java.io.Reader") {
        val reader = new StringReader("[1, 2, 3]")
        parse(reader) should be ('defined)
      }
    }

    describe("invalid JSON parsing") {
      it("throws an exception if JSON is not valid") {
        intercept[ZeisonException] {
          parse("{\"invalid\": }")
        }
      }
    }
  }
}
