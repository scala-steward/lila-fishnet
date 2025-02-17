package lila.fishnet

import chess.CoreArbitraries.given
import chess.format.Fen.Full
import org.scalacheck.Arbitrary.*
import org.scalacheck.{ Arbitrary, Cogen, Gen }

import java.time.Instant

object Arbitraries:

  given Arbitrary[WorkId]    = Arbitrary(Gen.stringOfN(8, Gen.alphaNumChar).map(WorkId.apply))
  given Arbitrary[GameId]    = Arbitrary(Gen.stringOfN(8, Gen.alphaNumChar).map(GameId.apply))
  given Arbitrary[ClientKey] = Arbitrary(Gen.uuid.map(_.toString).map(ClientKey.apply))
  given Arbitrary[Instant]   = Arbitrary(Gen.choose(0, 300).map(Instant.now.minusSeconds(_)))

  given Arbitrary[Work.Acquired] = Arbitrary:
    for
      key <- arbitrary[ClientKey]
      at  <- arbitrary[Instant]
    yield Work.Acquired(key, at)

  given Arbitrary[Lila.Clock] = Arbitrary:
    for
      wtime <- Gen.choose(0, 600)
      btime <- Gen.choose(0, 600)
      inc   <- Gen.choose(0, 600)
    yield Lila.Clock(wtime, btime, inc)

  given Arbitrary[Lila.Request] = Arbitrary:
    for
      id <- arbitrary[GameId]
      moves = ""
      level   <- Gen.choose(1, 8)
      clock   <- arbitrary[Option[Lila.Clock]]
      variant <- arbitrary[chess.variant.Variant]
    yield Lila.Request(id, Full.initial, variant, moves, level, clock)

  given Arbitrary[Work.Task] = Arbitrary:
    for
      workId    <- arbitrary[WorkId]
      request   <- arbitrary[Lila.Request]
      tries     <- Gen.choose(0, 3)
      acquired  <- arbitrary[Option[Work.Acquired]]
      createdAt <- arbitrary[Instant]
    yield Work.Task(workId, request, tries, acquired, createdAt)

  given Cogen[Work.Task] = Cogen(_.id.hashCode.toLong)

  given Arbitrary[AppState] = Arbitrary:
    arbitrary[List[Work.Task]].map(_.foldLeft(AppState.empty)(_.add(_)))
