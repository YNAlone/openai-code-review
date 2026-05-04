package plus.gaga.middleware.sdk;

import com.alibaba.fastjson2.JSON;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;
import plus.gaga.middleware.sdk.types.utils.WXAccessTokenUtils;
import plus.gaga.middleware.sdk.domain.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class OpenAiCodeReview {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

//    微信配置
    /** 微信公众号 AppID（开发者 ID） */
    private static final String APPID = "wx36df85cedb03f21e";
    /** 微信公众号 AppSecret（开发者密码） */
    private static final String SECRET = "50c424c65abaab1f605b34864a6fda1d";
    /** 授权类型，固定为 client_credential */
    private static final String GRANT_TYPE = "client_credential";
    /** 微信获取 access_token 的 API 地址模板 */
    private static final String URL_TEMPLATE = "https://api.weixin.qq.com/cgi-bin/token?grant_type=%s&appid=%s&secret=%s";

//    ChatGLM配置
    private String chatglm_apiHost = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private String chatglm_apiSecret = "";
//    Github配置
    private String github_review_log_uri;
    private String github_token;
//    工程配置 自动获取
    private String github_project;
    private String github_branch;
    private String github_author;

    public static void main(String[] args) throws Exception {
        System.out.println("openai 代码评审 ，测试执行");
        GitCommand gitCommand = new GitCommand(
                    getEnv( "GITHUB_REVIEW_LOG_URI"),
                    getEnv("GITHUB_TOKEN"),
                    getEnv("COMMIT_PROJECT"),
                    getEnv("COMMIT_BRANCH"),
                    getEnv("COMMIT_AUTHOR"),
                    getEnv("COMMIT_MESSAGE")
        );
        WeiXin weiXin = new WeiXin(
                getEnv("WEIXIN_APPID"),
                getEnv(  "WEIXIN_SECRET"),
                getEnv( "WEIXIN_TOUSER"),
                getEnv( "WEIXIN_TEMPLATE_ID")
        );

        IOpenAI chatGLM = new ChatGLM(getEnv("CHATGLM_APIHOST"), getEnv("CHATGLM_APIKEYSECRET"));

        OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, chatGLM, weiXin);
        openAiCodeReviewService.exec();
        logger.info("openai-code-review done！！");
    }

    private static String getEnv(String key) throws Exception {
        String value = System.getenv(key);
        if(value == null || value.isEmpty()) {
            throw new RuntimeException("value is null");
        }
        return value;
    }
}