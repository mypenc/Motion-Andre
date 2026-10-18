package com.mypenc.motionandre.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypenc.motionandre.R
import com.mypenc.motionandre.render.Draw
import java.util.Locale

fun fmt(ms:Long):String=String.format(Locale.US,"%02d:%02d:%02d",ms/60000,ms/1000%60,ms%1000/10)

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
            var last=withFrameNanos{it}
            while(vm.playing){
                val n=withFrameNanos{it}
                vm.tick(n-last)
                last=n
            }
        }
    }
    if(vm.dlg!=0)ProjectDialog(vm)
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
            Timeline(vm)
            ToolPanel(vm)
        }
    }
}

@Composable
fun Bar(vm:EditorVM){
    var menu by remember{mutableStateOf(false)}
    val l=vm.cur
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)){
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){
            IcBtn(R.drawable.ic_settings,"Project"){vm.dlg=2}
            BasicTextField(
                value=l?.name?:vm.pr.name,
                onValueChange={vm.rename(it)},
                singleLine=true,
                textStyle=TextStyle(color=Color.White,fontSize=18.sp),
                cursorBrush=SolidColor(Color.White),
                modifier=Modifier
                    .weight(1f)
                    .padding(horizontal=8.dp)
                    .onFocusChanged{if(!it.isFocused)vm.endLive()}
            )
            if(l!=null){
                IcBtn(R.drawable.ic_duplicate,"Copy"){vm.dup()}
                IcBtn(R.drawable.ic_delete,"Delete"){vm.del()}
                Box{
                    IcBtn(R.drawable.ic_more,"More"){menu=true}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                        val items=listOf<Pair<String,()->Unit>>(
                            "Flip Horizontal" to {vm.flip(true)},
                            "Flip Vertical" to {vm.flip(false)},
                            "Fit to Canvas" to {vm.fit(0)},
                            "Fill Canvas" to {vm.fit(1)},
                            "Stretch to Canvas" to {vm.fit(2)},
                            "Bring Forward" to {vm.move(1)},
                            "Send Backward" to {vm.move(-1)},
                            "Convert to Outline" to {vm.outline()}
                        )
                        items.forEach{(s,f)->
                            DropdownMenuItem(text={Text(s)},onClick={
                                menu=false
                                f()
                            })
                        }
                    }
                }
            }
            IcBtn(R.drawable.ic_export,"Export",vm.busy<0f){vm.export()}
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
            .clipToBounds()
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal=8.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween
    ){
        IcBtn(R.drawable.ic_undo,"Undo",vm.canUndo){vm.undo()}
        IcBtn(R.drawable.ic_redo,"Redo",vm.canRedo){vm.redo()}
        IcBtn(R.drawable.ic_to_start,"Start"){vm.seek(0L)}
        IcBtn(if(vm.playing)R.drawable.ic_pause else R.drawable.ic_play,if(vm.playing)"Pause" else "Play"){vm.toggle()}
        IcBtn(R.drawable.ic_to_end,"End"){vm.seek(vm.pr.dur)}
        IcBtn(R.drawable.ic_add,"Add"){vm.mode=Mode.ADD}
    }
}
