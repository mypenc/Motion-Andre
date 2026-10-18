package com.mypenc.motionandre.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypenc.motionandre.R
import com.mypenc.motionandre.model.Anim
import com.mypenc.motionandre.model.Blend
import com.mypenc.motionandre.model.Ease
import com.mypenc.motionandre.model.Fill
import com.mypenc.motionandre.model.Kind
import com.mypenc.motionandre.model.Layer
import com.mypenc.motionandre.model.Prop
import com.mypenc.motionandre.model.swatches
import com.mypenc.motionandre.render.Draw
import kotlin.math.roundToInt

@Composable
fun ToolPanel(vm:EditorVM){
    Box(Modifier.fillMaxWidth().height(215.dp).background(MaterialTheme.colorScheme.surface)){
        val l=vm.cur
        if(vm.mode==Mode.ADD)AddPanel(vm)
        else if(l==null)Hint(vm)
        else when(vm.mode){
            Mode.FILL->FillPanel(vm,l)
            Mode.BORDER->BorderPanel(vm,l)
            Mode.BLEND->BlendPanel(vm,l)
            Mode.XFORM->XformPanel(vm,l)
            Mode.SHAPE->ShapePanel(vm,l)
            else->Home(vm)
        }
    }
}


@Composable
fun Hint(vm:EditorVM){
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Text("Select a layer or add one",fontSize=13.sp)
        Spacer(Modifier.size(10.dp))
        Btn(R.drawable.ic_add,"Add Shape"){vm.mode=Mode.ADD}
    }
}

@Composable
fun Home(vm:EditorVM){
    Column(Modifier.fillMaxSize().padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Btn(R.drawable.ic_color_fill,"Color & Fill",Modifier.weight(1f).fillMaxHeight()){vm.mode=Mode.FILL}
            Btn(R.drawable.ic_border,"Border",Modifier.weight(1f).fillMaxHeight()){vm.mode=Mode.BORDER}
            Btn(R.drawable.ic_blend,"Blending & Opacity",Modifier.weight(1f).fillMaxHeight()){vm.mode=Mode.BLEND}
        }
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Btn(R.drawable.ic_transform,"Move & Transform",Modifier.weight(1f).fillMaxHeight()){vm.mode=Mode.XFORM}
            Btn(R.drawable.ic_edit_shape,"Edit Shape",Modifier.weight(1f).fillMaxHeight()){vm.mode=Mode.SHAPE}
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun Sub(vm:EditorVM,ps:List<Prop>,content:@Composable BoxScope.()->Unit){
    var m by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxSize()){
        Column(
            Modifier.width(56.dp).fillMaxHeight(),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.SpaceEvenly
        ){
            IcBtn(R.drawable.ic_back,"Back"){vm.mode=Mode.HOME}
            if(ps.isNotEmpty()){
                IcBtn(if(vm.hasKeys(ps))R.drawable.ic_keyframe_filled else R.drawable.ic_keyframe,"Key"){vm.toggleKeys(ps)}
                Box{
                    IcBtn(R.drawable.ic_easing,"Ease",vm.easeNow()!=null){m=true}
                    DropdownMenu(expanded=m,onDismissRequest={m=false}){
                        Ease.entries.forEach{e->
                            DropdownMenuItem(text={Text(e.name)},onClick={
                                m=false
                                vm.setEase(e)
                            })
                        }
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(),content=content)
    }
}

@Composable
fun XformPanel(vm:EditorVM,l:Layer){
    val ps=when(vm.xm){
        0->listOf(Prop.X,Prop.Y)
        1->listOf(Prop.ROT)
        2->listOf(Prop.SX,Prop.SY)
        else->listOf(Prop.SKEW)
    }
    Sub(vm,ps){
        Row(Modifier.fillMaxSize()){
            Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)){
                val t=vm.t
                val pw=vm.pr.w.toFloat()
                val ph=vm.pr.h.toFloat()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    when(vm.xm){
                        0->{
                            NumField("X",Anim.value(l,Prop.X,t)*pw,Modifier.weight(1f),done={vm.endLive()}){vm.setP(Prop.X,it/pw)}
                            NumField("Y",Anim.value(l,Prop.Y,t)*ph,Modifier.weight(1f),done={vm.endLive()}){vm.setP(Prop.Y,it/ph)}
                        }
                        1->NumField("Rotation",Anim.value(l,Prop.ROT,t),Modifier.weight(1f),done={vm.endLive()}){vm.setP(Prop.ROT,it)}
                        2->{
                            NumField("Scale X %",Anim.value(l,Prop.SX,t)*100f,Modifier.weight(1f),"%.0f",{vm.endLive()}){vm.setP(Prop.SX,it/100f)}
                            NumField("Scale Y %",Anim.value(l,Prop.SY,t)*100f,Modifier.weight(1f),"%.0f",{vm.endLive()}){vm.setP(Prop.SY,it/100f)}
                        }
                        else->NumField("Skew",Anim.value(l,Prop.SKEW,t),Modifier.weight(1f),done={vm.endLive()}){vm.setP(Prop.SKEW,it)}
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top=8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22222C))
                        .pointerInput(Unit){
                            detectDragGestures(
                                onDragEnd={vm.endLive()},
                                onDragCancel={vm.endLive()}
                            ){ch,d->
                                ch.consume()
                                vm.pad(d.x,d.y)
                            }
                        },
                    contentAlignment=Alignment.Center
                ){
                    Text(
                        when(vm.xm){
                            0->"Drag here to move layer"
                            1->"Drag here to rotate layer"
                            2->"Drag here to scale layer"
                            else->"Drag here to skew layer"
                        },
                        fontSize=12.sp
                    )
                }
            }
            Column(Modifier.width(56.dp).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){
                listOf(
                    R.drawable.ic_move to "Move",
                    R.drawable.ic_rotate to "Rotate",
                    R.drawable.ic_scale to "Scale",
                    R.drawable.ic_skew to "Skew"
                ).forEachIndexed{i,p->
                    IcBtn(p.first,p.second,true,Modifier.background(if(vm.xm==i)Color(0x33FF4D6D) else Color.Transparent)){vm.xm=i}
                }
            }
        }
    }
}

fun hex(c:Int):String="#"+String.format("%06X",c and 0xFFFFFF)

@Composable
fun HexField(c:Int,mod:Modifier,set:(Int)->Unit){
    var txt by remember{mutableStateOf(hex(c))}
    var foc by remember{mutableStateOf(false)}
    LaunchedEffect(c){
        if(!foc)txt=hex(c)
    }
    BasicTextField(
        value=txt,
        onValueChange={s->
            val v=s.removePrefix("#").take(6)
            txt="#"+v.uppercase()
            if(v.length==6)v.toLongOrNull(16)?.let{set((c and 0xFF000000.toInt()) or it.toInt())}
        },
        singleLine=true,
        textStyle=TextStyle(color=Color.White,fontSize=14.sp,textAlign=TextAlign.Center),
        cursorBrush=SolidColor(Color.White),
        modifier=mod
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2A2A36))
            .padding(10.dp)
            .onFocusChanged{
                foc=it.isFocused
                if(!it.isFocused){
                    txt=hex(c)
                    set(c)
                }
            }
    )
}

@Composable
fun Swatches(cur:Int,pick:(Int)->Unit){
    swatches.chunked(6).forEach{row->
        Row(Modifier.padding(vertical=3.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            row.forEach{s->
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(s))
                        .border(2.dp,if((cur and 0xFFFFFF)==(s and 0xFFFFFF))Color.White else Color.Transparent,RoundedCornerShape(8.dp))
                        .clickable{pick(s)}
                )
            }
        }
    }
}

@Composable
fun FillPanel(vm:EditorVM,l:Layer){
    var second by remember{mutableStateOf(false)}
    val sec=second&&l.fill==Fill.GRADIENT
    val c=if(sec)l.color2 else l.color
    Sub(vm,emptyList()){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Fill.entries.forEach{f->
                    val id=when(f){
                        Fill.NONE->R.drawable.ic_fill_none
                        Fill.SOLID->R.drawable.ic_fill_solid
                        Fill.GRADIENT->R.drawable.ic_fill_gradient
                    }
                    IcBtn(id,f.name.lowercase().replaceFirstChar{it.uppercase()},true,Modifier.background(if(l.fill==f)Color(0x33FF4D6D) else Color.Transparent)){vm.fill(f)}
                }
                if(l.fill==Fill.GRADIENT){
                    Spacer(Modifier.width(12.dp))
                    Btn(R.drawable.ic_fill_solid,"Color 1",on=!second){second=false}
                    Spacer(Modifier.width(6.dp))
                    Btn(R.drawable.ic_fill_solid,"Color 2",on=second){second=true}
                }
            }
            if(l.fill!=Fill.NONE){
                Row(Modifier.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
                    HexField(c,Modifier.width(110.dp)){vm.col(it,sec)}
                    Slider(
                        value=(c ushr 24)/255f,
                        onValueChange={a->vm.col((c and 0xFFFFFF) or ((a*255f).roundToInt() shl 24),sec)},
                        onValueChangeFinished={vm.endLive()},
                        modifier=Modifier.weight(1f).padding(start=10.dp)
                    )
                }
                Swatches(c){s->
                    vm.col((s and 0xFFFFFF) or (c and 0xFF000000.toInt()),sec)
                    vm.endLive()
                }
            }
        }
    }
}

@Composable
fun BorderPanel(vm:EditorVM,l:Layer){
    Sub(vm,emptyList()){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)){
            Sl("Border width "+l.strokeW.roundToInt(),l.strokeW,0f,60f,{vm.endLive()}){vm.stroke(it,null)}
            Text("Border color",fontSize=12.sp)
            Swatches(l.strokeC){s->
                vm.stroke(null,s)
                vm.endLive()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendPanel(vm:EditorVM,l:Layer){
    Sub(vm,listOf(Prop.ALPHA)){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)){
            val a=Anim.value(l,Prop.ALPHA,vm.t)
            Sl("Opacity "+(a*100f).roundToInt()+"%",a,0f,1f,{vm.endLive()}){vm.setP(Prop.ALPHA,it)}
            Text("Blending",fontSize=12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState())){
                Blend.entries.forEach{b->
                    FilterChip(
                        selected=l.blend==b,
                        onClick={vm.blend(b)},
                        label={Text(b.label)},
                        modifier=Modifier.padding(end=6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShapePanel(vm:EditorVM,l:Layer){
    val big=maxOf(vm.pr.w,vm.pr.h)*2f
    Sub(vm,emptyList()){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)){
            if(l.kind==Kind.TEXT){
                OutlinedTextField(
                    value=l.text,
                    onValueChange={vm.text(it)},
                    modifier=Modifier.fillMaxWidth().onFocusChanged{if(!it.isFocused)vm.endLive()},
                    singleLine=true,
                    label={Text("Text")}
                )
            }
            Sl("Width "+l.w.roundToInt(),l.w,10f,big,{vm.endLive()}){vm.size(it,l.h)}
            Sl("Height "+l.h.roundToInt(),l.h,10f,big,{vm.endLive()}){vm.size(l.w,it)}
            if(l.kind==Kind.RECT){
                Sl("Corner radius "+l.rad.roundToInt(),l.rad,0f,minOf(l.w,l.h)/2,{vm.endLive()}){vm.rad(it)}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Btn(R.drawable.ic_flip_h,"Flip H",Modifier.weight(1f)){vm.flip(true)}
                Btn(R.drawable.ic_flip_v,"Flip V",Modifier.weight(1f)){vm.flip(false)}
            }
        }
    }
}

@Composable
fun Thumb(k:Kind){
    Canvas(Modifier.size(52.dp)){
        drawIntoCanvas{cv->
            val n=cv.nativeCanvas
            val p=Paint(Paint.ANTI_ALIAS_FLAG)
            p.color=0xFFBDBDBD.toInt()
            n.save()
            n.translate(size.width/2,size.height/2)
            n.drawPath(Draw.path(k,size.width*0.9f,size.height*0.9f,size.width*0.12f),p)
            n.restore()
        }
    }
}

@Composable
fun AddPanel(vm:EditorVM){
    val kinds=listOf(Kind.RECT,Kind.ELLIPSE,Kind.TRIANGLE,Kind.STAR,Kind.HEXAGON,Kind.PLUS,Kind.TEXT)
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=4.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("Shapes",Modifier.weight(1f),fontWeight=FontWeight.Bold)
            IcBtn(R.drawable.ic_close,"Close"){vm.mode=Mode.HOME}
        }
        kinds.chunked(4).forEach{row->
            Row(Modifier.padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                row.forEach{k->
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF22222C))
                            .clickable{vm.add(k)},
                        contentAlignment=Alignment.Center
                    ){
                        if(k==Kind.TEXT)Text("Aa",fontSize=22.sp,fontWeight=FontWeight.Bold) else Thumb(k)
                    }
                }
            }
        }
    }
}
