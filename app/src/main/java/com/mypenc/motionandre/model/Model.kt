package com.mypenc.motionandre.model

enum class Prop(val label:String,val def:Float,val min:Float,val max:Float){
    X("Position X",0.5f,-0.5f,1.5f),
    Y("Position Y",0.5f,-0.5f,1.5f),
    SX("Scale X",1f,0f,4f),
    SY("Scale Y",1f,0f,4f),
    ROT("Rotation",0f,-360f,360f),
    ALPHA("Opacity",1f,0f,1f)
}

enum class Kind{RECT,ELLIPSE,TEXT}

enum class Ease{LINEAR,IN,OUT,INOUT,BOUNCE,HOLD}

data class Key(val t:Long,val v:Float,val ease:Ease=Ease.LINEAR)

data class Layer(
    val id:Long,
    val name:String,
    val kind:Kind,
    val color:Int,
    val w:Float,
    val h:Float,
    val text:String="",
    val inT:Long=0L,
    val outT:Long=5000L,
    val base:Map<Prop,Float> = emptyMap(),
    val keys:Map<Prop,List<Key>> = emptyMap()
)

data class Project(
    val w:Int=1280,
    val h:Int=720,
    val fps:Int=30,
    val dur:Long=5000L,
    val bg:Int=0xFF1B1B22.toInt(),
    val layers:List<Layer> = emptyList(),
    val nextId:Long=1L
)

val pal=listOf(
    0xFFFF4D6D.toInt(),
    0xFFFFB84D.toInt(),
    0xFF4DD0E1.toInt(),
    0xFF7C4DFF.toInt(),
    0xFF69F0AE.toInt(),
    0xFFFFFFFF.toInt()
)
