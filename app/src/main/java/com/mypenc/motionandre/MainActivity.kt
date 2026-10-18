package com.mypenc.motionandre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mypenc.motionandre.ui.Editor
import com.mypenc.motionandre.ui.EditorVM
import com.mypenc.motionandre.ui.MaTheme

class MainActivity:ComponentActivity(){
    private val vm:EditorVM by viewModels()

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        setContent{
            MaTheme{
                Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
                    Editor(vm)
                }
            }
        }
    }

    override fun onStop(){
        super.onStop()
        vm.save()
    }
}
