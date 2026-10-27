package id.wynn.furina

import android.graphics.*

/** Small app-owned furniture, drawn once into textures; original character art stays untouched. */
object StageFurniture {
 fun chair(): Bitmap {
  val b=Bitmap.createBitmap(256,320,Bitmap.Config.ARGB_8888);val c=Canvas(b)
  val p=Paint(Paint.ANTI_ALIAS_FLAG)
  fun line(x: Float,y: Float,xx: Float,yy: Float,w: Float,color: Int) {p.shader=null;p.color=color;p.strokeWidth=w;p.strokeCap=Paint.Cap.ROUND;c.drawLine(x,y,xx,yy,p)}
  val gold=Color.rgb(202,171,101);val dark=Color.rgb(26,44,83)
  // Curved gilded frame and velvet inset, highlights remain readable at overlay size.
  p.color=gold;c.drawRoundRect(40f,13f,216f,191f,40f,40f,p)
  p.shader=LinearGradient(55f,30f,205f,175f,intArrayOf(Color.rgb(76,116,174),dark),null,Shader.TileMode.CLAMP)
  c.drawRoundRect(48f,22f,208f,180f,32f,32f,p);p.shader=null
  p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.color=Color.rgb(139,168,208)
  c.drawRoundRect(57f,32f,199f,172f,26f,26f,p);p.style=Paint.Style.FILL
  p.color=gold
  for(x in listOf(93f,163f)) for(y in listOf(67f,118f)) {c.drawCircle(x,y,3f,p)}
  for(side in listOf(-1,1)) {
   val x=128f+side*81f
   line(x,178f,x+side*12f,296f,13f,dark)
   line(x+side*12f,278f,x+side*13f,301f,5f,gold)
   line(x,175f,x+side*17f,135f,9f,gold)
   line(x+side*17f,135f,x-side*20f,135f,12f,dark)
   line(x+side*17f,131f,x-side*20f,131f,3f,gold)
  }
  p.shader=LinearGradient(0f,157f,0f,199f,Color.rgb(98,142,201),dark,Shader.TileMode.CLAMP)
  c.drawRoundRect(28f,157f,228f,195f,13f,13f,p);p.shader=null
  line(38f,192f,218f,192f,4f,gold)
  p.color=gold;c.drawCircle(128f,14f,9f,p);p.color=Color.rgb(182,222,255);c.drawCircle(128f,14f,4f,p)
  return b
 }
 fun swing(): Bitmap {
  val b=Bitmap.createBitmap(256,512,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG)
  val gold=Color.rgb(204,181,126)
  p.style=Paint.Style.STROKE;p.strokeWidth=5f;p.color=gold;c.drawCircle(128f,20f,11f,p)
  val path=Path().apply {moveTo(128f,31f);lineTo(45f,100f);lineTo(45f,454f);moveTo(128f,31f);lineTo(211f,100f);lineTo(211f,454f)}
  p.strokeWidth=7f;p.strokeCap=Paint.Cap.ROUND;c.drawPath(path,p)
  p.color=Color.rgb(255,237,189);p.strokeWidth=2f;c.drawPath(path,p)
  // Water-drop ornaments and a blue padded bench, attached to the very same pivot.
  p.style=Paint.Style.FILL
  for(x in listOf(45f,211f)) {
   p.color=Color.rgb(146,206,242)
   val drop=Path().apply {moveTo(x,93f);cubicTo(x-16f,112f,x-10f,126f,x,126f);cubicTo(x+10f,126f,x+16f,112f,x,93f)}
   c.drawPath(drop,p);p.color=Color.rgb(226,246,255);c.drawCircle(x-3f,115f,3f,p)
  }
  p.shader=LinearGradient(0f,438f,0f,474f,Color.rgb(113,170,218),Color.rgb(28,52,98),Shader.TileMode.CLAMP)
  c.drawRoundRect(24f,438f,232f,474f,12f,12f,p);p.shader=null
  p.color=gold;c.drawRoundRect(20f,471f,236f,479f,4f,4f,p)
  p.color=Color.rgb(232,222,179);c.drawRoundRect(35f,440f,221f,443f,2f,2f,p)
  return b
 }
}
