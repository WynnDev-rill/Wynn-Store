package id.wynn.iwatchyou

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/** Accessed on the service worker or IO dispatcher; one atomic row per calendar day. */
class HistoryStore(context: Context) : SQLiteOpenHelper(context,"history.db",null,1) {
 init { setWriteAheadLoggingEnabled(true) }
 override fun onCreate(db: SQLiteDatabase) {
  db.execSQL("CREATE TABLE days(day TEXT PRIMARY KEY, screen INTEGER NOT NULL, apps TEXT NOT NULL, hours TEXT NOT NULL)")
 }
 override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
 fun read(day: String): TimeLedger.Snapshot? = readableDatabase.rawQuery("SELECT screen,apps,hours FROM days WHERE day=?",arrayOf(day)).use { c ->
  if (!c.moveToFirst()) null else {
   val apps=JSONObject(c.getString(1)); val h=JSONArray(c.getString(2))
   TimeLedger.Snapshot(day,c.getLong(0),apps.keys().asSequence().associateWith { apps.optLong(it).coerceAtLeast(0) },null,List(24){h.optLong(it).coerceAtLeast(0)})
  }
 }
 fun days(): List<String> = readableDatabase.rawQuery("SELECT day FROM days ORDER BY day DESC",null).use { c -> buildList { while(c.moveToNext()) add(c.getString(0)) } }
 fun write(s: TimeLedger.Snapshot) {
  val values=android.content.ContentValues().apply {
   put("day",s.day); put("screen",s.screenMs)
   put("apps",JSONObject(s.apps).toString()); put("hours",JSONArray(s.hours).toString())
  }
  writableDatabase.insertWithOnConflict("days",null,values,SQLiteDatabase.CONFLICT_REPLACE)
 }
 fun migrateLegacy(context: Context) {
  val p=context.getSharedPreferences("times",Context.MODE_PRIVATE)
  val date=p.getString("day",null) ?: return
  if (read(date)==null) {
   val apps=runCatching { JSONObject(p.getString("apps","{}") ?: "{}") }.getOrDefault(JSONObject())
   write(TimeLedger.Snapshot(date,p.getLong("screen",0).coerceAtLeast(0),apps.keys().asSequence().associateWith { apps.optLong(it).coerceAtLeast(0) },null))
  }
  p.edit().clear().apply()
 }
}
