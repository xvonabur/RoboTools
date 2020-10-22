package eu.livotov.labs.android.robotools.net;

import android.text.TextUtils;
import eu.livotov.labs.android.robotools.io.RTStreamUtil;
import eu.livotov.labs.android.robotools.net.method.HttpDeleteWithBody;
import eu.livotov.labs.android.robotools.net.multipart.*;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 12.09.12
 */
public class RTHTTPClient implements HttpRequestRetryHandler
{

    private DefaultHttpClient http;

    private RTHTTPClientConfiguration configuration = new RTHTTPClientConfiguration();


    public RTHTTPClient()
    {
        this(false);
    }

    public RTHTTPClient(boolean allowSelfSignedCerts)
    {
        configuration.setAllowSelfSignedCerts(allowSelfSignedCerts);
    }

    public RTHTTPClientConfiguration getConfiguration()
    {
        return configuration;
    }

    public DefaultHttpClient getRawHttpClient()
    {
        return http;
    }

    public HttpResponse executeGetRequest(final String url)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            HttpGet get = new HttpGet(url);
            return http.execute(get);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse executeGetRequest(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            StringBuffer finalUrl = new StringBuffer(url);

            int index = 1;
            if (!url.contains("?"))
            {
                index = 0;
                finalUrl.append("?");
            }

            for (RTPostParameter p : params)
            {
                if (index != 0)
                {
                    finalUrl.append("&");
                }

                finalUrl.append(String.format("%s=%s", p.getName(), URLEncoder.encode(p.getValue(), "utf-8")));
                index++;
            }

            HttpGet get = new HttpGet(finalUrl.toString());

            for (RTPostParameter h : headers)
            {
                get.addHeader(h.getName(), h.getValue());
            }

            return http.execute(get);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse executePutRequest(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
    {
        return executePutRequest(url, null, null, null, headers, params);
    }

    public HttpResponse executePutRequest(final String url, final String contentType, final String encoding, final String body, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
    {
        try
        {
            final String finalEncoding = TextUtils.isEmpty(encoding) ? "utf-8" : encoding;
            final String finalContentType = TextUtils.isEmpty(contentType) ? "application/binary" : contentType;

            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            StringBuffer finalUrl = new StringBuffer(url);

            int index = 1;
            if (!url.contains("?"))
            {
                index = 0;
                finalUrl.append("?");
            }

            HttpPut put = new HttpPut(finalUrl.toString());

            for (RTPostParameter h : headers)
            {
                put.addHeader(h.getName(), h.getValue());
            }

            if (!TextUtils.isEmpty(body))
            {
                StringEntity putEntity = new StringEntity(body, encoding);
                putEntity.setContentType(finalContentType + "; charset=" + finalEncoding);
                putEntity.setContentEncoding(finalEncoding);
                put.addHeader("Content-type", finalContentType);
                put.setEntity(putEntity);
            } else
            {
                StringPart[] parts = new StringPart[params.size()];
                int i = 0;

                for (RTPostParameter p : params)
                {
                    parts[i] = new StringPart(p.getName(), p.getValue());
                    i++;
                }

                MultipartEntity mp = new MultipartEntity(parts);
                put.setEntity(mp);
            }

            return http.execute(put);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse executeDeleteRequest(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
    {
        return executeDeleteRequest(url, null, null, null, headers, params);
    }

    public HttpResponse executeDeleteRequest(final String url, final String contentType, final String encoding, final String body, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
    {
        try
        {
            final String finalEncoding = TextUtils.isEmpty(encoding) ? "utf-8" : encoding;
            final String finalContentType = TextUtils.isEmpty(contentType) ? "application/binary" : contentType;

            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            StringBuffer finalUrl = new StringBuffer(url);

            int index = 1;
            if (!url.contains("?"))
            {
                index = 0;
                finalUrl.append("?");
            }

            for (RTPostParameter p : params)
            {
                if (index == 0)
                {
                    finalUrl.append("&");
                }

                finalUrl.append(String.format("%s=%s", p.getName(), URLEncoder.encode(p.getValue(), "utf-8")));
                index++;
            }

            HttpDeleteWithBody delete = new HttpDeleteWithBody(finalUrl.toString());

            for (RTPostParameter h : headers)
            {
                delete.addHeader(h.getName(), h.getValue());
            }

            if (!TextUtils.isEmpty(body))
            {
                StringEntity putEntity = new StringEntity(body, encoding);
                putEntity.setContentType(finalContentType + "; charset=" + finalEncoding);
                putEntity.setContentEncoding(finalEncoding);
                delete.addHeader("Content-type", finalContentType);
                delete.setEntity(putEntity);
            }

            return http.execute(delete);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse executeRaw(final HttpUriRequest request)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            return http.execute(request);
        } catch (Throwable e)
        {
            throw new RTHTTPError(e);
        }
    }

    public String executeGetRequestToString(final String url)
    {
        return executeGetRequestToString(url, "utf-8");
    }

    public String executeGetRequestToString(final String url, final String encoding)
    {
        HttpResponse response = executeGetRequest(url);
        return loadHttpResponseToString(response, encoding);
    }

    public HttpResponse executePostRequest(final String url, final String contentType, final String content, RTPostParameter... headers)
    {
        return executePostRequest(url, contentType, "utf-8", content, headers);
    }

    public HttpResponse executePostRequest(final String url, final String contentType, final String encoding, final String content, RTPostParameter... headers)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            HttpPost post = new HttpPost(url);

            StringEntity postEntity = new StringEntity(content, encoding);
            postEntity.setContentType(contentType + "; charset=" + encoding);
            postEntity.setContentEncoding("utf-8");

            if (headers != null)
            {
                for (RTPostParameter header : headers)
                {
                    post.addHeader(header.getName(), header.getValue());
                }
            }

            post.setEntity(postEntity);
            return http.execute(post);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public String executePostRequestToString(final String url, final String contentType, final String content, RTPostParameter... headers)
    {
        return executePostRequestToString(url, contentType, "utf-8", content, headers);
    }

    public String executePostRequestToString(final String url, final String contentType, final String encoding, final String content, RTPostParameter... headers)
    {
        HttpResponse response = executePostRequest(url, contentType, encoding, content, headers);
        return loadHttpResponseToString(response, encoding);
    }

    public HttpResponse submitMultipartForm(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> formFields, final String fileFeldName, File file)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            HttpPost httppost = new HttpPost(url);

            List<Part> parts = new ArrayList<Part>();

            for (RTPostParameter field : formFields)
            {
                parts.add(new StringPart(field.getName(), field.getValue(), "utf-8"));
            }

            FilePart pData = new FilePart(fileFeldName, file);
            pData.setTransferEncoding("8bit");
            parts.add(pData);

            MultipartEntity mpEntity = new MultipartEntity(parts.toArray(new Part[parts.size()]));
            httppost.setEntity(mpEntity);

            for (RTPostParameter header : headers)
            {
                httppost.addHeader(header.getName(), header.getValue());
            }

            return http.execute(httppost);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse submitMultipartFormWithProgressReporting(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> formFields, final String fileFeldName, File file, ProgressMultipartEntity.ProgressCallback callback)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            HttpPost httppost = new HttpPost(url);

            List<Part> parts = new ArrayList<Part>();

            for (RTPostParameter field : formFields)
            {
                parts.add(new StringPart(field.getName(), field.getValue(), "utf-8"));
            }

            FilePart pData = new FilePart(fileFeldName, file);
            pData.setTransferEncoding("8bit");
            parts.add(pData);

            ProgressMultipartEntity mpEntity = new ProgressMultipartEntity(parts.toArray(new Part[parts.size()]), callback);
            httppost.setEntity(mpEntity);

            for (RTPostParameter header : headers)
            {
                httppost.addHeader(header.getName(), header.getValue());
            }

            return http.execute(httppost);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse submitForm(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> formFields)
    {
        try
        {
            if (configuration.isDirty())
            {
                reconfigureHttpClient();
            }

            HttpPost httppost = new HttpPost(url);
            boolean hasAttachments = false;

            for (RTPostParameter field : formFields)
            {
                if (field.getAttachment() != null)
                {
                    hasAttachments = true;
                    break;
                }
            }

            if (hasAttachments)
            {
                List<Part> parts = new ArrayList<Part>();

                for (RTPostParameter field : formFields)
                {
                    if (field.getAttachment() != null)
                    {
                        File attachment = field.getAttachment();
                        if (attachment.exists() && attachment.length() > 0 && attachment.canRead())
                        {
                            FilePart filePart = new FilePart(field.getName(), attachment);
                            parts.add(filePart);
                        }
                    } else
                    {
                        parts.add(new StringPart(field.getName(), field.getValue(), "utf-8"));
                    }
                }

                MultipartEntity mpEntity = new MultipartEntity(parts.toArray(new Part[parts.size()]));
                httppost.setEntity(mpEntity);
            } else
            {
                httppost.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(formFields), "utf-8"));
            }

            for (RTPostParameter header : headers)
            {
                httppost.addHeader(header.getName(), header.getValue());
            }

            return http.execute(httppost);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public String loadHttpResponseToString(HttpResponse response, final String encoding)
    {
        try
        {
            final String body = RTStreamUtil.streamToString(response.getEntity().getContent(), encoding, true);
            response.getEntity().consumeContent();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201 || statusCode == 202
                    || statusCode == 203 || statusCode == 205 || statusCode == 206)
            {
                return body;
            } else
            {
                throw new RTHTTPError(response, body);
            }
        } catch (Throwable err)
        {
            if (err instanceof RTHTTPError)
            {
                throw (RTHTTPError) err;
            } else
            {
                throw new RTHTTPError(err);
            }
        }
    }

    protected void reconfigureHttpClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException
    {

        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        configureCookiePolicy(params);
        HttpClientParams.setRedirecting(params, configuration.isAllowRedirects());
        params.setParameter("http.protocol.expect-continue", false);
        HttpConnectionParams.setConnectionTimeout(params, configuration.getHttpConnectionTimeout());
        HttpConnectionParams.setSoTimeout(params, configuration.getHttpDataResponseTimeout());

        if (configuration.getSslSocketFactory() != null)
        {
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), configuration.getDefaultHttpPort()));
            registry.register(new Scheme("https", configuration.getSslSocketFactory(), configuration.getDefaultSslPort()));
            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            http = new DefaultHttpClient(ccm, params);
        } else if (configuration.isAllowSelfSignedCerts())
        {
            SSLContext context = SSLContext.getInstance("TLS");
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            org.apache.http.conn.ssl.SSLSocketFactory sf = new DummySslSocketFactory(trustStore);
            sf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), configuration.getDefaultHttpPort()));
            registry.register(new Scheme("https", sf, configuration.getDefaultSslPort()));
            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            http = new DefaultHttpClient(ccm, params);
        } else
        {
            http = new DefaultHttpClient(params);
        }

        http.setHttpRequestRetryHandler(this);

        if (configuration.isEnableGzipCompression())
        {
            http.addRequestInterceptor(new HttpRequestInterceptor()
            {
                public void process(final HttpRequest request, final HttpContext context) throws HttpException,
                        IOException
                {
                    if (!request.containsHeader("Accept-Encoding"))
                    {
                        request.addHeader("Accept-Encoding", "gzip");
                    }
                }

            });

            http.addResponseInterceptor(new HttpResponseInterceptor()
            {
                public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException
                {
                    HttpEntity entity = response.getEntity();
                    Header ceheader = entity.getContentEncoding();
                    if (ceheader != null)
                    {
                        HeaderElement[] codecs = ceheader.getElements();
                        for (int i = 0; i < codecs.length; i++)
                        {
                            if (codecs[i].getName().equalsIgnoreCase("gzip"))
                            {
                                response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }

            });
        }

        if (configuration.getCookieStore() != null)
        {
            http.setCookieStore(configuration.getCookieStore());
        }

        if (!TextUtils.isEmpty(configuration.getUserAgent()))
        {
            http.getParams().setParameter("http.useragent", configuration.getUserAgent());
        }

        http.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, configuration.isUseExpectContinue());

        configuration.clearDirtyFlag();
    }

    protected void configureCookiePolicy(HttpParams params)
    {
        HttpClientParams.setCookiePolicy(params, CookiePolicy.BEST_MATCH);
    }

    public boolean retryRequest(IOException e, int i, HttpContext httpContext)
    {
        if (configuration.getRequestRetryCount() == 0 || i > configuration.getRequestRetryCount())
        {
            return false;
        } else
        {
            return true;
        }
    }

    class DummySslSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory
    {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        public DummySslSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
                KeyStoreException, UnrecoverableKeyException
        {
            super(truststore);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            TrustManager tm = null;

            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    tm = trustManager;
                }
            }

            if (tm == null)
                throw new KeyManagementException("Cannot find X509TrustManager");

            sslContext.init(null, new TrustManager[]{tm}, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException
        {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException
        {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    class GzipDecompressingEntity extends HttpEntityWrapper
    {

        public GzipDecompressingEntity(final HttpEntity entity)
        {
            super(entity);
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException
        {

            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength()
        {
            return -1;
        }

    }
}
