package org.tsers.zeison

import java.io.ByteArrayInputStream

class ParsingSpec extends BaseSpec {
  import org.tsers.zeison.Zeison._

  describe("JSON parsing") {
    describe("type support") {
      it("returns JNum if top element is an integer number") {
        parse("123") should equal(JParsedNum("123"))
      }

      it("returns JNum if top element is a floating point value") {
        parse("123.1") should equal(JParsedNum("123.1"))
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

      /*it("include parsing from java.io.Reader") {
        val reader = new StringReader("[1, 2, 3]")
        parse(reader) should be ('defined)
      }*/
    }

    describe("invalid JSON parsing") {
      it("throws an exception if JSON is not valid") {
        intercept[ZeisonException] {
          parse("{\"invalid\": }")
        }
      }
      it("throws an exception if root element is not a valid JSON type") {
        intercept[ZeisonException] {
          parse("invalid")  // NOTE: no quotation marks -> invalid element
        }
      }
      it("throws an exception if given input stream is not valid") {
        intercept[ZeisonException] {
          val is = new ByteArrayInputStream("[1, 2".getBytes)
          parse(is)
        }
      }
    }
  }
}
