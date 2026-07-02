package org.xmsleep.app.ui.starsky

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BookmarkCheckFilled: ImageVector
  get() {
    if (_bookmarkCheckFilled != null) {
      return _bookmarkCheckFilled!!
    }
    _bookmarkCheckFilled =
      ImageVector.Builder(
          name = "bookmark_check",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
          ) {
            moveTo(10.95f, 14f)
            lineTo(15.9f, 9.05f)
            lineTo(14.48f, 7.65f)
            lineToRelative(-3.53f, 3.53f)
            lineTo(9.53f, 9.75f)
            lineTo(8.1f, 11.18f)
            lineTo(10.95f, 14f)
            close()
            moveTo(5f, 21f)
            verticalLineTo(5f)
            quadTo(5f, 4.17f, 5.59f, 3.59f)
            reflectiveQuadTo(7f, 3f)
            horizontalLineTo(17f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(19f, 5f)
            verticalLineTo(21f)
            lineTo(12f, 18f)
            lineTo(5f, 21f)
            close()
          }
        }
        .build()
    return _bookmarkCheckFilled!!
  }

private var _bookmarkCheckFilled: ImageVector? = null
