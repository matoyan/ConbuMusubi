package com.matoyan.conbumusubi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.multiplayer.FeedRenderable;
import mobisocial.socialkit.musubi.multiplayer.TurnBasedMultiplayer;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.MemObj;
import mobisocial.socialkit.musubi.Musubi;

import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.utils.base64;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

// post data into Facebook / Musubi
public class PostDataActivity extends Activity implements OnClickListener{
	
	// UI
	private EditText postText = null;
	private Button launchCamera = null;
	private Button submitMulti = null;
	private Button submitFB = null;
	private Button submitMB = null;
	private Button getMB = null;

	// Image
	private Uri mImageUri;
	private File mTmpFile;
	private String uploadURL;
	private String thumbURL;

	// Server Control
	private int serverResponseCode=0;
	private String serverResponseMessage="";
	
	// Facebook
    private Facebook m_facebook;
    private AsyncFacebookRunner m_facebook_runner;

    // Musubi
    Musubi mMusubi;
    DbFeed mFeed;
    
    // PostData
	private String poststr;

    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post);
		
        // for camera
// 独自のカメラ機能を実装する場合
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        setContentView(new CameraView(this));

		
        // Initial setup for musubi
        if(!Musubi.isMusubiIntent(getIntent())){
            Toast.makeText(this, "Please launch from Musubi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

		mMusubi = Musubi.getInstance(this);
		mFeed = mMusubi.getFeedFromIntent(getIntent());
        mFeed.registerStateObserver(mFeedObserver);
        
        // UI
		postText = (EditText)findViewById( R.id.postText);
		launchCamera = (Button)findViewById( R.id.launchCamera);
		submitMulti = (Button)findViewById( R.id.submitMulti);
		submitFB = (Button)findViewById( R.id.submitFB);
		submitMB = (Button)findViewById( R.id.submitMB);
		getMB = (Button)findViewById( R.id.getMB);
		
		// Add listener to buttons
		launchCamera.setOnClickListener( this);
		submitMulti.setOnClickListener( this);
		submitFB.setOnClickListener( this);
		submitMB.setOnClickListener( this);
		getMB.setOnClickListener( this);

		// Initial setup for Facebook
		connect_facebook ();

	}

	@Override
	public void onClick(View v) {
		int vid = v.getId();
		String pressed="";
		switch (vid) {
		// Multi
		case R.id.launchCamera:
			pressed = "Camera";
			break;
		// Multi
		case R.id.submitMulti:
			pressed = "Multi";
			break;
		// FB
		case R.id.submitFB:
			pressed = "Facebook";
			break;
		// MB
		case R.id.submitMB:
			pressed = "Musubi";
			break;
		case R.id.getMB:
			pressed = "getMB";
			break;
		}

		poststr = postText.getText().toString();
		Toast.makeText(this, poststr, Toast.LENGTH_SHORT).show();

		if(pressed.equals("Camera")) {
			launchCameraApp();
		}else if(pressed.equals("getMB")) {
//			getMB();
		}else{
			showDL(pressed);
		}
	};

	private void launchCameraApp(){
		// 標準のカメラアプリを利用する場合
		Intent intent = new Intent();
		intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
		mTmpFile = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures", "img_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
		// 第二引数は戻ってきたときの判別用の適当なint
		startActivityForResult(intent, 99);
	}

	private void showDL(final String pressed){
		
		// 確認ダイアログの表示
		AlertDialog.Builder builder = new AlertDialog.Builder( this);
		// アイコン設定
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		// タイトル設定
		builder.setTitle("Are you sure you want to post data?");
		// OKボタン設定
		builder.setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
//				Toast toast = Toast.makeText(PostDataActivity.this, R.string.okpress, Toast.LENGTH_SHORT);
//				toast.show();
		        
				// upload to Server
				if(mTmpFile.isFile()){
					Log.d ("ConbuMusubi", "imagePath" + mTmpFile.getAbsolutePath());
					// uploadToMyServer(mTmpFile.getAbsolutePath());
					try {
						uploadToMB();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	  
					Log.d ("ConbuMusubi", "serverResponseCode " + serverResponseCode);
					Log.d ("ConbuMusubi", "serverResponseMessage " + serverResponseMessage);
					
					uploadURL = "http://mtrecimg.com/musubi/data/"+mTmpFile.getName();
					thumbURL = "http://mtrecimg.com/musubi/imgresize.php?maxsize=200&fname="+mTmpFile.getName();
				}
		        if (pressed.equals("Facebook")){
					postFB();
				}else if(pressed.equals("Musubi")) {
					postMB();
				}else if(pressed.equals("Multi")) {
					postMulti();
				}
				Log.d("CombuMusubiDBG", "POSTFIN==================================");
			}
		});
		// キャンセルボタン設定
		builder.setNegativeButton( android.R.string.cancel, null);
		// ダイアログの表示
		builder.show();
	}

	// Multi---------------------------------------------------------------------------------------------
	public void postMulti(){
	    
		// check
		if(!m_facebook.isSessionValid()){
			Toast toast = Toast.makeText(PostDataActivity.this, "FB session is expired. Please login again.", Toast.LENGTH_SHORT);
			toast.show();
			return;
	    }

		// Musubi Post
	    try {
			mMusubi.getFeed().postObj(getFeedRenderableObj());
        } catch (JSONException e) {
        	Log.wtf("CombuMusubi", "Failed to post through Musubi", e);
        }
	    Bundle params = new Bundle();
	    params.putString("message", poststr);
	    
	    if(mTmpFile.isFile()){
		    params.putString("link", uploadURL);
//	    	params.putString("name", "Photo");
//	    	params.putString("caption", "CaptionText");
//	    	params.putString("description", "DescriptionText");
	    	params.putString("picture", thumbURL);
	    }

	    // FB Post
	    m_facebook_runner = new AsyncFacebookRunner (m_facebook);
	    m_facebook_runner.request("me/feed", params, "POST", new RequestListener() {
	        @Override
			public void onMalformedURLException(MalformedURLException e, final Object state) {}
	        @Override
			public void onIOException(IOException e, final Object state) {}
	        @Override
			public void onFileNotFoundException(FileNotFoundException e, final Object state) {}
	        @Override
			public void onFacebookError(FacebookError e, final Object state) {}
	        @Override
			public void onComplete(String response, final Object state) {
	        	Log.e("PostDataActivity",response);
	        }
	    }, new Object()); 

	    Toast.makeText(PostDataActivity.this, "Posting to your Wall...", Toast.LENGTH_SHORT).show();       
	}
	
	private Obj getFeedRenderableObj() throws JSONException {
		String msgstr = poststr;
	    if(mTmpFile.isFile()){
	    	msgstr = "<img src='"+thumbURL+"' /><br />"+poststr;
	    }
		return FeedRenderable.fromHtml(msgstr).getObj();
//		return FeedRenderable.fromB64Image(b64JImage); //?
	}
	private JSONObject getJSONObj() throws JSONException {
        JSONObject o = new JSONObject();
    	o.put("data", poststr);
    	return o;
	}
	
	private void uploadToMB() throws Exception{
		JSONObject jso = new JSONObject();
		jso.put("filename", mTmpFile.getName());
		
		// for test 
//		mTmpFile = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures", "small.jpg");
//		mTmpFile = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures", "mid.jpg");
		mTmpFile = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures", "large.jpg");
//		mTmpFile = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures", "img_1319504069753.jpg");

		jso.put("filedata", Base64.encodeToString(file2byte(mTmpFile), Base64.DEFAULT));
		MemObj mbj = new MemObj("imgdata", jso);
		
//		MemObj mbj = new MemObj("imgdata", jso, file2byte(mTmpFile));
		Log.d("CombuMusubiDBG", "1==================================");
		mMusubi.getFeed().postObj(mbj);
		Log.d("CombuMusubiDBG", "2==================================");
	}
	
	private void uploadToMyServer(String pathToOurFile){
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		DataInputStream inputStream = null;

		String urlServer = "http://mtrecimg.com/musubi/upload.php";
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1*1024*1024;

		try
		{
		FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );

		URL url = new URL(urlServer);
		connection = (HttpURLConnection) url.openConnection();

		// Allow Inputs & Outputs
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		// Enable POST method
		connection.setRequestMethod("POST");

		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

		outputStream = new DataOutputStream( connection.getOutputStream() );
		outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile +"\"" + lineEnd);
		outputStream.writeBytes(lineEnd);

		bytesAvailable = fileInputStream.available();
		bufferSize = Math.min(bytesAvailable, maxBufferSize);
		buffer = new byte[bufferSize];

		// Read file
		bytesRead = fileInputStream.read(buffer, 0, bufferSize);

		while (bytesRead > 0)
		{
		outputStream.write(buffer, 0, bufferSize);
		bytesAvailable = fileInputStream.available();
		bufferSize = Math.min(bytesAvailable, maxBufferSize);
		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		}

		outputStream.writeBytes(lineEnd);
		outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		// Responses from the server (code and message)
		serverResponseCode = connection.getResponseCode();
		serverResponseMessage = connection.getResponseMessage();

		fileInputStream.close();
		outputStream.flush();
		outputStream.close();
		}
		catch (Exception ex)
		{
		//Exception handling
		}
	}

	// Musubi---------------------------------------------------------------------------------------------
	// get data from Musubi
	/*
	private void getMB(){
		String localUname = mMusubi.getFeed().getLocalUser().getName();
    	Log.d("CombuMusubi", "localUname:".concat(localUname));
		Set<User> RemoteUnames = mMusubi.getFeed().getRemoteUsers();
		
    	Log.d("CombuMusubi", "RemoteUnames--->");
        Iterator<User> ite = RemoteUnames.iterator();     //実際のプログラム中に、null判定を忘れずに  
        while(ite.hasNext()) {              //ループ  
        	User usr = ite.next();        //該当オブジェクト取得 
        	Log.d("CombuMusubi", usr.getName());
        }
    	Log.d("CombuMusubi", "<---RemoteUnames");
    	
    	Uri myuri = mMusubi.getFeed().getUri();
    	Log.d("CombuMusubi", "FeedUri:".concat(myuri.toString()));
    	
    	JSONObject jsobj = mMusubi.getFeed().getLatestObj();
    	if(jsobj != null){
    		Log.d("CombuMusubi", "LatestObj:".concat(jsobj.toString()));
    	}
	}
	*/
	// post message to Musubi
	public void postMB(){
        //mDungBeetle.getFeed().setApplicationState(getApplicationState(), getSnapshotText());
        try {
			mMusubi.getFeed().postObj(getFeedRenderableObj());
        } catch (JSONException e) {
        	Log.wtf("CombuMusubi", "Failed to post through Musubi", e);
        }
	}
//	private AppState getAppState() throws JSONException {
//        JSONObject o = new JSONObject();
//    	o.put("data", poststr);
//    	AppState as = new AppState(o);
//    	as.arg = "arg";
//    	as.thumbnailText = "thumbnailText";
//    	as.thumbnailImage = "http://yimg.dip.jp/themes/day_break/logo2.gif";
//    	as.thumbnailHtml = "<html><header></header><body><strong>thumbnailHtml</strong></body></html>";
//    	return as;
//	}
	
	private FeedObserver mFeedObserver = new FeedObserver() {
        @Override
        public void onUpdate(Obj stateObj){
    		Log.d("CombuMusubiDBG", "RCV==================================");

        	JSONObject state = stateObj.getJson();
        	
//			Toast.makeText(PostDataActivity.this, "Message Received", Toast.LENGTH_SHORT).show();

        	String getstr = "";
        	try {
        		if(state != null){
        			Log.d("ConbuMusubi", "Message Rcvd ---------: ".concat(stateObj.getType()));
        			
        			if(stateObj.getType().equals("imgdata")){
            			Toast.makeText(PostDataActivity.this, "ImageData Received!", Toast.LENGTH_SHORT).show();
        			}
        			if(!state.isNull("filename")){
//        				File fdata = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures/", "img_cp_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
//        				File fdata = new File(Environment.getExternalStorageDirectory()+"/MusubiPictures/", "myimg.jpg");
//        				String fpath = Environment.getExternalStorageDirectory()+"/MusubiPictures/myimg.jpg";
        				String fpath = Environment.getExternalStorageDirectory()+"/MusubiPictures/copy"+state.getString("filename");
        				Log.d("ConbuMusubi2", state.getString("filename"));
        				Log.d("ConbuMusubi2", fpath);
        				byte2file(Base64.decode(state.getString("filedata"), 0), fpath);
//        				byte2file(stateObj.getRaw(), fpath);
        			}
        			if(!state.isNull("data")){
        				getstr = state.getString("data");
        				Toast.makeText(PostDataActivity.this, "Message Received!: " + '"' + getstr + '"', Toast.LENGTH_SHORT).show();
        			}
        		}else{
        			
        			Log.d("ConbuMusubi", "Message Rcvd ---------: null");
        			Toast.makeText(PostDataActivity.this, "Message Received!: ... null", Toast.LENGTH_SHORT).show();
        			
        		}
			} catch (JSONException e) {
				getstr = "[ERROR] Failed to get data";
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
    };

	// FB---------------------------------------------------------------------------------------------
	// post message to Facebook
	public void postFB(){
	    if(!m_facebook.isSessionValid()){
			Toast toast = Toast.makeText(PostDataActivity.this, "FB session is expired. Please login again.", Toast.LENGTH_SHORT);
			toast.show();
			return;
	    }
	    Bundle params = new Bundle();
	    params.putString("message", poststr);
	    if(mTmpFile.isFile()){
		    params.putString("link", uploadURL);
//	    	params.putString("name", "Photo");
//	    	params.putString("caption", "CaptionText");
//	    	params.putString("description", "DescriptionText");
	    	params.putString("picture", uploadURL);
	    }
	    
	    m_facebook_runner = new AsyncFacebookRunner (m_facebook);
	    m_facebook_runner.request("me/feed", params, "POST", new RequestListener() {
	        @Override
			public void onMalformedURLException(MalformedURLException e, final Object state) {}
	        @Override
			public void onIOException(IOException e, final Object state) {}
	        @Override
			public void onFileNotFoundException(FileNotFoundException e, final Object state) {}
	        @Override
			public void onFacebookError(FacebookError e, final Object state) {}
	        @Override
			public void onComplete(String response, final Object state) {
	        	Log.e("PostDataActivity",response);
	        }
	    }, new Object()); 

	    Toast.makeText(PostDataActivity.this, "Posting to your Wall...", Toast.LENGTH_SHORT).show();       
	}

	
    // FB connect
    private void connect_facebook ()
    {
   	  Toast.makeText(PostDataActivity.this, "connecting", Toast.LENGTH_SHORT).show();       
      m_facebook = new Facebook ("177064775705380");
      if(!m_facebook.isSessionValid()){
    	  m_facebook.authorize (this, new String[] {"email", "publish_stream"}, new LoginListener ());
      }
    }
    
    // FB login
    public class LoginListener implements DialogListener
    {
      @Override
        public void onComplete (Bundle values)
        {
    	  Toast.makeText(PostDataActivity.this, "FB login Completed.", Toast.LENGTH_SHORT).show();       
//          m_facebook_runner = new AsyncFacebookRunner (m_facebook);
//          m_facebook_runner.request ("me/friends", new FriendsRequestListener ());
        }

      @Override
        public void onFacebookError (FacebookError e)
        {
          Log.e ("Facebook", "authorize : Facebook Error:" + e.getMessage ());
        }

      @Override
        public void onError (DialogError e)
        {
          Log.e ("Facebook", "authorize : Error:" + e.getMessage ());
        }

      @Override
        public void onCancel () {}
    }

    // callback
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data)
    {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == 99) {
          // ImageViewに表示するなら
          ImageView imageView = (ImageView) findViewById(R.id.imgpreview);
//          imageView.setImageURI(mImageUri);
          
          Bitmap myBitmap = BitmapFactory.decodeFile(mTmpFile.getAbsolutePath());
          imageView.setImageBitmap(myBitmap);
              	  
      }else{
    	  m_facebook.authorizeCallback (requestCode, resultCode, data);
      }
    }
 	
    //file -> byte[]
    private byte[] file2byte(File FileData) throws Exception {
        int size;
        byte[] w = new byte[1024];
        FileInputStream fin=null;
        ByteArrayOutputStream out=null;
        try {
            fin=new FileInputStream(FileData);
            out=new ByteArrayOutputStream();
            while (true) {
                size=fin.read(w);
                if (size<=0) break;
                out.write(w,0,size);
            }
            fin.close();
            out.close();
            return out.toByteArray();
        } catch (Exception e) {
            try {
                if (fin!=null) fin.close();
                if (out!=null) out.close();
            } catch (Exception e2) {
            }
            throw e;
        }
    }
    // byte[] -> file
    private void byte2file(byte[] w, String file) 
        throws Exception {
        FileOutputStream fos=null;
        try {
            fos=new FileOutputStream(file);
            fos.write(w);
            fos.close();
        } catch (Exception e) {
            if (fos!=null) fos.close();
            throw e;
        }
    }
}
