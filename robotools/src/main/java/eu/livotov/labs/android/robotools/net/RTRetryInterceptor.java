package eu.livotov.labs.android.robotools.net;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RTRetryInterceptor implements Interceptor {
    private int MAX_TRY_COUNT = 4;
    private int RETRY_BACKOFF_DELAY = 500;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // try the request
        Response response = null;
        int tryCount = 1;
        while (tryCount <= MAX_TRY_COUNT) {
            try {
                response = chain.proceed(request);
                break;
            } catch (Exception e) {
                if ("Canceled".equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                if (tryCount >= MAX_TRY_COUNT) {
                    throw e;
                }

                try {
                    Thread.sleep(RETRY_BACKOFF_DELAY * tryCount);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
                tryCount++;
            }
        }
        return response;
    }
}
