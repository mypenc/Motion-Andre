package com.mypenc.motionandre.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Ic(id:Int,desc:String,mod:Modifier=Modifier,tint:Color=LocalContentColor.current){
    Icon(painterResource(id),desc,mod.size(24.dp),tint)
}

@Composable
fun Tool(id:Int,label:String,on:Boolean=true,click:()->Unit){
    Column(
        Modifier
            .alpha(if(on)1f else 0.4f)
            .clickable(enabled=on,onClick=click)
            .padding(horizontal=10.dp,vertical=6.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ){
        Ic(id,label)
        Text(label,fontSize=10.sp,maxLines=1)
    }
}
