package saarland.cispa.tanapuch.thesis

import java.io.File
import java.nio.file.Files


class AverageFormattingForPlot{

}

private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true rerun_3_apps"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true remix"

fun main(args: Array<String>) {

    val apkName1 = "com.eleybourn.bookcatalogue"
    val apkName2 = "com.everycar.android"
    val apkName3 = "com.kayak.android"
    val apkName4 = "com.lonelyplanet.guides"
    val apkName5 = "com.mirkoo.apps.mislibros"
    val apkName6 = "com.addressbook"
    val apkName7 = "com.rakesh.addressbook"

    avgFormattingForPlot(apkName1)
    //avgFormattingForPlot(apkName2)
    avgFormattingForPlot(apkName3)
    //avgFormattingForPlot(apkName4)
    avgFormattingForPlot(apkName5)

    avgFormattingForPlot(apkName6)
    avgFormattingForPlot(apkName7)

}


fun avgFormattingForPlot(apkName: String, interval: Int = 180){

    val saiGen_disabled = mutableListOf<Float>()
    val saiGen_enabled = mutableListOf<Float>()

    File(coverageFolderPath).walk().forEach { file ->
        if(file.name.contains("${apkName}_avgCovOverTime.txt")){
            val covOverTime = Files.readAllLines(file.toPath())
            if(file.name.contains("disabled")){
                covOverTime.forEach{
                    saiGen_disabled.add(it.toFloat())
                }
            }
            if(file.name.contains("enabled")){
                covOverTime.forEach{
                    saiGen_enabled.add(it.toFloat())
                }
            }
        }
    }

    if (saiGen_disabled.size == saiGen_enabled.size){
        println("Coverage Over Time of ${apkName} wtih interval: ${interval}")
        println("INTERVAL SAIGEN-DISABLED SAIGEN-ENABLED")

        val size = saiGen_disabled.size
        var i = 0
        while (i < size){
            println("${(i+1)*interval} ${saiGen_disabled[i]} ${saiGen_enabled[i]}")
            i++
        }
        println("")
    }

    val outFileName = coverageFolderPath + "\\graphFormatted_${apkName}_avgCovOverTime.txt"
    File(outFileName).printWriter().use { out ->
        if (saiGen_disabled.size == saiGen_enabled.size){
            val size = saiGen_disabled.size
            var i = 0
            while (i < size){
                out.println("${(i+1)*interval} ${saiGen_disabled[i]} ${saiGen_enabled[i]}")
                i++
            }
        }
    }
}