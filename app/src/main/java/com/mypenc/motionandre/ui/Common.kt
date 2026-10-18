package com.mypenc.motionandre.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

const val showLabels=true

@Composable
fun Ic(id:Int,desc:String,mod:Modifier=Modifier,tint:Color=LocalContentColor.current){
    Icon(painterResource(id),desc,mod.size(24.dp),tint)
}

@Composable
fun IcBtn(id:Int,desc:String,on:Boolean=true,mod:Modifier=Modifier,click:()->Unit){
    Column(
        mod
            .widthIn(min=44.dp)
            .heightIn(min=44.dp)
            .alpha(if(on)1f else 0.4f)
            .clickable(enabled=on,onClick=click)
            .padding(horizontal=4.dp,vertical=4.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center
    ){
        Ic(id,desc)
        if(showLabels)Text(desc,fontSize=8.sp,maxLines=1,softWrap=false)
    }
}

@Composable
fun Btn(id:Int,label:String,mod:Modifier=Modifier,on:Boolean=false,click:()->Unit){
    Column(
        mod
            .clip(RoundedCornerShape(8.dp))
            .background(if(on)MaterialTheme.colorScheme.primary.copy(alpha=0.35f) else Color(0xFF2A2A36))
            .clickable(onClick=click)
            .padding(8.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center
    ){
        Ic(id,label)
        Text(label,fontSize=11.sp,maxLines=1)
    }
}

@Composable
fun NumField(label:String,value:Float,mod:Modifier=Modifier,fmt:String="%.1f",done:()->Unit={},set:(Float)->Unit){
    var txt by remember{mutableStateOf(String.format(Locale.US,fmt,value))}
    var foc by remember{mutableStateOf(false)}
    LaunchedEffect(value){
        if(!foc)txt=String.format(Locale.US,fmt,value)
    }
    Column(mod,horizontalAlignment=Alignment.CenterHorizontally){
        BasicTextField(
            value=txt,
            onValueChange={s->
                txt=s
                s.toFloatOrNull()?.let(set)
            },
            singleLine=true,
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            textStyle=TextStyle(color=MaterialTheme.colorScheme.secondary,fontSize=14.sp,textAlign=TextAlign.Center),
            cursorBrush=SolidColor(Color.White),
            modifier=Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A36))
                .padding(8.dp)
                .onFocusChanged{
                    foc=it.isFocused
                    if(!it.isFocused){
                        txt=String.format(Locale.US,fmt,value)
                        done()
                    }
                }
        )
        Text(label,fontSize=10.sp)
    }
}

@Composable
fun Sl(label:String,v:Float,min:Float,max:Float,fin:()->Unit,set:(Float)->Unit){
    Column(Modifier.fillMaxWidth()){
        Text(label,fontSize=12.sp)
        Slider(
            value=v.coerceIn(min,max),
            onValueChange=set,
            valueRange=min..max,
            onValueChangeFinished=fin
        )
    }
}
