package config

final case class ServerConfig(host: String, port: Int)
final case class AlbagramConfig(db: DatabaseConfig, server: ServerConfig)
