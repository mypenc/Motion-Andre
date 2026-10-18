package com.mypenc.motionandre.render

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.provider.MediaStore
import android.view.Surface
import com.mypenc.motionandre.model.Project

object Exporter{
    fun run(ctx:Context,pr:Project,onP:(Float)->Unit){
        val res=ctx.contentResolver
        val cv=ContentValues().apply{
            put(MediaStore.Video.Media.DISPLAY_NAME,"MotionAndre_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/MotionAndre")
            put(MediaStore.Video.Media.IS_PENDING,1)
        }
        val uri=res.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cv)?:error("Storage unavailable")
        val pfd=res.openFileDescriptor(uri,"w")?:error("Cannot open output")
        val fmt=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,pr.w,pr.h).apply{
            setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE,8_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE,pr.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        }
        val enc=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var mux:MediaMuxer?=null
        var surf:Surface?=null
        var started=false
        var ok=false
        try{
            enc.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            val sf=enc.createInputSurface()
            surf=sf
            enc.start()
            val m=MediaMuxer(pfd.fileDescriptor,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            mux=m
            val info=MediaCodec.BufferInfo()
            var track=-1
            var out=0L
            fun drain(eos:Boolean){
                while(true){
                    val i=enc.dequeueOutputBuffer(info,10_000)
                    if(i==MediaCodec.INFO_TRY_AGAIN_LATER){
                        if(!eos)return
                    }else if(i==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                        track=m.addTrack(enc.outputFormat)
                        m.start()
                        started=true
                    }else if(i>=0){
                        val buf=enc.getOutputBuffer(i)!!
                        if((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)info.size=0
                        if(info.size>0&&started){
                            buf.position(info.offset)
                            buf.limit(info.offset+info.size)
                            info.presentationTimeUs=out*1_000_000L/pr.fps
                            out++
                            m.writeSampleData(track,buf,info)
                        }
                        enc.releaseOutputBuffer(i,false)
                        if((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)return
                    }
                }
            }
            val total=(pr.dur*pr.fps/1000L).toInt().coerceAtLeast(1)
            for(i in 0 until total){
                val c=sf.lockCanvas(null)
                Draw.frame(c,pr,i*1000L/pr.fps)
                sf.unlockCanvasAndPost(c)
                drain(false)
                onP((i+1f)/total)
            }
            enc.signalEndOfInputStream()
            drain(true)
            ok=true
        }finally{
            runCatching{enc.stop()}
            runCatching{enc.release()}
            runCatching{surf?.release()}
            if(started)runCatching{mux?.stop()}
            runCatching{mux?.release()}
            runCatching{pfd.close()}
            if(ok){
                val u=ContentValues().apply{put(MediaStore.Video.Media.IS_PENDING,0)}
                res.update(uri,u,null,null)
            }else{
                res.delete(uri,null,null)
            }
        }
    }
}
