package com.crozzers.postboxgo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.R
import com.crozzers.postboxgo.utils.parsePostboxType

@Composable
fun PostboxIcon(modifier: Modifier = Modifier, type: String?, inactive: Boolean = false) {
    Icon(
        painter = painterResource(id = getIconFromPostboxType(type)),
        contentDescription = type,
        modifier = modifier
            .padding(10.dp)
            .alpha(if (inactive) 0.5f else 1.0f),
        tint = Color.Companion.Unspecified,
    )
}

fun getIconFromPostboxType(type: String?): Int {
    return parsePostboxType(type).second ?: R.drawable.pillar_generic
}
