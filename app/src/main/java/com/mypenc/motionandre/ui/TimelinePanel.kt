package com.mypenc.motionandre.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypenc.motionandre.R
import com.mypenc.motionandre.model.Kind

@Composable
fun ColumnScope.TimelinePanel(vm:EditorVM){
    val pr=vm.pr
    val has=vm.sel!=null
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){
        Tool(R.drawable.ic_rect,"Rectangle"){vm.add(Kind.RECT)}
        Tool(R.drawable.ic_ellipse,"Ellipse"){vm.add(Kind.ELLIPSE)}
        Tool(R.drawable.ic_text,"Text"){vm.add(Kind.TEXT)}
        Tool(R.drawable.ic_duplicate,"Duplicate",has){vm.dup()}
        Tool(R.drawable.ic_delete,"Delete",has){vm.del()}
        Tool(R.drawable.ic_layer_up,"Raise",has){vm.move(1)}
        Tool(R.drawable.ic_layer_down,"Lower",has){vm.move(-1)}
    }
    val d=LocalDensity.current
    val rowH=40.dp
    val rulH=24.dp
    val rowPx=with(d){rowH.toPx()}
    val rulPx=with(d){rulH.toPx()}
    val rows=pr.layers.asReversed()
    Row(Modifier.weight(1f).verticalScroll(rememberScrollState())){
        Column(Modifier.width(96.dp)){
            Spacer(Modifier.height(rulH))
            rows.forEach{l->
                Box(
                    Modifier
                        .height(rowH)
                        .fillMaxWidth()
                        .background(if(l.id==vm.sel)MaterialTheme.colorScheme.primary.copy(alpha=0.25f) else Color.Transparent)
                        .clickable{vm.sel=l.id}
                        .padding(horizontal=6.dp),
                    contentAlignment=Alignment.CenterStart
                ){
                    Text(l.name,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
        }
        Canvas(
            Modifier
                .weight(1f)
                .height(rulH+rowH*rows.size)
                .pointerInput(Unit){
                    detectTapGestures{o->
                        vm.seek((o.x/size.width*vm.pr.dur).toLong())
                        if(o.y>=rulPx){
                            vm.pr.layers.asReversed().getOrNull(((o.y-rulPx)/rowPx).toInt())?.let{vm.sel=it.id}
                        }
                    }
                }
                .pointerInput(Unit){
                    detectHorizontalDragGestures{ch,_->
                        ch.consume()
                        vm.seek((ch.position.x/size.width*vm.pr.dur).toLong())
                    }
                }
        ){
            val dur=vm.pr.dur.toFloat()
            val w=size.width
            drawRect(Color(0xFF22222C),Offset.Zero,Size(w,rulPx))
            val step=if(dur<=6000f)500L else 1000L
            var m=0L
            while(m<=dur){
                val x=m/dur*w
                drawLine(Color.Gray,Offset(x,rulPx*0.5f),Offset(x,rulPx),1f)
                m+=step
            }
            rows.forEachIndexed{i,l->
                val y=rulPx+i*rowPx
                if(l.id==vm.sel)drawRect(Color(0x33FF4D6D),Offset(0f,y),Size(w,rowPx))
                drawRoundRect(
                    Color(l.color).copy(alpha=0.55f),
                    Offset(l.inT/dur*w,y+rowPx*0.2f),
                    Size((l.outT-l.inT)/dur*w,rowPx*0.6f),
                    CornerRadius(8f,8f)
                )
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
    }
}
