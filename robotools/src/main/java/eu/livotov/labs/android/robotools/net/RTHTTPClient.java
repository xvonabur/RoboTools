package eu.livotov.labs.android.robotools.net;

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

    public Response executeGetRequest(final String url)
    {
        try
        {
            OkHttpClient httpClient = createHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            return httpClient.newCall(request).execute();
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public Response executeGetRequest(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> params)
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
            return httpClient.newCall(request).execute();
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
        Response response = executeGetRequest(url);
        return loadHttpResponseToString(response, encoding);
    }

    public Response executePostRequest(final String url, final String contentType, final String content, RTPostParameter... headers)
    {
        return executePostRequest(url, contentType, "utf-8", content, headers);
    }

    public Response executePostRequest(final String url, final String contentType, final String encoding, final String content, RTPostParameter... headers)
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

            return client.newCall(request).execute();
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
        Response response = executePostRequest(url, contentType, encoding, content, headers);
        return loadHttpResponseToString(response, encoding);
    }

    public Response submitForm(final String url, Collection<RTPostParameter> headers, Collection<RTPostParameter> formFields)
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

            return client.newCall(request).execute();
        } catch (Throwable err)
        {
            throw new RTHTTPError(err);
        }
    }

    public String loadHttpResponseToString(Response response, final String encoding)
    {
        try
        {
            final String body = RTStreamUtil.streamToString(response.body().byteStream(), encoding, true);
            int statusCode = response.code();
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

    private OkHttpClient createHttpClient() {
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new RTRetryInterceptor());
        return httpClient;
    }
}
