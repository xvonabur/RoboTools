package eu.livotov.labs.android.robotools.imaging;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import eu.livotov.labs.android.robotools.async.RTQueueExecutor;
import eu.livotov.labs.android.robotools.device.RTDevice;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;


/**
 * Now deprecated, use great Android-Universal-Image-Loader for such things !
 */
@Deprecated
public class RTAsyncImageLoader
{

    protected static final int DEFAULT_IMAGE_SIZE = 0;
    private static final String DRAWABLE_STUB_SCHEMA = "drawable://";

    protected int defaultImageDownscaledSize = DEFAULT_IMAGE_SIZE;
    protected final ExecutorService executor;
    protected LruCache<String, Bitmap> cache;
    protected Handler handler;
    protected File cacheDir = null;

    private int loadingStubResource;
    private int failoverStubResource;

    private ImageView.ScaleType finalImageScaleType = ImageView.ScaleType.CENTER_CROP;
    private ImageView.ScaleType loadingImageScaleType = ImageView.ScaleType.CENTER_INSIDE;
    private ImageView.ScaleType failoverImageScaleType = ImageView.ScaleType.CENTER_INSIDE;

    protected Context context;


    public RTAsyncImageLoader(Context ctx)
    {
        this(ctx, DEFAULT_IMAGE_SIZE);
    }

    public RTAsyncImageLoader(Context ctx, int defaultImageDownscaledSize)
    {
        this(ctx, 0, defaultImageDownscaledSize, 0);
    }

    public RTAsyncImageLoader(Context ctx, int maxCacheSizeKb, int defaultImageDownscaledSize, int numberOfWorkers)
    {
        this.defaultImageDownscaledSize = defaultImageDownscaledSize;
        this.cacheDir = ctx.getCacheDir();
        this.context = ctx;
        this.handler = new Handler();

        setMemoryCacheSizeKb(maxCacheSizeKb > 0 ? maxCacheSizeKb : (((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8));

        executor = RTQueueExecutor.create(numberOfWorkers > 0 ? numberOfWorkers : RTDevice.getCpuCoresCount() * 2);
    }

    public synchronized void setMemoryCacheSizeKb(final int kb)
    {
        if (cache != null)
        {
            cache.evictAll();
        }

        this.cache = new LruCache<String, Bitmap>(kb)
        {
            protected int sizeOf(final String key, final Bitmap value)
            {
                if (Build.VERSION.SDK_INT < 12)
                {
                    return (value.getRowBytes() * value.getHeight()) / 1024;
                } else
                {
                    return value.getByteCount() / 1024;
                }
            }
        };
    }

    public void setLoadingImageStubResource(final int res)
    {
        this.loadingStubResource = res;
    }

    public void setFailoverImageStubResource(final int res)
    {
        this.failoverStubResource = res;
    }

    public ImageView.ScaleType getFinalImageScaleType()
    {
        return finalImageScaleType;
    }

    public void setFinalImageScaleType(final ImageView.ScaleType finalImageScaleType)
    {
        this.finalImageScaleType = finalImageScaleType;
    }

    public ImageView.ScaleType getLoadingImageScaleType()
    {
        return loadingImageScaleType;
    }

    public void setLoadingImageScaleType(final ImageView.ScaleType loadingImageScaleType)
    {
        this.loadingImageScaleType = loadingImageScaleType;
    }

    public ImageView.ScaleType getFailoverImageScaleType()
    {
        return failoverImageScaleType;
    }

    public void setFailoverImageScaleType(final ImageView.ScaleType failoverImageScaleType)
    {
        this.failoverImageScaleType = failoverImageScaleType;
    }

    public void loadImage(ImageView view, String url, int width)
    {
        loadImage(view, url, width, null);
    }

    public void loadImage(ImageView view, String url)
    {
        loadImage(view, url, defaultImageDownscaledSize, null);
    }

    public void loadImage(ImageView view, String url, ImageLoadListener loadListener)
    {
        loadImage(view, url, defaultImageDownscaledSize, loadListener);
    }

    public void loadImage(ImageView view, String url, int maxSize, ImageLoadListener onLoadListener)
    {
        loadImage(view, url, maxSize, false, onLoadListener);
    }

    public void loadImage(ImageView view, String url, int maxSize, boolean cacheOnly, ImageLoadListener onLoadListener)
    {
        final String tag = UUID.randomUUID().toString();

        final int sz = maxSize >= 0 ? maxSize : defaultImageDownscaledSize;

        if (sz > 0 && hasMemoryCachedImage(url, sz))
        {
            loadPreCachedImage(view, url, sz);

            if (onLoadListener != null)
            {
                onLoadListener.onImageLoaded(view);
            }
        } else
        {
            if (loadingStubResource > 0)
            {
                if (loadingImageScaleType != null)
                {
                    view.setScaleType(loadingImageScaleType);
                }

                view.setImageResource(loadingStubResource);
            }

            ImageLoadRequest request = new ImageLoadRequest(view, url, onLoadListener, sz);
            request.tag = tag;
            request.view.setTag(tag);
            request.cacheOnly = cacheOnly;
            executor.execute(new SingleImageLoadTask(request));
        }
    }

    public void loadImageFromResource(final ImageView im, final int resourceId)
    {
        loadImage(im, DRAWABLE_STUB_SCHEMA + resourceId);
    }

    public void loadImageFromResource(final ImageView im, final int resourceId, final ImageLoadListener listener)
    {
        loadImage(im, DRAWABLE_STUB_SCHEMA + resourceId, listener);
    }

    public void loadImageFromFile(final ImageView im, final File file)
    {
        loadImage(im, Uri.fromFile(file).toString());
    }

    public void loadImageFromFile(final ImageView im, final File file, final ImageLoadListener listener)
    {
        loadImage(im, Uri.fromFile(file).toString(), listener);
    }

    /**
     * Checks if the local cache has the cached instance of the image, specified by url
     *
     * @param url image url to load
     * @return <code>true</code> if the local cache has the image
     */
    public boolean hasCachedImageonDisk(String url)
    {
        return generateLocalCacheFileName(url).exists() && generateLocalCacheFileName(url).length() > 0;
    }

    public boolean hasMemoryCachedImage(String url, int reqSize)
    {
        return cache.get(url + reqSize) != null;
    }

    /**
     * Returns cached image immideately
     *
     * @param url url of the remote image
     * @return image instance as a Bitmap. If image is not chached, a null will be returned
     */
    public Bitmap getCachedImage(String url, int reqSize)
    {
        final String key = url + reqSize;

        try
        {
            Bitmap bitmap = cache.get(key);

            if (bitmap == null)
            {
                bitmap = decodeFile(generateLocalCacheFileName(url), reqSize);
                cache.put(key, bitmap);
            }

            return bitmap;
        } catch (Throwable e)
        {
            Log.e(RTAsyncImageLoader.class.getSimpleName(), "Failed to decode loaded image: " + url + " > " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns cached image immideately
     *
     * @return image instance as a Bitmap. If image is not chached, a null will be returned
     */
    public Bitmap getCachedDrawableImage(int resourceId)
    {
        final String key = DRAWABLE_STUB_SCHEMA + resourceId;

        try
        {
            Bitmap bitmap = cache.get(key);

            if (bitmap == null)
            {
                bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
                cache.put(key, bitmap);
            }

            return bitmap;
        } catch (Throwable e)
        {
            Log.e(RTAsyncImageLoader.class.getSimpleName(), "Failed to decode resource: " + key + " > " + e.getMessage(), e);
            return null;
        }
    }

    public Bitmap getCachedFileImage(String fileUrl)
    {
        try
        {
            Bitmap bitmap = cache.get(fileUrl);

            if (bitmap == null)
            {
                bitmap = BitmapFactory.decodeFile(fileUrl.substring(7));
                cache.put(fileUrl, bitmap);
            }

            return bitmap;
        } catch (Throwable e)
        {
            Log.e(RTAsyncImageLoader.class.getSimpleName(), "Failed to decode resource: " + fileUrl + " > " + e.getMessage(), e);
            return null;
        }
    }

    protected void loadPreCachedImage(ImageView view, String url, int reqSize)
    {
        Bitmap bitmap = getCachedImage(url, reqSize);

        if (finalImageScaleType != null)
        {
            view.setScaleType(finalImageScaleType);
        }

        disposeOldImageState(view);
        view.setImageBitmap(bitmap);
    }

    /**
     * Terminates the image loader background thread. Usually this method is called automatically on application closure,
     * but you may call it manually. Note, that once thread is terminated, it cannot be resumed and you'll need to
     * create a new instance of the ImageLoader then.
     */
    public void terminateLoader()
    {
        executor.shutdown();
    }

    private void loadToView(final ImageLoadRequest request, final Bitmap image)
    {
        if (request.getView() != null && request.tag != null && request.tag.equals(request.view.getTag()))
        {
            handler.post(new Runnable()
            {

                public void run()
                {
                    if (!request.cacheOnly)
                    {
                        disposeOldImageState(request.getView());

                        if (image == null)
                        {
                            if (failoverStubResource != 0)
                            {
                                if (failoverImageScaleType != null)
                                {
                                    request.getView().setScaleType(failoverImageScaleType);
                                }

                                request.getView().setImageResource(failoverStubResource);
                            } else
                            {
                                request.getView().setImageResource(0);
                            }

                            if (request.imageLoadListener != null)
                            {
                                request.imageLoadListener.onImageFailedToLoad(request.getUrl());
                            }
                        } else
                        {
                            if (finalImageScaleType != null)
                            {
                                request.getView().setScaleType(finalImageScaleType);
                            }

                            request.getView().setImageBitmap(image);

                            if (request.imageLoadListener != null)
                            {
                                request.imageLoadListener.onImageLoaded(request.getView());
                            }
                        }
                    } else
                    {
                        if (request.imageLoadListener != null)
                        {
                            request.imageLoadListener.onImageCached(generateLocalCacheFileName(request.url));
                        }
                    }
                }
            });

        }
    }

    private void loadToView(final ImageLoadRequest request, final Uri uri)
    {
        if (request.getView() != null && request.tag != null && request.tag.equals(request.view.getTag()))
        {
            handler.post(new Runnable()
            {

                public void run()
                {
                    disposeOldImageState(request.getView());

                    if (uri == null)
                    {
                        if (failoverStubResource != 0)
                        {
                            if (failoverImageScaleType != null)
                            {
                                request.getView().setScaleType(failoverImageScaleType);
                            }

                            request.getView().setImageResource(failoverStubResource);
                        } else
                        {
                            request.getView().setImageResource(0);
                        }

                        if (request.imageLoadListener != null)
                        {
                            request.imageLoadListener.onImageFailedToLoad(request.getUrl());
                        }
                    } else
                    {
                        if (finalImageScaleType != null)
                        {
                            request.getView().setScaleType(finalImageScaleType);
                        }

                        request.getView().setImageURI(uri);

                        if (request.imageLoadListener != null)
                        {
                            request.imageLoadListener.onImageLoaded(request.getView());
                        }
                    }
                }
            });

        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        terminateLoader();
        super.finalize();
    }

    public File getCacheFolder()
    {
        return cacheDir;
    }

    public File generateLocalCacheFileName(String url)
    {
        try
        {
            final String extension = url.contains(".") ? url.substring(url.lastIndexOf(".")) : url.substring(url.lastIndexOf("/") + 1);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest((url).getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);

            while (md5.length() < 32)
            {
                md5 = "0" + md5;
            }

            return new File(cacheDir, "ILC-" + md5 + extension);
        } catch (NoSuchAlgorithmException e)
        {
            Log.e("MD5", e.getMessage());
            return new File("ILC-" + UUID.randomUUID().toString());
        }
    }

    public void clearCache()
    {
        File[] tempFiles = cacheDir.listFiles();

        for (File file : tempFiles)
        {
            if (file.isFile() && file.getName().startsWith("ILC"))
            {
                file.delete();
            }
        }

        cache.evictAll();
    }

    /**
     * Container to hold a single image loading request. Used internally to manage the queue.
     */
    public class ImageLoadRequest
    {

        private String tag;
        private ImageView view;
        private String url;
        private int reqSize;
        private int retryCount = 0;
        private boolean cacheOnly;

        private ImageLoadListener imageLoadListener;

        public ImageLoadRequest(ImageView view, String url, ImageLoadListener imageLoadListener, int reqSize)
        {
            this.view = view;
            this.url = url;
            this.imageLoadListener = imageLoadListener;
            this.reqSize = reqSize;
        }

        public ImageLoadRequest(String url)
        {
            this.view = null;
            this.url = url;
        }


        public String getUrl()
        {
            return url;
        }

        public ImageView getView()
        {
            return view;
        }

        public int getRetryCount()
        {
            return retryCount;
        }

        public int incrementAndGetRetryCount()
        {
            retryCount++;
            return retryCount;
        }

    }

    public static Bitmap decodeFile(File file, int reqSize)
            throws IllegalArgumentException, FileNotFoundException
    {
        return RTBitmaps.loadBitmapFromFile(file, reqSize);
    }

    private void disposeOldImageState(ImageView view)
    {
    }

    public interface ImageLoadListener
    {

        void onImageCached(File imageFile);

        void onImageLoaded(ImageView view);

        void onImageFailedToLoad(final String imageUrl);
    }

    public class SingleImageLoadTask implements Runnable
    {

        ImageLoadRequest request;

        public SingleImageLoadTask(final ImageLoadRequest request)
        {
            this.request = request;
        }

        public void run()
        {
            Log.d("SingleImageLoadTask", "Loading image " + request.url);

            try
            {
                if (!hasCachedImageonDisk(request.url))
                {
                    if (request.url.startsWith(DRAWABLE_STUB_SCHEMA))
                    {
                        int drawable = Integer.parseInt(request.url.substring(request.url.lastIndexOf("/") + 1));
                        loadToView(request, getCachedDrawableImage(drawable));
                    } else if (request.url.startsWith("file://"))
                    {
                        loadToView(request, getCachedFileImage(request.url));
                    } else
                    {
                        InputStream is = new BufferedHttpEntity(executeHttpRequest(request.url)).getContent();
                        FileOutputStream fos = new FileOutputStream(generateLocalCacheFileName(request.url));

                        final byte buffer[] = new byte[8192];
                        int read = 1;

                        while (read > 0)
                        {
                            read = is.read(buffer);
                            if (read > 0)
                            {
                                fos.write(buffer, 0, read);
                            }
                        }

                        is.close();
                        fos.flush();
                        fos.close();
                        loadToView(request, request.cacheOnly ? null : getCachedImage(request.url, request.reqSize));
                    }
                } else
                {
                    loadToView(request, request.cacheOnly ? null : getCachedImage(request.url, request.reqSize));
                }
            } catch (Throwable err)
            {
                if (request.incrementAndGetRetryCount() < 3)
                {
                    Log.e("SingleImageLoadTask", "Retrying task due to error for " + request.getUrl(), err);
                    executor.execute(new SingleImageLoadTask(request));
                } else
                {
                    Log.e("SingleImageLoadTask", "Error loading task for " + request.getUrl(), err);
                }
            } finally
            {
                Log.d("SingleImageLoadTask", "Loading task finished for " + request.getUrl());
            }
        }

        private HttpEntity executeHttpRequest(String url) throws IOException
        {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            return httpClient.execute(get).getEntity();
        }


    }


}