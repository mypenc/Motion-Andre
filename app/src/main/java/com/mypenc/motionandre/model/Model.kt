package com.mypenc.motionandre.model

enum class Prop(val label:String,val def:Float,val min:Float,val max:Float){
    X("Position X",0.5f,-0.5f,1.5f),
    Y("Position Y",0.5f,-0.5f,1.5f),
    SX("Scale X",1f,0f,4f),
    SY("Scale Y",1f,0f,4f),
    ROT("Rotation",0f,-360f,360f),
    SKEW("Skew",0f,-60f,60f),
    ALPHA("Opacity",1f,0f,1f)
}

enum class Kind{RECT,ELLIPSE,TEXT,TRIANGLE,STAR,HEXAGON,PLUS}

enum class Fill{NONE,SOLID,GRADIENT}

enum class Blend(val label:String){
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    ADD("Add")
}

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
    val keys:Map<Prop,List<Key>> = emptyMap(),
    val color2:Int=0xFF4DD0E1.toInt(),
    val fill:Fill=Fill.SOLID,
    val strokeW:Float=0f,
    val strokeC:Int=-1,
    val blend:Blend=Blend.NORMAL,
    val rad:Float=16f,
    val flipH:Boolean=false,
    val flipV:Boolean=false,
    val hidden:Boolean=false
)

data class Project(
    val w:Int=1280,
    val h:Int=720,
    val fps:Int=30,
    val dur:Long=5000L,
    val bg:Int=0xFFD9D9D9.toInt(),
    val layers:List<Layer> = emptyList(),
    val nextId:Long=1L,
    val name:String="New Project"
)

val pal=listOf(
    0xFFFF4D6D.toInt(),
    0xFFFFB84D.toInt(),
    0xFF4DD0E1.toInt(),
    0xFF7C4DFF.toInt(),
    0xFF69F0AE.toInt(),
    0xFFFFFFFF.toInt()
)

val swatches=listOf(
    0xFFFF4444.toInt(),
    0xFFF4B342.toInt(),
    0xFFF9E85F.toInt(),
    0xFF22D622.toInt(),
    0xFF1DE0E0.toInt(),
    0xFF3D4DF5.toInt(),
    0xFFD94DDB.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFBBBBBB.toInt(),
    0xFF888888.toInt(),
    0xFF444444.toInt(),
    0xFF000000.toInt()
)
