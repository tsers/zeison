package org.tsers.zeison

class TraversalSpec extends BaseSpec {
  import Zeison._

  describe("JSON object traversing") {
    it("is possible with Scala's dot operator") {
      val json = parse(
        """
          | {
          |   "foo": {"bar": 12},
          |   "foobar": 15
          | }
        """.stripMargin)

      json.foo.bar.toInt should equal(12)
      json.foobar.toInt should equal(15)
    }

    it("returns undefined if trying to access non-existing field") {
      val json = parse("""{"foo": 123}""")
      json.foo should be('defined)
      json.bar should not be 'defined
    }

    it("returns undefined if trying to access sub-field from non-objects") {
      val json = parse("""{"foo": 1}""")
      json.foo.bar should not be 'defined
    }

    it("throws an exception if trying to access from undefined") {
      val json = parse("""{"foo": 1}""")
      intercept[ZeisonException] {
        json.bar.foo
      }
    }

    it("allows field accessing with apply-style") {
      val json = parse("""{"foo": 1, "bar": {"foobar": 2}}""")
      json("foo").toInt should equal(1)
      json("bar")("foobar").toInt should equal(2)
    }
  }

  describe("JSON array traversing") {
    it("is possible directly with indexes") {
      val json = parse("""{"arr": [1, 5, 2]}""")
      json.arr(0).toInt should equal(1)
      json.arr(2).toInt should equal(2)
    }

    it("returns undefined if trying to access negative index") {
      val json = parse("""{"arr": [1, 5, 2]}""")
      json(-1) should not be 'defined
    }

    it("returns undefined if trying to access out of array bounds") {
      val json = parse("""{"arr": [1, 5, 2]}""")
      json(3) should not be 'defined
    }

    it("can use Scala's iterable functions") {
      val json = parse("""{"arr": [1, "foobar", 2, 4]}""")
      val pow2 = json.arr
        .filter(_.isInt)
        .map(j => j.toInt * j.toInt)
        .drop(1)
        .toSeq
      pow2 should equal(Seq(2*2, 4*4))
    }
  }

}
