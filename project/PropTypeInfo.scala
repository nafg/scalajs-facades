import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, Encoder}


sealed trait PropTypeInfo {
  def code: String
  def safeCode: String
  def imports: Set[String]
  def presets: Seq[PropTypeInfo.Preset]

  def =>:(unit: Unit)                                       = PropTypeInfo.Function(Nil, this)
  def =>:(arg: PropTypeInfo)                                = PropTypeInfo.Function(List(arg), this)
  def =>:(args: (PropTypeInfo, PropTypeInfo))               = PropTypeInfo.Function(List(args._1, args._2), this)
  def =>:(args: (PropTypeInfo, PropTypeInfo, PropTypeInfo)) =
    PropTypeInfo.Function(List(args._1, args._2, args._3), this)

  def sequence: PropTypeInfo = PropTypeInfo.Simple.Sequence(this)

  def |(that: PropTypeInfo) = PropTypeInfo.Union(Seq(this, that)).simplify
}
object PropTypeInfo       {
  private implicit val circeConfig =
    Configuration.default.withDefaults.copy(transformConstructorNames = s => s.head.toLower + s.tail)

  sealed abstract class Simple private (override val code: String, override val imports: Set[String] = Set.empty)
      extends PropTypeInfo {
    override def safeCode             = code
    override def presets: Seq[Preset] = Nil
  }
  object Simple            {
    case object bool         extends Simple("Boolean")
    case object int          extends Simple("Int")
    case object double       extends Simple("Double")
    case object string       extends Simple("String")
    case object jsAny        extends Simple("js.Any")
    case object jsObject     extends Simple("js.Object")
    case object jsDictionary extends Simple("js.Dictionary[js.Any]")
    case object callback     extends Simple("Callback", CommonImports.Callback)
    case object elementType  extends Simple("ElementType", CommonImports.ElementType)
    case object element      extends Simple("Element", CommonImports.Element)
    case object vdomNode     extends Simple("VdomNode", CommonImports.VdomNode)
    case object vdomElement  extends Simple("VdomElement", CommonImports.VdomElement)

    lazy val values = Seq(
      bool,
      int,
      double,
      string,
      jsAny,
      jsObject,
      jsDictionary,
      callback,
      elementType,
      element,
      vdomNode,
      vdomElement
    )

    case class Sequence(arrayOf: PropTypeInfo) extends Simple(s"Seq[${arrayOf.code}]", arrayOf.imports)
    object Sequence {
      implicit lazy val codecSequence: Codec[Sequence] = deriveConfiguredCodec[Sequence]
    }

    case class React(name: String) extends Simple(name, Set(CommonImports.react(name)))

    def fromPropType(simple: PropType.Simple): Simple =
      simple match {
        case PropType.Any         => jsAny
        case PropType.Bool        => bool
        case PropType.Element     => vdomElement
        case PropType.ElementType => elementType
        case PropType.Node        => vdomNode
        case PropType.Number      => double
        case PropType.Object      => jsObject
        case PropType.String      => string
      }

    implicit lazy val encodeSimple: Encoder[Simple] = Encoder.instance {
      case s @ Sequence(base) => Sequence.codecSequence(s)
      case React(name)        => name.asJson
      case s                  => s.toString.asJson
    }

    implicit val decodeSimple: Decoder[Simple] =
      Sequence.codecSequence.or {
        Decoder.decodeString.map { str =>
          values.find(_.toString == str)
            .getOrElse(React(str))
        }
      }
  }
  case class Function(args: Seq[PropTypeInfo], result: PropTypeInfo) extends PropTypeInfo {
    override def code     =
      args match {
        case Seq(one) => s"${one.code} => ${result.safeCode}"
        case _        => s"(${args.map(_.code).mkString(", ")}) => ${result.safeCode}"
      }
    override def safeCode = s"($code)"
    override def imports  = args.flatMap(_.imports).toSet ++ result.imports
    override def presets  = Nil
  }
  case class Union(anyOf: Seq[PropTypeInfo]) extends PropTypeInfo {
    override def code                   = anyOf.map(_.safeCode).distinct.mkString(" | ")
    override def safeCode               = s"($code)"
    override def imports                = anyOf.flatMap(_.imports).toSet ++ CommonImports.|
    override def presets                = anyOf.flatMap(_.presets)
    def simplify                        =
      copy(anyOf =
        anyOf
          .flatMap {
            case Union(types) => types
            case other        => Seq(other)
          }
      )
    override def sequence: PropTypeInfo = Simple.Sequence(this)
  }
  case class WithPresets(base: PropTypeInfo, override val presets: Seq[Preset]) extends PropTypeInfo {
    override def code     = base.code
    override def safeCode = base.safeCode
    override def imports  = base.imports
  }

  sealed abstract class Preset(val name: Identifier, val code: String)
  object Preset {
    case class Unquoted(value: String) extends Preset(Identifier(value), value)
    case class Quoted(string: String)  extends Preset(Identifier(string), '"' + string + '"')

    implicit val codecQuoted: Codec[Quoted]    = deriveConfiguredCodec
    implicit val encodePreset: Encoder[Preset] = Encoder.instance[Preset] {
      case u: Unquoted => u.value.asJson
      case q: Quoted   => q.asJson
    }
    implicit val decodePreset: Decoder[Preset] =
      codecQuoted
        .or(Decoder.decodeString.map[Preset](Unquoted))
        .or(Decoder.decodeBoolean.map(b => Unquoted(b.toString)))
  }

  val int: PropTypeInfo                         = Simple.int
  def string: PropTypeInfo                      = Simple.string
  val jsObject: PropTypeInfo                    = Simple.jsObject
  val jsAny: PropTypeInfo                       = Simple.jsAny
  val jsAnyDict: PropTypeInfo                   = Simple.jsDictionary
  private def react(name: String): PropTypeInfo = Simple.React(name)
  val reactEvent                                = react("ReactEvent")
  val reactEventFromInput                       = react("ReactEventFromInput")
  val reactMouseEventFromHtml                   = react("ReactMouseEventFromHtml")
  val callback: PropTypeInfo                    = Simple.callback
  val vdomNode: PropTypeInfo                    = Simple.vdomNode
  val vdomElement: PropTypeInfo                 = Simple.vdomElement
  val element: PropTypeInfo                     = Simple.element

  private val stringEnumValueRE = "'(.*)'".r
  private val litEnumValueRE    = """(true|false|-?\d+\.\d+|-?\d+)""".r

  def apply(propType: PropType): PropTypeInfo =
    propType match {
      case simple: PropType.Simple     => Simple.fromPropType(simple)
        case PropType.Func               => jsAny =>: jsAny
      case PropType.ArrayOf(param)     => apply(param).sequence
      case PropType.Union(types)       => Union(types.map(apply)).simplify
        case PropType.Enum(base, values) =>
          WithPresets(apply(base),
          values.collect {
            case litEnumValueRE(s)    => Preset.Unquoted(s)
            case stringEnumValueRE(s) => Preset.Quoted(s)
          }
        )
    }

  implicit val enumCodec: Codec[PropTypeInfo.WithPresets]  = deriveConfiguredCodec
  implicit val unionCodec: Codec[PropTypeInfo.Union]       = deriveConfiguredCodec
  implicit val functionCodec: Codec[PropTypeInfo.Function] = deriveConfiguredCodec
  implicit val encodePropTypeInfo: Encoder[PropTypeInfo]   = Encoder.instance[PropTypeInfo] {
    case s: Simple      => Simple.encodeSimple(s)
    case e: WithPresets => enumCodec(e)
    case u: Union       => unionCodec(u)
    case f: Function    => functionCodec(f)
  }
  implicit val decodePropTypeInfo: Decoder[PropTypeInfo]   =
    unionCodec
      .or(enumCodec.map(identity[PropTypeInfo]))
      .or(functionCodec.map(identity[PropTypeInfo]))
      .or(Simple.decodeSimple.map(identity[PropTypeInfo]))
}
