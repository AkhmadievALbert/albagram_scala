package infrastructure.endpoint

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

object QueryParam {
  import QueryParamDecoder._

  object ThemeTitleMatcher extends QueryParamDecoderMatcher[String]("title")
  object PostIdMatcher extends QueryParamDecoderMatcher[Long]("postId")
  object ThemeIdMatcher extends QueryParamDecoderMatcher[Long]("themeId")
}
