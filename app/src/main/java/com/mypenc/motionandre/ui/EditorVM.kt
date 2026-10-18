package com.mypenc.motionandre.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypenc.motionandre.data.Store
import com.mypenc.motionandre.model.Anim
import com.mypenc.motionandre.model.Ease
import com.mypenc.motionandre.model.Kind
import com.mypenc.motionandre.model.Layer
import com.mypenc.motionandre.model.Project
import com.mypenc.motionandre.model.Prop
import com.mypenc.motionandre.model.delKey
import com.mypenc.motionandre.model.pal
import com.mypenc.motionandre.model.putKey
import com.mypenc.motionandre.model.setVal
import com.mypenc.motionandre.model.upd
import com.mypenc.motionandre.model.vis
import com.mypenc.motionandre.render.Draw
import com.mypenc.motionandre.render.Exporter
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditorVM(app:Application):AndroidViewModel(app){
    var pr by mutableStateOf(Project())
        private set
    var sel by mutableStateOf<Long?>(null)
    var t by mutableLongStateOf(0L)
        private set
    var playing by mutableStateOf(false)
    var busy by mutableStateOf(-1f)
    var msg by mutableStateOf<String?>(null)
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    private val us=ArrayDeque<Project>()
    private val rs=ArrayDeque<Project>()
    private var gest=false
    private var rem=0L
    private val file=File(app.filesDir,"project.json")

    val cur:Layer? get()=pr.layers.firstOrNull{it.id==sel}

    init{
        runCatching{if(file.exists())pr=Store.dec(file.readText())}
        if(pr.layers.isEmpty())demo()
    }

    private fun demo(){
        val c=Layer(1L,"Circle",Kind.ELLIPSE,pal[0],240f,240f)
            .putKey(Prop.X,0L,0.25f,Ease.OUT)
            .putKey(Prop.X,2500L,0.75f,Ease.INOUT)
            .putKey(Prop.X,5000L,0.25f)
            .putKey(Prop.SX,0L,0.6f,Ease.BOUNCE)
            .putKey(Prop.SX,1500L,1.2f)
            .putKey(Prop.SY,0L,0.6f,Ease.BOUNCE)
            .putKey(Prop.SY,1500L,1.2f)
        val x=Layer(2L,"Title",Kind.TEXT,pal[5],620f,110f,"Motion Andre")
            .copy(base=mapOf(Prop.Y to 0.18f))
            .putKey(Prop.ALPHA,0L,0f)
            .putKey(Prop.ALPHA,800L,1f)
        pr=Project(layers=listOf(c,x),nextId=3L)
    }

    private fun refresh(){
        canUndo=us.isNotEmpty()
        canRedo=rs.isNotEmpty()
    }

    private fun push(){
        us.addLast(pr)
        if(us.size>100)us.removeFirst()
        rs.clear()
        refresh()
    }

    private fun fixSel(){
        if(pr.layers.none{it.id==sel})sel=null
    }

    fun undo(){
        if(us.isEmpty())return
        rs.addLast(pr)
        pr=us.removeLast()
        fixSel()
        refresh()
        save()
    }

    fun redo(){
        if(rs.isEmpty())return
        us.addLast(pr)
        pr=rs.removeLast()
        fixSel()
        refresh()
        save()
    }

    private fun live(f:(Project)->Project){
        if(!gest){
            push()
            gest=true
        }
        pr=f(pr)
    }

    fun endLive(){
        gest=false
        save()
    }

    private fun act(f:(Project)->Project){
        push()
        pr=f(pr)
        save()
    }

    fun save(){
        val s=Store.enc(pr)
        runCatching{file.writeText(s)}
    }

    fun seek(ms:Long){
        val f=(ms*pr.fps/1000f).roundToInt()
        t=(f*1000L/pr.fps).coerceIn(0L,pr.dur)
    }

    fun step(d:Int)=seek(t+d*1000L/pr.fps)

    fun toggle(){
        if(playing){
            playing=false
            return
        }
        if(t>=pr.dur)t=0L
        rem=0L
        playing=true
    }

    fun tick(ns:Long){
        rem+=ns
        val ms=rem/1_000_000L
        rem%=1_000_000L
        t=(t+ms).coerceAtMost(pr.dur)
        if(t>=pr.dur)playing=false
    }

    fun add(k:Kind){
        act{p->
            val id=p.nextId
            val n=p.layers.count{it.kind==k}+1
            val col=pal[id.toInt()%pal.size]
            val l=when(k){
                Kind.RECT->Layer(id,"Rectangle $n",k,col,320f,200f)
                Kind.ELLIPSE->Layer(id,"Ellipse $n",k,col,240f,240f)
                Kind.TEXT->Layer(id,"Text $n",k,pal[5],520f,110f,"Text")
            }
            sel=id
            p.copy(layers=p.layers+l.copy(outT=p.dur),nextId=id+1L)
        }
    }

    fun del(){
        val s=sel?:return
        act{it.copy(layers=it.layers.filter{l->l.id!=s})}
        sel=null
    }

    fun dup(){
        val l=cur?:return
        act{p->
            val id=p.nextId
            sel=id
            p.copy(layers=p.layers+l.copy(id=id,name=l.name+" copy"),nextId=id+1L)
        }
    }

    fun move(d:Int){
        val s=sel?:return
        act{p->
            val i=p.layers.indexOfFirst{it.id==s}
            val j=i+d
            if(i<0||j<0||j>=p.layers.size)p
            else p.copy(layers=p.layers.toMutableList().also{m->m.add(j,m.removeAt(i))})
        }
    }

    fun setP(pp:Prop,v:Float){
        val s=sel?:return
        val tt=t
        live{it.upd(s){l->l.setVal(pp,v,tt)}}
    }

    fun nudge(dx:Float,dy:Float){
        val s=sel?:return
        val tt=t
        live{
            it.upd(s){l->
                l.setVal(Prop.X,Anim.value(l,Prop.X,tt)+dx,tt)
                    .setVal(Prop.Y,Anim.value(l,Prop.Y,tt)+dy,tt)
            }
        }
    }

    fun toggleKey(pp:Prop){
        val s=sel?:return
        val tt=t
        act{
            it.upd(s){l->
                if(l.keys[pp].orEmpty().any{k->k.t==tt})l.delKey(pp,tt)
                else l.putKey(pp,tt,Anim.value(l,pp,tt))
            }
        }
    }

    fun setEase(e:Ease){
        val s=sel?:return
        val tt=t
        act{
            it.upd(s){l->
                l.copy(keys=l.keys.mapValues{(_,ks)->ks.map{k->if(k.t==tt)k.copy(ease=e) else k}})
            }
        }
    }

    fun color(c:Int){
        val s=sel?:return
        act{it.upd(s){l->l.copy(color=c)}}
    }

    fun text(v:String){
        val s=sel?:return
        live{it.upd(s){l->l.copy(text=v)}}
    }

    fun trim(a:Long,b:Long){
        val s=sel?:return
        live{it.upd(s){l->l.copy(inT=a,outT=b)}}
    }

    fun grab(x:Float,y:Float):Boolean{
        val l=pr.layers.lastOrNull{it.vis(t)&&Draw.hit(pr,it,t,x,y)}
        if(l!=null)sel=l.id
        return l!=null
    }

    fun pick(x:Float,y:Float){
        sel=pr.layers.lastOrNull{it.vis(t)&&Draw.hit(pr,it,t,x,y)}?.id
    }

    fun export(){
        if(busy>=0f)return
        playing=false
        val p=pr
        busy=0f
        viewModelScope.launch(Dispatchers.Default){
            val r=runCatching{Exporter.run(getApplication(),p){f->busy=f}}
            busy=-1f
            msg=r.fold({"Saved to Movies/MotionAndre"},{"Export failed: ${it.message}"})
        }
    }

    override fun onCleared(){
        save()
        super.onCleared()
    }
}
