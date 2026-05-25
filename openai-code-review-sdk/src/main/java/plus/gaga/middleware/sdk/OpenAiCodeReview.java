package plus.gaga.middleware.sdk;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.agent.AgenticReviewWorkflow;
import plus.gaga.middleware.sdk.agent.CodeReviewAgent;
import plus.gaga.middleware.sdk.agent.CodeReviewWorkflow;
import plus.gaga.middleware.sdk.agent.ReviewAssistant;
import plus.gaga.middleware.sdk.agent.ReviewContext;
import plus.gaga.middleware.sdk.agent.ReviewRequest;
import plus.gaga.middleware.sdk.agent.agents.ArchiveAgent;
import plus.gaga.middleware.sdk.agent.agents.BugRiskReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.ChangedFileAgent;
import plus.gaga.middleware.sdk.agent.agents.DiffPreprocessAgent;
import plus.gaga.middleware.sdk.agent.agents.GitDiffAgent;
import plus.gaga.middleware.sdk.agent.agents.GitHubPrCommentAgent;
import plus.gaga.middleware.sdk.agent.agents.MaintainabilityReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.PerformanceReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.ReportAggregatorAgent;
import plus.gaga.middleware.sdk.agent.agents.SecurityReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.WechatNotifyAgent;
import plus.gaga.middleware.sdk.agent.context.DiffPreprocessor;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.infrastructure.git.ChangedFileParser;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.github.GitHubPrCommentClient;
import plus.gaga.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import plus.gaga.middleware.sdk.infrastructure.openai.langchain4j.LangChain4jChatModelAdapter;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;

import com.alibaba.fastjson2.JSON;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class OpenAiCodeReview {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

    public static void main(String[] args) throws Exception {
        logger.info("openai-code-review multi-agent workflow start");

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
        ChatGLM chatGLM = new ChatGLM(getEnv("CHATGLM_APIHOST"), getEnv("CHATGLM_APIKEYSECRET"));
        ChatModel chatModel = new LangChain4jChatModelAdapter(chatGLM, Model.GLM_4_FLASH.getCode());
        ReviewAssistant reviewAssistant = AiServices.create(ReviewAssistant.class, chatModel);

        ReviewRequest request = new ReviewRequest(
                getEnv("COMMIT_PROJECT"),
                getEnv("COMMIT_BRANCH"),
                getEnvOrDefault("COMMIT_ID", ""),
                getEnv("COMMIT_AUTHOR"),
                getEnv("COMMIT_MESSAGE"),
                getEnvOrDefault("GITHUB_EVENT_NAME", "push"),
                resolvePullRequestNumber(),
                getEnvOrDefault("COMMIT_RANGE", "")
        );
//          感知智能体包括git diff Agent  和 ChangedFileAgent 拉取Git diff 和 获取修改的文件
        List<CodeReviewAgent> perceptionAgents = Arrays.asList(
                new GitDiffAgent(gitCommand),
                new ChangedFileAgent(new ChangedFileParser()),
                new DiffPreprocessAgent(new DiffPreprocessor())
        );
//        评审智能体 包括安全性审查、风险漏洞审查、性能、可维护性各个方法的智能体
        List<CodeReviewAgent> reviewAgents = Arrays.asList(
                new SecurityReviewAgent(reviewAssistant),
                new BugRiskReviewAgent(reviewAssistant),
                new PerformanceReviewAgent(reviewAssistant),
                new MaintainabilityReviewAgent(reviewAssistant)
        );
//              报告智能体 整理报告、提交报告、推动报告
        List<CodeReviewAgent> postAgents = Arrays.asList(
                new ReportAggregatorAgent(),
                new ArchiveAgent(gitCommand),
                new GitHubPrCommentAgent(new GitHubPrCommentClient(getEnv("GITHUB_TOKEN"))),
                new WechatNotifyAgent(weiXin)
        );

        CodeReviewWorkflow workflow = new AgenticReviewWorkflow(perceptionAgents, reviewAgents, postAgents);
        ReviewContext context = workflow.run(request);
        logger.info("openai-code-review done, archiveUrl={}", context.getArchiveUrl());
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("environment variable is empty: " + key);
        }
        return value;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static String resolvePullRequestNumber() {
        String explicit = getEnvOrDefault("GITHUB_PR_NUMBER", "");
        if (!explicit.isEmpty()) {
            return explicit;
        }
        String eventPath = getEnvOrDefault("GITHUB_EVENT_PATH", "");
        if (eventPath.isEmpty()) {
            return "";
        }
        try {
            String eventJson = Files.readString(Paths.get(eventPath));
            Object number = JSON.parseObject(eventJson).get("number");
            return number == null ? "" : String.valueOf(number);
        } catch (Exception e) {
            logger.warn("Failed to resolve pull request number from event file: {}", eventPath, e);
            return "";
        }
    }
}
