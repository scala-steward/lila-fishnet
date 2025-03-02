package lila.fishnet

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import io.chrisdavenport.rediculous.RedisPubSub
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import Lila.Request

trait RedisSubscriberJob:
  def run(): Resource[IO, Unit]

object RedisSubscriberJob:
  def apply(executor: Executor, pubsub: RedisPubSub[IO])(using LoggerFactory[IO]): RedisSubscriberJob = new:
    given Logger[IO] = LoggerFactory[IO].getLoggerFromName("RedisSubscriberJob")
    def run(): Resource[IO, Unit] =
      (Logger[IO].info("Subscribing to fishnet-out") *>
        pubsub.subscribe(
          "fishnet-out",
          msg =>
            Lila
              .readMoveReq(msg.message)
              .match
                case Some(request) => executor.add(request)
                case None          => Logger[IO].warn(s"Failed to parse message from lila: $msg")
            >> Logger[IO].debug(s"Received message: $msg")
        ) *> pubsub.runMessages).background.void
