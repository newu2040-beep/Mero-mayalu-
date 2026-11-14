package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private fun simpleIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    materialIcon(name = name) { materialPath(pathBuilder = block) }

val Icons.Filled.AddAPhoto: ImageVector by lazy {
    simpleIcon("Filled.AddAPhoto") {
        moveTo(3.0f, 4.0f); verticalLineTo(1.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(3.0f)
        horizontalLineToRelative(3.0f); verticalLineToRelative(2.0f); horizontalLineTo(5.0f); verticalLineToRelative(3.0f)
        horizontalLineTo(3.0f); verticalLineTo(6.0f); horizontalLineTo(0.0f); verticalLineTo(4.0f); horizontalLineToRelative(3.0f); close()
        moveTo(6.0f, 10.0f); verticalLineTo(7.0f); horizontalLineToRelative(3.0f); verticalLineTo(4.0f)
        horizontalLineToRelative(7.0f); lineToRelative(1.83f, 2.0f); horizontalLineTo(21.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f); verticalLineToRelative(12.0f)
        curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f); horizontalLineTo(5.0f)
        curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f); verticalLineTo(10.0f); horizontalLineToRelative(3.0f); close()
        moveTo(13.0f, 19.0f); curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
        reflectiveCurveToRelative(-2.24f, -5.0f, -5.0f, -5.0f); reflectiveCurveToRelative(-5.0f, 2.24f, -5.0f, 5.0f)
        reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f); close()
    }
}

val Icons.Filled.AutoAwesome: ImageVector by lazy {
    simpleIcon("Filled.AutoAwesome") {
        moveTo(19.0f, 9.0f); lineToRelative(1.25f, -2.75f); lineTo(23.0f, 5.0f); lineToRelative(-2.75f, -1.25f)
        lineTo(19.0f, 1.0f); lineToRelative(-1.25f, 2.75f); lineTo(15.0f, 5.0f); lineToRelative(2.75f, 1.25f); lineTo(19.0f, 9.0f); close()
        moveTo(11.5f, 9.5f); lineTo(9.0f, 4.0f); lineTo(6.5f, 9.5f); lineTo(1.0f, 12.0f)
        lineToRelative(5.5f, 2.5f); lineTo(9.0f, 20.0f); lineToRelative(2.5f, -5.5f); lineTo(17.0f, 12.0f); lineToRelative(-5.5f, -2.5f); close()
        moveTo(19.0f, 15.0f); lineToRelative(-1.25f, 2.75f); lineTo(15.0f, 19.0f); lineToRelative(2.75f, 1.25f)
        lineTo(19.0f, 23.0f); lineToRelative(1.25f, -2.75f); lineTo(23.0f, 19.0f); lineToRelative(-2.75f, -1.25f); lineTo(19.0f, 15.0f); close()
    }
}

val Icons.Filled.BookmarkAdd: ImageVector by lazy {
    simpleIcon("Filled.BookmarkAdd") {
        moveTo(21.0f, 7.0f); horizontalLineToRelative(-2.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(-2.0f)
        verticalLineTo(7.0f); horizontalLineToRelative(-2.0f); verticalLineTo(5.0f); horizontalLineToRelative(2.0f)
        verticalLineTo(3.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(2.0f); verticalLineTo(7.0f); close()
        moveTo(19.0f, 21.0f); lineToRelative(-7.0f, -3.0f); lineToRelative(-7.0f, 3.0f); verticalLineTo(5.0f)
        curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f); horizontalLineToRelative(7.0f)
        curveToRelative(-0.63f, 0.84f, -1.0f, 1.87f, -1.0f, 3.0f); curveToRelative(0.0f, 2.76f, 2.24f, 5.0f, 5.0f, 5.0f)
        curveToRelative(0.34f, 0.0f, 0.68f, -0.03f, 1.0f, -0.1f); verticalLineTo(21.0f); close()
    }
}

val Icons.Filled.CalendarMonth: ImageVector by lazy {
    simpleIcon("Filled.CalendarMonth") {
        moveTo(19.0f, 4.0f); horizontalLineToRelative(-1.0f); verticalLineTo(2.0f); horizontalLineToRelative(-2.0f)
        verticalLineToRelative(2.0f); horizontalLineTo(8.0f); verticalLineTo(2.0f); horizontalLineTo(6.0f)
        verticalLineToRelative(2.0f); horizontalLineTo(5.0f); curveTo(3.89f, 4.0f, 3.01f, 4.9f, 3.01f, 6.0f)
        lineTo(3.0f, 20.0f); curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(14.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(6.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
        moveTo(19.0f, 20.0f); horizontalLineTo(5.0f); verticalLineTo(10.0f); horizontalLineToRelative(14.0f); verticalLineTo(20.0f); close()
    }
}

val Icons.Filled.CameraAlt: ImageVector by lazy {
    simpleIcon("Filled.CameraAlt") {
        moveTo(12.0f, 12.0f); moveToRelative(-3.2f, 0.0f)
        arcToRelative(3.2f, 3.2f, 0.0f, true, true, 6.4f, 0.0f)
        arcToRelative(3.2f, 3.2f, 0.0f, true, true, -6.4f, 0.0f)
        moveTo(9.0f, 2.0f); lineTo(7.17f, 4.0f); horizontalLineTo(4.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(12.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(16.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(6.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
        horizontalLineToRelative(-3.17f); lineTo(15.0f, 2.0f); horizontalLineTo(9.0f); close()
        moveTo(12.0f, 17.0f); curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
        reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f); reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
        reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f); close()
    }
}

val Icons.Filled.Celebration: ImageVector by lazy {
    simpleIcon("Filled.Celebration") {
        moveTo(2.0f, 22.0f); lineToRelative(14.0f, -5.0f); lineTo(7.0f, 8.0f); lineTo(2.0f, 22.0f); close()
        moveTo(14.53f, 12.53f); lineTo(21.0f, 6.05f); lineToRelative(-1.41f, -1.41f); lineToRelative(-6.47f, 6.47f); lineToRelative(1.41f, 1.42f); close()
        moveTo(11.0f, 9.0f); lineToRelative(5.0f, -5.0f); lineToRelative(-1.41f, -1.41f); lineTo(9.59f, 7.59f); lineTo(11.0f, 9.0f); close()
        moveTo(15.0f, 13.0f); lineToRelative(5.0f, 5.0f); lineToRelative(1.41f, -1.41f); lineToRelative(-5.0f, -5.0f); lineTo(15.0f, 13.0f); close()
    }
}

val Icons.Filled.ContentCopy: ImageVector by lazy {
    simpleIcon("Filled.ContentCopy") {
        moveTo(16.0f, 1.0f); horizontalLineTo(4.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(14.0f); horizontalLineToRelative(2.0f); verticalLineTo(3.0f); horizontalLineToRelative(12.0f); verticalLineTo(1.0f); close()
        moveTo(19.0f, 5.0f); horizontalLineTo(8.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(14.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(11.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(7.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
        moveTo(19.0f, 21.0f); horizontalLineTo(8.0f); verticalLineTo(7.0f); horizontalLineToRelative(11.0f); verticalLineTo(21.0f); close()
    }
}

val Icons.Filled.DeleteForever: ImageVector by lazy {
    simpleIcon("Filled.DeleteForever") {
        moveTo(6.0f, 19.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(8.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(7.0f); horizontalLineTo(6.0f); verticalLineToRelative(12.0f); close()
        moveTo(8.46f, 11.88f); lineToRelative(1.41f, -1.41f); lineTo(12.0f, 12.59f); lineToRelative(2.12f, -2.12f)
        lineToRelative(1.41f, 1.41f); lineTo(13.41f, 14.0f); lineToRelative(2.12f, 2.12f); lineToRelative(-1.41f, 1.41f)
        lineTo(12.0f, 15.41f); lineToRelative(-2.12f, 2.12f); lineToRelative(-1.41f, -1.41f); lineTo(10.59f, 14.0f); lineToRelative(-2.13f, -2.12f); close()
        moveTo(15.5f, 4.0f); lineToRelative(-1.0f, -1.0f); horizontalLineToRelative(-5.0f); lineToRelative(-1.0f, 1.0f)
        horizontalLineTo(5.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(14.0f); verticalLineTo(4.0f); close()
    }
}

val Icons.Filled.Download: ImageVector by lazy {
    simpleIcon("Filled.Download") {
        moveTo(5.0f, 20.0f); horizontalLineToRelative(14.0f); verticalLineToRelative(-2.0f); horizontalLineTo(5.0f); verticalLineToRelative(2.0f); close()
        moveTo(19.0f, 9.0f); horizontalLineToRelative(-4.0f); verticalLineTo(3.0f); horizontalLineTo(9.0f)
        verticalLineToRelative(6.0f); horizontalLineTo(5.0f); lineToRelative(7.0f, 7.0f); lineToRelative(7.0f, -7.0f); close()
    }
}

val Icons.Filled.EmojiEvents: ImageVector by lazy {
    simpleIcon("Filled.EmojiEvents") {
        moveTo(19.0f, 5.0f); horizontalLineToRelative(-2.0f); verticalLineTo(3.0f); horizontalLineTo(7.0f)
        verticalLineToRelative(2.0f); horizontalLineTo(5.0f); curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f)
        verticalLineToRelative(1.0f); curveToRelative(0.0f, 2.55f, 1.92f, 4.63f, 4.39f, 4.94f)
        curveToRelative(0.63f, 1.5f, 1.98f, 2.63f, 3.61f, 2.96f); verticalLineTo(19.0f); horizontalLineTo(7.0f)
        verticalLineToRelative(2.0f); horizontalLineToRelative(10.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(-4.0f)
        verticalLineToRelative(-3.1f); curveToRelative(1.63f, -0.33f, 2.98f, -1.46f, 3.61f, -2.96f)
        curveTo(19.08f, 12.63f, 21.0f, 10.55f, 21.0f, 8.0f); verticalLineTo(7.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
    }
}

val Icons.Filled.ErrorOutline: ImageVector by lazy {
    simpleIcon("Filled.ErrorOutline") {
        moveTo(11.0f, 15.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(-2.0f); verticalLineToRelative(-2.0f); close()
        moveTo(11.0f, 7.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(6.0f); horizontalLineToRelative(-2.0f); verticalLineTo(7.0f); close()
        moveTo(11.99f, 2.0f); curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f); reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
        curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f); reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f); close()
        moveTo(12.0f, 20.0f); curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
        reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f); reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
        reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f); close()
    }
}

val Icons.Filled.Explore: ImageVector by lazy {
    simpleIcon("Filled.Explore") {
        moveTo(12.0f, 10.9f); curveToRelative(-0.61f, 0.0f, -1.1f, 0.49f, -1.1f, 1.1f); reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
        curveToRelative(0.61f, 0.0f, 1.1f, -0.49f, 1.1f, -1.1f); reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f); close()
        moveTo(12.0f, 2.0f); curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f); reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f); reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f); close()
        moveTo(14.19f, 14.19f); lineTo(6.0f, 18.0f); lineToRelative(3.81f, -8.19f); lineTo(18.0f, 6.0f); lineToRelative(-3.81f, 8.19f); close()
    }
}

val Icons.Filled.Fingerprint: ImageVector by lazy {
    simpleIcon("Filled.Fingerprint") {
        moveTo(17.81f, 4.47f); curveToRelative(-0.08f, 0.0f, -0.16f, -0.02f, -0.23f, -0.06f)
        curveTo(15.66f, 3.42f, 14.0f, 3.0f, 12.01f, 3.0f); curveToRelative(-1.98f, 0.0f, -3.86f, 0.47f, -5.57f, 1.41f)
        curveToRelative(-0.24f, 0.13f, -0.54f, 0.04f, -0.68f, -0.2f); curveToRelative(-0.13f, -0.24f, -0.04f, -0.55f, 0.2f, -0.68f)
        curveTo(7.82f, 2.52f, 9.86f, 2.0f, 12.01f, 2.0f); curveToRelative(2.13f, 0.0f, 3.99f, 0.47f, 6.03f, 1.52f)
        curveToRelative(0.25f, 0.13f, 0.34f, 0.43f, 0.21f, 0.67f); curveToRelative(-0.09f, 0.18f, -0.26f, 0.28f, -0.44f, 0.28f); close()
        moveTo(12.0f, 6.0f); curveToRelative(-3.86f, 0.0f, -7.0f, 3.14f, -7.0f, 7.0f); verticalLineToRelative(4.0f)
        horizontalLineToRelative(2.0f); verticalLineToRelative(-4.0f); curveToRelative(0.0f, -2.76f, 2.24f, -5.0f, 5.0f, -5.0f)
        reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f); verticalLineToRelative(4.0f); horizontalLineToRelative(2.0f)
        verticalLineToRelative(-4.0f); curveToRelative(0.0f, -3.86f, -3.14f, -7.0f, -7.0f, -7.0f); close()
        moveTo(12.0f, 10.0f); curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f); verticalLineToRelative(6.0f)
        horizontalLineToRelative(2.0f); verticalLineToRelative(-6.0f); curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
        reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f); verticalLineToRelative(6.0f); horizontalLineToRelative(2.0f)
        verticalLineToRelative(-6.0f); curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f); close()
    }
}

val Icons.Filled.FolderOpen: ImageVector by lazy {
    simpleIcon("Filled.FolderOpen") {
        moveTo(20.0f, 6.0f); horizontalLineToRelative(-8.0f); lineToRelative(-2.0f, -2.0f); horizontalLineTo(4.0f)
        curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f); lineTo(2.0f, 18.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
        horizontalLineToRelative(16.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(8.0f)
        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
        moveTo(20.0f, 18.0f); horizontalLineTo(4.0f); verticalLineTo(8.0f); horizontalLineToRelative(16.0f); verticalLineToRelative(10.0f); close()
    }
}

val Icons.Filled.HighQuality: ImageVector by lazy {
    simpleIcon("Filled.HighQuality") {
        moveTo(19.0f, 4.0f); horizontalLineTo(5.0f); curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(12.0f); curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(14.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(6.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
        moveTo(11.0f, 15.0f); horizontalLineTo(9.5f); verticalLineToRelative(-2.0f); horizontalLineToRelative(-2.0f)
        verticalLineToRelative(2.0f); horizontalLineTo(6.0f); verticalLineTo(9.0f); horizontalLineToRelative(1.5f)
        verticalLineToRelative(2.5f); horizontalLineToRelative(2.0f); verticalLineTo(9.0f); horizontalLineTo(11.0f); verticalLineToRelative(6.0f); close()
        moveTo(18.0f, 14.0f); curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f); horizontalLineToRelative(-0.75f)
        verticalLineToRelative(1.5f); horizontalLineToRelative(-1.5f); verticalLineTo(15.0f); horizontalLineTo(14.0f)
        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f); verticalLineToRelative(-4.0f)
        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f); horizontalLineToRelative(3.0f)
        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f); verticalLineToRelative(4.0f); close()
    }
}

val Icons.Filled.HourglassTop: ImageVector by lazy {
    simpleIcon("Filled.HourglassTop") {
        moveTo(6.0f, 2.0f); lineToRelative(0.01f, 6.0f); lineTo(10.0f, 12.0f); lineToRelative(-3.99f, 4.01f)
        lineTo(6.0f, 22.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(-5.99f); lineTo(14.0f, 12.0f)
        lineToRelative(3.99f, -4.01f); lineTo(18.0f, 2.0f); horizontalLineTo(6.0f); close()
        moveTo(16.0f, 16.5f); verticalLineTo(20.0f); horizontalLineTo(8.0f); verticalLineToRelative(-3.5f)
        lineToRelative(4.0f, -4.0f); lineToRelative(4.0f, 4.0f); close()
    }
}

val Icons.Filled.Image: ImageVector by lazy {
    simpleIcon("Filled.Image") {
        moveTo(21.0f, 19.0f); verticalLineTo(5.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
        horizontalLineTo(5.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f); verticalLineToRelative(14.0f)
        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(14.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); close()
        moveTo(8.5f, 13.5f); lineToRelative(2.5f, 3.01f); lineTo(14.5f, 12.0f); lineToRelative(4.5f, 6.0f)
        horizontalLineTo(5.0f); lineToRelative(3.5f, -4.5f); close()
    }
}

val Icons.Filled.Key: ImageVector by lazy {
    simpleIcon("Filled.Key") {
        moveTo(21.0f, 10.0f); horizontalLineToRelative(-8.35f)
        curveTo(11.83f, 7.67f, 9.61f, 6.0f, 7.0f, 6.0f); curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
        reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f); curveToRelative(2.61f, 0.0f, 4.83f, -1.67f, 5.65f, -4.0f)
        horizontalLineTo(13.0f); lineToRelative(2.0f, 2.0f); lineToRelative(2.0f, -2.0f); lineToRelative(2.0f, 2.0f)
        lineToRelative(4.0f, -4.04f); lineTo(21.0f, 10.0f); close()
        moveTo(7.0f, 15.0f); curveToRelative(-1.65f, 0.0f, -3.0f, -1.35f, -3.0f, -3.0f)
        curveToRelative(0.0f, -1.65f, 1.35f, -3.0f, 3.0f, -3.0f); reflectiveCurveToRelative(3.0f, 1.35f, 3.0f, 3.0f)
        curveToRelative(0.0f, 1.65f, -1.35f, 3.0f, -3.0f, 3.0f); close()
    }
}

val Icons.Filled.Language: ImageVector by lazy {
    simpleIcon("Filled.Language") {
        moveTo(11.99f, 2.0f); curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f); reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
        curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f); reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f); close()
        moveTo(18.92f, 8.0f); horizontalLineToRelative(-2.95f); curveToRelative(-0.32f, -1.25f, -0.78f, -2.45f, -1.38f, -3.56f)
        curveTo(16.43f, 5.07f, 17.96f, 6.35f, 18.92f, 8.0f); close()
        moveTo(12.0f, 4.04f); curveToRelative(0.83f, 1.2f, 1.48f, 2.53f, 1.91f, 3.96f); horizontalLineToRelative(-3.82f)
        curveToRelative(0.43f, -1.43f, 1.08f, -2.76f, 1.91f, -3.96f); close()
        moveTo(4.26f, 14.0f); curveTo(4.1f, 13.36f, 4.0f, 12.69f, 4.0f, 12.0f); reflectiveCurveToRelative(0.1f, -1.36f, 0.26f, -2.0f)
        horizontalLineToRelative(3.38f); curveToRelative(-0.08f, 0.66f, -0.14f, 1.32f, -0.14f, 2.0f)
        reflectiveCurveToRelative(0.06f, 1.34f, 0.14f, 2.0f); horizontalLineTo(4.26f); close()
    }
}

val Icons.Filled.LocalFlorist: ImageVector by lazy {
    simpleIcon("Filled.LocalFlorist") {
        moveTo(12.0f, 22.0f); curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
        curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f); close()
        moveTo(5.6f, 10.25f); curveToRelative(0.0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f)
        curveToRelative(0.53f, 0.0f, 1.01f, -0.16f, 1.42f, -0.44f); lineToRelative(-0.02f, 0.19f)
        curveToRelative(0.0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f); reflectiveCurveToRelative(2.5f, -1.12f, 2.5f, -2.5f)
        lineToRelative(-0.02f, -0.19f); curveToRelative(0.4f, 0.28f, 0.89f, 0.44f, 1.42f, 0.44f)
        curveToRelative(1.38f, 0.0f, 2.5f, -1.12f, 2.5f, -2.5f); curveToRelative(0.0f, -1.0f, -0.59f, -1.85f, -1.43f, -2.25f)
        curveToRelative(0.84f, -0.4f, 1.43f, -1.25f, 1.43f, -2.25f); curveToRelative(0.0f, -1.38f, -1.12f, -2.5f, -2.5f, -2.5f)
        curveToRelative(-0.53f, 0.0f, -1.01f, 0.16f, -1.42f, 0.44f); lineToRelative(0.02f, -0.19f)
        curveTo(14.5f, 2.12f, 13.38f, 1.0f, 12.0f, 1.0f); reflectiveCurveTo(9.5f, 2.12f, 9.5f, 3.5f)
        lineToRelative(0.02f, 0.19f); curveTo(9.11f, 3.41f, 8.63f, 3.25f, 8.1f, 3.25f)
        curveToRelative(-1.38f, 0.0f, -2.5f, 1.12f, -2.5f, 2.5f); curveToRelative(0.0f, 1.0f, 0.59f, 1.85f, 1.43f, 2.25f)
        curveToRelative(-0.84f, 0.4f, -1.43f, 1.25f, -1.43f, 2.25f); close()
        moveTo(12.0f, 5.5f); curveToRelative(1.38f, 0.0f, 2.5f, 1.12f, 2.5f, 2.5f); reflectiveCurveTo(13.38f, 10.5f, 12.0f, 10.5f)
        reflectiveCurveTo(9.5f, 9.38f, 9.5f, 8.0f); reflectiveCurveTo(10.62f, 5.5f, 12.0f, 5.5f); close()
        moveTo(3.0f, 13.0f); curveToRelative(0.0f, 4.97f, 4.03f, 9.0f, 9.0f, 9.0f); curveToRelative(0.0f, -4.97f, -4.03f, -9.0f, -9.0f, -9.0f); close()
    }
}

val Icons.Filled.Mic: ImageVector by lazy {
    simpleIcon("Filled.Mic") {
        moveTo(12.0f, 14.0f); curveToRelative(1.66f, 0.0f, 2.99f, -1.34f, 2.99f, -3.0f); lineTo(15.0f, 5.0f)
        curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f); reflectiveCurveTo(9.0f, 3.34f, 9.0f, 5.0f)
        verticalLineToRelative(6.0f); curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f); close()
        moveTo(17.3f, 11.0f); curveToRelative(0.0f, 3.0f, -2.54f, 5.1f, -5.3f, 5.1f); reflectiveCurveTo(6.7f, 14.0f, 6.7f, 11.0f)
        horizontalLineTo(5.0f); curveToRelative(0.0f, 3.41f, 2.72f, 6.23f, 6.0f, 6.72f); verticalLineTo(21.0f)
        horizontalLineToRelative(2.0f); verticalLineToRelative(-3.28f); curveToRelative(3.28f, -0.48f, 6.0f, -3.3f, 6.0f, -6.72f); horizontalLineToRelative(-1.7f); close()
    }
}

val Icons.Filled.MyLocation: ImageVector by lazy {
    simpleIcon("Filled.MyLocation") {
        moveTo(12.0f, 8.0f); curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f); reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
        reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f); reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f); close()
        moveTo(20.94f, 11.0f); curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f); verticalLineTo(1.0f)
        horizontalLineToRelative(-2.0f); verticalLineToRelative(2.06f); curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11.0f)
        horizontalLineTo(1.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(2.06f); curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f)
        verticalLineTo(23.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(-2.06f); curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f)
        horizontalLineTo(23.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(-2.06f); close()
        moveTo(12.0f, 19.0f); curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f); reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
        reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f); reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f); close()
    }
}

val Icons.Filled.NoteAdd: ImageVector by lazy {
    simpleIcon("Filled.NoteAdd") {
        moveTo(14.0f, 2.0f); horizontalLineTo(6.0f); curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
        lineTo(4.0f, 20.0f); curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f); horizontalLineTo(18.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(8.0f); lineToRelative(-6.0f, -6.0f); close()
        moveTo(16.0f, 16.0f); horizontalLineToRelative(-3.0f); verticalLineToRelative(3.0f); horizontalLineToRelative(-2.0f)
        verticalLineToRelative(-3.0f); horizontalLineTo(8.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(3.0f)
        verticalLineToRelative(-3.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(3.0f); horizontalLineToRelative(3.0f); verticalLineToRelative(2.0f); close()
        moveTo(13.0f, 9.0f); verticalLineTo(3.5f); lineTo(18.5f, 9.0f); horizontalLineTo(13.0f); close()
    }
}

val Icons.Filled.PhotoLibrary: ImageVector by lazy {
    simpleIcon("Filled.PhotoLibrary") {
        moveTo(22.0f, 16.0f); verticalLineTo(4.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
        horizontalLineTo(8.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f); verticalLineToRelative(12.0f)
        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(12.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); close()
        moveTo(11.0f, 12.0f); lineToRelative(2.03f, 2.71f); lineTo(16.0f, 11.0f); lineToRelative(4.0f, 5.0f)
        horizontalLineTo(8.0f); lineToRelative(3.0f, -4.0f); close()
        moveTo(2.0f, 6.0f); verticalLineToRelative(14.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
        horizontalLineToRelative(14.0f); verticalLineToRelative(-2.0f); horizontalLineTo(4.0f); verticalLineTo(6.0f); horizontalLineTo(2.0f); close()
    }
}

val Icons.Filled.Poll: ImageVector by lazy {
    simpleIcon("Filled.Poll") {
        moveTo(19.0f, 3.0f); horizontalLineTo(5.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(14.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(14.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(5.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
        moveTo(9.0f, 17.0f); horizontalLineTo(7.0f); verticalLineToRelative(-7.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(7.0f); close()
        moveTo(13.0f, 17.0f); horizontalLineToRelative(-2.0f); verticalLineTo(7.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(10.0f); close()
        moveTo(17.0f, 17.0f); horizontalLineToRelative(-2.0f); verticalLineToRelative(-4.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(4.0f); close()
    }
}

val Icons.Filled.Quiz: ImageVector by lazy {
    simpleIcon("Filled.Quiz") {
        moveTo(4.0f, 6.0f); horizontalLineTo(2.0f); verticalLineToRelative(14.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
        horizontalLineToRelative(14.0f); verticalLineToRelative(-2.0f); horizontalLineTo(4.0f); verticalLineTo(6.0f); close()
        moveTo(20.0f, 2.0f); horizontalLineTo(8.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
        verticalLineToRelative(12.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(12.0f)
        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(4.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
        moveTo(14.01f, 15.0f); horizontalLineToRelative(-2.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(2.0f); close()
        moveTo(16.1f, 8.26f); curveToRelative(-0.32f, 0.59f, -0.88f, 0.97f, -1.39f, 1.46f)
        curveToRelative(-0.48f, 0.45f, -0.7f, 0.82f, -0.7f, 1.78f); horizontalLineToRelative(-1.9f)
        curveToRelative(0.0f, -1.19f, 0.42f, -1.8f, 1.07f, -2.42f); curveToRelative(0.5f, -0.48f, 1.13f, -0.87f, 1.13f, -1.62f)
        curveToRelative(0.0f, -0.82f, -0.62f, -1.36f, -1.35f, -1.36f); curveToRelative(-0.69f, 0.0f, -1.29f, 0.46f, -1.4f, 1.17f)
        horizontalLineTo(9.67f); curveTo(9.82f, 5.65f, 11.17f, 4.5f, 12.99f, 4.5f)
        curveToRelative(1.9f, 0.0f, 3.31f, 1.24f, 3.31f, 2.91f); curveToRelative(0.0f, 0.31f, -0.07f, 0.6f, -0.2f, 0.85f); close()
    }
}

val Icons.Filled.Restaurant: ImageVector by lazy {
    simpleIcon("Filled.Restaurant") {
        moveTo(11.0f, 9.0f); horizontalLineTo(9.0f); verticalLineTo(2.0f); horizontalLineTo(7.0f); verticalLineToRelative(7.0f)
        horizontalLineTo(5.0f); verticalLineTo(2.0f); horizontalLineTo(3.0f); verticalLineToRelative(7.0f)
        curveToRelative(0.0f, 2.12f, 1.66f, 3.84f, 3.75f, 3.97f); verticalLineTo(22.0f); horizontalLineToRelative(2.5f)
        verticalLineToRelative(-9.03f); curveTo(11.34f, 12.84f, 13.0f, 11.12f, 13.0f, 9.0f); verticalLineTo(2.0f); horizontalLineToRelative(-2.0f); verticalLineToRelative(7.0f); close()
        moveTo(16.0f, 6.0f); verticalLineToRelative(8.0f); horizontalLineToRelative(2.5f); verticalLineToRelative(8.0f)
        horizontalLineTo(21.0f); verticalLineTo(2.0f); curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 4.0f); close()
    }
}

val Icons.Filled.Schedule: ImageVector by lazy {
    simpleIcon("Filled.Schedule") {
        moveTo(11.99f, 2.0f); curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f); reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
        curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f); reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f); close()
        moveTo(12.0f, 20.0f); curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
        reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f); reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
        reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f); close()
        moveTo(12.5f, 7.0f); horizontalLineTo(11.0f); verticalLineToRelative(6.0f); lineToRelative(5.25f, 3.15f)
        lineToRelative(0.75f, -1.23f); lineToRelative(-4.5f, -2.67f); close()
    }
}

val Icons.Filled.Shield: ImageVector by lazy {
    simpleIcon("Filled.Shield") {
        moveTo(12.0f, 1.0f); lineTo(3.0f, 5.0f); verticalLineToRelative(6.0f)
        curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f); curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
        verticalLineTo(5.0f); lineToRelative(-9.0f, -4.0f); close()
    }
}

val Icons.Filled.Spa: ImageVector by lazy {
    simpleIcon("Filled.Spa") {
        moveTo(15.49f, 9.63f); curveToRelative(-0.18f, -2.79f, -1.31f, -5.51f, -3.49f, -7.63f)
        curveToRelative(-2.18f, 2.12f, -3.31f, 4.84f, -3.49f, 7.63f); curveToRelative(1.28f, 0.68f, 2.46f, 1.56f, 3.49f, 2.63f)
        curveToRelative(1.03f, -1.07f, 2.21f, -1.95f, 3.49f, -2.63f); close()
        moveTo(12.0f, 15.45f); curveTo(9.85f, 12.17f, 6.18f, 10.0f, 2.0f, 10.0f)
        curveToRelative(0.0f, 5.32f, 3.36f, 9.82f, 8.03f, 11.49f); curveToRelative(0.63f, 0.23f, 1.29f, 0.4f, 1.97f, 0.51f)
        curveToRelative(0.68f, -0.11f, 1.34f, -0.28f, 1.97f, -0.51f); curveTo(18.64f, 19.82f, 22.0f, 15.32f, 22.0f, 10.0f)
        curveToRelative(-4.18f, 0.0f, -7.85f, 2.17f, -10.0f, 5.45f); close()
    }
}

val Icons.Filled.Stop: ImageVector by lazy {
    simpleIcon("Filled.Stop") {
        moveTo(6.0f, 6.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(12.0f); horizontalLineTo(6.0f); close()
    }
}

val Icons.Filled.TempleBuddhist: ImageVector by lazy {
    simpleIcon("Filled.TempleBuddhist") {
        moveTo(21.0f, 9.02f); curveToRelative(0.0f, 1.09f, -0.89f, 1.98f, -1.98f, 1.98f); horizontalLineTo(4.98f)
        curveTo(3.89f, 11.0f, 3.0f, 10.11f, 3.0f, 9.02f); horizontalLineTo(1.0f)
        curveToRelative(0.0f, 1.86f, 1.28f, 3.4f, 3.0f, 3.84f); verticalLineTo(22.0f); horizontalLineToRelative(6.0f)
        verticalLineToRelative(-5.0f); horizontalLineToRelative(4.0f); verticalLineToRelative(5.0f); horizontalLineToRelative(6.0f)
        verticalLineToRelative(-9.14f); curveToRelative(1.72f, -0.44f, 3.0f, -1.98f, 3.0f, -3.84f); horizontalLineTo(21.0f); close()
        moveTo(12.0f, 1.0f); lineTo(8.25f, 6.0f); horizontalLineToRelative(7.5f); lineTo(12.0f, 1.0f); close()
        moveTo(6.0f, 8.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(2.0f); horizontalLineTo(6.0f); verticalLineTo(8.0f); close()
    }
}

val Icons.Filled.Verified: ImageVector by lazy {
    simpleIcon("Filled.Verified") {
        moveTo(23.0f, 12.0f); lineToRelative(-2.44f, -2.79f); lineToRelative(0.34f, -3.69f); lineToRelative(-3.61f, -0.82f)
        lineToRelative(-1.89f, -3.2f); lineTo(12.0f, 2.96f); lineTo(8.6f, 1.5f); lineTo(6.71f, 4.69f)
        lineTo(3.1f, 5.5f); lineToRelative(0.34f, 3.7f); lineTo(1.0f, 12.0f); lineToRelative(2.44f, 2.79f)
        lineToRelative(-0.34f, 3.7f); lineToRelative(3.61f, 0.82f); lineTo(8.6f, 22.5f); lineTo(12.0f, 21.04f)
        lineToRelative(3.4f, 1.46f); lineToRelative(1.89f, -3.19f); lineToRelative(3.61f, -0.82f); lineToRelative(-0.34f, -3.69f); lineTo(23.0f, 12.0f); close()
        moveTo(10.09f, 16.72f); lineToRelative(-3.8f, -3.81f); lineToRelative(1.48f, -1.48f); lineToRelative(2.32f, 2.33f)
        lineToRelative(5.85f, -5.87f); lineToRelative(1.48f, 1.48f); lineTo(10.09f, 16.72f); close()
    }
}
