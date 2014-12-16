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

    libraryDependencies += "org.tsers.zeison" %% "zeison" % "0.3.2"

All methods and types are inside object `org.tsers.zeison.Zeison` so in order to
use them in your code, you must add the following import

    import org.tsers.zeison.Zeison._

## API

Zeison API is designed to be extremely simple so that all it's features can be
demonstrated under one hundred LOC.

### Parsing

```scala
object ParsingExample extends App {
  import org.tsers.zeison.Zeison._
  // parse: (String) => JValue
  // parse: (java.io.Reader) => JValue
  // parse: (InputStream) => JValue
  val json = parse("""{ "hello": "zeison!" }""")
}
```

### Navigation

```scala
object NavigationExample extends App {
  import org.tsers.zeison.Zeison._

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
}
```

### Manipulation and rendering

**Attention!** Only valid JSON data types are accepted (e.g. Date must be formatted before
the object building/rendering is done)

```scala
object RenderingExample extends App {
  import org.tsers.zeison.Zeison._
  
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
}
```

### Custom types

Some libraries (for example Casbah) enable non-standard JSON types. To support 
these libraries, Zeison provides a way to define simple custom data types that 
can be built, extracted and rendered from JSON objects. 

```scala
object CustomTypeExample extends App {
  import org.tsers.zeison.Zeison._

  def toISO8601(date: Date) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz") { setTimeZone(TimeZone.getTimeZone("UTC")) }
    sdf.format(date).replaceAll("UTC$", "+00:00")
  }

  // In order to extend JCustom type, you must override the following methods:
  //    def value: AnyRef
  //    def valueAsJson: String
  case class JDate(value: Date) extends JCustom {
    override def valueAsJson: String = "\"" + toISO8601(value) + "\""
  }

  // building and rendering
  val now  = new Date()
  val json = obj("createdAt" -> JDate(now))
  assert(render(json) == s"""{"createdAt":"${toISO8601(now)}"}""")

  // type checking and extraction
  assert(json.createdAt.is[Date])
  assert(json.createdAt.to[Date] == now)
  assert(!json.createdAt.isInt)
  assert(Try(json.createdAt.toInt).isFailure)
}
``` 
