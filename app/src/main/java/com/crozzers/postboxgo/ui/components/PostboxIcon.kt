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
    val type = (type ?: "").lowercase()

    return if (type.contains("k type pillar")) {
        R.drawable.k_type_pillar
    } else if (type.contains("wall box c type")) {
        R.drawable.wall_box_c_type
    } else if (type.contains("wall box")) {
        R.drawable.wall_box_b_type
    } else if (type.contains("lamp pedastal") || type.contains("l type")) {
        R.drawable.lamp_post
    } else if (type.contains("bantam n")) {
        R.drawable.bantam_n_type
    } else if (type.contains("parcel")) {
        R.drawable.parcel
    } else if (type.contains("type c") || type.contains("c type")) {
        R.drawable.type_c
    } else if (type.contains("m type")) {
        R.drawable.m_type
    } else {
        R.drawable.pillar_generic
    }
}