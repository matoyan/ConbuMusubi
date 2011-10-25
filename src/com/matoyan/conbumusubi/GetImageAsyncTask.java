package com.matoyan.conbumusubi;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

import android.util.Log;

import android.os.AsyncTask;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;


public class GetImageAsyncTask extends AsyncTask<String, Void, Bitmap>
{
	  private Context m_context;
	  private ImageView m_image_view;

	  public GetImageAsyncTask (Context context, ImageView image)
	  {
	    this.m_context = context;
	    this.m_image_view = image;
	  }

	  @Override
	    protected void onPreExecute ()
	    {
	      /*
	      // バックグラウンドの処理前にUIスレッドでダイアログ表示
	      m_progress = new ProgressDialog (this.m_context);

	      //m_progress.setMessage (getResources().getText(R.string.data_loading));
	      m_progress.setIndeterminate (true);
	      m_progress.show ();
	        */
	    }

	  @Override
	    protected Bitmap doInBackground (String... params)
	    {
	      synchronized (m_context)
	      {
	        try
	        {
	          //キャッシュより画像データを取得
	          Bitmap image = ImageCache.get (params[0]);
	          if (image == null)
	          {
	            //キャッシュにデータが存在しない場合はwebより画像データを取得
	            URL image_url = new URL (params[0]);
	            InputStream is;
	            is = image_url.openStream ();
	            image = BitmapFactory.decodeStream (is);

	            //取得した画像データをキャッシュに保持
	            ImageCache.set (params[0], image);
	          }

	          return image;
	        }
	        catch (MalformedURLException e)
	        {
	          Log.d ("GetImageAsyncTask", "Failed AsyncImageGetTask MalformedURLException = " + e.getMessage ());
	          return null;
	        }
	        catch (IOException e)
	        {
	          Log.d ("GetImageAsyncTask", "Failed AsyncImageGetTask IOException = " + e.getMessage ());
	          return null;
	        }
	      }
	    }

	  @Override
	    protected void onPostExecute (Bitmap result)
	    {
	      // 処理中ダイアログをクローズ
	      //m_progress.dismiss();

	      m_image_view.setImageBitmap (result);
	    }
}