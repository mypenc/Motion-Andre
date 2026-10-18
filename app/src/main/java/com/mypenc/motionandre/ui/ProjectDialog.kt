package com.mypenc.motionandre.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

@Composable
fun <T> Drop(label:String,v:T,opts:List<T>,show:(T)->String,set:(T)->Unit){
    var x by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Text(label,Modifier.weight(1f))
        Box{
            OutlinedButton(onClick={x=true}){Text(show(v))}
            DropdownMenu(expanded=x,onDismissRequest={x=false}){
                opts.forEach{o->
                    DropdownMenuItem(text={Text(show(o))},onClick={
                        x=false
                        set(o)
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDialog(vm:EditorVM){
    val isNew=vm.dlg==1
    var name by remember{mutableStateOf(if(isNew)"New Project" else vm.pr.name)}
    var ar by remember{mutableIntStateOf(0)}
    var res by remember{mutableIntStateOf(1080)}
    var fps by remember{mutableIntStateOf(if(isNew)30 else vm.pr.fps)}
    var dur by remember{mutableLongStateOf(if(isNew)5000L else vm.pr.dur)}
    var bg by remember{mutableIntStateOf(if(isNew)0xFFD9D9D9.toInt() else vm.pr.bg)}
    val ars=listOf("16:9" to (16 to 9),"9:16" to (9 to 16),"4:5" to (4 to 5),"1:1" to (1 to 1),"4:3" to (4 to 3))
    val bgs=listOf(0xFFD9D9D9.toInt(),0xFFFFFFFF.toInt(),0xFF000000.toInt(),0xFF1B1B22.toInt(),0xFF1E3A5F.toInt(),0xFF5F1E3A.toInt())
    Dialog(onDismissRequest={vm.dlg=0}){
        Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                Text(if(isNew)"New Project" else "Project Settings",fontWeight=FontWeight.Bold)
                OutlinedTextField(value=name,onValueChange={name=it},singleLine=true,modifier=Modifier.fillMaxWidth(),label={Text("Name")})
                if(isNew){
                    Text("Aspect ratio")
                    Row(Modifier.horizontalScroll(rememberScrollState())){
                        ars.forEachIndexed{i,a->
                            FilterChip(selected=ar==i,onClick={ar=i},label={Text(a.first)},modifier=Modifier.padding(end=6.dp))
                        }
                    }
                    Drop("Resolution",res,listOf(480,720,1080),{"${it}p"}){res=it}
                }
                Drop("Frame rate",fps,listOf(24,30,60),{"$it fps"}){fps=it}
                Text("Duration")
                Row(Modifier.horizontalScroll(rememberScrollState())){
                    listOf(5000L,10000L,15000L,30000L).forEach{d->
                        FilterChip(selected=dur==d,onClick={dur=d},label={Text("${d/1000} s")},modifier=Modifier.padding(end=6.dp))
                    }
                }
                Text("Background")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    bgs.forEach{c->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(c))
                                .border(2.dp,if(bg==c)MaterialTheme.colorScheme.primary else Color.Gray,RoundedCornerShape(8.dp))
                                .clickable{bg=c}
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                    TextButton(onClick={vm.dlg=0}){Text("Cancel")}
                    Button(onClick={
                        val nm=name.ifBlank{"Untitled"}
                        if(isNew){
                            val aw=ars[ar].second.first
                            val ah=ars[ar].second.second
                            val lg=(res*maxOf(aw,ah).toFloat()/minOf(aw,ah)/2f).roundToInt()*2
                            val w=if(aw>=ah)lg else res
                            val h=if(aw>=ah)res else lg
                            vm.newProject(nm,w,h,fps,dur,bg)
                        }else{
                            vm.applySettings(nm,fps,dur,bg)
                        }
                    }){Text(if(isNew)"Create" else "Apply")}
                }
            }
        }
    }
}
