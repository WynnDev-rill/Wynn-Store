package id.wynn.furina

object ActivityCatalog {
 val sustained=setOf(PetMood.READ,PetMood.CAKE,PetMood.SIT,PetMood.SWING)
 private val words=mapOf(
  PetMood.READ to listOf("read","book","読書","阅读","看书"),
  PetMood.CAKE to listOf("eat","cake","食べ","吃蛋糕","吃饭"),
  PetMood.SIT to listOf("sit","seated","座る","坐下"),
  PetMood.SWING to listOf("swing","ブランコ","秋千"))
 fun <T> motion(motions: List<Pair<String,T>>,mood: PetMood,selected: String=""): Pair<String,T>? =
  motions.firstOrNull {selected.isNotEmpty() && it.first==selected} ?: motions.firstOrNull { item ->
   words[mood]?.any { word ->
    if(word.all {it.code<128}) Regex("(^|[^a-z])${Regex.escape(word)}([^a-z]|$)",RegexOption.IGNORE_CASE).containsMatchIn(item.first)
    else item.first.contains(word,true)
   }==true
  }
}
