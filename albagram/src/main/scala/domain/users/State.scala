package domain.users

import cats._

final case class State(stateRepr: String)

object State {
  val Active: State = State("Active")
  val Banned: State = State("Banned")

  def getRepr(t: State): String = t.stateRepr

  implicit val eqRole: Eq[State] = Eq.fromUniversalEquals[State]
}
