package saarland.cispa.tanapuch.thesis

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


fun main(args: Array<String>) {

    val apkName = mutableListOf<String>()
    apkName.add("com.eleybourn.bookcatalogue")
    //apkName.add("com.everycar.android")
    apkName.add("com.kayak.android")
    //apkName.add("com.lonelyplanet.guides")
    apkName.add("com.mirkoo.apps.mislibros")
    apkName.add("com.addressbook")
    apkName.add("com.rakesh.addressbook")

    var i = 0
    var j = 0
    var k = 0
    val eTCountList = mutableListOf<Triple<Double,Int, Int>>() // Triple<Percentage, #EditText, #AllWidgets>
    val saiGen = mutableListOf<String>("disabled", "enabled")
    var totalEditTexts = 0.0
    var totalAllWidgets = 0.0

    //val parent = "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false"
    //val parent = "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true"
    //val parent = "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false additional address books"
    //val parent = "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true additional address books"
    //val parent = "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true rerun_3_apps"
    val parent = "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true remix"

    val outFileName = System.getProperty("user.dir") + parent + "\\averageEditTextDistribution.txt"

    File(outFileName).printWriter().use { out ->

        while (k < 2) { //saiGen[k] disabled and enabled
            while (j < apkName.size) {//5) { //for the number of apkName, could be apkName.size
                while (i < 5) { //for number of seeds (different seed runs)
                    val path = System.getProperty("user.dir") + parent + "\\saiGen_${saiGen[k]}_runWithSeed$i\\model\\${apkName[j]}\\states"
                    eTCountList.add(InputFieldCount(path))
                    i++
                }

                var averageETCount = 0.0
                var averageEditTextSize = 0.0
                var averageAllWidgetSize = 0.0
                eTCountList.forEach {
                    averageETCount += it.first
                    averageEditTextSize += it.second
                    averageAllWidgetSize += it.third
                    //totalEditTexts += it.second
                    //totalAllWidgets += it.third
                }
                averageETCount = averageETCount / eTCountList.size
                averageEditTextSize = averageEditTextSize / eTCountList.size
                averageAllWidgetSize = averageAllWidgetSize / eTCountList.size

                totalEditTexts += averageEditTextSize
                totalAllWidgets += averageAllWidgetSize

                println("SAIGEN: ${saiGen[k]}, Average percentage of EditText Widgets for ${apkName[j]}: $averageETCount ; allEditTextSize: $averageEditTextSize ;allWidgetSize: ${averageAllWidgetSize}")
                out.println("SAIGEN: ${saiGen[k]}, Average percentage of EditText Widgets for ${apkName[j]}: $averageETCount ; allEditTextSize: $averageEditTextSize ;allWidgetSize: ${averageAllWidgetSize}")

                eTCountList.clear()
                i = 0
                j++
            }
            j = 0
            k++
        }
        println("")
        println("totalEditTexts : ${totalEditTexts/2}")
        println("totalAllWidgets: ${totalAllWidgets/2}")
        println("Ratio          : ${totalEditTexts/totalAllWidgets.toFloat()} ")
        out.println("")
        out.println("totalEditTexts : ${totalEditTexts/2}")
        out.println("totalAllWidgets: ${totalAllWidgets/2}")
        out.println("Ratio          : ${totalEditTexts/totalAllWidgets.toFloat()} ")
    }
}

fun InputFieldCount(path: String): Triple<Double, Int, Int>{
    val res: MutableMap<String, String> = mutableMapOf()
    Files.list(Paths.get(path))
            .forEach { file ->
                if (file.toString().endsWith("csv")) {
                    val fileData = Files.lines(file)
                    fileData.forEach { line ->
                        if (line.contains(";")) {
                            val uid = line.split(";")[0]
                            val className = line.split(";")[2]

                            res.put(uid, className)
                        }
                    }
                }
            }

    //res.forEach { uid, c ->
    //    println("$uid\t$c")
    //}

    val percentageText = res.values.count { it.contains(".EditText") }
    //println("Percentage of text edits ${percentageText.toDouble() / res.size}")
    println("result size (all widgets): ${res.size}")
    return Triple (percentageText.toDouble() / res.size, percentageText ,res.size)
}