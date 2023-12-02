package lila.fishnet
package http

import scala.concurrent.duration.*

import cats.syntax.all.*
import cats.effect.IO
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.middleware.*

final class HttpApi(executor: Executor, healthCheck: HealthCheck, config: HttpServerConfig):

  private val fishnetRoutes = FishnetRoutes(executor).routes
  private val healthRoutes  = HealthRoutes(healthCheck).routes

  private val routes: HttpRoutes[IO] = fishnetRoutes <+> healthRoutes

  type Middleware = HttpRoutes[IO] => HttpRoutes[IO]

  private val autoSlash: Middleware = AutoSlash(_)
  private val timeout: Middleware   = Timeout(60.seconds)

  private val middleware = autoSlash andThen timeout

  private val loggers: HttpApp[IO] => HttpApp[IO] =
    RequestLogger.httpApp[IO](false, true) andThen
      ResponseLogger.httpApp[IO, Request[IO]](false, true)

  val httpApp: HttpApp[IO] =
    val app = middleware(routes).orNotFound
    if config.apiLogger then loggers(app)
    else app