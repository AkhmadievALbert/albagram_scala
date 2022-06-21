import cats.effect._
import config._
import domain.authentification.Authentification
import domain.posts.{PostService, PostValidationInterpreter}
import domain.themes.{ThemeService, ThemeValidationInterpreter}
import domain.users.{UserService, UserValidationInterpreter}
import doobie.util.ExecutionContexts
import infrastructure.endpoint.post.PostEndpoints
import infrastructure.endpoint.theme.ThemeEndpoints
import infrastructure.endpoint.user.UserEndpoints
import infrastructure.repository.post.DoobiePostRepositoryInterpreter
import infrastructure.repository.{DoobieAuthRepositoryInterpreter, DoobieThemeRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import io.circe.config.parser
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, AlbagramConfig]("albagram"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.eval(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      themeRepo = DoobieThemeRepositoryInterpreter[F](xa)
      postRepo = DoobiePostRepositoryInterpreter[F](xa)
//      bookRepo = DoobieBookRepositoryInterpreter[F](xa)
      userValidation = UserValidationInterpreter[F](userRepo)
      themeValidation = ThemeValidationInterpreter[F](themeRepo)
      postValidation = PostValidationInterpreter[F](postRepo)
//      bookValidation = BookValidationInterpreter[F](bookRepo)
      userService = UserService[F](userRepo, userValidation)
      themeService = ThemeService[F](themeRepo, themeValidation)
      postService = PostService[F](postRepo, postValidation)

//      bookService = BookService[F](bookRepo, bookValidation)
      authenticator = Authentification.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
        "/themes" -> ThemeEndpoints
          .endpoints[F, HMACSHA256](themeService, routeAuth),
        "/posts" -> PostEndpoints
          .endpoints[F, HMACSHA256](postService, routeAuth)
//        "/books" -> BookEndpoints.endpoints[F, HMACSHA256](bookService, routeAuth)
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}
