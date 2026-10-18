package com.mypenc.motionandre.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.mypenc.motionandre.model.Anim
import com.mypenc.motionandre.model.Blend
import com.mypenc.motionandre.model.Fill
import com.mypenc.motionandre.model.Kind
import com.mypenc.motionandre.model.Layer
import com.mypenc.motionandre.model.Project
import com.mypenc.motionandre.model.Prop
import com.mypenc.motionandre.model.vis
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object Draw{
    fun frame(c:Canvas,pr:Project,t:Long){
        c.drawColor(pr.bg)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        pr.layers.forEach{if(it.vis(t))one(c,pr,it,t,p)}
    }

    fun path(k:Kind,w:Float,h:Float,rad:Float):Path{
        val p=Path()
        val a=-w/2
        val b=-h/2
        val c=w/2
        val d=h/2
        when(k){
            Kind.RECT->p.addRoundRect(a,b,c,d,rad,rad,Path.Direction.CW)
            Kind.TEXT->p.addRect(a,b,c,d,Path.Direction.CW)
            Kind.ELLIPSE->p.addOval(a,b,c,d,Path.Direction.CW)
            Kind.TRIANGLE->{
                p.moveTo(0f,b)
                p.lineTo(c,d)
                p.lineTo(a,d)
                p.close()
            }
            Kind.STAR->{
                for(i in 0 until 10){
                    val r=if(i%2==0)1f else 0.42f
                    val an=Math.toRadians(-90.0+i*36.0)
                    val x=(cos(an)*r*w/2).toFloat()
                    val y=(sin(an)*r*h/2).toFloat()
                    if(i==0)p.moveTo(x,y) else p.lineTo(x,y)
                }
                p.close()
            }
            Kind.HEXAGON->{
                for(i in 0 until 6){
                    val an=Math.toRadians(-90.0+i*60.0)
                    val x=(cos(an)*w/2).toFloat()
                    val y=(sin(an)*h/2).toFloat()
                    if(i==0)p.moveTo(x,y) else p.lineTo(x,y)
                }
                p.close()
            }
            Kind.PLUS->{
                val x1=-w*0.17f
                val x2=w*0.17f
                val y1=-h*0.17f
                val y2=h*0.17f
                p.moveTo(x1,b)
                p.lineTo(x2,b)
                p.lineTo(x2,y1)
                p.lineTo(c,y1)
                p.lineTo(c,y2)
                p.lineTo(x2,y2)
                p.lineTo(x2,d)
                p.lineTo(x1,d)
                p.lineTo(x1,y2)
                p.lineTo(a,y2)
                p.lineTo(a,y1)
                p.lineTo(x1,y1)
                p.close()
            }
        }
        return p
    }

    private fun tf(c:Canvas,pr:Project,l:Layer,t:Long){
        c.translate(Anim.value(l,Prop.X,t)*pr.w,Anim.value(l,Prop.Y,t)*pr.h)
        c.rotate(Anim.value(l,Prop.ROT,t))
        val k=Anim.value(l,Prop.SKEW,t)
        if(k!=0f)c.skew(tan(Math.toRadians(k.toDouble())).toFloat(),0f)
        c.scale(
            Anim.value(l,Prop.SX,t)*(if(l.flipH)-1f else 1f),
            Anim.value(l,Prop.SY,t)*(if(l.flipV)-1f else 1f)
        )
    }

    private fun mode(b:Blend):PorterDuff.Mode?{
        return when(b){
            Blend.NORMAL->null
            Blend.MULTIPLY->PorterDuff.Mode.MULTIPLY
            Blend.SCREEN->PorterDuff.Mode.SCREEN
            Blend.OVERLAY->PorterDuff.Mode.OVERLAY
            Blend.DARKEN->PorterDuff.Mode.DARKEN
            Blend.LIGHTEN->PorterDuff.Mode.LIGHTEN
            Blend.ADD->PorterDuff.Mode.ADD
        }
    }

    private fun one(c:Canvas,pr:Project,l:Layer,t:Long,p:Paint){
        val a=Anim.value(l,Prop.ALPHA,t).coerceIn(0f,1f)
        if(a<=0f)return
        c.save()
        tf(c,pr,l,t)
        p.reset()
        p.isAntiAlias=true
        p.xfermode=mode(l.blend)?.let{PorterDuffXfermode(it)}
        val isT=l.kind==Kind.TEXT
        if(isT){
            p.textSize=l.h
            p.textAlign=Paint.Align.CENTER
            p.isFakeBoldText=true
        }
        val ty=-(p.ascent()+p.descent())/2
        val path=if(isT)null else path(l.kind,l.w,l.h,l.rad)
        if(l.fill!=Fill.NONE){
            p.style=Paint.Style.FILL
            p.shader=if(l.fill==Fill.GRADIENT)LinearGradient(-l.w/2,-l.h/2,l.w/2,l.h/2,l.color,l.color2,Shader.TileMode.CLAMP) else null
            p.color=l.color
            p.alpha=((l.color ushr 24)*a).toInt()
            if(path==null)c.drawText(l.text,0f,ty,p) else c.drawPath(path,p)
        }
        if(l.strokeW>0f){
            p.shader=null
            p.style=Paint.Style.STROKE
            p.strokeWidth=l.strokeW
            p.color=l.strokeC
            p.alpha=((l.strokeC ushr 24)*a).toInt()
            if(path==null)c.drawText(l.text,0f,ty,p) else c.drawPath(path,p)
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
