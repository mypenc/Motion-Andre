package com.mypenc.motionandre.render

import android.graphics.Canvas
import android.graphics.Paint
import com.mypenc.motionandre.model.Anim
import com.mypenc.motionandre.model.Kind
import com.mypenc.motionandre.model.Layer
import com.mypenc.motionandre.model.Project
import com.mypenc.motionandre.model.Prop
import com.mypenc.motionandre.model.vis
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Draw{
    fun frame(c:Canvas,pr:Project,t:Long){
        c.drawColor(pr.bg)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        pr.layers.forEach{if(it.vis(t))one(c,pr,it,t,p)}
    }

    private fun tf(c:Canvas,pr:Project,l:Layer,t:Long){
        c.translate(Anim.value(l,Prop.X,t)*pr.w,Anim.value(l,Prop.Y,t)*pr.h)
        c.rotate(Anim.value(l,Prop.ROT,t))
        c.scale(Anim.value(l,Prop.SX,t),Anim.value(l,Prop.SY,t))
    }

    private fun one(c:Canvas,pr:Project,l:Layer,t:Long,p:Paint){
        val a=Anim.value(l,Prop.ALPHA,t).coerceIn(0f,1f)
        if(a<=0f)return
        c.save()
        tf(c,pr,l,t)
        p.style=Paint.Style.FILL
        p.color=l.color
        p.alpha=((l.color ushr 24)*a).toInt()
        when(l.kind){
            Kind.RECT->c.drawRoundRect(-l.w/2,-l.h/2,l.w/2,l.h/2,16f,16f,p)
            Kind.ELLIPSE->c.drawOval(-l.w/2,-l.h/2,l.w/2,l.h/2,p)
            Kind.TEXT->{
                p.textSize=l.h
                p.textAlign=Paint.Align.CENTER
                p.isFakeBoldText=true
                c.drawText(l.text,0f,-(p.ascent()+p.descent())/2,p)
                p.isFakeBoldText=false
                p.textAlign=Paint.Align.LEFT
            }
        }
        c.restore()
    }

    fun outline(c:Canvas,pr:Project,l:Layer,t:Long,sw:Float){
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        p.style=Paint.Style.STROKE
        p.strokeWidth=sw
        p.color=0xFF4DD0E1.toInt()
        c.save()
        tf(c,pr,l,t)
        c.drawRect(-l.w/2,-l.h/2,l.w/2,l.h/2,p)
        c.restore()
    }

    fun hit(pr:Project,l:Layer,t:Long,x:Float,y:Float):Boolean{
        val sx=Anim.value(l,Prop.SX,t)
        val sy=Anim.value(l,Prop.SY,t)
        if(sx==0f||sy==0f)return false
        val r=Math.toRadians(-Anim.value(l,Prop.ROT,t).toDouble())
        val dx=x-Anim.value(l,Prop.X,t)*pr.w
        val dy=y-Anim.value(l,Prop.Y,t)*pr.h
        val rx=(dx*cos(r)-dy*sin(r)).toFloat()/sx
        val ry=(dx*sin(r)+dy*cos(r)).toFloat()/sy
        return abs(rx)<=l.w/2&&abs(ry)<=l.h/2
    }
}
