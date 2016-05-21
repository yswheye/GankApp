package com.android.ted.gank.network;


import com.android.ted.gank.GankApplication;
import com.android.ted.gank.config.Constants;
import com.android.ted.gank.model.DayGoodsResult;
import com.android.ted.gank.model.GoodsResult;
import com.android.ted.gank.utils.NetworkUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

public class GankCloudApi {
    public static GankCloudApi instance;

    public static GankCloudApi getInstance() {
        if (null == instance) {
            synchronized (GankCloudApi.class) {
                if (null == instance) {
                    instance = new GankCloudApi();
                }
            }
        }
        return instance;
    }

    /**每次加载条目*/
    public static final int LOAD_LIMIT = 20;
    /**加载起始页面*/
    public static final int LOAD_START = 1;

    public static final String ENDPOINT = Constants.GANK_SERVER_IP;

    private final GankCloudService mWebService;

    public static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            if (NetworkUtils.isNetworkConnected(GankApplication.getContext())) {
                int maxAge = 60 * 60; // read from cache for 1 minute
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .removeHeader("Pragma")// 清除头信息，因为服务器如果不支持，会返回一些干扰信息，不清除下面无法生效
                        .build();
            } else {
                int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .removeHeader("Pragma")
                        .build();
            }
        }
    };

    public GankCloudApi() {
        Cache cache;
        OkHttpClient.Builder okHttpClient = null;
        try {
            File cacheDir = new File(GankApplication.getContext().getCacheDir().getPath(), "gank_cache.json");
            cache = new Cache(cacheDir, 20 * 1024 * 1024);// 20M
            okHttpClient = new OkHttpClient.Builder();
            okHttpClient
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();

                            // Customize the request
                            Request request = original.newBuilder()
//                                    .header("Accept", "application/json")
//                                    .header("Authorization", "auth-token")
                                    .addHeader("Cache-Control", "public, max-age=" + 60 * 60 * 4)
                                    .addHeader("Content-Type", "application/json")
                                    .method(original.method(), original.body())
                                    .build();

                            Response response = chain.proceed(request);

                            // Customize or return the response
                            return response;
                        }
                    })
                    .cache(cache)
                    .networkInterceptors().add(REWRITE_CACHE_CONTROL_INTERCEPTOR)
            ;
        } catch (Exception e) {

        }

        /**
         * 1.0
         */
//        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setEndpoint(ENDPOINT)
//                .setClient(new OkClient(okHttpClient))
//                .setConverter(new GsonConverter(gson))
//                .setRequestInterceptor(mRequestInterceptor)
//                .build();
//        mWebService = restAdapter.create(GankCloudService.class);
        /**
         * 2.0
         */
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .client(okHttpClient.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        mWebService = retrofit.create(GankCloudService.class);
    }

    /**
     * 1.0
     */
//    private RequestInterceptor mRequestInterceptor = new RequestInterceptor() {
//        @Override
//        public void intercept(RequestFacade request) {
//            request.addHeader("Cache-Control", "public, max-age=" + 60 * 60 * 4);
//            request.addHeader("Content-Type", "application/json");
//        }
//    };

    public interface GankCloudService {

        @GET("data/Android/{limit}/{page}")
        Observable<GoodsResult> getAndroidGoods(
                @Path("limit") int limit,
                @Path("page") int page
        );

        @GET("data/iOS/{limit}/{page}")
        Observable<GoodsResult> getIosGoods(
                @Path("limit") int limit,
                @Path("page") int page
        );

        @GET("data/all/{limit}/{page}")
        Observable<GoodsResult> getAllGoods(
                @Path("limit") int limit,
                @Path("page") int page
        );

        @GET("data/福利/{limit}/{page}")
        Observable<GoodsResult> getBenefitsGoods(
                @Path("limit") int limit,
                @Path("page") int page
        );

        @GET("day/{year}/{month}/{day}")
        Observable<DayGoodsResult> getGoodsByDay(
                @Path("year") int year,
                @Path("month") int month,
                @Path("day") int day
        );
    }

    public Observable<GoodsResult> getCommonGoods(String type,int limit, int page) {
        if("Android".equalsIgnoreCase(type)){
            return mWebService.getAndroidGoods(limit, page);
        }
        if("IOS".equalsIgnoreCase(type)){
            return mWebService.getIosGoods(limit, page);
        }
        return mWebService.getAndroidGoods(limit, page);
    }

    public Observable<GoodsResult> getAndroidGoods(int limit, int page) {
        return mWebService.getAndroidGoods(limit, page);
    }

    public Observable<GoodsResult> getIosGoods(int limit, int page) {
        return mWebService.getIosGoods(limit, page);
    }

    public Observable<GoodsResult> getAllGoods(int limit, int page) {
        return mWebService.getAllGoods(limit, page);
    }

    public Observable<GoodsResult> getBenefitsGoods(int limit, int page) {
        return mWebService.getBenefitsGoods(limit, page);
    }

    public Observable<DayGoodsResult> getGoodsByDay(int year,int month,int day) {
        return mWebService.getGoodsByDay(year, month,day);
    }

}
