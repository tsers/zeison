# Zeison

Small, easy and fast JSON library for Scala

**TODO:** docs

## Examples
    
```scala    
object Examples extends App {
  import org.tsers.zeison._

  // PARSING

  val json = parse("""
      | {
      |   "firstName": "John",
      |   "lastName": "Smith",
      |   "age": 25,
      |   "address": {
      |     "streetAddress": "21 2nd Street",
      |     "city": "New York",
      |     "state": "NY",
      |     "postalCode": "10021"
      |   },
      |   "phoneNumber": [
      |     {
      |       "type": "home",
      |       "number": "212 555-1234"
      |     },
      |     {
      |       "type": "fax",
      |       "number": "646 555-4567"
      |     }
      |   ]
      | }
    """.stripMargin)


  // TRAVERSAL

  println(json.firstName.toStr)                        // -> "John"

  println(json.age.toInt)                              // -> 25

  println(json.address.city.toStr)                     // ->"New York"

  println(json.phoneNumber(0).number.toStr)            // -> "212 555-1234"

  println(json.phoneNumber.map(_.`type`.toStr).toSeq)  // -> Seq("home", "fax")

  
  // RENDERING

  println(render(json.phoneNumber(0)))  // -> {"number":"212 555-1234","type":"home"}

}
```
