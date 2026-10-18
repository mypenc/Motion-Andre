package com.mypenc.motionandre.data

import com.mypenc.motionandre.model.Blend
import com.mypenc.motionandre.model.Ease
import com.mypenc.motionandre.model.Fill
import com.mypenc.motionandre.model.Key
import com.mypenc.motionandre.model.Kind
import com.mypenc.motionandre.model.Layer
import com.mypenc.motionandre.model.Project
import com.mypenc.motionandre.model.Prop
import org.json.JSONArray
import org.json.JSONObject

object Store{
    fun enc(p:Project):String{
        val ls=JSONArray()
        p.layers.forEach{l->
            val b=JSONObject()
            l.base.forEach{(k,v)->b.put(k.name,v.toDouble())}
            val ks=JSONObject()
            l.keys.forEach{(k,v)->
                val a=JSONArray()
                v.forEach{x->a.put(JSONObject().put("t",x.t).put("v",x.v.toDouble()).put("e",x.ease.name))}
                ks.put(k.name,a)
            }
            ls.put(
                JSONObject()
                    .put("id",l.id).put("name",l.name).put("kind",l.kind.name).put("color",l.color)
                    .put("w",l.w.toDouble()).put("h",l.h.toDouble()).put("text",l.text)
                    .put("in",l.inT).put("out",l.outT).put("base",b).put("keys",ks)
                    .put("color2",l.color2).put("fill",l.fill.name).put("strokeW",l.strokeW.toDouble())
                    .put("strokeC",l.strokeC).put("blend",l.blend.name).put("rad",l.rad.toDouble())
                    .put("flipH",l.flipH).put("flipV",l.flipV).put("hidden",l.hidden)
            )
        }
        return JSONObject()
            .put("w",p.w).put("h",p.h).put("fps",p.fps).put("dur",p.dur).put("bg",p.bg)
            .put("next",p.nextId).put("name",p.name).put("layers",ls).toString()
    }

    fun dec(s:String):Project{
        val o=JSONObject(s)
        val la=o.getJSONArray("layers")
        val ls=(0 until la.length()).map{i->
            val lo=la.getJSONObject(i)
            val b=lo.getJSONObject("base")
            val k=lo.getJSONObject("keys")
            Layer(
                id=lo.getLong("id"),
                name=lo.getString("name"),
                kind=Kind.valueOf(lo.getString("kind")),
                color=lo.getInt("color"),
                w=lo.getDouble("w").toFloat(),
                h=lo.getDouble("h").toFloat(),
                text=lo.getString("text"),
                inT=lo.getLong("in"),
                outT=lo.getLong("out"),
                base=b.keys().asSequence().associate{Prop.valueOf(it) to b.getDouble(it).toFloat()},
                keys=k.keys().asSequence().associate{n->
                    val a=k.getJSONArray(n)
                    Prop.valueOf(n) to (0 until a.length()).map{j->
                        val x=a.getJSONObject(j)
                        Key(x.getLong("t"),x.getDouble("v").toFloat(),Ease.valueOf(x.getString("e")))
                    }
                },
                color2=lo.optInt("color2",lo.getInt("color")),
                fill=Fill.valueOf(lo.optString("fill","SOLID")),
                strokeW=lo.optDouble("strokeW",0.0).toFloat(),
                strokeC=lo.optInt("strokeC",-1),
                blend=Blend.valueOf(lo.optString("blend","NORMAL")),
                rad=lo.optDouble("rad",16.0).toFloat(),
                flipH=lo.optBoolean("flipH",false),
                flipV=lo.optBoolean("flipV",false),
                hidden=lo.optBoolean("hidden",false)
            )
        }
        return Project(
            w=o.getInt("w"),
            h=o.getInt("h"),
            fps=o.getInt("fps"),
            dur=o.getLong("dur"),
            bg=o.getInt("bg"),
            layers=ls,
            nextId=o.getLong("next"),
            name=o.optString("name","New Project")
        )
    }
}
