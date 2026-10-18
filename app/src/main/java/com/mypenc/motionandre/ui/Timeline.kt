package com.mypenc.motionandre.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypenc.motionandre.R
import kotlin.math.abs

@Composable
fun Timeline(vm:EditorVM){
    val d=LocalDensity.current
    val rowH=40.dp
    val rulH=30.dp
    val rowPx=with(d){rowH.toPx()}
    val rulPx=with(d){rulH.toPx()}
    val edge=with(d){24.dp.toPx()}
    val rows=vm.pr.layers.asReversed()
    var cw by remember{mutableIntStateOf(0)}
    var md by remember{mutableIntStateOf(0)}
    var tid by remember{mutableLongStateOf(0L)}
    Row(Modifier.fillMaxWidth().height(150.dp).verticalScroll(rememberScrollState())){
        Column(Modifier.width(112.dp)){
            Spacer(Modifier.height(rulH))
            rows.forEach{l->
                Row(
                    Modifier
                        .height(rowH)
                        .fillMaxWidth()
                        .background(if(l.id==vm.sel)MaterialTheme.colorScheme.primary.copy(alpha=0.25f) else Color.Transparent),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    IcBtn(if(l.hidden)R.drawable.ic_eye_off else R.drawable.ic_eye,"Show",true,Modifier.size(36.dp)){vm.hide(l.id)}
                    Text(
                        l.name,
                        Modifier.weight(1f).clickable{vm.sel=l.id}.padding(end=4.dp),
                        fontSize=12.sp,
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(Modifier.weight(1f).height(rulH+rowH*rows.size).onSizeChanged{cw=it.width}){
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit){
                        detectTapGestures{o->
                            vm.seek((o.x/size.width*vm.pr.dur).toLong())
                            if(o.y>=rulPx){
                                vm.pr.layers.asReversed().getOrNull(((o.y-rulPx)/rowPx).toInt())?.let{vm.sel=it.id}
                            }
                        }
                    }
                    .pointerInput(Unit){
                        detectHorizontalDragGestures(
                            onDragStart={o->
                                md=0
                                val l=if(o.y>=rulPx)vm.pr.layers.asReversed().getOrNull(((o.y-rulPx)/rowPx).toInt()) else null
                                if(l!=null){
                                    val x0=l.inT.toFloat()/vm.pr.dur*size.width
                                    val x1=l.outT.toFloat()/vm.pr.dur*size.width
                                    if(abs(o.x-x0)<edge){
                                        md=1
                                        tid=l.id
                                        vm.sel=l.id
                                    }else if(abs(o.x-x1)<edge){
                                        md=2
                                        tid=l.id
                                        vm.sel=l.id
                                    }
                                }
                            },
                            onDragEnd={vm.endLive()},
                            onDragCancel={vm.endLive()}
                        ){ch,_->
                            ch.consume()
                            val ms=(ch.position.x/size.width*vm.pr.dur).toLong()
                            if(md==0)vm.seek(ms) else vm.trimEdge(tid,md,ms)
                        }
                    }
            ){
                val dur=vm.pr.dur.toFloat()
                val w=size.width
                drawRect(Color(0xFF22222C),Offset.Zero,Size(w,rulPx))
                val step=if(dur<=6000f)500L else if(dur<=15000f)1000L else 2000L
                var m=0L
                while(m<=dur){
                    val x=m/dur*w
                    drawLine(Color.Gray,Offset(x,rulPx*0.6f),Offset(x,rulPx),1f)
                    m+=step
                }
                rows.forEachIndexed{i,l->
                    val y=rulPx+i*rowPx
                    val sel=l.id==vm.sel
                    if(sel)drawRect(Color(0x33FF4D6D),Offset(0f,y),Size(w,rowPx))
                    val x0=l.inT/dur*w
                    val x1=l.outT/dur*w
                    drawRoundRect(
                        Color(l.color).copy(alpha=if(l.hidden)0.2f else 0.6f),
                        Offset(x0,y+rowPx*0.2f),
                        Size(x1-x0,rowPx*0.6f),
                        CornerRadius(8f,8f)
                    )
                    if(sel){
                        drawRoundRect(Color.White,Offset(x0,y+rowPx*0.2f),Size(8f,rowPx*0.6f),CornerRadius(4f,4f))
                        drawRoundRect(Color.White,Offset(x1-8f,y+rowPx*0.2f),Size(8f,rowPx*0.6f),CornerRadius(4f,4f))
                    }
                    val r=rowPx*0.14f
                    val cy=y+rowPx/2
                    l.keys.values.flatten().map{it.t}.distinct().forEach{kt->
                        val x=kt/dur*w
                        rotate(45f,Offset(x,cy)){
                            drawRect(Color.White,Offset(x-r,cy-r),Size(2*r,2*r))
                        }
                    }
                    drawLine(Color(0x22FFFFFF),Offset(0f,y+rowPx),Offset(w,y+rowPx),1f)
                }
                val px=vm.t/dur*w
                drawLine(Color.White,Offset(px,0f),Offset(px,size.height),3f)
            }
            Text(
                fmt(vm.t),
                fontSize=11.sp,
                modifier=Modifier
                    .offset{IntOffset((vm.t.toFloat()/vm.pr.dur*cw-50f).toInt().coerceIn(0,(cw-100).coerceAtLeast(0)),0)}
                    .background(Color(0xFF2A2A36),RoundedCornerShape(6.dp))
                    .padding(horizontal=6.dp,vertical=2.dp)
            )
        }
    }
}
