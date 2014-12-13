# Zeison

Small, fast and easy-to-use JSON library for Scala.

## Motivation

Oh why? Why must the JSON parsing to be so challenging in Scala? First you must
download tons of dependencies, then remember to use right package imports (for
implicit conversions) and/or implicit formats. C'moon! JSON has **six** valid
data types (+ null). It's not a rocket science.

Zeison tries to simplify the JSON parsing, management and rendering so that
you don't need to know any implicit values or conversions. Under the hood, it 
uses [json-smart](https://code.google.com/p/json-smart/) for parsing and rendering
so it is **fast** too.

Zeison is extremely lightweight - it and it's transient dependencies (well.. just 
json-smart) require under 150KB of space.


## Usage

To use Zeison in you project, add the following line to your `build.sbt`

    libraryDependencies += "org.tsers.zeison" %% "zeison" % "0.2.0"

All methods and types are inside object `org.tsers.zeison.Zeison` so in order to
use them in your code, you must add the following import

    import org.tsers.zeison.Zeison._

## API

Zeison API is designed to be extremely simple so that all it's features can be
demonstrated under one hundred LOC.

### Parsing

```scala
// parse: (String) => JValue
// parse: (java.io.Reader) => JValue
// parse: (InputStream) => JValue
val json = parse("""{ "hello": "zeison!" }""")
```

### Navigation

```scala
val json = parse("""
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
assert(Try(json.messages.toInt).isSuccess == false)         // bad type cast
assert(Try(json.non_existing.toInt).isSuccess == false)     // undefined has no value
assert(Try(json.non_existing.sub_field).isSuccess == false) // undefined has no member x
```

### Manipulation and rendering

**Attention!** Only valid JSON data types are accepted (e.g. Date must be formatted before
the object building/rendering is done)

```scala
val src = parse("""
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
val primes = Seq(1,2,3,5)
assert(render(arr.from(primes)) == "[1,2,3,5]")
val config = Map("version" -> 2)
assert(render(obj.from(config)) == """{"version":2}""")
```
