import java.net.*;
import java.io.*;
public class NetworkProbe {
 public static void main(String[] args) {
  try {
   HttpURLConnection c=(HttpURLConnection)new URL(args[0]).openConnection();
   c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","IWatchYou");
   System.out.println("HTTP status="+c.getResponseCode());
   InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
   while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException("Response limit");out.write(b,0,n);}
   in.close();c.disconnect();System.out.println(out.toString("UTF-8"));
  } catch(Throwable t){t.printStackTrace(System.out);System.exit(1);}
 }
}
