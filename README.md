# feishuservice
## 项目介绍
`feishuservice`是一款专为飞书设计的自定义应用机器人，旨在让用户随时随地通过飞书进行与OpenAI大模型对话。这款机器人优化了对话体验，方便用户在飞书平台上无缝访问和使用先进的AI对话功能。
### 适用场景
- **团队或企业内部提效**：
  - 如果你管理着一个使用飞书的团队或公司，可以借鉴这个项目来开发一个飞书应用机器人，为团队或公司员工提供便利，提升工作效率。
- **后端开发学习**：
  - 如果你正在学习后端开发相关知识，并希望接触到企业级项目的实际应用，那么这个项目将是一个不错的选择，帮助你更好地理解和实践后端开发原理。
### 功能介绍
- 智能对话：通过机器人与ChatGPT进行自然语言交互，实现智能对话。
- 个性化提示词：支持从系统提供的提示词中配置个人常用提示词，每次请求OpenAI接口时自动携带这些提示词，以增强对话体验。支持每个用户自定义自己的提示词集。
- AI图片生成：根据用户需求，调用OpenAI的模型生成定制化图片，满足多样化的视觉创意需求。
- 天气查询：通过和风天气API获取未来三天天气预报，为用户的日常生活和出行计划提供参考。
### 项目演示
![image](https://github.com/user-attachments/assets/de9ec611-6482-4ce4-b0f7-52aa461fd583)
![image](https://github.com/user-attachments/assets/44f64406-b2d2-4e29-a0fd-3eb4fa6be98d)
![image](https://github.com/user-attachments/assets/d5a69488-44c7-48e1-91c5-814955aff5f5)
![image](https://github.com/user-attachments/assets/ba7ad3cc-489c-49e6-a0e6-8da0ca452135)
![image](https://github.com/user-attachments/assets/eee86576-b71f-4096-88a0-edf85e11b964)
### 技术选型
| 技术                 | 说明                      |
| -------------------- | ------------------------- |
| SpringBoot           | Web应用开发框架            |
| Maven                | Java包管理工具             |
| MyBatis              | ORM框架                   |
| Redis                | 内存数据存储               |
| Redisson             | Java的高性能Redis客户端    |
| Postgresql           | 数据库                    |
| Nginx                | 静态资源服务器             |
| Docker               | 应用容器引擎               |
| Jenkins              | 自动化部署工具             |
| Hikari               | 数据库连接池               |
| Lombok               | Java语言增强库             |
| Hutool               | Java工具类库               |
| okhttp               | Http请求工具               |
| Logback              | 日志框架                   |
| Apollo               | 配置中心                   |
### 架构图
![image](https://github.com/user-attachments/assets/1fff4412-2008-4e10-9e43-27e269497918)
### 开发环境
| 工具          | 版本号 | 
| ------------- | ------ |
| JDK           | 17     |
| Postgresql    | 17.2   |
| Redis         | 7.4.1  |
| Maven         | 3.8.1  |
| Apollo        | 2.3.0  |
### 搭建部署
1. **创建一个飞书团队**
  - 如果还没有飞书团队，请访问飞书官方网站或下载一个飞书APP进行注册，并创建一个团队。
2. **创建自定义应用机器人**
  - 登录飞书后台管理控制台。
  - 导航到“应用管理”部分，选择“创建应用”。
  - 填写基本信息，创建一个新的自定义应用机器人。
3. **为机器人赋予权限**
  - 在飞书开放平台进入开发者后台，为应用添加“机器人”能力。
  - 为应用添加权限，本项目只需开启以下权限：
  <img width="733" alt="7593802c438288942d25728234baa35" src="https://github.com/user-attachments/assets/ed174122-fe2c-4366-9341-f587b99472db" />
  
4. **在OpenAI官网获取apikey（也可以使用其他大模型，但是需要自己修改请求和响应参数）**
  - 登录OpenAI官网，创建一个或多个apikey并给账号充值
5. **获取和风天气的apikey（可选）**
  - 登录和风天气的控制台并获取apikey
6. **搭建各项服务**
  - 搭建Postgresql
  - 搭建Redis
  - 搭建Apollo（如果不使用Apollo可以直接将配置写在配置文件中）
7. **在Apollo中配置项目参数**
  - 根据项目中的xxxConfg类在apollo或配置文件中配置参数
8. **日志存储**
  - 在腾讯云控制台开通日志服务cls，并修改logback.xml中的密钥（也可以使用ELK或阿里云等）
9. **启动项目**
