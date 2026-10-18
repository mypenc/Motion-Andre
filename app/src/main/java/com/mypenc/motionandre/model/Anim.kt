package com.mypenc.motionandre.model

object Anim{
    fun ease(e:Ease,x:Float):Float{
        return when(e){
            Ease.LINEAR->x
            Ease.IN->x*x*x
            Ease.OUT->1f-(1f-x)*(1f-x)*(1f-x)
            Ease.INOUT->if(x<0.5f)4f*x*x*x else 1f-Math.pow((-2.0*x+2.0),3.0).toFloat()/2f
            Ease.HOLD->0f
            Ease.BOUNCE->bounce(x)
        }
    }

    private fun bounce(x:Float):Float{
        val n=7.5625f
        val d=2.75f
        return when{
            x<1f/d->n*x*x
            x<2f/d->{val a=x-1.5f/d;n*a*a+0.75f}
            x<2.5f/d->{val a=x-2.25f/d;n*a*a+0.9375f}
            else->{val a=x-2.625f/d;n*a*a+0.984375f}
        }
    }

    fun value(l:Layer,p:Prop,t:Long):Float{
        val ks=l.keys[p]
        if(ks.isNullOrEmpty())return l.base[p]?:p.def
        if(t<=ks.first().t)return ks.first().v
        if(t>=ks.last().t)return ks.last().v
        val i=ks.indexOfLast{it.t<=t}
        val a=ks[i]
        val b=ks[i+1]
        val f=(t-a.t).toFloat()/(b.t-a.t)
        return a.v+(b.v-a.v)*ease(a.ease,f)
    }
}

fun Layer.vis(t:Long):Boolean=t>=inT&&t<=outT

fun Layer.putKey(p:Prop,t:Long,v:Float,e:Ease?=null):Layer{
    val cur=keys[p].orEmpty()
    val old=cur.firstOrNull{it.t==t}
    val k=Key(t,v,e?:old?.ease?:Ease.LINEAR)
    val n=(cur.filter{it.t!=t}+k).sortedBy{it.t}
    return copy(keys=keys+(p to n))
}

fun Layer.delKey(p:Prop,t:Long):Layer{
    val v=Anim.value(this,p,t)
    val n=keys[p].orEmpty().filter{it.t!=t}
    return if(n.isEmpty())copy(keys=keys-p,base=base+(p to v)) else copy(keys=keys+(p to n))
}

fun Layer.setVal(p:Prop,v:Float,t:Long):Layer{
    return if(keys[p].orEmpty().isNotEmpty())putKey(p,t,v) else copy(base=base+(p to v))
}

fun Project.upd(id:Long,f:(Layer)->Layer):Project=copy(layers=layers.map{if(it.id==id)f(it) else it})
