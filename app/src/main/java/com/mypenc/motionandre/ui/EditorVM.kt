package com.mypenc.motionandre.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypenc.motionandre.data.Store
import com.mypenc.motionandre.model.Anim
import com.mypenc.motionandre.model.Blend
import com.mypenc.motionandre.model.Ease
import com.mypenc.motionandre.model.Fill
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

enum class Mode{HOME,FILL,BORDER,BLEND,XFORM,SHAPE,ADD}

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
    var mode by mutableStateOf(Mode.HOME)
    var xm by mutableIntStateOf(0)
    var dlg by mutableIntStateOf(0)
    private val us=ArrayDeque<Project>()
    private val rs=ArrayDeque<Project>()
    private var gest=false
    private var rem=0L
    private val file=File(app.filesDir,"project.json")

    val cur:Layer? get()=pr.layers.firstOrNull{it.id==sel}

    private val u:Float get()=minOf(pr.w,pr.h)/720f

    init{
        val ok=runCatching{
            if(file.exists()){
                pr=Store.dec(file.readText())
                true
            }else false
        }.getOrDefault(false)
        if(!ok)dlg=1
    }

    fun newProject(name:String,w:Int,h:Int,fps:Int,dur:Long,bg:Int){
        us.clear()
        rs.clear()
        refresh()
        sel=null
        playing=false
        mode=Mode.HOME
        t=0L
        pr=Project(w=w,h=h,fps=fps,dur=dur,bg=bg,name=name)
        dlg=0
        save()
    }

    fun applySettings(name:String,fps:Int,dur:Long,bg:Int){
        act{it.copy(name=name,fps=fps,dur=dur,bg=bg)}
        t=t.coerceAtMost(dur)
        dlg=0
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

    private fun edit(f:(Layer)->Layer){
        val s=sel?:return
        live{it.upd(s,f)}
    }

    private fun editA(f:(Layer)->Layer){
        val s=sel?:return
        act{it.upd(s,f)}
    }

    fun save(){
        val s=Store.enc(pr)
        runCatching{file.writeText(s)}
    }

    fun seek(ms:Long){
        val f=(ms*pr.fps/1000f).roundToInt()
        t=(f*1000L/pr.fps).coerceIn(0L,pr.dur)
    }

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
            val d=when(k){
                Kind.RECT->320f to 200f
                Kind.ELLIPSE->240f to 240f
                Kind.TRIANGLE->260f to 240f
                Kind.STAR->260f to 250f
                Kind.HEXAGON->260f to 230f
                Kind.PLUS->220f to 220f
                Kind.TEXT->520f to 110f
            }
            val nm=k.name.lowercase().replaceFirstChar{it.uppercase()}
            val l=Layer(
                id=id,
                name="$nm $n",
                kind=k,
                color=if(k==Kind.TEXT)pal[5] else pal[id.toInt()%pal.size],
                w=d.first*u,
                h=d.second*u,
                text=if(k==Kind.TEXT)"Text" else "",
                outT=p.dur,
                color2=pal[(id.toInt()+2)%pal.size],
                rad=16f*u
            )
            sel=id
            mode=Mode.HOME
            p.copy(layers=p.layers+l,nextId=id+1L)
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

    fun hide(id:Long){
        act{it.upd(id){l->l.copy(hidden=!l.hidden)}}
    }

    fun rename(v:String){
        if(sel!=null)edit{it.copy(name=v)} else live{it.copy(name=v)}
    }

    fun setP(pp:Prop,v:Float){
        val tt=t
        edit{l->l.setVal(pp,v,tt)}
    }

    private fun adj(pp:Prop,d:Float){
        val tt=t
        edit{l->l.setVal(pp,(Anim.value(l,pp,tt)+d).coerceIn(pp.min,pp.max),tt)}
    }

    fun nudge(dx:Float,dy:Float){
        val tt=t
        edit{l->
            l.setVal(Prop.X,Anim.value(l,Prop.X,tt)+dx,tt)
                .setVal(Prop.Y,Anim.value(l,Prop.Y,tt)+dy,tt)
        }
    }

    fun pad(dx:Float,dy:Float){
        if(cur==null)return
        val tt=t
        val k=pr.w/720f
        when(xm){
            0->nudge(dx*k/pr.w,dy*k/pr.h)
            1->adj(Prop.ROT,dx*0.4f)
            2->{
                val f=1f+dx*0.004f
                edit{a->
                    a.setVal(Prop.SX,(Anim.value(a,Prop.SX,tt)*f).coerceIn(0f,4f),tt)
                        .setVal(Prop.SY,(Anim.value(a,Prop.SY,tt)*f).coerceIn(0f,4f),tt)
                }
            }
            else->adj(Prop.SKEW,dx*0.2f)
        }
    }

    fun hasKeys(ps:List<Prop>):Boolean{
        val l=cur?:return false
        return ps.all{p->l.keys[p].orEmpty().any{k->k.t==t}}
    }

    fun toggleKeys(ps:List<Prop>){
        val tt=t
        editA{l->
            val all=ps.all{p->l.keys[p].orEmpty().any{k->k.t==tt}}
            ps.fold(l){a,p->if(all)a.delKey(p,tt) else a.putKey(p,tt,Anim.value(a,p,tt))}
        }
    }

    fun easeNow():Ease?{
        val l=cur?:return null
        return l.keys.values.flatten().firstOrNull{it.t==t}?.ease
    }

    fun setEase(e:Ease){
        val tt=t
        editA{l->
            l.copy(keys=l.keys.mapValues{(_,ks)->ks.map{k->if(k.t==tt)k.copy(ease=e) else k}})
        }
    }

    fun fill(f:Fill)=editA{it.copy(fill=f)}

    fun col(c:Int,second:Boolean){
        edit{if(second)it.copy(color2=c) else it.copy(color=c)}
    }

    fun stroke(w:Float?,c:Int?){
        edit{it.copy(strokeW=w?:it.strokeW,strokeC=c?:it.strokeC)}
    }

    fun blend(b:Blend)=editA{it.copy(blend=b)}

    fun size(w:Float,h:Float){
        edit{it.copy(w=w,h=h)}
    }

    fun rad(r:Float){
        edit{it.copy(rad=r)}
    }

    fun text(v:String){
        edit{it.copy(text=v)}
    }

    fun flip(h:Boolean){
        editA{if(h)it.copy(flipH=!it.flipH) else it.copy(flipV=!it.flipV)}
    }

    fun fit(m:Int){
        val tt=t
        val pw=pr.w
        val ph=pr.h
        editA{l->
            val sx=pw/l.w
            val sy=ph/l.h
            val a=when(m){
                0->minOf(sx,sy)
                1->maxOf(sx,sy)
                else->sx
            }
            val b=if(m>1)sy else a
            l.setVal(Prop.X,0.5f,tt).setVal(Prop.Y,0.5f,tt)
                .setVal(Prop.SX,a,tt).setVal(Prop.SY,b,tt).setVal(Prop.ROT,0f,tt)
        }
    }

    fun outline(){
        val w=(6f*u).coerceAtLeast(2f)
        editA{it.copy(fill=Fill.NONE,strokeW=w,strokeC=it.color)}
    }

    fun trimEdge(id:Long,which:Int,ms:Long){
        live{p->
            p.upd(id){l->
                if(which==1)l.copy(inT=ms.coerceIn(0L,l.outT-100L))
                else l.copy(outT=ms.coerceIn(l.inT+100L,p.dur))
            }
        }
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
