package eu.livotov.labs.android.robotools.api;

import eu.livotov.labs.android.robotools.net.RTHTTPError;

import java.io.IOException;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 29.01.13
 */
public class RTApiError extends RuntimeException
{

    private int errorCode;
    private String responseBody;

    public RTApiError(Throwable internal)
    {
        super("Internal application error: " + internal.getMessage(), internal);
        analyzeAndSetErrorCode();
    }

    public RTApiError(int bawErrorCode)
    {
        super((bawErrorCode < ErrorCodes.InternalError ? "API error: " : "Error: ") + bawErrorCode);
        errorCode = bawErrorCode;
    }

    public RTApiError(int bawErrorCode, final String details)
    {
        super((bawErrorCode < ErrorCodes.InternalError ? "API error: " : "Error: ") + bawErrorCode + ", " + details);
        errorCode = bawErrorCode;
    }

    public RTApiError(int bawErrorCode, final String details, String responseBody)
    {
        this(bawErrorCode, details);
        this.responseBody = responseBody;
    }

    public RTApiError(RTHTTPError httpError)
    {
        super(httpError.getMessage(), httpError);
        this.responseBody = httpError.getResponseBody();
        analyzeAndSetErrorCode();
    }

    private void analyzeAndSetErrorCode()
    {
        errorCode = ErrorCodes.InternalError;
        Throwable cause = getCause();

        if (cause != null)
        {
            if (cause instanceof RTHTTPError)
            {
                if (cause.getCause() != null)
                {
                    Throwable rootCause = cause.getCause();
                    if (rootCause instanceof IOException)
                    {
                        errorCode = ErrorCodes.NetworkError;
                        return;
                    }
                } else
                {
                    errorCode = ((RTHTTPError) cause).getStatusCode();
                }
            }

            if (cause instanceof IOException)
            {
                errorCode = ErrorCodes.NetworkError;
                return;
            }
        }
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }


    public int getErrorCode()
    {
        return errorCode;
    }

    public boolean isInternalError()
    {
        return getErrorCode() >= ErrorCodes.InternalError;
    }

    public static class ErrorCodes
    {

        public final static int Unauthorized = 401;
        public final static int InternalError = 100000;
        public final static int NetworkError = InternalError + 1;
        public final static int JsonError = InternalError + 2;
        public final static int TooMuchRetries = InternalError + 3;
    }

    public static void rethrow(Object err)
    {
        if (err instanceof Throwable)
        {
            throw wrap((Throwable)err);
        }
    }

    public static RTApiError wrap(Throwable err)
    {
        if (err instanceof RTApiError)
        {
            return (RTApiError) err;
        } else
        {
            return new RTApiError(err);
        }
    }

    public String getResponseBody()
    {
        return responseBody;
    }
}
