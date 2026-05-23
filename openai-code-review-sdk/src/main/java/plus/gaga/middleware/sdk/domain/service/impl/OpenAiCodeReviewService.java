package plus.gaga.middleware.sdk.domain.service.impl;

import plus.gaga.middleware.sdk.agent.AgenticReviewWorkflow;
import plus.gaga.middleware.sdk.agent.CodeReviewAgent;
import plus.gaga.middleware.sdk.agent.ReviewAssistant;
import plus.gaga.middleware.sdk.agent.ReviewRequest;
import plus.gaga.middleware.sdk.agent.agents.ArchiveAgent;
import plus.gaga.middleware.sdk.agent.agents.BugRiskReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.ChangedFileAgent;
import plus.gaga.middleware.sdk.agent.agents.GitDiffAgent;
import plus.gaga.middleware.sdk.agent.agents.MaintainabilityReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.PerformanceReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.ReportAggregatorAgent;
import plus.gaga.middleware.sdk.agent.agents.SecurityReviewAgent;
import plus.gaga.middleware.sdk.agent.agents.WechatNotifyAgent;
import plus.gaga.middleware.sdk.domain.service.AbstractOpenAiCodeReviewService;
import plus.gaga.middleware.sdk.infrastructure.git.ChangedFileParser;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.IOException;

public class OpenAiCodeReviewService extends AbstractOpenAiCodeReviewService {

    private final ReviewAssistant reviewAssistant;

    public OpenAiCodeReviewService(GitCommand gitCommand, IOpenAI openAI, WeiXin weiXin) {
        super(gitCommand, openAI, weiXin);
        this.reviewAssistant = userMessage -> {
            try {
                ChatCompletionRequestDTO requestDTO = new ChatCompletionRequestDTO();
                requestDTO.setMessages(Collections.singletonList(new ChatCompletionRequestDTO.Prompt("user", userMessage)));
                ChatCompletionSyncResponseDTO response = openAI.completions(requestDTO);
                return response.getChoices().get(0).getMessage().getContent();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public void exec() {
        List<CodeReviewAgent> perceptionAgents = Arrays.asList(
                new GitDiffAgent(gitCommand),
                new ChangedFileAgent(new ChangedFileParser())
        );
        List<CodeReviewAgent> reviewAgents = Arrays.asList(
                new SecurityReviewAgent(reviewAssistant),
                new BugRiskReviewAgent(reviewAssistant),
                new PerformanceReviewAgent(reviewAssistant),
                new MaintainabilityReviewAgent(reviewAssistant)
        );
        List<CodeReviewAgent> postAgents = Arrays.asList(
                new ReportAggregatorAgent(),
                new ArchiveAgent(gitCommand),
                new WechatNotifyAgent(weiXin)
        );

        ReviewRequest request = new ReviewRequest(
                gitCommand.getProject(),
                gitCommand.getBranch(),
                "",
                gitCommand.getAuthor(),
                gitCommand.getMessage(),
                "push",
                "",
                ""
        );
        try {
            new AgenticReviewWorkflow(perceptionAgents, reviewAgents, postAgents).run(request);
        } catch (Exception e) {
            throw new RuntimeException("openai-code-review exec failed", e);
        }
    }

    @Override
    protected String getDiffCode() throws IOException, InterruptedException {
        return gitCommand.diff();
    }

    @Override
    protected String codeReview(String diffCode) {
        return reviewAssistant.chat(diffCode);
    }

    @Override
    protected String recordCodeReview(String recommend) throws Exception {
        return gitCommand.commitAndPush(recommend);
    }

    @Override
    protected void pushMessage(String logUrl) throws Exception {
        // The new workflow owns notification. This method is kept for compatibility.
    }
}
