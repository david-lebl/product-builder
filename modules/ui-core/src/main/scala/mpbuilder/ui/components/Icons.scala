package mpbuilder.ui.components

import kyo.*
import kyo.UI.*

/** Crisp 24×24 stroke icon set rendered with typed `kyo.Svg`.
  *
  * Every glyph strokes with `currentColor`, so icons inherit the text color of the button, badge or
  * nav item they sit in and re-theme automatically in dark mode. Geometry follows the Lucide icon
  * set (ISC License, https://lucide.dev).
  */
object Icons:

  enum Icon:
    case Cart, User, Users, Menu, Close, Search, Check, ChevronDown, ChevronRight, ChevronLeft, ArrowRight,
      ArrowLeft, Plus, Minus, Trash, Pencil, Copy, Upload, Download, Eye, Refresh, LogIn, LogOut, Dashboard,
      Inbox, Package, Tag, Sliders, Globe, Sun, Moon, Monitor, Sparkles, Info, AlertTriangle, CheckCircle,
      XCircle, Clock, Truck, Zap, More, Filter, Layers, Wand, Store, Shield, Printer, FileText, CreditCard,
      Mail, Lock, ExternalLink, Link, Pause, Play, Image, Star, Message, Building, TrendingUp, Percent,
      Calendar, Box, Settings, Grid, Phone, Palette, Ruler, Wallet, Book

  private sealed trait Shape
  private final case class P(d: String)                                        extends Shape
  private final case class C(cx: Double, cy: Double, r: Double, filled: Boolean = false) extends Shape
  private final case class R(x: Double, y: Double, w: Double, h: Double, rx: Double) extends Shape

  private def shapes(icon: Icon): Seq[Shape] = icon match
    case Icon.Cart =>
      Seq(C(8, 21, 1), C(19, 21, 1), P("M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"))
    case Icon.User  => Seq(P("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"), C(12, 7, 4))
    case Icon.Users =>
      Seq(P("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"), C(9, 7, 4), P("M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"))
    case Icon.Menu         => Seq(P("M4 6h16M4 12h16M4 18h16"))
    case Icon.Close        => Seq(P("M18 6 6 18M6 6l12 12"))
    case Icon.Search       => Seq(C(11, 11, 7.5), P("m21 21-4.3-4.3"))
    case Icon.Check        => Seq(P("M20 6 9 17l-5-5"))
    case Icon.ChevronDown  => Seq(P("m6 9 6 6 6-6"))
    case Icon.ChevronRight => Seq(P("m9 18 6-6-6-6"))
    case Icon.ChevronLeft  => Seq(P("m15 18-6-6 6-6"))
    case Icon.ArrowRight   => Seq(P("M5 12h14M12 5l7 7-7 7"))
    case Icon.ArrowLeft    => Seq(P("M19 12H5M12 19l-7-7 7-7"))
    case Icon.Plus         => Seq(P("M5 12h14M12 5v14"))
    case Icon.Minus        => Seq(P("M5 12h14"))
    case Icon.Trash =>
      Seq(P("M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M10 11v6M14 11v6"))
    case Icon.Pencil => Seq(P("M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5ZM15 5l4 4"))
    case Icon.Copy   => Seq(R(8, 8, 14, 14, 2), P("M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"))
    case Icon.Upload => Seq(P("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"))
    case Icon.Download =>
      Seq(P("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"))
    case Icon.Eye => Seq(P("M2.06 12.35a1 1 0 0 1 0-.7 10.75 10.75 0 0 1 19.88 0 1 1 0 0 1 0 .7 10.75 10.75 0 0 1-19.88 0"), C(12, 12, 3))
    case Icon.Refresh =>
      Seq(P("M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8M21 3v5h-5M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16M8 16H3v5"))
    case Icon.LogIn  => Seq(P("M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3"))
    case Icon.LogOut => Seq(P("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"))
    case Icon.Dashboard =>
      Seq(R(3, 3, 7, 9, 1.5), R(14, 3, 7, 5, 1.5), R(14, 12, 7, 9, 1.5), R(3, 16, 7, 5, 1.5))
    case Icon.Inbox =>
      Seq(P("M22 12h-6l-2 3h-4l-2-3H2"), P("M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"))
    case Icon.Package =>
      Seq(
        P("M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"),
        P("M12 22V12M3.29 7 12 12l8.71-5M7.5 4.27l9 5.15"),
      )
    case Icon.Tag =>
      Seq(
        P("M12.59 2.59A2 2 0 0 0 11.17 2H4a2 2 0 0 0-2 2v7.17a2 2 0 0 0 .59 1.42l8.7 8.7a2.43 2.43 0 0 0 3.42 0l6.58-6.58a2.43 2.43 0 0 0 0-3.42z"),
        C(7.5, 7.5, 1.1, filled = true),
      )
    case Icon.Sliders =>
      Seq(P("M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4"))
    case Icon.Globe   => Seq(C(12, 12, 10), P("M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20M2 12h20"))
    case Icon.Sun =>
      Seq(C(12, 12, 4), P("M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"))
    case Icon.Moon    => Seq(P("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"))
    case Icon.Monitor => Seq(R(2, 3, 20, 14, 2), P("M8 21h8M12 17v4"))
    case Icon.Sparkles =>
      Seq(
        P("M9.94 15.5A2 2 0 0 0 8.5 14.06l-6.14-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5l1.58-6.14a.5.5 0 0 1 .96 0L14.06 8.5A2 2 0 0 0 15.5 9.94l6.14 1.58a.5.5 0 0 1 0 .96L15.5 14.06a2 2 0 0 0-1.44 1.44l-1.58 6.14a.5.5 0 0 1-.96 0z"),
        P("M20 3v4M22 5h-4"),
      )
    case Icon.Info          => Seq(C(12, 12, 10), P("M12 16v-4M12 8h.01"))
    case Icon.AlertTriangle => Seq(P("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3ZM12 9v4M12 17h.01"))
    case Icon.CheckCircle   => Seq(C(12, 12, 10), P("m9 12 2 2 4-4"))
    case Icon.XCircle       => Seq(C(12, 12, 10), P("m15 9-6 6M9 9l6 6"))
    case Icon.Clock         => Seq(C(12, 12, 10), P("M12 6v6l4 2"))
    case Icon.Truck =>
      Seq(
        P("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2M15 18H9M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.62l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
        C(17, 18, 2),
        C(7, 18, 2),
      )
    case Icon.Zap =>
      Seq(P("M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"))
    case Icon.More   => Seq(C(5, 12, 1.2, filled = true), C(12, 12, 1.2, filled = true), C(19, 12, 1.2, filled = true))
    case Icon.Filter => Seq(P("M22 3H2l8 9.46V19l4 2v-8.54L22 3z"))
    case Icon.Layers =>
      Seq(
        P("M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z"),
        P("M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17"),
      )
    case Icon.Wand =>
      Seq(P("m21.64 3.64-1.28-1.28a1.21 1.21 0 0 0-1.72 0L2.36 18.64a1.21 1.21 0 0 0 0 1.72l1.28 1.28a1.2 1.2 0 0 0 1.72 0L21.64 5.36a1.2 1.2 0 0 0 0-1.72M14 7l3 3M5 6v4M19 14v4M10 2v2M7 8H3M21 16h-4M11 3H9"))
    case Icon.Store =>
      Seq(P("M2 7l2-4h16l2 4M4 7v13a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V7M2 7h20M9 21v-6h6v6M2 7a3 3 0 0 0 5 2.24A3 3 0 0 0 12 9a3 3 0 0 0 5 .24A3 3 0 0 0 22 7"))
    case Icon.Shield =>
      Seq(P("M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"))
    case Icon.Printer =>
      Seq(P("M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2M6 9V3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v6"), R(6, 14, 12, 8, 1))
    case Icon.FileText =>
      Seq(P("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M10 9H8M16 13H8M16 17H8"))
    case Icon.CreditCard   => Seq(R(2, 5, 20, 14, 2), P("M2 10h20"))
    case Icon.Mail         => Seq(R(2, 4, 20, 16, 2), P("m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"))
    case Icon.Lock         => Seq(R(3, 11, 18, 11, 2), P("M7 11V7a5 5 0 0 1 10 0v4"))
    case Icon.ExternalLink => Seq(P("M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"))
    case Icon.Link =>
      Seq(P("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"))
    case Icon.Pause => Seq(R(14, 4, 4, 16, 1), R(6, 4, 4, 16, 1))
    case Icon.Play  => Seq(P("M6 3l14 9-14 9z"))
    case Icon.Image => Seq(R(3, 3, 18, 18, 2), C(9, 9, 2), P("m21 15-3.09-3.09a2 2 0 0 0-2.82 0L6 21"))
    case Icon.Star =>
      Seq(P("M11.53 2.3a.53.53 0 0 1 .95 0l2.31 4.68a2.12 2.12 0 0 0 1.6 1.16l5.16.76a.53.53 0 0 1 .3.9l-3.74 3.64a2.12 2.12 0 0 0-.61 1.88l.88 5.14a.53.53 0 0 1-.77.56l-4.62-2.43a2.12 2.12 0 0 0-1.97 0L6.4 21.01a.53.53 0 0 1-.77-.56l.88-5.14a2.12 2.12 0 0 0-.61-1.88L2.16 9.8a.53.53 0 0 1 .3-.91l5.16-.75a2.12 2.12 0 0 0 1.6-1.16z"))
    case Icon.Message  => Seq(P("M7.9 20A9 9 0 1 0 4 16.1L2 22Z"))
    case Icon.Building => Seq(R(4, 2, 16, 20, 2), P("M9 22v-4h6v4M8 6h.01M16 6h.01M12 6h.01M12 10h.01M12 14h.01M16 10h.01M16 14h.01M8 10h.01M8 14h.01"))
    case Icon.TrendingUp => Seq(P("M22 7 13.5 15.5 8.5 10.5 2 17M16 7h6v6"))
    case Icon.Percent    => Seq(P("M19 5 5 19"), C(6.5, 6.5, 2.5), C(17.5, 17.5, 2.5))
    case Icon.Calendar   => Seq(R(3, 4, 18, 18, 2), P("M16 2v4M8 2v4M3 10h18"))
    case Icon.Box =>
      Seq(P("M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"), P("m3.3 7 8.7 5 8.7-5M12 22V12"))
    case Icon.Settings =>
      Seq(
        P("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"),
        C(12, 12, 3),
      )
    case Icon.Grid    => Seq(R(3, 3, 7, 7, 1.5), R(14, 3, 7, 7, 1.5), R(14, 14, 7, 7, 1.5), R(3, 14, 7, 7, 1.5))
    case Icon.Phone =>
      Seq(P("M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"))
    case Icon.Palette =>
      Seq(
        C(13.5, 6.5, 1, filled = true),
        C(17.5, 10.5, 1, filled = true),
        C(8.5, 7.5, 1, filled = true),
        C(6.5, 12.5, 1, filled = true),
        P("M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.93 0 1.65-.75 1.65-1.69 0-.44-.18-.84-.44-1.13-.29-.29-.44-.65-.44-1.13a1.64 1.64 0 0 1 1.67-1.67h2c3.05 0 5.56-2.5 5.56-5.55C21.97 6.01 17.46 2 12 2z"),
      )
    case Icon.Ruler =>
      Seq(P("M21.3 15.3a2.4 2.4 0 0 1 0 3.4l-2.6 2.6a2.4 2.4 0 0 1-3.4 0L2.7 8.7a2.41 2.41 0 0 1 0-3.4l2.6-2.6a2.41 2.41 0 0 1 3.4 0ZM14.5 12.5l2-2M11.5 9.5l2-2M8.5 6.5l2-2M17.5 15.5l2-2"))
    case Icon.Book =>
      Seq(P("M12 7v14M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z"))
    case Icon.Wallet =>
      Seq(P("M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"))

  /** Renders an icon at `size` px (square) with the given stroke width, inheriting `color` from its parent. */
  def apply(icon: Icon, size: Int = 18, strokeWidth: Double = 2.0): UI =
    val stroke = Svg.Paint.CurrentColor
    val nodes: Seq[Svg.SvgChild] = shapes(icon).map {
      case P(d) =>
        Svg.path
          .d(Svg.PathData.raw(d))
          .fill(Svg.Paint.None)
          .stroke(stroke)
          .strokeWidth(strokeWidth)
          .strokeLinecap(Svg.StrokeLinecap.Round)
          .strokeLinejoin(Svg.StrokeLinejoin.Round)
      case C(cx, cy, r, filled) =>
        Svg.circle
          .cx(cx)
          .cy(cy)
          .r(r)
          .fill(if filled then stroke else Svg.Paint.None)
          .stroke(stroke)
          .strokeWidth(strokeWidth)
      case R(x, y, w, h, rx) =>
        Svg.rect
          .x(x)
          .y(y)
          .width(w)
          .height(h)
          .rx(rx)
          .fill(Svg.Paint.None)
          .stroke(stroke)
          .strokeWidth(strokeWidth)
          .strokeLinejoin(Svg.StrokeLinejoin.Round)
    }
    span
      .cssClass("mp-icon")
      .aria("hidden", "true")
      .style(Style.width(size.px).height(size.px))(
        Svg.svg.width(size).height(size).viewBox(Svg.ViewBox(0, 0, 24, 24))(nodes*),
      )

  /** Icon followed by a text label — the standard content of an icon button. */
  def labeled(icon: Icon, label: UI, size: Int = 16): UI =
    UI.fragment(apply(icon, size), span(label))
