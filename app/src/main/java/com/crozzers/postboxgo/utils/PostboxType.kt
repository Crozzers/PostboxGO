package com.crozzers.postboxgo.utils

import com.crozzers.postboxgo.R

/**
 * Parses a postbox string type and gives a category and icon
 */
fun parsePostboxType(type: String?): Pair<String?, Int?> {
    val type = (type ?: "").lowercase()

    // TODO penfold boxes 1866-1879, historical fluted postbox 1856-? (eg one in warwick),
    // type D+E (1931), liverpool special, type B "nigerian" 1979-1980
    var category: String? = null
    var icon: Int? = null

    if (type.contains("wall box")) {
        category = "Wall Box"
        icon =
            if (type.contains("c type")) R.drawable.wall_box_c_type else R.drawable.wall_box_b_type
    } else if (type.contains("type c") || type.contains("c type")) {
        category = "Double"
        icon = R.drawable.type_c
    } else if (type.contains("lamp pedastal") || type.contains("l type")) {
        category = "Lamppost Box"
        icon = R.drawable.lamp_post
    } else if (type.contains("bantam n")) {
        category = "Lamppost Box"
        icon = R.drawable.bantam_n_type
    } else if (type.contains("m type")) {
        category = "Lamppost Box"
        icon = R.drawable.m_type
    } else if (type.contains("parcel")) {
        category = "Parcel"
        icon = R.drawable.parcel
    } else if (type.contains("pillar")) {
        category = "Pillar"
        icon = if (type.contains("k type")) R.drawable.k_type_pillar else R.drawable.pillar_generic
    } else if (type.contains("indoor")) {
        category = "Indoor"
    }

    return Pair(category, icon)
}