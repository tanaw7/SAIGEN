package saarland.cispa.tanapuch.thesis

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.newCoroutineContext
import org.droidmate.exploration.statemodel.StateData
import org.droidmate.exploration.statemodel.Widget
import org.droidmate.exploration.statemodel.features.ModelFeature
import org.droidmate.exploration.ExplorationContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.math.abs
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import formsolver.CommonData
import formsolver.FormSolver
import kotlinx.coroutines.experimental.Deferred
import org.droidmate.deviceInterface.guimodel.WidgetData
import org.droidmate.exploration.statemodel.ActionData
import org.droidmate.exploration.statemodel.ModelConfig
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import javax.imageio.ImageIO

class CpTestModelFeature : ModelFeature() {
    override val context: CoroutineContext =
            newCoroutineContext(context = CoroutineName("testModelFeature"), parent = job)

    init {
        // we don't want to wait for other features (or having them wait for us), therefore create our own (child) job
        job = Job(parent = (this.job))
    }


    private var masterMap = mutableMapOf<Widget, Widget>()
    private var masterMapWithID = mutableMapOf<String, MutableMap<Widget, Widget>>()

    private val nulWidget = Widget() // a null widget template for dataWidget that doesn't have a match.
    private var dataWidgetCount: Float = 0.toFloat()
    private var matchedPairCount: Float = 0.toFloat()

    private fun mapWidgets(state: StateData): MutableMap<Widget, Widget>{
        // Paper 1 implementation

        // Clone the list to prevent concurrent access error
        val uiWidgets = state.widgets.toList()
        val wMap: Map<Widget, Widget> = emptyMap()
        val widgetsMap: MutableMap<Widget, Widget> = wMap.toMutableMap()

        uiWidgets.forEach{
            if (it.className.toLowerCase().contains("edittext") && !uidExistsInMasterMap(it.uid, masterMap) && it.visible) { // the data widget must have a text field && its uid must not already exist in masterMap
                //dataWidgetCount++
                //println("***************************************************************dataWidgetCount: $dataWidgetCount *****************")
                // This part addresses Android peculiarity
                //getNouns() here is to check that the text present contains noun to be used as label, otherwise the label is better obtained via another label widget.
                if (/*it.contentDesc != "" ||*/ it.text != "" && getNouns(it.text).isNotEmpty()){ //has to be an attribute of it describing the widget
                    widgetsMap[it] = it

                }

                // This part uses guessDescription as intended by Mariani's paper mapping a label widget to a textfield.
                else {

                    //widgetsWithText filters out noisy widgets that doesn't have either text or **content description**
                    val widgetsWithText =
                            state.widgets.filter { it.text.trim().isNotEmpty() || it.contentDesc.trim().isNotEmpty() }
                    var bestWidget = guessDescription(it, uiWidgets, widgetsWithText,
                            false, true, false, false)
                    if (bestWidget != null){
                        widgetsMap[it] = bestWidget
                    }
                    else {
                        bestWidget = this.nulWidget
                        widgetsMap[it] = bestWidget
                    }
                }
            }
        }

        //This part checks the Master Map if there exists already a key widget with a uid then do nothing,
        //else it adds the widget to the master map. The purpose is to add the widget as a key as it first occurs,
        //when the text value is still the correct hint and not after it has been changed to something else.
        if(widgetsMap.isNotEmpty() && masterMap.isNotEmpty()) {

            /*val newWidgets = widgetsMap.filter { !masterMap.containsKey(it.key) }

            if(newWidgets.isNotEmpty()) {
                masterMap.putAll(newWidgets)
                val stateIDString = "${state.stateId.first}_${state.stateId.second}"
                masterMapWithID[stateIDString] = newWidgets.toMutableMap()
            }
            */

            masterMap.putAll(widgetsMap)
            val stateIDString = "${state.stateId.first}_${state.stateId.second}"
            masterMapWithID[stateIDString] = widgetsMap

        }

        else if(widgetsMap.isNotEmpty()){
            masterMap = widgetsMap
            val stateIDString = "${state.stateId.first}_${state.stateId.second}"
            masterMapWithID[stateIDString] = widgetsMap

        }

        return widgetsMap
    }

    private fun uidExistsInMasterMap(uid: UUID, masterMap: MutableMap<Widget, Widget>): Boolean{
        if (masterMap.isEmpty()){
            return false
        }
        else {
            masterMap.forEach{
                if (uid == it.key.uid){
                    return true
                }
            }
        }
        return false
    }

    private fun guessDescription(dataWidget: Widget, uiWidgets: List<Widget>, widgetWithText: List<Widget>, optNoiseIncremental: Boolean,
                                 optSearchLocal: Boolean, optVisibleOnly: Boolean, optHierarchical: Boolean): Widget?{
        // opts are options for fine-tuning the algorithm

        var min: Int = Int.MAX_VALUE
        var bestWidget : Widget? = null // the best label widget for the data widget description

        if (!optNoiseIncremental) { // if optNoiseIncremental is false, we only remove noise at the beginning.
            //uiWidgets = removeNoisyWidgets(uiWidgets) // this function is to be implemented
        }

        widgetWithText.forEach point@{
            if (!isCandidate(dataWidget, uiWidgets, it,optNoiseIncremental, optSearchLocal, optVisibleOnly,
                            optHierarchical)){
                return@point //continue the loop without considering the code in the loop below.
            }
            //select the closest descriptive widget
            val dist: Int = computeDistance(it, dataWidget).toInt()

            if (dist < min){
                min = dist
                bestWidget = it
            }
        }

        /* If no descriptor is found, this will add the description of the container as a candidate
        to describe the data widget. */
        if (min == Int.MAX_VALUE) {
            if (optSearchLocal && optHierarchical){
                return guessDescription(dataWidget, uiWidgets, widgetWithText, optNoiseIncremental,
                        optSearchLocal, optVisibleOnly, optHierarchical)
            }
            else {
                return null // returns null when bestWidget is not found
            }
        }
        else {
            return bestWidget
        }
    }

    private fun isCandidate(dataWidget: Widget, uiWidgets: List<Widget>, currentWidget: Widget,
                            optNoiseIncremental: Boolean, optSearchLocal: Boolean, optVisibleOnly: Boolean,
                            optHierarchical: Boolean): Boolean {

        if (!currentWidget.className.toLowerCase().contains("view")){
            return false
        }

        //if ((dataWidget.bounds.x < currentWidget.bounds.x) || (dataWidget.bounds.y < currentWidget.bounds.y)){

        if(currentWidget.className.toLowerCase().contains("edittext")){
            return false
        }

        if ((computePosition(dataWidget)[0] < currentWidget.bounds.x) ||
                computePosition(dataWidget)[1] < currentWidget.bounds.y) {
            return false // Proximity Principle: skips widgets that are outside the interesting area of dataWidget
        }
        if (optNoiseIncremental && isNoisy(currentWidget)) {
            return false // incrementally ignore noisy widgets
        }
        //This checks if dataWidget and currentWidget belong to the same container (Having the same parentID)
        // can use dataWidget.parentID == currentWidget.parentID ?? Positively sure.
        if (optSearchLocal){
            if (dataWidget.parentId != currentWidget.parentId) {
                return false // ignore descriptive widgets in other containers
            }
        }
        if (!optSearchLocal && optVisibleOnly && !currentWidget.visible) {
            return false // skips widgets that are not visible
        }
        //To make sure that the label candidate contains at least one descriptive Noun.
        if(getNouns(currentWidget.text).isEmpty()){
            return false
        }
        return true
    }

    private fun containerWidget(dataWidget: Widget, allWidgets: List<Widget>): Widget {
        // to be implemented, needs to add the container description of the data widget to be considered
        // needs to create a function that extract the container of a given widget

        val parent = allWidgets.first { it.id == dataWidget.parentId }

        return parent
    }

    //A function to determine whether a widget is noisy, that is not to be considered as candidate for description.
    private fun isNoisy(widget: Widget): Boolean{
        //to be implemented
        return false
    }

    private fun removeNoisyWidgets(windowsWidgets: MutableList<Widget>){
        //to be implemented
    }

    // function for computing the distance between two widgets' representative points
    // If the candidate widget is lower than the data widget, apply top right as rep point. (x+width, y)
    // Else apply buttom left as rep point. (x, y+height)
    private fun computeDistance(candidateWidget: Widget, dataWidget: Widget): Double{
        val dataWidgetRepPoint: MutableList<Double> =
                mutableListOf(dataWidget.bounds.x.toDouble(), dataWidget.bounds.y.toDouble()) // topleft coord [x, y]

        val candidateWidgetPos = computePosition(candidateWidget)
        //val dataWidgetPos = computePosition(dataWidget)
        var candidateWidgetRepPoint: MutableList<Double> = mutableListOf(0.toDouble(), 0.toDouble())

        if(candidateWidget.bounds.y >= dataWidget.bounds.y) {
            val candidateX = (candidateWidget.bounds.x + candidateWidget.bounds.width).toDouble() //top right
            val candidateY = candidateWidget.bounds.y.toDouble() //top right
            candidateWidgetRepPoint = mutableListOf(candidateX, candidateY)
        }
        else {
            val candidateX = candidateWidget.bounds.x.toDouble() // buttom left
            val candidateY = (candidateWidget.bounds.y + candidateWidget.bounds.height).toDouble() //buttom left
            candidateWidgetRepPoint = mutableListOf(candidateX, candidateY)
        }

        val dist: Double = abs( //distance calculation sqRoot(pow2(x1-x2) + pow2(y1-y2))
                Math.sqrt(
                        (Math.pow(candidateWidgetRepPoint[0]-dataWidgetRepPoint[0], 2.toDouble())) +
                                (Math.pow(candidateWidgetRepPoint[1]-dataWidgetRepPoint[1], 2.toDouble()))
                )
        )
        return dist
    }

    // A helper function for the local computeDistance function. It computes the position of a widget(Center of Widget).
    private fun computePosition(widget: Widget): MutableList<Double>{
        val wX: Double = widget.bounds.x.toDouble()
        val wY: Double = widget.bounds.y.toDouble()
        val wHeight: Double = widget.bounds.height.toDouble()
        val wWidth: Double = widget.bounds.width.toDouble()
        val pos = mutableListOf( (wX + (wWidth/2)), (wY + (wHeight/2)))
        return pos //returns a list of x as first member and y as the second member [x,y]
    }


    private fun getQuery(widget: Widget): String{
        // Call LINK
        return ""
    }

    override suspend fun onNewAction(traceId: UUID, deferredAction: Deferred<ActionData>, prevState: StateData, newState: StateData) {
        //val lastAction = deferredAction.await()
        //val newState =newState
        val stateID = newState.stateId                  // State ID

        //test query
        //var testNouns = getNouns("relative city name").joinToString(separator = " ")
        //val listOfQueries = getQueryFromStrings(mutableListOf("artist", "genre", "title"))
        //println(listOfQueries)

        val myMap = mapWidgets(newState)  //This statement is the key to call in the mapping

        // This part transform the labels' text from myMap into queries using Link.
        if (myMap.isNotEmpty()) {
            val matchedOnlyMap = myMap.filterValues { it.text.toLowerCase() != "empty" }
            var listOfString = mutableListOf<String>()
            matchedOnlyMap.forEach {
                val nounsLabel = getNouns(it.value.text).joinToString(separator = " ")
                listOfString.add(nounsLabel)
            }

            //val listOfQueries = getQueryFromStrings(listOfString)
            println("")
        }

        println("----MASTER MAP -------\n")
        masterMap.forEach {
            println("${it.key} -------- ${it.value}")
            println("${it.key.parentId} -------- ${it.value.parentId}")
        }

        println("----MASTER MAP with StateID -------\n")
        masterMapWithID.forEach {
            println("${it.key} -------- ${it.value}")
        }

    }

/*
    override suspend fun onNewInteracted(targetWidget: Widget?, prevState: StateData, newState: StateData) {
        val myMap = mapWidgets(newState)
        //println("---MY MAP -------, $myMap ")
        //println("----MASTER MAP ------- $masterMap")
        /*
        myMap.forEach { data ->
            val labelWidget = data.key
            val targetWidget = data.value
            if (!myHash.containsKey(targetWidget.uid)) {
                val query = getQuery(labelWidget)

                myHash[targetWidget.uid] = query
            }
        }
        */
   }
*/

    private fun getValueFromQuery(query: String): String{
        // query DBPedia
        return ""
    }

    private val myHash = ConcurrentHashMap<UUID, String>() // counts how often any state was explored

    fun getValue(widget: Widget): String{
        return if (myHash.containsKey(widget.uid))
            getValueFromQuery(myHash[widget.uid]!!)
        else
            ""
    }

    fun canFillSomething(state: StateData): Boolean{
        // check if can fill some empty field
        return false
    }


    //Tagger function from Standford NLP group
    //It takes a string and returns only the nouns to be used as the label for the descriptive widget
    private  fun getNouns(label: String): MutableList<String> {
        //Path to the ta][gger
        val tagger: MaxentTagger = MaxentTagger("models/english-left3words-distsim.tagger")
        val tagged: String = tagger.tagString(label)
        //Putting each separated tagged word into a list
        var separatel = tagged.split(" ")
        // Filtering and keeping the members that are nouns only
        val nounsList: MutableList<String> = separatel.filter {
            it.contains("NN") || it.contains("NP") }.toMutableList()
        //println("NOUNS ONLY ------- $nounsList ------------------")

        //Remove the _NN* tags part to keep the raw words only. To be further parsed into Link
        val nounsListCut: MutableList<String> = emptyList<String>().toMutableList()
        nounsList.forEach{
            nounsListCut.add(it.substringBefore("_").toLowerCase())
        }
        //println("NOUNS CUT ONLY------ $nounsListCut ---------------")

        return nounsListCut
        //reference for PenTreeBankConstituents e.g. NN-Noun singular , NNS-Noun Plural, NNP-Proper Noun singular,
        //NNPS-Proper Noun plural, NP-Noun Phrase
        //http://www.surdeanu.info/mihai/teaching/ista555-fall13/readings/PennTreebankConstituents.html#NNP
    }

    private fun drawRectangleOnWidgets(packageName: String,masterMapID: MutableMap<String, MutableMap<Widget, Widget>>){
        println("-------Starts drawing rectangles on the mapped label and data widgets-----")
        masterMapID.forEach{ data ->
            val stateID = data.key
            val masterMap = data.value
            var pathName: String = ""
            try {
                pathName = System.getProperty("user.dir") + "\\out\\droidMate\\model\\$packageName\\states\\"
                File("$pathName\\drawn").mkdirs()
            }
            catch (IOException: Exception) {

            }

            masterMap.forEach{
                try {
                    val filePath = "$pathName$stateID.png"
                    val img: BufferedImage = ImageIO.read(File(filePath))
                    val g2d = img.createGraphics()
                    val labelBounds = it.value.bounds
                    val dataBounds = it.key.bounds

                    g2d.color = Color.RED
                    g2d.stroke = BasicStroke(10f)
                    g2d.drawRect(labelBounds.x, labelBounds.y, labelBounds.width, labelBounds.height)

                    g2d.color = Color.BLUE
                    g2d.stroke = BasicStroke(4f)
                    g2d.drawRect(dataBounds.x, dataBounds.y, dataBounds.width, dataBounds.height)

                    //draw another using parentID for layout

                    val writeName = "$pathName\\drawn\\${stateID}_${it.key.uid}_${it.value.uid}.png"

                    if(!File(writeName).exists()) { //checks if the image file has already been written
                        ImageIO.write(img, "PNG", File(writeName))
                        println("image written to $pathName\\drawn\\${stateID}_${it.key.uid}_${it.value.uid}.png")
                    }
                    g2d.dispose()
                }
                catch (IOException: Exception){

                }
            }
        }
    }

    //for getQueryFromStrings function, should take in a list of string from labels (those matched with the dataWidgets)
    private fun getQueryFromStrings(ListOfString: MutableList<String>): MutableList<String>{
        var internalArgs = ListOfString
        val sogliaAssociazioni = 50
        val usoSoglia = false
        val euristica = 2
        val prop = Properties()
        var input: FileInputStream? = null

        FormSolver.endpoint = "http://dbpedia.org/sparql/"

        if (internalArgs.isEmpty()) {
            println("Missing inputs")
            return mutableListOf()
        }
        var etichette = internalArgs.toTypedArray()
        while (etichette.isNotEmpty()) {
            val risolutore = FormSolver(etichette, usoSoglia, sogliaAssociazioni, euristica)
            val etichetteTagliate = risolutore.runProcess(1)
            etichette = etichetteTagliate.toTypedArray()
        }
        var i = 0
        while (i < CommonData.outputQueries.size) {
            val query2 = CommonData.outputQueries[i]
            println("*********************************")
            println("*********************************")
            println("*********************************")
            println(query2)
            ++i
        }
        return CommonData.outputQueries
    }

    override suspend fun dump(context: ExplorationContext) {
        super.dump(context)
        println("WHATEVER ENDS HERE FOR MODEL FEATURE?")

        val packageName = context.apk.packageName
        println("----This is the apk package name: $packageName")

        drawRectangleOnWidgets(packageName, masterMapWithID)
        //masterMapWithID.forEach {
        //	File(System.getProperty("user.dir") + "\\out\\droidMate\\model\\$packageName\\states\\masterMap.txt").bufferedWriter().use { out -> out.write(it.key) }
        //}


        File(System.getProperty("user.dir") + "\\out\\droidMate\\model\\$packageName\\states\\masterMap.txt").printWriter().use { out ->
            masterMap.forEach {
                out.println("Data: ${it.key}")
                out.println("LABL: ${it.value}")
            }
        }

        File(System.getProperty("user.dir") + "\\out\\droidMate\\model\\$packageName\\states\\masterMap3.txt").printWriter().use { out ->


            masterMapWithID.forEach {
                out.println("StateID: ${it.key}")
                it.value.forEach{
                    out.println("__ Data: ${it.key} === Parent: ${it.key.parentId}")
                    out.println("__ LABL: ${it.value} === Parent: ${it.value.parentId}")
                    dataWidgetCount++
                    if(it.value.text.toLowerCase() != "empty"){
                        matchedPairCount++
                    }
                }
                out.println("\n")
            }
            out.println("\n")
            out.println("Data Widget Counts(DC): ${dataWidgetCount}")
            out.println("Matched Pair Counts(MC): ${matchedPairCount}")
            out.println("Matching Ratio (MC/DC): ${matchedPairCount/dataWidgetCount}")
            out.println("\n")
            out.println("tp=")
            out.println("tn=")
            out.println("fp=")
            out.println("fn=")
            out.println("\n")
            out.println("Precision (Correctly matched labels [tp/tp+fp]): (Manaully filled)")
            out.println("Recall (#of labels which were not matched [tp/tp+fn]): (Manually filled)")
        }

    }

}