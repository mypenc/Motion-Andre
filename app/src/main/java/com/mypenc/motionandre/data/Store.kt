package com.mypenc.motionandre.data

import com.mypenc.motionandre.model.Ease
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
            )
        }
        return JSONObject()
            .put("w",p.w).put("h",p.h).put("fps",p.fps).put("dur",p.dur).put("bg",p.bg)
            .put("next",p.nextId).put("layers",ls).toString()
    }

    fun dec(s:String):Project{
        val o=JSONObject(s)
        val la=o.getJSONArray("layers")
        val ls=(0 until la.length()).map{i->
            val lo=la.getJSONObject(i)
            val b=lo.getJSONObject("base")
            val k=lo.getJSONObject("keys")
            Layer(
                lo.getLong("id"),lo.getString("name"),Kind.valueOf(lo.getString("kind")),lo.getInt("color"),
                lo.getDouble("w").toFloat(),lo.getDouble("h").toFloat(),lo.getString("text"),
                lo.getLong("in"),lo.getLong("out"),
                b.keys().asSequence().associate{Prop.valueOf(it) to b.getDouble(it).toFloat()},
                k.keys().asSequence().associate{n->
                    val a=k.getJSONArray(n)
                    Prop.valueOf(n) to (0 until a.length()).map{j->
                        val x=a.getJSONObject(j)
                        Key(x.getLong("t"),x.getDouble("v").toFloat(),Ease.valueOf(x.getString("e")))
                    }
                }
            )
        }
        return Project(o.getInt("w"),o.getInt("h"),o.getInt("fps"),o.getLong("dur"),o.getInt("bg"),ls,o.getLong("next"))
    }
}
