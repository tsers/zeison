package org.tsers.zeison

class ConversionsSpec extends BaseSpec {
  import Zeison._

  val json = parse(
    """
      | {
      |   "int": 123,
      |   "dbl": 123.3,
      |   "bool": true,
      |   "str": "foobar",
      |   "obj": {"foo": 1, "bar": 2},
      |   "arr": [3, 2, 1],
      |   "null": null
      | }
    """.stripMargin)

  describe("JSON basic data types") {

    it("supports integers") {
      json.int should be('int)
      json.int.toInt should equal(123)
      json.bool should not be 'int
      intercept[ZeisonException] {
        json.bool.toInt
      }
    }

    it("supports doubles") {
      json.dbl should be('double)
      json.dbl.toDouble should equal(123.3)
      json.bool should not be 'double
      intercept[ZeisonException] {
        json.int.toBool
      }
    }

    it("supports booleans") {
      json.bool should be('bool)
      json.bool.toBool should equal(true)
      json.int should not be 'bool
      intercept[ZeisonException] {
        json.int.toBool
      }
    }

    it("supports strings") {
      json.str should be('str)
      json.str.toStr should equal("foobar")
      json.bool should not be 'str
      intercept[ZeisonException] {
        json.bool.toStr
      }
    }

    it("supports nulls") {
      json.`null` should be('null)
      json.bool should not be 'null
    }

    it("supports objects") {
      json.obj should be('object)
      json.bool should not be 'object
    }

    it("supports arrays") {
      json.arr should be('array)
      json.bool should not be 'array
    }

  }

  describe("JSON conversions") {
    it("support object casting to map") {
      json.obj.toMap.mapValues(_.toInt) should equal(Map("foo" -> 1, "bar" -> 2))
    }

    it("support array casting to seq") {
      json.arr.toSeq.map(_.toInt) should equal(Seq(3, 2, 1))
    }

    it("support value casting to options so that nulls and undefined values are converted to None") {
      json.int.toOption should be('defined)
      json.dbl.toOption should be('defined)
      json.bool.toOption should be('defined)
      json.str.toOption should be('defined)
      json.arr.toOption should be('defined)
      json.obj.toOption should be('defined)

      json.`null`.toOption should not be 'defined
      json.non_existing.toOption should not be 'defined
    }
  }

}
