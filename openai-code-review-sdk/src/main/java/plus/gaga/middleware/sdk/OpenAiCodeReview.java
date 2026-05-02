package plus.gaga.middleware.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;

import java.util.HashMap;
import java.util.Map;

public class OpenAiCodeReview {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

    // 默认配置（环境变量未设置时使用）
    private static final Map<String, String> DEFAULT_CONFIG = new HashMap<String, String>() {{
        // Github 配置
        put("GITHUB_REVIEW_LOG_URI", "https://github.com/YNAlone/openai-code-review-log");
        put("GITHUB_TOKEN", "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");
        // 微信配置
        put("WEIXIN_APPID", "wx5a228ff69e28a91f");
        put("WEIXIN_SECRET", "0bea03aa1310bac050aae79dd8703928");
        put("WEIXIN_TOUSER", "or0Ab6ivwmypESVp_bYuk92T6SvU");
        put("WEIXIN_TEMPLATE_ID", "l2HTkntHB71R4NQTW77UkcqvSOIFqE_bss1DAVQSybc");
        // ChatGLM 配置
        put("CHATGLM_APIHOST", "https://open.bigmodel.cn/api/paas/v4/chat/completions");
        put("CHATGLM_APIKEYSECRET", "50838fdd6ee3414594e47ece0cd5be30.fdn4C393LncqLKa6");
        // 工程配置（CI 环境自动注入，本地运行需要手动设置或在此处配置默认值）
        put("COMMIT_PROJECT", "");
        put("COMMIT_BRANCH", "");
        put("COMMIT_AUTHOR", "");
        put("COMMIT_MESSAGE", "");
    }};

    public static void main(String[] args) throws Exception {
        GitCommand gitCommand = new GitCommand(
                getEnv("GITHUB_REVIEW_LOG_URI"),
                getEnv("GITHUB_TOKEN"),
                getEnv("COMMIT_PROJECT"),
                getEnv("COMMIT_BRANCH"),
                getEnv("COMMIT_AUTHOR"),
                getEnv("COMMIT_MESSAGE")
        );

        WeiXin weiXin = new WeiXin(
                getEnv("WEIXIN_APPID"),
                getEnv("WEIXIN_SECRET"),
                getEnv("WEIXIN_TOUSER"),
                getEnv("WEIXIN_TEMPLATE_ID")
        );

        IOpenAI openAI = new ChatGLM(getEnv("CHATGLM_APIHOST"), getEnv("CHATGLM_APIKEYSECRET"));

        OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, openAI, weiXin);
        openAiCodeReviewService.exec();

        logger.info("openai-code-review done!");
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (null == value || value.isEmpty()) {
            String defaultValue = DEFAULT_CONFIG.get(key);
            if (defaultValue != null && !defaultValue.isEmpty()) {
                logger.warn("环境变量 '{}' 未设置，使用项目默认配置", key);
                return defaultValue;
            }
            throw new RuntimeException("Environment variable '" + key + "' is not set and no default configured");
        }
        return value;
    }

}
