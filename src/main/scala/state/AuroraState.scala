import com.axiom.Configurations.Layout
import org.aurora.sjsast.LHMap

object AuroraStates {

  var globalLayoutOptions: LHMap[String,String] = LHMap(
            "elk.algorithm" -> "layered",
            "elk.direction" -> "TOP",
            "elk.spacing.nodeNode" -> "10",
            "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
          )

  // Hide Narratives / Show Narratives; like the layout, it holds across saves and patient switches
  var hideNarratives: Boolean = false
}
