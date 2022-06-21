package domain.posts

final case class Post(
                      id: Option[Long] = None,
                      title: String,
                      description: Option[String] = None,
                      themeId: Long,
                      userId: Long
                     )