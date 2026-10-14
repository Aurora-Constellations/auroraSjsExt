import com.axiom.Configurations.Layout
import org.aurora.sjsast.LHMap

object AuroraStates {

  var globalLayoutOptions: LHMap[String,String] = LHMap(
            "elk.algorithm" -> "layered",
            "elk.direction" -> "TOP",
            "elk.spacing.nodeNode" -> "10",
            "elk.layered.spacing.nodeNodeBetweenLayers" -> "30"
          )

  // Hide Narratives / Show Narratives and Hide Named Groups. Unlike the layout, these hold only while the same
  // patient is shown: drawing another patient resets them, so hidden content never carries over unnoticed.
  var hideNarratives: Boolean = false
  var hiddenNamedGroups: Set[String] = Set.empty

  // The patient the hide filters above belong to
  private var filteredPatient: Option[String] = None

  def resetFiltersOnPatientChange(patient: String): Unit =
    if (!filteredPatient.contains(patient)) {
      filteredPatient = Some(patient)
      hideNarratives = false
      hiddenNamedGroups = Set.empty
    }
}
