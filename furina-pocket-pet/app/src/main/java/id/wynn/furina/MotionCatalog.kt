package id.wynn.furina

/** Prefer the model's own animation. Never apply another model's incompatible parameter curves. */
object MotionCatalog {
 fun <T> walk(motions: List<Pair<String,T>>, selected: String): Pair<String,T>? =
  motions.firstOrNull { selected.isNotEmpty() && it.first==selected } ?:
  motions.firstOrNull { entry -> listOf("walk","run","歩","走路","行走","奔跑").any { entry.first.contains(it,true) } }
}
