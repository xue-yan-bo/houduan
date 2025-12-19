package com.jlm.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor {

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	/*设置执行顺序*/
	@Override
	public int getOrder() {
		return 0;
	}

	private ChatClientRequest before(ChatClientRequest request) {
		log.info("AI Request: {}", request.prompt());
		return request;
	}

	private void observeAfter(ChatClientResponse chatClientResponse) {
		String text = chatClientResponse.chatResponse().getResult().getOutput().getText();
		log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());

		/*String dealText = text.replaceAll("/", "");
		chatClientResponse.context().put("dealedText",dealText);*/
		/*if(text.contains("\\end")){
			text = text.replace("\\end", "");
		}
		if(text.contains("\\n")){
			text = text.replace("\\n", "");
		}*/

	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
		chatClientRequest = before(chatClientRequest);
		ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
		observeAfter(chatClientResponse);
		return chatClientResponse;
	}







}
