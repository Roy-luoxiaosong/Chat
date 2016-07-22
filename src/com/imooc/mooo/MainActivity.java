package com.imooc.mooo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;



import com.baidu.speech.VoiceRecognitionService;

import com.imooc.mooo.bean.ChatMessage;
import com.imooc.mooo.bean.ChatMessage.Type;
import com.imooc.mooo.utils.HttpUtils;

public class MainActivity extends Activity implements RecognitionListener
{
	
    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    
    private static final String TAG = "Sdk2Api";
    private static final int REQUEST_UI = 1;

	private ListView mMsgs;
	private ChatMessageAdapter mAdapter;
	private List<ChatMessage> mDatas;

	private EditText mInputMsg;
	private Button mSendMsg;
	private Button mVoice;

	private Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			// �ȴ����գ����߳�������ݵķ���
			ChatMessage fromMessge = (ChatMessage) msg.obj;
			mDatas.add(fromMessge);
			mAdapter.notifyDataSetChanged();
			mMsgs.setSelection(mDatas.size()-1);
		};

	};
	private SpeechRecognizer speechRecognizer;
	 private int status = STATUS_None;
	    //private TextView txtResult;
	    private long speechEndTime = -1;
	    private static final int EVENT_ERROR = 11;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));
		speechRecognizer.setRecognitionListener(this);
		initView();
		initDatas();
		// ��ʼ���¼�
		initListener();
	}

	

	private void initListener()
	{
		mSendMsg.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final String toMsg = mInputMsg.getText().toString();
				if (TextUtils.isEmpty(toMsg))
				{
					Toast.makeText(MainActivity.this, "������Ϣ����Ϊ�գ�",
							Toast.LENGTH_SHORT).show();
					return;
				}
				
				ChatMessage toMessage = new ChatMessage();
				toMessage.setDate(new Date());
				toMessage.setMsg(toMsg);
				toMessage.setType(Type.OUTCOMING);
				mDatas.add(toMessage);
				mAdapter.notifyDataSetChanged();
				mMsgs.setSelection(mDatas.size()-1);
				
				mInputMsg.setText("");
				
				new Thread()
				{
					public void run()
					{
						ChatMessage fromMessage = HttpUtils.sendMessage(toMsg);
						Message m = Message.obtain();
						m.obj = fromMessage;
						mHandler.sendMessage(m);
					};
				}.start();

			}
		});
		mVoice.setOnClickListener(new OnClickListener(
				) {
			
			@Override
			public void onClick(View v) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                boolean api = sp.getBoolean("api", false);
                if (api) {
                    switch (status) {
                        case STATUS_None:
                            start();
                            //btn.setText("ȡ��");
                            status = STATUS_WaitingReady;
                            break;
                        case STATUS_WaitingReady:
                            cancel();
                            status = STATUS_None;
                            //btn.setText("��ʼ");
                            break;
                        case STATUS_Ready:
                            cancel();
                            status = STATUS_None;
                            //btn.setText("��ʼ");
                            break;
                        case STATUS_Speaking:
                            stop();
                            status = STATUS_Recognition;
                           // btn.setText("ʶ����");
                            break;
                        case STATUS_Recognition:
                            cancel();
                            status = STATUS_None;
                            //btn.setText("��ʼ");
                            break;
                    }
                } else {
                    start();
                }
				
			}
		});
	}

	private void initDatas()
	{
		mDatas = new ArrayList<ChatMessage>();
		mDatas.add(new ChatMessage("������ã�RoyΪ������", Type.INCOMING, new Date()));
		mAdapter = new ChatMessageAdapter(this, mDatas);
		mMsgs.setAdapter(mAdapter);
	}

	private void initView()
	{
		mMsgs = (ListView) findViewById(R.id.id_listview_msgs);
		mInputMsg = (EditText) findViewById(R.id.id_input_msg);
		mSendMsg = (Button) findViewById(R.id.id_send_msg);
		mVoice  = (Button)findViewById(R.id.id_voice_btn);
	}
	@Override
    protected void onDestroy() {
        speechRecognizer.destroy();
        super.onDestroy();
    }
	 @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        super.onActivityResult(requestCode, resultCode, data);
	        if (resultCode == RESULT_OK) {
	            onResults(data.getExtras());
	        }
	    }
	 
	 public void bindParams(Intent intent) {
	        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
	        if (sp.getBoolean("tips_sound", true)) {
	            intent.putExtra(Constant.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start);
	            intent.putExtra(Constant.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);
	            intent.putExtra(Constant.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
	            intent.putExtra(Constant.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);
	            intent.putExtra(Constant.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);
	        }
	        if (sp.contains(Constant.EXTRA_INFILE)) {
	            String tmp = sp.getString(Constant.EXTRA_INFILE, "").replaceAll(",.*", "").trim();
	            intent.putExtra(Constant.EXTRA_INFILE, tmp);
	        }
	        if (sp.getBoolean(Constant.EXTRA_OUTFILE, false)) {
	            intent.putExtra(Constant.EXTRA_OUTFILE, "sdcard/outfile.pcm");
	        }
	        if (sp.contains(Constant.EXTRA_SAMPLE)) {
	            String tmp = sp.getString(Constant.EXTRA_SAMPLE, "").replaceAll(",.*", "").trim();
	            if (null != tmp && !"".equals(tmp)) {
	                intent.putExtra(Constant.EXTRA_SAMPLE, Integer.parseInt(tmp));
	            }
	        }
	        if (sp.contains(Constant.EXTRA_LANGUAGE)) {
	            String tmp = sp.getString(Constant.EXTRA_LANGUAGE, "").replaceAll(",.*", "").trim();
	            if (null != tmp && !"".equals(tmp)) {
	                intent.putExtra(Constant.EXTRA_LANGUAGE, tmp);
	            }
	        }
	        if (sp.contains(Constant.EXTRA_NLU)) {
	            String tmp = sp.getString(Constant.EXTRA_NLU, "").replaceAll(",.*", "").trim();
	            if (null != tmp && !"".equals(tmp)) {
	                intent.putExtra(Constant.EXTRA_NLU, tmp);
	            }
	        }

	        if (sp.contains(Constant.EXTRA_VAD)) {
	            String tmp = sp.getString(Constant.EXTRA_VAD, "").replaceAll(",.*", "").trim();
	            if (null != tmp && !"".equals(tmp)) {
	                intent.putExtra(Constant.EXTRA_VAD, tmp);
	            }
	        }
	        String prop = null;
	        if (sp.contains(Constant.EXTRA_PROP)) {
	            String tmp = sp.getString(Constant.EXTRA_PROP, "").replaceAll(",.*", "").trim();
	            if (null != tmp && !"".equals(tmp)) {
	                intent.putExtra(Constant.EXTRA_PROP, Integer.parseInt(tmp));
	                prop = tmp;
	            }
	        }

	        // offline asr
	        {
	            intent.putExtra(Constant.EXTRA_OFFLINE_ASR_BASE_FILE_PATH, "/sdcard/easr/s_1");
	            intent.putExtra(Constant.EXTRA_LICENSE_FILE_PATH, "/sdcard/easr/license-tmp-20150530.txt");
	            if (null != prop) {
	                int propInt = Integer.parseInt(prop);
	                if (propInt == 10060) {
	                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_Navi");
	                } else if (propInt == 20000) {
	                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_InputMethod");
	                }
	            }
	            intent.putExtra(Constant.EXTRA_OFFLINE_SLOT_DATA, buildTestSlotData());
	        }
	    }
	 
	 
	 private String buildTestSlotData() {
	        JSONObject slotData = new JSONObject();
	        JSONArray name = new JSONArray().put("��ӿȪ").put("������");
	        JSONArray song = new JSONArray().put("������").put("����ѩ");
	        JSONArray artist = new JSONArray().put("�ܽ���").put("������");
	        JSONArray app = new JSONArray().put("�ֻ��ٶ�").put("�ٶȵ�ͼ");
	        JSONArray usercommand = new JSONArray().put("�ص�").put("����");
	        try {
	            slotData.put(Constant.EXTRA_OFFLINE_SLOT_NAME, name);
	            slotData.put(Constant.EXTRA_OFFLINE_SLOT_SONG, song);
	            slotData.put(Constant.EXTRA_OFFLINE_SLOT_ARTIST, artist);
	            slotData.put(Constant.EXTRA_OFFLINE_SLOT_APP, app);
	            slotData.put(Constant.EXTRA_OFFLINE_SLOT_USERCOMMAND, usercommand);
	        } catch (JSONException e) {

	        }
	        return slotData.toString();
	    }
	 
	 

	    private void start() {
	        //txtLog.setText("");
	        print("����ˡ���ʼ��");
	        Intent intent = new Intent();
	        bindParams(intent);
	        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
	        {

	            String args = sp.getString("args", "");
	            if (null != args) {
	                print("��������" + args);
	                intent.putExtra("args", args);
	            }
	        }
	        boolean api = sp.getBoolean("api", false);
	        if (api) {
	            speechEndTime = -1;
	            speechRecognizer.startListening(intent);
	        } else {
	            intent.setAction("com.baidu.action.RECOGNIZE_SPEECH");
	            startActivityForResult(intent, REQUEST_UI);
	        }

	        //txtResult.setText("");
	    }
	    
	    
	    private void stop() {
	        speechRecognizer.stopListening();
	        print("����ˡ�˵���ˡ�");
	    }

	    private void cancel() {
	        speechRecognizer.cancel();
	        status = STATUS_None;
	        print("����ˡ�ȡ����");
	    }

	@Override
	public void onBeginningOfSpeech() {
		// TODO Auto-generated method stub
		status = STATUS_Speaking;
        //btn.setText("˵����");
        print("��⵽�û����Ѿ���ʼ˵��");
		
	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEndOfSpeech() {
		// TODO Auto-generated method stub
		 speechEndTime = System.currentTimeMillis();
	        status = STATUS_Recognition;
	        print("��⵽�û����Ѿ�ֹͣ˵��");
	        //btn.setText("ʶ����");
		
	}

	@Override
	public void onError(int error) {
		// TODO Auto-generated method stub
		 status = STATUS_None;
	        StringBuilder sb = new StringBuilder();
	        switch (error) {
	            case SpeechRecognizer.ERROR_AUDIO:
	                sb.append("��Ƶ����");
	                break;
	            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
	                sb.append("û����������");
	                break;
	            case SpeechRecognizer.ERROR_CLIENT:
	                sb.append("�����ͻ��˴���");
	                break;
	            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
	                sb.append("Ȩ�޲���");
	                break;
	            case SpeechRecognizer.ERROR_NETWORK:
	                sb.append("��������");
	                break;
	            case SpeechRecognizer.ERROR_NO_MATCH:
	                sb.append("û��ƥ���ʶ����");
	                break;
	            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
	                sb.append("����æ");
	                break;
	            case SpeechRecognizer.ERROR_SERVER:
	                sb.append("����˴���");
	                break;
	            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
	                sb.append("���ӳ�ʱ");
	                break;
	        }
	        sb.append(":" + error);
	        print("ʶ��ʧ�ܣ�" + sb.toString());
	        //btn.setText("��ʼ");
		
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub
		 switch (eventType) {
         case EVENT_ERROR:
             String reason = params.get("reason") + "";
             print("EVENT_ERROR, " + reason);
             break;
         case VoiceRecognitionService.EVENT_ENGINE_SWITCH:
             int type = params.getInt("engine_type");
             print("*�����л���" + (type == 0 ? "����" : "����"));
             break;
     }
		
	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub
		ArrayList<String> nbest = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest.size() > 0) {
            print("~��ʱʶ������" + Arrays.toString(nbest.toArray(new String[0])));
            mInputMsg.setText(nbest.get(0));
        }
		
	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		// TODO Auto-generated method stub
		status = STATUS_Ready;
        print("׼�����������Կ�ʼ˵��");
		
	}

	@Override
	public void onResults(Bundle results) {
		// TODO Auto-generated method stub
		long end2finish = System.currentTimeMillis() - speechEndTime;
        status = STATUS_None;
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        print("ʶ��ɹ���" + Arrays.toString(nbest.toArray(new String[nbest.size()])));
        String json_res = results.getString("origin_result");
        try {
            print("origin_result=\n" + new JSONObject(json_res).toString(4));
        } catch (Exception e) {
            print("origin_result=[warning: bad json]\n" + json_res);
        }
       // btn.setText("��ʼ");
        String strEnd2Finish = "";
        if (end2finish < 60 * 1000) {
            strEnd2Finish = "(waited " + end2finish + "ms)";
        }
        mInputMsg.setText(nbest.get(0) + strEnd2Finish);
		
	}

	@Override
	public void onRmsChanged(float rmsdB) {
		// TODO Auto-generated method stub
		
	}
	 private void print(String msg) {
	       // txtLog.append(msg + "\n");
	        //ScrollView sv = (ScrollView) txtLog.getParent();
	        //sv.smoothScrollTo(0, 1000000);
	        Log.d(TAG, "----" + msg);
	    }

}
