package de.tu_darmstadt.polarhrserver

import polar.com.sdk.api.model.PolarAccelerometerData
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData

fun PolarEcgData.toUdpString() = "ECG:$timeStamp;${samples.joinToString(separator = ",")}"

fun PolarAccelerometerData.toUdpString() =
    "ACC:$timeStamp;${samples.map { "(${it.x}/${it.y}/${it.z})" }.joinToString(separator = ",")}"

fun PolarHrData.toUdpString() = "HR:$hr;${rrsMs.joinToString(separator = ",")}"