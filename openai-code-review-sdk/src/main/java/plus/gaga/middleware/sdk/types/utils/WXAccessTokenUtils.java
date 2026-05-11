package plus.gaga.middleware.sdk.types.utils;

import com.alibaba.fastjson2.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众平台 Access Token 获取工具类
 * <p>
 * 通过微信公众号的 appId 和 appSecret 调用微信 API 获取全局唯一接口调用凭据 access_token。
 * access_token 有效期为 2 小时（7200秒），通过 Guava Cache 本地缓存 110 分钟，避免频繁请求。
 *
 * @see <a href="https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html">微信官方文档</a>
 */
public class WXAccessTokenUtils {

    private static final Logger logger = LoggerFactory.getLogger(WXAccessTokenUtils.class);

    /** 微信公众号 AppID（开发者 ID） */
    private static final String APPID = "wx36df85cedb03f21e";
    /** 微信公众号 AppSecret（开发者密码） */
    private static final String SECRET = "50c424c65abaab1f605b34864a6fda1d";
    /** 授权类型，固定为 client_credential */
    private static final String GRANT_TYPE = "client_credential";
    /** 微信获取 access_token 的 API 地址模板 */
    private static final String URL_TEMPLATE = "https://api.weixin.qq.com/cgi-bin/token?grant_type=%s&appid=%s&secret=%s";

    /** access_token 缓存，过期时间 110 分钟（微信默认 120 分钟，提前 10 分钟刷新） */
    private static final Cache<String, String> tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(110, TimeUnit.MINUTES)
            .build();

    /**
     * 使用默认的 APPID 和 SECRET 获取 access_token（优先从缓存获取）
     *
     * @return access_token 字符串，失败返回 null
     */
    public static String getAccessToken() {
        return getAccessToken(APPID, SECRET);
    }

    /**
     * 调用微信 API 获取 access_token（优先从缓存获取）
     *
     * @param appid  微信公众号 AppID
     * @param secret 微信公众号 AppSecret
     * @return access_token 字符串，失败返回 null
     */
    public static String getAccessToken(String appid, String secret) {
        String cacheKey = appid + ":" + secret;
        String cachedToken = tokenCache.getIfPresent(cacheKey);
        if (cachedToken != null) {
            return cachedToken;
        }

        try {
            String urlString = String.format(URL_TEMPLATE, GRANT_TYPE, appid, secret);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                logger.info("WX access_token response code: {}", responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }

                    logger.info("WX access_token response: {}", response);

                    Token token = JSON.parseObject(response.toString(), Token.class);
                    String accessToken = token.getAccess_token();
                    if (accessToken != null) {
                        tokenCache.put(cacheKey, accessToken);
                    }
                    return accessToken;
                } else {
                    logger.error("WX access_token request failed, response code: {}", responseCode);
                    return null;
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.error("WX access_token request error", e);
            return null;
        }
    }

    /**
     * 微信 access_token 接口响应体
     * <p>
     * 对应微信 API 返回的 JSON：{"access_token":"...", "expires_in":7200}
     */
    public static class Token {
        private String access_token;
        private Integer expires_in;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public Integer getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(Integer expires_in) {
            this.expires_in = expires_in;
        }
    }


}
