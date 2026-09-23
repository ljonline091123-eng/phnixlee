package com.zhaocai.business.filez.util;

import javax.activation.MimetypesFileTypeMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @desciption MimeType工具类
 */
public class MimeTypeUtil
{
  public static final MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();
  static
  {
    try
    {
      InputStream is = MimeTypeUtil.class.getResourceAsStream("/concord.mime.types"); //$NON-NLS-1$
      BufferedReader reader = null;
      if (is != null)
      {
        try
        {
          reader = new BufferedReader(new InputStreamReader(is));
          String line;
          while ((line = reader.readLine()) != null)
          {
            int hashPos = line.indexOf('#');
            if (hashPos != -1)
            {
              line = line.substring(0, hashPos);
            }
            if (line.trim().length() > 0) {
              MIME_TYPE_MAP.addMimeTypes(line);
            }
          }
        }
        finally
        {
          if(reader != null)
          {
            reader.close();
          }
        }
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
}
