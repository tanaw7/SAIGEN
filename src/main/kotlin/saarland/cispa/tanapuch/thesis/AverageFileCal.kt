package saarland.cispa.tanapuch.thesis

import java.io.File
import java.nio.file.Files

class AverageFileCal{
}

//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true rerun_3_apps"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true remix"
private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\out"

fun main(args: Array<String>) {

/*
    val apkName1 = "com.eleybourn.bookcatalogue"
    //val apkName2 = "com.everycar.android"
    val apkName3 = "com.kayak.android"
    //val apkName4 = "com.lonelyplanet.guides"
    val apkName5 = "com.mirkoo.apps.mislibros"

    var saiGen = "disabled"
    avgCal(saiGen, apkName1)
    //avgCal(saiGen, apkName2)
    avgCal(saiGen, apkName3)
    //avgCal(saiGen, apkName4)
    avgCal(saiGen, apkName5)
    saiGen = "enabled"
    avgCal(saiGen, apkName1)
    //avgCal(saiGen, apkName2)
    avgCal(saiGen, apkName3)
    //avgCal(saiGen, apkName4)
    avgCal(saiGen, apkName5)
*/

    val apkName6 = "com.addressbook"
    val apkName7 = "com.rakesh.addressbook"
    var saiGen = "disabled"
    avgCal(saiGen, apkName6)
    avgCal(saiGen, apkName7)
    saiGen = "enabled"
    avgCal(saiGen, apkName6)
    avgCal(saiGen, apkName7)

}

fun avgCal(saiGen: String, apkName: String){

    var itPF = ""
    var itPF2 = ""
    var itPF3 = ""
    var itPF4 = ""
    var itPF4Path = ""

    var covList = mutableListOf<MutableList<Float>>()


    File(coverageFolderPath).walk().forEach {
        if (it.path.contains("coverageOverTime.txt")
                && it.parentFile.parentFile.parentFile.name.contains(saiGen)  //saiGen disabled or enabled
                //&& it.path.contains(saiGen)
                && it.path.contains(apkName)) {
            val itPath = it.path
            val itParent = it.parent

            itPF = it.parentFile.name
            itPF2 = it.parentFile.parentFile.name
            itPF3 = it.parentFile.parentFile.parentFile.name
            itPF4 = it.parentFile.parentFile.parentFile.parentFile.name

            itPF4Path = it.parentFile.parentFile.parentFile.parent

            val covOverTime = Files.readAllLines(it.toPath())
            covOverTime.removeAt(0)

            val covOnly = mutableListOf<Float>()

            covOverTime.forEach{
                covOnly.add(it.substringAfter(";").toFloat())
            }

            covList.add(covOnly)
        }
    }

    val avgList = mutableListOf<Float>()
    var i = 0

    while (i < 16) {

        var holderList = mutableListOf<Float>()
        covList.forEach {
            holderList.add(it[i])
        }

        avgList.add((holderList[0]+holderList[1]+holderList[2]+holderList[3]+holderList[4])/5.toFloat())
        i++
    }

    val outFileName = itPF4Path + "\\saiGen_${saiGen}_${itPF}_avgCovOverTime.txt"
    File(outFileName).printWriter().use { out ->
        avgList.forEach{
            out.println(it)
        }
    }

    println("")
}