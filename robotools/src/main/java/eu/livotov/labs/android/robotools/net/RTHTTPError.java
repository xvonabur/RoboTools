package eu.livotov.labs.android.robotools.net;

import android.text.TextUtils;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created with IntelliJ IDEA.
 * User: dlivotov
 * Date: 9/14/12
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class RTHTTPError extends RuntimeException
{

    private int statusCode;
    private String statusText;
    private String responseBody;
    private String protocolVersion;

    public RTHTTPError(Throwable cause)
    {
        super(cause);
        statusCode = ErrorCodes.InternalApplicationError;
        statusText = null;
        protocolVersion = null;
    }

    public RTHTTPError(Response rsp, final String responseBody)
    {
        statusCode = rsp.code();
        statusText = rsp.message();
        ResponseBody respBody = rsp.body();

        if (TextUtils.isEmpty(responseBody))
        {
            try
            {
                this.responseBody = respBody != null ? respBody.string() : "";
            } catch (Throwable err)
            {
            }
        } else
        {
            this.responseBody = responseBody;
        }

        protocolVersion = rsp.protocol().toString();
    }

    public String getMessage()
    {
        if (statusCode != ErrorCodes.InternalApplicationError)
        {
            return (statusText == null || TextUtils.isEmpty(statusText)) ? ("HTTP Error: " + statusCode) : statusText;
        } else
        {
            return super.getMessage();
        }
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getStatusText()
    {
        return statusText;
    }

    public String getProtocolVersion()
    {
        return protocolVersion;
    }

    public String getLocalizedMessage()
    {
        if (statusCode != ErrorCodes.InternalApplicationError)
        {
            return getMessage();
        } else
        {
            return super.getLocalizedMessage();
        }
    }

    public String getResponseBody()
    {
        return responseBody;
    }

    public static class ErrorCodes
    {

        public final static int InternalApplicationError = -65535;
    }
}
