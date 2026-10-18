package com.mypenc.motionandre.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypenc.motionandre.R
import com.mypenc.motionandre.render.Draw
import java.util.Locale

fun fmt(ms:Long):String=String.format(Locale.US,"%d:%02d.%02d",ms/60000,ms/1000%60,ms%1000/10)

@Composable
fun Editor(vm:EditorVM){
    val snack=remember{SnackbarHostState()}
    LaunchedEffect(vm.msg){
        vm.msg?.let{
            snack.showSnackbar(it)
            vm.msg=null
        }
    }
    LaunchedEffect(vm.playing){
        if(vm.playing){
            var last=androidx.compose.runtime.withFrameNanos{it}
            while(vm.playing){
                val n=androidx.compose.runtime.withFrameNanos{it}
                vm.tick(n-last)
                last=n
            }
        }
    }
    Scaffold(
        snackbarHost={SnackbarHost(snack)},
        topBar={Bar(vm)},
        containerColor=MaterialTheme.colorScheme.background
    ){pad->
        Column(Modifier.padding(pad).fillMaxSize()){
            Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
                Preview(vm)
            }
            Transport(vm)
            Panel(vm)
        }
    }
}

@Composable
fun Bar(vm:EditorVM){
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)){
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Motion Andre",Modifier.weight(1f),fontWeight=FontWeight.Bold,fontSize=18.sp)
            Tool(R.drawable.ic_undo,"Undo",vm.canUndo){vm.undo()}
            Tool(R.drawable.ic_redo,"Redo",vm.canRedo){vm.redo()}
            Tool(R.drawable.ic_export,"Export",vm.busy<0f){vm.export()}
        }
        if(vm.busy>=0f)LinearProgressIndicator(progress=vm.busy,modifier=Modifier.fillMaxWidth())
    }
}

@Composable
fun Preview(vm:EditorVM){
    var sz by remember{mutableStateOf(IntSize.Zero)}
    var hold by remember{mutableStateOf(false)}
    Canvas(
        Modifier
            .aspectRatio(vm.pr.w.toFloat()/vm.pr.h)
            .onSizeChanged{sz=it}
            .pointerInput(Unit){
                detectTapGestures{o->
                    vm.pick(o.x*vm.pr.w/sz.width,o.y*vm.pr.h/sz.height)
                }
            }
            .pointerInput(Unit){
                detectDragGestures(
                    onDragStart={o->hold=vm.grab(o.x*vm.pr.w/sz.width,o.y*vm.pr.h/sz.height)},
                    onDragEnd={vm.endLive()},
                    onDragCancel={vm.endLive()}
                ){ch,d->
                    ch.consume()
                    if(hold)vm.nudge(d.x/sz.width,d.y/sz.height)
                }
            }
    ){
        drawIntoCanvas{cv->
            val n=cv.nativeCanvas
            val pr=vm.pr
            val s=size.width/pr.w
            n.save()
            n.scale(s,s)
            Draw.frame(n,pr,vm.t)
            vm.cur?.let{Draw.outline(n,pr,it,vm.t,2f/s)}
            n.restore()
        }
    }
}

@Composable
fun Transport(vm:EditorVM){
    Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
        Tool(R.drawable.ic_prev_frame,"Prev"){vm.step(-1)}
        Tool(if(vm.playing)R.drawable.ic_pause else R.drawable.ic_play,if(vm.playing)"Pause" else "Play"){vm.toggle()}
        Tool(R.drawable.ic_next_frame,"Next"){vm.step(1)}
        Text(fmt(vm.t)+" / "+fmt(vm.pr.dur),Modifier.weight(1f).padding(start=8.dp),fontSize=13.sp)
    }
}

@Composable
fun Panel(vm:EditorVM){
    var tab by remember{mutableIntStateOf(0)}
    Column(Modifier.fillMaxWidth().height(310.dp).background(MaterialTheme.colorScheme.surface)){
        TabRow(selectedTabIndex=tab){
            listOf("Timeline","Properties").forEachIndexed{i,s->
                Tab(selected=tab==i,onClick={tab=i},text={Text(s)})
            }
        }
        if(tab==0)TimelinePanel(vm) else PropsPanel(vm)
    }
}
