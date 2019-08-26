package saarland.cispa.tanapuch.thesis

import org.droidmate.configuration.ConfigProperties
import org.droidmate.exploration.statemodel.features.StatementCoverageMF
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.streams.toList

class CoverageOverTime {
}

private val log: Logger by lazy { LoggerFactory.getLogger(StatementCoverageMF::class.java) }
private val instrumentationDir = Paths.get(System.getProperty("user.dir") + "\\instrumentation-logs\\")

/** coverageFolderPath is the root folder for which its subtrees contain all the coverage files ended with "logcat__000"
 *  Change it to the root folder of your choice to calculate all coverage files within its subtrees.
 *  The result coverage would be printed to coverageResult.txt in the same folder of its coverage file.
 *  "user.dir" is just the current directory of this project.
 * */
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\100actions tests"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\out"
private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased false additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true additional address books"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests biased true remix"

fun main(args: Array<String>){

    println("Hello, Coverage World!")

    var numOfCoverageFile = 0
    File(coverageFolderPath).walk().forEach {
        if (it.path.contains("logcat__000")){
            numOfCoverageFile++
        }
    }

    println("Number of coverage files: $numOfCoverageFile")
    println("")

    numOfCoverageFile = 0
    File(coverageFolderPath).walk().forEach {
        if (it.path.contains("logcat__000")) {

            val coverageFile = Files.readAllLines(it.toPath())
            var timeAndStatements = mutableListOf<Pair<Int, String>>()

            coverageFile.removeAt(0)
            coverageFile.removeAt(0)
            /*
            coverageFile.forEach{
                coverageFileUidOnly.add(it.substringAfterLast("uuid="))
            }
            coverageFileUidOnly = coverageFileUidOnly.toList().distinct().toMutableList()
            */

            if (numOfCoverageFile == 5){
                println("")
            }

            coverageFile.forEach{
                if(it != "read: unexpected EOF!" && it != "--------- beginning of main" && it != "--------- beginning of system" && it != "--------- beginning of crash") {

                    val string = it.substring(0, 14) // example: "09-30 09:31:13"
                    val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.ENGLISH)
                    val date1 = format.parse(string)

                    val hour = date1.hours
                    val hSecond = hour * 3600
                    val minute = date1.minutes
                    val mSecond = minute * 60
                    val second = date1.seconds

                    val timeFlat = hSecond + mSecond + second

                    timeAndStatements.add(Pair(timeFlat, it.substringAfter("reach 1: ")))
                }
            }

            timeAndStatements = timeAndStatements.distinctBy { it.second }.toMutableList()

            val startTime = timeAndStatements[0].first //timeFlat of the first entry pair.
            val interval = 180
            val checkPoint1 = startTime + interval
            val checkPoint2 = checkPoint1 + interval
            val checkPoint3 = checkPoint2 + interval
            val checkPoint4 = checkPoint3 + interval
            val checkPoint5 = checkPoint4 + interval
            val checkPoint6 = checkPoint5 + interval
            val checkPoint7 = checkPoint6 + interval
            val checkPoint8 = checkPoint7 + interval
            val checkPoint9 = checkPoint8 + interval
            val checkPoint10 = checkPoint9 + interval
            val checkPoint11 = checkPoint10 + interval
            val checkPoint12 = checkPoint11 + interval
            val checkPoint13 = checkPoint12 + interval
            val checkPoint14 = checkPoint13 + interval
            val checkPoint15 = checkPoint14 + interval
            val checkPoint16 = checkPoint15 + interval

            //val baseTime = Pair(coverageFileUidOnly[0].first, coverageFileUidOnly[0].second)

            val fullPath = it.path
            val apkName = fullPath.substringAfterLast("\\").substringBeforeLast("-logcat__000")
            //val instrumentedJson = getInstrumentation(apkName).keys.toList().distinct()
            val instrumentedJson = getInstrumentation(apkName).values.toList().distinct()

            var coverageCounter = 0

            var c1 = 0
            var c2 = 0
            var c3 = 0
            var c4 = 0
            var c5 = 0
            var c6 = 0
            var c7 = 0
            var c8 = 0
            var c9 = 0
            var c10 = 0
            var c11 = 0
            var c12 = 0
            var c13 = 0
            var c14 = 0
            var c15 = 0
            var c16 = 0

            var intervalIndex = 0
            var intervalCounter = startTime + interval //initialize startTime + 90

            timeAndStatements.forEach{
                if (it.second in instrumentedJson){
                    coverageCounter++

                    if (it.first <= checkPoint1) c1++
                    if ((checkPoint1 < it.first) && (it.first <= checkPoint2)) c2++
                    else if ((checkPoint2 < it.first) && (it.first <= checkPoint3)) c3++
                    else if ((checkPoint3 < it.first) && (it.first <= checkPoint4)) c4++
                    else if ((checkPoint4 < it.first) && (it.first <= checkPoint5)) c5++
                    else if ((checkPoint5 < it.first) && (it.first <= checkPoint6)) c6++

                    else if ((checkPoint6 < it.first) && (it.first <= checkPoint7)) c7++
                    else if ((checkPoint7 < it.first) && (it.first <= checkPoint8)) c8++
                    else if ((checkPoint8 < it.first) && (it.first <= checkPoint9)) c9++
                    else if ((checkPoint9 < it.first) && (it.first <= checkPoint10)) c10++
                    else if ((checkPoint10 < it.first) && (it.first <= checkPoint11)) c11++
                    else if ((checkPoint11 < it.first) && (it.first <= checkPoint12)) c12++

                    else if ((checkPoint12 < it.first) && (it.first <= checkPoint13)) c13++
                    else if ((checkPoint13 < it.first) && (it.first <= checkPoint14)) c14++
                    else if ((checkPoint14 < it.first) && (it.first <= checkPoint15)) c15++
                    else if ((checkPoint15 < it.first) && (it.first <= checkPoint16)) c16++

                }
            }

            var coverageList = mutableListOf<Pair<Int, Int>>() //Pair(CHECKPOINT, COVERAGE)

            coverageList.add(Pair(checkPoint1, c1))
            coverageList.add(Pair(checkPoint2, c2 + coverageList.last().second))
            coverageList.add(Pair(checkPoint3, c3 + coverageList.last().second))
            coverageList.add(Pair(checkPoint4, c4 + coverageList.last().second))
            coverageList.add(Pair(checkPoint5, c5 + coverageList.last().second))
            coverageList.add(Pair(checkPoint6, c6 + coverageList.last().second))
            coverageList.add(Pair(checkPoint7, c7 + coverageList.last().second))
            coverageList.add(Pair(checkPoint8, c8 + coverageList.last().second))
            coverageList.add(Pair(checkPoint9, c9 + coverageList.last().second))
            coverageList.add(Pair(checkPoint10, c10 + coverageList.last().second))
            coverageList.add(Pair(checkPoint11, c11 + coverageList.last().second))
            coverageList.add(Pair(checkPoint12, c12 + coverageList.last().second))
            coverageList.add(Pair(checkPoint13, c13 + coverageList.last().second))
            coverageList.add(Pair(checkPoint14, c14 + coverageList.last().second))
            coverageList.add(Pair(checkPoint15, c15 + coverageList.last().second))
            coverageList.add(Pair(checkPoint16, c16 + coverageList.last().second))

            println("#: ${numOfCoverageFile+1}")
            println("fileName: $apkName")
            println("FileLocation: $it")
            println("FileParent  : ${it.parent}")
            println("")
            println("Coverage counter       : $coverageCounter")
            println("Instrumented state size: ${instrumentedJson.size}")
            println("Ratio                  : ${coverageCounter.toFloat()/instrumentedJson.size.toFloat()}")
            println("")

            println("Coverage Over Time: ")
            println("Time CheckPoint; Coverage")
            coverageList.forEach{
                println("${it.first - startTime};${it.second.toFloat()/instrumentedJson.size.toFloat()}")
            }
            println("----------------------------------------------------------------------------")
            println("")

            File(it.parent + "\\coverageResult.txt").printWriter().use { out ->
                out.println("#: ${numOfCoverageFile+1}")
                out.println("fileName: $apkName")
                out.println("FileLocation: $it")
                out.println("FileParent  : ${it.parent}")
                out.println("")
                out.println("Coverage counter       : $coverageCounter")
                out.println("Instrumented state size: ${instrumentedJson.size}")
                out.println("Ratio                  : ${coverageCounter.toFloat()/instrumentedJson.size.toFloat()}")
                out.println("----------------------------------------------------------------------------")
                out.println("End")
            }

            File( it.parent + "\\coverageOverTime.txt").printWriter().use { out ->
                out.println("Time CheckPoint; Coverage")
                coverageList.forEach{
                    out.println("${it.first - startTime};${it.second.toFloat()/instrumentedJson.size.toFloat()}")
                }
            }

            numOfCoverageFile++
        }
    }

    println("Number of processed coverage files: $numOfCoverageFile")

    println("")


}


private fun getInstrumentation(apkName: String): Map<String, String> {
    return if (!Files.exists(instrumentationDir)) {
        log.warn("Provided statementCoverageDir does not exist: ${ConfigProperties.ModelProperties.Features.statementCoverageDir}. DroidMate will monitor coverage will not be able to calculate coverage.")
        emptyMap()
    }
    else {
        val instrumentationFile = getInstrumentationFile(apkName)

        if (instrumentationFile != null)
            readInstrumentationFile(instrumentationFile)
        else
            emptyMap()
    }
}

private fun getInstrumentationFile(apkName: String): Path? {
    return Files.list(instrumentationDir)
            .toList()
            .firstOrNull{ it.fileName.toString().contains(apkName)
                    && it.fileName.toString().endsWith(".apk.json")}
}

@Throws(IOException::class)
private fun readInstrumentationFile(instrumentationFile: Path): Map<String, String> {
    val jsonData = String(Files.readAllBytes(instrumentationFile))
    val jObj = JSONObject(jsonData)

    val jArr = JSONArray(jObj.getJSONArray("allMethods").toString())

    val l = "9946a686-9ef6-494f-b893-ac8b78efb667".length
    val statements : MutableMap<String, String> = mutableMapOf()
    (0 until jArr.length()).forEach { idx ->
        val method = jArr[idx]

        if (!method.toString().contains("CoverageHelper")) {
            val parts = method.toString().split("uuid=".toRegex(), 2).toTypedArray()
            val uuid = parts.last()

            assert(uuid.length == l) { "Invalid UUID $uuid $method" }

            statements[uuid] = method.toString()
        }
    }

    return statements
}