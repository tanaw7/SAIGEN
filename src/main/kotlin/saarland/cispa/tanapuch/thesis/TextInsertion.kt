package saarland.cispa.tanapuch.thesis

import org.droidmate.deviceInterface.guimodel.ActionQueue
import org.droidmate.deviceInterface.guimodel.ExplorationAction
import org.droidmate.exploration.actions.setText
import org.droidmate.exploration.statemodel.Widget
import org.droidmate.exploration.strategy.widget.ExplorationStrategy

// Strategy -> Do something
open class TextInsertion: ExplorationStrategy(){

    private val saigen: SemanticAwareInputGenerator
            get() = eContext.getOrCreateWatcher()

    override fun chooseAction(): ExplorationAction {
        val valuesToFill: List<Pair<Widget, String>> = saigen.getValues(currentState.actionableWidgets)

        //return list of enter text [widget.setText("AAAA")]
        return ActionQueue(valuesToFill.map { it.first.setText(it.second) }, 0)
    }
    protected open fun getAvailableWidgets(): List<Widget> {
        return currentState.actionableWidgets
    }
}