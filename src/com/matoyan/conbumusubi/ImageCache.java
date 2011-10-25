package com.matoyan.conbumusubi;

import android.graphics.Bitmap;
import java.util.HashMap;

public class ImageCache
{
  private static HashMap<String, Bitmap> cache = new HashMap<String, Bitmap> ();

  public static Bitmap get (String key)
  {
    if (cache.containsKey (key))
      return cache.get (key);

    return null;
  }

  public static void set (String key, Bitmap image)
  {
    cache.put (key, image);
  }

  public static void clear ()
  {
    cache = null;
    cache = new HashMap<String,Bitmap> ();
  }
}