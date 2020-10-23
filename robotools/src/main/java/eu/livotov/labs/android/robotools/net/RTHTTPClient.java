package eu.livotov.labs.android.robotools.net;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collection;

import eu.livotov.labs.android.robotools.io.RTStreamUtil;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 12.09.12
 */
public class RTHTTPClient
{

    public RTHTTPClient() {}

    public HttpResponse executeGetRequest(final String url)
    {
        try
        {
            OkHttpClient httpClient = createHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();

            return getLegacyHttpResponse(response);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public HttpResponse executeGetRequest(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
    {
        try
        {
            OkHttpClient httpClient = createHttpClient();
            StringBuilder finalUrl = new StringBuilder(url);

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

            Request.Builder requestBuilder = new Request.Builder()
                    .url(finalUrl.toString());

            for (RTPostParameter h : headers)
            {
                requestBuilder.addHeader(h.getName(), h.getValue());
            }

            Request request = requestBuilder.build();
            Response response = httpClient.newCall(request).execute();

            return getLegacyHttpResponse(response);
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
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
            OkHttpClient client = new OkHttpClient();
            String contentTypeWithEncoding = contentType + "; charset=" + encoding;
            RequestBody requestBody = RequestBody.create(MediaType.parse(contentTypeWithEncoding), content);

            Request.Builder requestBuilder = new Request.Builder().url(url);
            for (RTPostParameter header : headers) {
                requestBuilder.addHeader(header.getName(), header.getValue());
            }
            Request request = requestBuilder.post(requestBody).build();

            Response response = client.newCall(request).execute();

            return getLegacyHttpResponse(response);
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

    public HttpResponse submitForm(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> formFields)
    {
        try
        {
            OkHttpClient client = new OkHttpClient();

            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (RTPostParameter field : formFields)
            {
                formBodyBuilder.addEncoded(field.getName(), field.getValue());
            }
            RequestBody formBody = formBodyBuilder.build();

            Request.Builder requestBuilder = new Request.Builder().url(url);
            for (RTPostParameter header : headers)
            {
                requestBuilder.addHeader(header.getName(), header.getValue());
            }
            Request request = requestBuilder.post(formBody).build();

            Response response = client.newCall(request).execute();
            return getLegacyHttpResponse(response);
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

    public HttpResponse getLegacyHttpResponse(Response response) {
        HttpResponseFactory factory = new DefaultHttpResponseFactory();

        if (response.body() != null) {
            InputStream bodyStream = response.body().byteStream();

            BasicStatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, response.code(), null);
            HttpResponse legacyResponse = factory.newHttpResponse(statusLine, null);
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(bodyStream);

            legacyResponse.setEntity(entity);

            return legacyResponse;
        } else {
            BasicStatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Empty API response");
            return factory.newHttpResponse(statusLine, null);
        }

    }

    private OkHttpClient createHttpClient() {
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new RTRetryInterceptor());
        return httpClient;
    }
}
