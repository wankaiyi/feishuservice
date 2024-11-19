package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 飞书发送消息DTO，参考<a href="https://open.feishu.cn/document/server-docs/im-v1/message/create">文档</a>
 *
 * @author wky
 * @date 2024/11/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeishuSendUserMsgDTO {

    /**
     * 接收者的id，根据receive_type决定是open_id，user_id，email，union_id，或者chat_id
     */
    private String receiveId;

    /**
     * 消息类型
     * <p>
     * 枚举：{@link MessageType}
     */
    private String msgType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * uuid，可选，每次调用前更换
     */
    private String uuid;

    @Getter
    @AllArgsConstructor
    public enum MessageType {
        TEXT("文本", "text"),
        POST("富文本", "post"),
        IMAGE("图片", "image"),
        FILE("文件", "file"),
        AUDIO("语音", "audio"),
        MEDIA("视频", "media"),
        STICKER("表情包", "sticker"),
        INTERACTIVE("卡片", "interactive"),
        SHARE_CHAT("分享群名片", "share_chat"),
        SHARE_USER("分享个人名片", "share_user"),
        SYSTEM("系统消息", "system");

        private final String description;
        private final String value;
    }

}
