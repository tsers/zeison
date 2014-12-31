# Zeison

Small, fast and easy-to-use JSON library for Scala.

## Motivation

Oh why? Why must JSON parsing be so challenging in Scala? First you must
download tons of dependencies, then remember to use right package imports (for
implicit conversions) and/or implicit formats. C'mon! JSON has only **six**
valid data types (+ null). It's not rocket science.

Zeison tries to simplify the JSON parsing, management and rendering so that
you don't need to know any implicit values or conversions. Under the hood, it
uses [jawn](https://github.com/non/jawn) for parsing so it is **fast** too.

Zeison is extremely lightweight - binaries (including jawn-parser) require under
150KB of space.


## Usage

To use Zeison in you project, add the following line to your `build.sbt`

    libraryDependencies += "org.tsers.zeison" %% "zeison" % "0.5.3"

All methods and types are inside object `org.tsers.zeison.Zeison` so in order to
use them in your code, you must add the following import

```scala
import org.tsers.zeison.Zeison
// or if you want to use methods without "Zeison." prefix
import org.tsers.zeison.Zeison._
```


## API

Zeison API is designed to be extremely simple so that all its features can be
demonstrated with one hundred LOC.

### Parsing

```scala
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
```

### JSON operations

```scala
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
```

### Rendering

```scala
Zeison.render(obj)
Zeison.renderPretty(obj)
```

### Custom types

Some libraries (for example Casbah) enable non-standard JSON types. To support
these libraries, Zeison provides a way to define simple custom data types that
can be built, extracted and rendered from JSON objects.

```scala
def toISO8601(date: Date) = {
  val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz") { setTimeZone(TimeZone.getTimeZone("UTC")) }
  sdf.format(date).replaceAll("UTC$", "+00:00")
}
case class JDate(value: Date) extends Zeison.JCustom {
  override def valueAsJson: String = "\"" + toISO8601(value) + "\""
}

val now    = new Date()
val custom = Zeison.toJson(Map("createdAt" -> JDate(now)))

assert(Zeison.render(custom) == s"""{"createdAt":"${toISO8601(now)}"}""")
assert(custom.createdAt.is[Date])
assert(custom.createdAt.to[Date] == now)
```
