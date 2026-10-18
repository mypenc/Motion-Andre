package com.mypenc.motionandre.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypenc.motionandre.R
import com.mypenc.motionandre.model.Anim
import com.mypenc.motionandre.model.Ease
import com.mypenc.motionandre.model.Kind
import com.mypenc.motionandre.model.Prop
import com.mypenc.motionandre.model.pal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.PropsPanel(vm:EditorVM){
    val l=vm.cur
    if(l==null){
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
            Text("Select a layer")
        }
        return
    }
    val t=vm.t
    val ease=l.keys.values.flatten().firstOrNull{it.t==t}?.ease
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)){
        Text(l.name,fontWeight=FontWeight.Bold)
        if(l.kind==Kind.TEXT){
            OutlinedTextField(
                value=l.text,
                onValueChange={vm.text(it)},
                modifier=Modifier.fillMaxWidth().onFocusChanged{if(!it.isFocused)vm.endLive()},
                singleLine=true,
                label={Text("Text")}
            )
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=6.dp)){
            pal.forEach{c->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(2.dp,if(c==l.color)Color.White else Color.Transparent,CircleShape)
                        .clickable{vm.color(c)}
                )
            }
        }
        Text("Visible ${fmt(l.inT)} to ${fmt(l.outT)}",fontSize=12.sp)
        RangeSlider(
            value=l.inT.toFloat()..l.outT.toFloat(),
            onValueChange={vm.trim(it.start.toLong(),it.endInclusive.toLong())},
            valueRange=0f..vm.pr.dur.toFloat(),
            onValueChangeFinished={vm.endLive()}
        )
        Prop.entries.forEach{pp->
            val k=l.keys[pp].orEmpty().any{it.t==t}
            val v=Anim.value(l,pp,t)
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text(pp.label+": "+String.format(Locale.US,"%.2f",v),fontSize=12.sp)
                    Slider(
                        value=v.coerceIn(pp.min,pp.max),
                        onValueChange={vm.setP(pp,it)},
                        valueRange=pp.min..pp.max,
                        onValueChangeFinished={vm.endLive()}
                    )
                }
                Tool(if(k)R.drawable.ic_keyframe_filled else R.drawable.ic_keyframe,"Key"){vm.toggleKey(pp)}
            }
        }
        Spacer(Modifier.height(6.dp))
        if(ease==null){
            Text("Add a keyframe at the playhead to choose its easing",fontSize=12.sp)
        }else{
            Text("Easing at playhead",fontSize=12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState())){
                Ease.entries.forEach{e->
                    FilterChip(
                        selected=ease==e,
                        onClick={vm.setEase(e)},
                        label={Text(e.name)},
                        modifier=Modifier.padding(end=6.dp)
                    )
                }
            }
        }
    }
}
