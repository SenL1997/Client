package cn.sencs.client;

import android.content.Context;

import android.util.Log;


import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import eu.amirs.JSON;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by sensj on 2017/12/13.
 */

public class MyVoice implements EventListener{
    private EventManager asr;

    private boolean logTime = true;

    private boolean enableOffline = true; // 测试离线命令词，需要改成true

    private Context mContext;
    private MainActivity mActivity;

    MyVoice(Context context){
        mContext=context;
        mActivity=(MainActivity)mContext;
        asr = EventManagerFactory.create(mContext, "asr");
        asr.registerListener(this);
        if (enableOffline) {
            loadOfflineEngine(); //测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
    }

    /**
     * 测试参数填在这里
     */
    void start() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String event = null;
        event = SpeechConstant.ASR_START; // 替换成测试的event

        if (enableOffline){
            params.put(SpeechConstant.DECODER, 2);
        }
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        //  params.put(SpeechConstant.NLU, "enable");
        // params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800);
        // params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        //  params.put(SpeechConstant.PROP ,20000);
        params.put(SpeechConstant.LANGUAGE,1536);
        // 请先使用如‘在线识别’界面测试和生成识别参数。 params同ActivityRecog类中myRecognizer.start(params);
        String json = null; //可以替换成自己的json
        json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json
        asr.send(event, json, null, 0, 0);
        printLog("输入参数：" + json);
    }

    void stop() {
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0); //
    }

    private void loadOfflineEngine() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.DECODER, 2);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
        asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
    }

    private void unloadOfflineEngine() {
        asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0); //
    }


    void close() {
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        if (enableOffline) {
            unloadOfflineEngine(); //测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {

        if(Objects.equals(name, "asr.partial")){
            try {
                JSON json = new JSON(params);
                String best_result = json.key("best_result").stringValue();
                String result_type = json.key("result_type").stringValue();
                Log.i("MyVoice", best_result + " " + result_type);
                mActivity.textInf.setText(best_result);
                switch (best_result) {
                    case "前进":
                        MyControler.bluetoothmsg = "w";
                        break;
                    case "后退":
                        MyControler.bluetoothmsg = "s";
                        break;
                    case "左转":
                        MyControler.bluetoothmsg = "a";
                        break;
                    case "右转":
                        MyControler.bluetoothmsg = "d";
                        break;
                    case "停止":
                        MyControler.bluetoothmsg = "x";
                        break;
                    case "王睿杰我是你爹":
                        MyControler.bluetoothmsg = "q";
                        break;
                    case "王瑞杰我是你爹":
                        MyControler.bluetoothmsg = "e";
                        break;
                }
            }
            catch (Exception e){

            }
        }


        String logTxt = "name: " + name;


        if (params != null && !params.isEmpty()) {
            logTxt += " ;params :" + params;
        }
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            if (params.contains("\"nlu_result\"")) {
                if (length > 0 && data.length > 0) {
                    logTxt += ", 语义解析结果：" + new String(data, offset, length);
                }
            }
        } else if (data != null) {
            logTxt += " ;data length=" + data.length;
        }
        printLog(logTxt);


    }




    private void printLog(String text) {
        if (logTime) {
            text += "  ;time=" + System.currentTimeMillis();
        }
        text += "\n";
        Log.i("MyVoice", text);
    }

}
