package org.tsers


package object zeison {
  import scala.language.dynamics

  sealed abstract class JValue extends Dynamic {
    def selectDynamic(field: String) = {

    }

    def updateDynamic(field: String)(newValue: JValue) = {
    }
  }

}
