import io.circe.Codec


object Util {
  def withoutEmpty[A](codec: Codec.AsObject[A]): Codec[A] =
    Codec.from(codec, codec.mapJson(_.dropNullValues.dropEmptyValues))
}
