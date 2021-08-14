import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * WebRequest ver 1
 * Created by :
 * mohammadrezataghdis@gmail.com
 * bahrani.mohammadreza@yahoo.com
 * Updated on 05/08/2019
 */


public class WebRequest
{
    public TimeUnit TimeoutUnit = TimeUnit.SECONDS;
    public int CallTimeout =60;
    public int ConnectTimeout = 60;
    public int ReadTimeout = 60;
    public int WriteTimeout = 60;
    public String Method;
    public String Url;
    public HashMap<String, String> Header;
    public boolean TrustEverySSL;
    public RequestResult RequestResult;
    public okhttp3.RequestBody RequestBody;
    private Handler mainHandler;
    private static TrustManager[] trustManagers;
    public WebRequest()
    {
        Header = new HashMap<>();
        RequestBody = null;
    }

    public WebRequest(String method, String url, RequestResult requestResult)
    {
        Method = method.toUpperCase();
        Url = url;
        RequestResult = requestResult;
        Header = new HashMap<>();
    }

    public void SetJson(String json)
    {
        try {
            RequestBody = okhttp3.RequestBody.create(MediaType.get("application/json; charset=utf-8"), json);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }



    public void SetJson(byte[] image)
    {
        if(image==null || image.length==0) return;
        RequestBody = okhttp3.RequestBody.create(MediaType.get("application/octet-stream"),image);
    }


    public void SetImage(Bitmap image, int quality)
    {
        try
        {
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, quality, data);
            //RequestBody = okhttp3.RequestBody.create(MediaType.get("image/jpeg"), data.toByteArray());
            RequestBody =  new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "kkkhkkk", okhttp3.RequestBody.create(MediaType.get("image/jpeg"), data.toByteArray()))
                .build();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    public void SetImage(Bitmap image)
    {
        SetImage(image, 60);
    }

    public void Run()
    {
        if (mainHandler != null && mainHandler.getLooper().getThread().isAlive()) return;
        HandlerThread handlerThread = new HandlerThread("WebRequest");
        handlerThread.start();
        mainHandler = new Handler(handlerThread.getLooper());
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                URL url;
                try
                {
                    url = new URL(Url);
                } catch (MalformedURLException e)
                {
                    e.printStackTrace();
                    if (RequestResult != null)
                        RequestResult.Result(new ResponseData(null, e));
                    return;
                }

                Response response = null;
                try
                {
                    Request.Builder requestBuilder = new Request.Builder();
                    requestBuilder.url(url);
                    requestBuilder.method(Method, RequestBody);
                    Set<String> keys = Header.keySet();
                    for (String key : keys) requestBuilder.addHeader(key, Header.get(key));


                    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
                    clientBuilder.callTimeout(CallTimeout, TimeoutUnit);
                    clientBuilder.connectTimeout(ConnectTimeout, TimeoutUnit);
                    clientBuilder.readTimeout(ReadTimeout, TimeoutUnit);
                    clientBuilder.writeTimeout(WriteTimeout, TimeoutUnit);



                    if (TrustEverySSL) SetSSL(clientBuilder);
                        allowAllSSL();
                    response = clientBuilder.build().newCall(requestBuilder.build()).execute();

                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                if (RequestResult != null)
                    RequestResult.Result(new ResponseData(response, null));
            }
        };
        mainHandler.post(run);
    }

    public interface RequestResult
    {
        void Result(ResponseData response);
    }

    public class ResponseData
    {
        public Response Response;
        public java.lang.Exception Exception;
        private String Text;
        private boolean HasText;
        private Bitmap Image;
        private boolean HasImage;

        public ResponseData(Response response, java.lang.Exception exception)
        {
            Response = response;
            Exception = exception;
        }

        public Bitmap GetImage()
        {
            if (HasImage) return Image;
            if (Response == null || Response.body() == null) return null;
            try
            {
                BufferedInputStream bis = new BufferedInputStream(Response.body().byteStream());
                return BitmapFactory.decodeStream(bis);
            } catch (java.lang.Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }

        public String GetText()
        {
            if (HasText) return Text;
            if (Response == null || Response.body() == null) return "";
            BufferedReader br = new BufferedReader(new InputStreamReader(Response.body().byteStream()));
            StringBuilder total = new StringBuilder();
            try
            {
                for (String line; (line = br.readLine()) != null; )
                {
                    total.append(line).append('\n');
                }
            } catch (java.lang.Exception e)
            {
                e.printStackTrace();
            }
            HasText = true;
            Text = total.toString();
            return Text;
        }
    }

    private static void SetSSL(OkHttpClient.Builder clientBuilder)
    {
        try
        {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager()
                    {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)// throws CertificateException
                        {
                        }
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)// throws CertificateException
                        {
                        }
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers()
                        {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            clientBuilder.hostnameVerifier(new HostnameVerifier()
            {
                @Override
                public boolean verify(String hostname, SSLSession session)
                {
                    return true;
                }
            });
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static class _FakeX509TrustManager implements
            javax.net.ssl.X509TrustManager {
        private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};

        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        public boolean isClientTrusted(X509Certificate[] chain) {
            return (true);
        }

        public boolean isServerTrusted(X509Certificate[] chain) {
            return (true);
        }

        public X509Certificate[] getAcceptedIssuers() {
            return (_AcceptedIssuers);
        }
    }

    public static void allowAllSSL() {

        javax.net.ssl.HttpsURLConnection
                .setDefaultHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });

        javax.net.ssl.SSLContext context = null;

        if (trustManagers == null) {
            trustManagers = new javax.net.ssl.TrustManager[] { new _FakeX509TrustManager() };
        }

        try {
            context = javax.net.ssl.SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new SecureRandom());
        } catch (Exception e) {
            Log.e("allowAllSSL", e.toString());
        }
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context
                .getSocketFactory());
    }




}

/*
* how to use class
*
*WebRequest r = new WebRequest("Method","Url", new RequestResult() {
                    @Override
                    public void Result(WebRequest.ResponseData response) {
                     if(response!= null && response.Response.code()/100 ==2 && response.Response.isSuccessful())
                        {
                        }
                        else
                        {
                        }
                    }
                });
                r.SetJson("String Json");
                r.Header.put("key", "value");
                r.TrustEverySSL = true;
                r.Run();
* */