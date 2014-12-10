# Zeison

Small, easy and fast JSON library for Scala.

## Motivation

JSON is a simple format - most Scala JSON "frameworks" are not. There are tons of
transient dependencies, DSLs and implicit conversions. This library tries to simplify
the JSON parsing in Scala so that it feels almost like coding JavaScript. 

No hidden dependencies! Only [json-smart](https://code.google.com/p/json-smart/).


## Usage

To use zeison, add `import org.tsers.zeison._` to your imports

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
