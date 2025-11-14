package com.crozzers.postboxgo.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.Monarch
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadablePostboxAgeEstimate
import com.crozzers.postboxgo.utils.humanReadablePostboxType
import com.crozzers.postboxgo.utils.parsePostboxType
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.min

@Composable
fun StatisticsView(saveFile: SaveFile) {
    val registeredOverTime = mutableMapOf<Long, Int>()
    val monarchCount = Monarch.entries.associate { Pair(it.ordinal, 0) }.toMutableMap()
    val monarchNameCount = Monarch.entries.associate {
        if (it == Monarch.SCOTTISH_CROWN) {
            Pair("Unmarked", 0)
        } else {
            Pair(it.displayName.split(" ")[0], 0)
        }
    }.toMutableMap()
    var postboxTypeCount = mutableMapOf<String?, Int>()
    val postboxTypeCategoryCount = mutableMapOf<String?, Int>()
    var earliestRegisteredPostbox: LocalDateTime? = null
    var earliestRegisteredPostboxDay: Long? = null
    var lastRegisteredPostbox: LocalDateTime? = null
    var inactivePostboxCount = 0
    var unverifiedPostboxCount = 0
    var mostDailyRegistrations: Pair<Long, Int>? = null
    val ageBounds = Pair(mutableListOf<Int>(), mutableListOf<Int>())

    if (saveFile.getPostboxes().isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {
            Text(
                "No statistics available because no postboxes have been registered yet.",
                textAlign = TextAlign.Center
            )
            Text(
                "Click the plus icon on the navigation bar to register a new postbox.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // build the stats
    saveFile.getPostboxes().toSortedMap(
        compareBy { LocalDateTime.parse(saveFile.getPostbox(it)!!.dateRegistered) }
    ).forEach { (id, postbox) ->
        // calculate day registered
        val dateRegistered = LocalDateTime.parse(postbox.dateRegistered)
        val day =
            dateRegistered.toEpochSecond(ZoneOffset.UTC)
                .div(60 * 60 * 24)
        if (registeredOverTime.containsKey(day)) {
            registeredOverTime[day] = registeredOverTime[day]!! + 1
        } else {
            registeredOverTime[day] = 1
        }
        earliestRegisteredPostboxDay =
            earliestRegisteredPostboxDay?.let { min(day, it) } ?: day

        // set the earliest and latest dates for the postbox registration
        earliestRegisteredPostbox = if (earliestRegisteredPostbox == null) {
            dateRegistered
        } else {
            if (earliestRegisteredPostbox < dateRegistered) earliestRegisteredPostbox else dateRegistered
        }
        lastRegisteredPostbox = if (lastRegisteredPostbox == null) {
            dateRegistered
        } else {
            if (lastRegisteredPostbox > dateRegistered) lastRegisteredPostbox else dateRegistered
        }

        // totals tallies
        if (postbox.inactive) {
            inactivePostboxCount++
        }
        if (!postbox.verified) {
            unverifiedPostboxCount++
        }

        // monarch counting
        monarchCount[postbox.monarch.ordinal] = monarchCount[postbox.monarch.ordinal]!! + 1
        // monarch name counting
        val monarchFirstName =
            if (postbox.monarch == Monarch.SCOTTISH_CROWN) "Unmarked" else postbox.monarch.displayName.split(
                " "
            )[0]
        monarchNameCount[monarchFirstName] = monarchNameCount[monarchFirstName]!! + 1

        // type counting
        if (postboxTypeCount.containsKey(postbox.type)) {
            postboxTypeCount[postbox.type] = postboxTypeCount[postbox.type]!! + 1
        } else {
            postboxTypeCount[postbox.type] = 1
        }

        // type category counting
        val postboxTypeCategory = parsePostboxType(postbox.type).first ?: "Unknown"
        if (postboxTypeCategoryCount.containsKey(postboxTypeCategory)) {
            postboxTypeCategoryCount[postboxTypeCategory] =
                postboxTypeCategoryCount[postboxTypeCategory]!! + 1
        } else {
            postboxTypeCategoryCount[postboxTypeCategory] = 1
        }

        // build age distribution
        val ageEstimate = postbox.getAgeEstimate()
        if (ageEstimate != null) {
            ageBounds.first.add(ageEstimate.first)
            ageBounds.second.add(ageEstimate.second ?: LocalDateTime.now().year)
        }
    }

    // make sure the rest of the days are inserted into the over time graph
    if (earliestRegisteredPostboxDay != null) {
        for (day in (earliestRegisteredPostboxDay..LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            .div(60 * 60 * 24))) {
            if (!registeredOverTime.containsKey(day)) {
                registeredOverTime[day] = 0
            } else {
                // calculate day with most registered
                if (mostDailyRegistrations == null || registeredOverTime[day]!! > mostDailyRegistrations.second) {
                    mostDailyRegistrations = Pair(day * 60 * 60 * 24, registeredOverTime[day]!!)
                }
            }
        }
    }
    // ensure our postbox types are sorted most to least common for easier reading
    postboxTypeCount =
        postboxTypeCount.entries
            .sortedWith(compareBy({ -it.value }, { it.key }))
            .associate { it.toPair() }.toMutableMap()

    // process age distribution and turn it into something useful
    val ageDistribution = mutableMapOf<Int, Int>()
    ageBounds.first.sort()
    ageBounds.second.sort()
    val lowerBounds = ageBounds.first.toMutableList()
    val upperBounds = ageBounds.second.toMutableList()
    var count = 0
    var averagePostboxAge = 0
    var total = 0
    for (year in lowerBounds[0]..upperBounds[upperBounds.size - 1]) {
        while (lowerBounds.isNotEmpty() && year >= lowerBounds[0]) {
            count++
            lowerBounds.removeAt(0)
        }
        while (upperBounds.isNotEmpty() && year >= upperBounds[0]) {
            count--
            upperBounds.removeAt(0)
        }
        ageDistribution[year] = count
        averagePostboxAge += year * count
        total += count
    }
    averagePostboxAge /= total

    val registeredOverTimeModel = remember { CartesianChartModelProducer() }
    val monarchsModel = remember { CartesianChartModelProducer() }
    val monarchNameModel = remember { CartesianChartModelProducer() }
    val typesModel = remember { CartesianChartModelProducer() }
    val typeCategoriesModel = remember { CartesianChartModelProducer() }
    val ageDistributionModel = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        registeredOverTimeModel.runTransaction {
            columnSeries {
                series(registeredOverTime.keys, registeredOverTime.values)
            }
        }
        monarchsModel.runTransaction {
            columnSeries {
                series(monarchCount.keys, monarchCount.values)
            }
        }
        monarchNameModel.runTransaction {
            columnSeries {
                series((0..<monarchNameCount.size).toList(), monarchNameCount.values)
            }
        }
        typesModel.runTransaction {
            columnSeries {
                series((0..<postboxTypeCount.size).toList(), postboxTypeCount.values)
            }
        }
        typeCategoriesModel.runTransaction {
            columnSeries {
                series(
                    (0..<postboxTypeCategoryCount.size).toList(),
                    postboxTypeCategoryCount.values
                )
            }
        }
        ageDistributionModel.runTransaction {
            columnSeries {
                series(ageDistribution.keys, ageDistribution.values)
            }
        }
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Row {
            Text("Registered Postboxes:", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Text(
                saveFile.getPostboxes().size.toString(),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Row {
            Text("Inactive:")
            Spacer(Modifier.weight(1f))
            Text(inactivePostboxCount.toString())
        }
        Row {
            Text("Unverified:")
            Spacer(Modifier.weight(1f))
            Text(unverifiedPostboxCount.toString())
        }
        Spacer(Modifier.size(16.dp))

        Text("Registrations:", style = MaterialTheme.typography.headlineSmall)
        Text(
            "First registration: ${
                earliestRegisteredPostbox?.let { humanReadableDate(it) } ?: "N/A"
            }"
        )
        Text(
            "Last registration: ${
                lastRegisteredPostbox?.let { humanReadableDate(it) } ?: "N/A"
            }"
        )
        if (mostDailyRegistrations != null) {
            Text(
                "Most daily registrations: ${mostDailyRegistrations.second} (${
                    LocalDateTime.ofEpochSecond(
                        mostDailyRegistrations.first,
                        0,
                        ZoneOffset.UTC
                    ).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                })"
            )
        }
        Text("Over time:")
        BarChart(
            modelProducer = registeredOverTimeModel,
            xFormatter = DateFormatter
        )

        Text("Monarchs:", style = MaterialTheme.typography.headlineSmall)
        BarChart(modelProducer = monarchsModel, xFormatter = MonarchFormatter)
        Text("By First Name:")
        BarChart(
            modelProducer = monarchNameModel,
            xFormatter = getPostboxTypeFormatter(monarchNameCount.keys.toList())
        )

        Text("Postbox Types:", style = MaterialTheme.typography.headlineSmall)
        Text("Categories:")
        BarChart(
            modelProducer = typeCategoriesModel,
            xFormatter = getPostboxTypeFormatter(postboxTypeCategoryCount.keys.toList())
        )
        Text("All types:")
        BarChart(
            modelProducer = typesModel,
            xFormatter = getPostboxTypeFormatter(postboxTypeCount.keys.toList())
        )

        Text("Age Distribution:", style = MaterialTheme.typography.headlineSmall)
        if (ageBounds.first.isNotEmpty() && ageBounds.second.isNotEmpty()) {
            Text(
                "Oldest Postbox: ${
                    humanReadablePostboxAgeEstimate(
                        Pair(
                            ageBounds.first.first(),
                            ageBounds.second.first()
                        )
                    )
                }"
            )
            Text(
                "Newest Postbox: ${
                    humanReadablePostboxAgeEstimate(
                        Pair(
                            ageBounds.first.last(),
                            ageBounds.second.last()
                        )
                    )
                }"
            )
        }
        Text("Average Age: ${LocalDateTime.now().year - averagePostboxAge} years")
        BarChart(
            modelProducer = ageDistributionModel,
            colSpacing = -1f
        )
    }
}

@Composable
fun BarChart(
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer,
    xFormatter: CartesianValueFormatter = CartesianValueFormatter.Default,
    colSpacing: Float = 16f
) {
    ProvideVicoTheme(
        rememberM3VicoTheme(
            columnCartesianLayerColors = MaterialTheme.colorScheme.run {
                listOf(
                    onBackground,
                    secondary,
                    tertiary
                )
            }
        )) {
        CartesianChartHost(
            rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnCollectionSpacing = colSpacing.dp
                ),
                startAxis = VerticalAxis.rememberStart(
                    // make sure we don't have decimal labels on the Y axis
                    itemPlacer = remember { VerticalAxis.ItemPlacer.step({ 1.0 }) },
                    label = rememberAxisLabelComponent()
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = xFormatter, labelRotationDegrees = -90f,
                    // use longest monarch name to set axis size
                    size = BaseAxis.Size.Text(Monarch.SCOTTISH_CROWN.displayName),
                    label = rememberAxisLabelComponent(
                        lineCount = 2
                    )
                )
            ),
            modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .requiredHeight(350.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = true),
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content)
        )
    }
}

private val DateFormatter = CartesianValueFormatter { context, value, verticalAxisPosition ->
    LocalDateTime
        .ofEpochSecond(value.toLong() * 60 * 60 * 24, 0, ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
}


private val MonarchFormatter = CartesianValueFormatter { context, value, verticalAxisPosition ->
    Monarch.entries[value.toInt()].displayName
        // replace the bracketed cypher helper to keep the label a reasonable size
        .replace(Regex(""" \(.*\)"""), "")
}

fun getPostboxTypeFormatter(typeList: List<String?>): CartesianValueFormatter {
    return CartesianValueFormatter { context, value, verticalAxisPosition ->
        humanReadablePostboxType(
            if (value.toInt() < typeList.size) typeList[value.toInt()]
                ?: "Unknown" else "Unknown"
        )
    }
}
