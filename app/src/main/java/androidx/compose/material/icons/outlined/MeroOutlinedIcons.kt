package androidx.compose.material.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private var _spaOutlined: ImageVector? = null
val Icons.Outlined.Spa: ImageVector
    get() {
        if (_spaOutlined != null) return _spaOutlined!!
        _spaOutlined = materialIcon(name = "Outlined.Spa") {
            materialPath {
                moveTo(15.49f, 9.63f)
                curveToRelative(-0.18f, -2.79f, -1.31f, -5.51f, -3.49f, -7.63f)
                curveToRelative(-2.18f, 2.12f, -3.31f, 4.84f, -3.49f, 7.63f)
                curveToRelative(1.28f, 0.68f, 2.46f, 1.56f, 3.49f, 2.63f)
                curveToRelative(1.03f, -1.07f, 2.21f, -1.95f, 3.49f, -2.63f)
                close()
                moveTo(12.0f, 15.45f)
                curveTo(9.85f, 12.17f, 6.18f, 10.0f, 2.0f, 10.0f)
                curveToRelative(0.0f, 5.32f, 3.36f, 9.82f, 8.03f, 11.49f)
                curveToRelative(0.63f, 0.23f, 1.29f, 0.4f, 1.97f, 0.51f)
                curveToRelative(0.68f, -0.11f, 1.34f, -0.28f, 1.97f, -0.51f)
                curveTo(18.64f, 19.82f, 22.0f, 15.32f, 22.0f, 10.0f)
                curveToRelative(-4.18f, 0.0f, -7.85f, 2.17f, -10.0f, 5.45f)
                close()
            }
        }
        return _spaOutlined!!
    }
